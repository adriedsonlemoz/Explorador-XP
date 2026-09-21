package com.exploradorxp.app

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlin.math.max
import kotlin.math.min

/**
 * Motor de arquivo grande usado pelo editor de texto/código.
 *
 * O arquivo completo permanece no disco. Apenas uma janela limitada é decodificada para o
 * EditText, evitando transformar arquivos de vários MB em uma String gigante e em centenas de
 * spans/históricos na memória. As gravações substituem somente a faixa carregada usando um
 * arquivo temporário e cópia em streaming do prefixo/sufixo.
 */
internal object LargeTextFileEngine {
    const val WINDOW_TARGET_BYTES: Long = 384L * 1024L
    private const val LINE_ALIGNMENT_SCAN_BYTES: Long = 32L * 1024L
    private const val COPY_BUFFER_BYTES = 64 * 1024

    data class Window(
        val text: String,
        val startByte: Long,
        val endByte: Long,
        val fileSizeBytes: Long,
        val firstLine: Int,
        val firstColumn: Int,
    ) {
        val hasPrevious: Boolean get() = startByte > 0L
        val hasNext: Boolean get() = endByte < fileSizeBytes
        val lineBreaks: Int get() = text.count { it == '\n' }
    }

    data class SearchMatch(
        val line: Int,
        val column: Int,
        val ordinal: Int,
    )

    data class SearchResult(
        val match: SearchMatch?,
        val total: Int,
        val totalLines: Int,
    )

    fun loadInitial(
        file: File,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        cancelled: () -> Boolean = { false },
    ): Window = loadForward(
        file = file,
        requestedStart = 0L,
        firstLine = 1,
        firstColumn = 1,
        charset = charset,
        bom = bom,
        onProgress = onProgress,
        cancelled = cancelled,
    )

    fun loadNext(
        file: File,
        current: Window,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        cancelled: () -> Boolean = { false },
    ): Window? {
        if (!current.hasNext) return null
        val lastBreak = current.text.lastIndexOf('\n')
        val nextColumn = if (lastBreak >= 0) {
            current.text.length - lastBreak
        } else {
            current.firstColumn + current.text.length
        }
        return loadForward(
            file = file,
            requestedStart = current.endByte,
            firstLine = current.firstLine + current.lineBreaks,
            firstColumn = nextColumn.coerceAtLeast(1),
            charset = charset,
            bom = bom,
            onProgress = onProgress,
            cancelled = cancelled,
        )
    }

    fun loadPrevious(
        file: File,
        current: Window,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        cancelled: () -> Boolean = { false },
    ): Window? {
        if (!current.hasPrevious) return null
        checkCancelled(cancelled)
        val fileSize = file.length()
        val contentFloor = if (bom.isNotEmpty()) bom.size.toLong() else 0L
        val approximate = max(contentFloor, current.startByte - WINDOW_TARGET_BYTES)
        val aligned = if (approximate <= contentFloor) {
            0L
        } else {
            findPreviousLineBoundary(file, approximate, charset, bom, LINE_ALIGNMENT_SCAN_BYTES, cancelled)
                ?: alignCharacterBoundary(file, approximate, charset, bom)
        }
        val safeStart = aligned.coerceIn(0L, current.startByte)
        val text = readRange(
            file = file,
            start = safeStart,
            end = current.startByte,
            charset = charset,
            bom = bom,
            onProgress = onProgress,
            cancelled = cancelled,
        )
        val firstLine = (current.firstLine - text.count { it == '\n' }).coerceAtLeast(1)
        val firstColumn = columnAtByteOffset(file, safeStart, charset, bom, cancelled)
        return Window(
            text = text,
            startByte = safeStart,
            endByte = current.startByte,
            fileSizeBytes = fileSize,
            firstLine = firstLine,
            firstColumn = firstColumn,
        )
    }

    fun loadAtLine(
        file: File,
        requestedLine: Int,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        cancelled: () -> Boolean = { false },
    ): Window? {
        if (requestedLine <= 1) return loadInitial(file, charset, bom, onProgress, cancelled)
        val offset = findLineByteOffset(file, requestedLine, charset, bom, onProgress, cancelled) ?: return null
        return loadForward(
            file = file,
            requestedStart = offset,
            firstLine = requestedLine,
            firstColumn = 1,
            charset = charset,
            bom = bom,
            onProgress = onProgress,
            cancelled = cancelled,
        )
    }

    fun search(
        file: File,
        query: String,
        currentLine: Int,
        currentColumn: Int,
        forward: Boolean,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        cancelled: () -> Boolean = { false },
    ): SearchResult {
        if (query.isEmpty()) return SearchResult(null, 0, 1)
        val totalBytes = file.length().coerceAtLeast(1L)
        FileInputStream(file).use { raw ->
            val counting = CountingInputStream(BufferedInputStream(raw, COPY_BUFFER_BYTES))
            skipFully(counting, bom.size.toLong(), cancelled)
            InputStreamReader(counting, charset).use { reader ->
                val prefix = buildPrefixTable(query)
                val startLines = IntArray(query.length.coerceAtLeast(1))
                val startColumns = IntArray(query.length.coerceAtLeast(1))
                var ringWrite = 0
                var matched = 0
                var line = 1
                var column = 1
                var total = 0
                var first: SearchMatch? = null
                var last: SearchMatch? = null
                var directional: SearchMatch? = null
                val buffer = CharArray(32 * 1024)
                var lastProgress = -1

                while (true) {
                    checkCancelled(cancelled)
                    val read = reader.read(buffer)
                    if (read <= 0) break
                    for (i in 0 until read) {
                        val c = buffer[i]
                        if (query.isNotEmpty()) {
                            startLines[ringWrite] = line
                            startColumns[ringWrite] = column
                            ringWrite = (ringWrite + 1) % query.length

                            while (matched > 0 && !sameCharIgnoreCase(c, query[matched])) {
                                matched = prefix[matched - 1]
                            }
                            if (sameCharIgnoreCase(c, query[matched])) matched++
                            if (matched == query.length) {
                                val slot = ringWrite % query.length
                                val matchLine = startLines[slot]
                                val matchColumn = startColumns[slot]
                                total++
                                val match = SearchMatch(matchLine, matchColumn, total)
                                if (first == null) first = match
                                last = match
                                val afterCurrent = matchLine > currentLine || (matchLine == currentLine && matchColumn > currentColumn)
                                val beforeCurrent = matchLine < currentLine || (matchLine == currentLine && matchColumn < currentColumn)
                                if (directional == null && ((forward && afterCurrent) || (!forward && beforeCurrent))) {
                                    directional = match
                                } else if (!forward && beforeCurrent) {
                                    // Para busca para trás, queremos a última ocorrência anterior ao cursor.
                                    directional = match
                                }
                                // O editor normal usa ocorrências não sobrepostas; mantém a mesma regra.
                                matched = 0
                            }
                        }

                        if (c == '\n') {
                            line++
                            column = 1
                        } else {
                            column++
                        }
                    }
                    val pct = ((counting.count * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    if (pct != lastProgress) {
                        lastProgress = pct
                        onProgress(counting.count.coerceAtMost(totalBytes), totalBytes)
                    }
                }

                val selected = directional ?: if (forward) first else last
                val fixed = selected?.let { chosen ->
                    if (chosen.ordinal > 0) chosen else chosen.copy(ordinal = 1)
                }
                // Se a busca para trás escolheu o último candidato durante a varredura, seu ordinal já é final.
                // Para a volta ao último resultado, last também contém o ordinal correto.
                return SearchResult(fixed, total, line.coerceAtLeast(1))
            }
        }
    }

    /**
     * Regrava apenas a janela editada no próprio arquivo. Prefixo e sufixo são copiados em streaming.
     */
    fun patchInPlace(
        file: File,
        window: Window,
        editedText: String,
        charset: Charset,
        bom: ByteArray,
        preferredLineEnding: String,
        cancelled: () -> Boolean = { false },
    ): Window {
        val parent = file.parentFile ?: error("Pasta do arquivo indisponível.")
        val temp = File(parent, ".${file.name}.large-${UUID.randomUUID()}.tmp")
        val normalized = normalizeLineEndings(editedText, preferredLineEnding)
        val body = normalized.toByteArray(charset)
        check(String(body, charset) == normalized) { "A codificação não preservou o trecho editado." }
        val insertedLength = body.size.toLong() + if (window.startByte == 0L) bom.size.toLong() else 0L
        val expectedSize = window.fileSizeBytes - (window.endByte - window.startByte) + insertedLength
        val readable = file.canRead()
        val writable = file.canWrite()
        val executable = file.canExecute()
        try {
            buildPatchedFile(file, temp, window, body, bom, cancelled)
            check(temp.length() == expectedSize) { "A validação do arquivo temporário falhou; o original foi preservado." }
            replaceAtomically(temp, file)
            if (readable) file.setReadable(true, true)
            if (writable) file.setWritable(true, true)
            if (executable) file.setExecutable(true, true)
        } finally {
            if (temp.exists()) temp.delete()
        }
        val newEnd = window.startByte + insertedLength
        return window.copy(
            text = editedText,
            endByte = newEnd,
            fileSizeBytes = expectedSize,
        )
    }

    /** Cria uma cópia completa aplicando a edição da janela, sem carregar o arquivo inteiro. */
    fun writePatchedCopy(
        source: File,
        target: File,
        window: Window,
        editedText: String,
        charset: Charset,
        bom: ByteArray,
        preferredLineEnding: String,
        cancelled: () -> Boolean = { false },
    ) {
        val parent = target.parentFile ?: error("Pasta de destino indisponível.")
        require(parent.exists() && parent.isDirectory) { "A pasta de destino não existe." }
        val temp = File(parent, ".${target.name}.large-copy-${UUID.randomUUID()}.tmp")
        val normalized = normalizeLineEndings(editedText, preferredLineEnding)
        val body = normalized.toByteArray(charset)
        check(String(body, charset) == normalized) { "A codificação não preservou o trecho editado." }
        val insertedLength = body.size.toLong() + if (window.startByte == 0L) bom.size.toLong() else 0L
        val expectedSize = window.fileSizeBytes - (window.endByte - window.startByte) + insertedLength
        try {
            buildPatchedFile(source, temp, window, body, bom, cancelled)
            check(temp.length() == expectedSize) { "A cópia temporária não passou na validação." }
            replaceAtomically(temp, target)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun loadForward(
        file: File,
        requestedStart: Long,
        firstLine: Int,
        firstColumn: Int,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit,
        cancelled: () -> Boolean,
    ): Window {
        checkCancelled(cancelled)
        val fileSize = file.length()
        if (fileSize <= 0L) return Window("", 0L, 0L, 0L, 1, 1)
        val start = if (requestedStart <= bom.size.toLong()) {
            0L
        } else {
            alignCharacterBoundary(file, requestedStart.coerceAtMost(fileSize), charset, bom)
        }
        val target = min(fileSize, start + WINDOW_TARGET_BYTES)
        val lineAlignedEnd = if (target < fileSize) {
            findNextLineBoundary(file, target, charset, bom, LINE_ALIGNMENT_SCAN_BYTES, cancelled)
        } else null
        var end = lineAlignedEnd ?: alignCharacterBoundary(file, target, charset, bom)
        if (end <= start && start < fileSize) {
            end = min(fileSize, alignCharacterBoundary(file, start + WINDOW_TARGET_BYTES, charset, bom))
            if (end <= start) end = fileSize
        }
        val text = readRange(file, start, end, charset, bom, onProgress, cancelled)
        return Window(
            text = text,
            startByte = start,
            endByte = end,
            fileSizeBytes = fileSize,
            firstLine = firstLine.coerceAtLeast(1),
            firstColumn = firstColumn.coerceAtLeast(1),
        )
    }

    private fun readRange(
        file: File,
        start: Long,
        end: Long,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit,
        cancelled: () -> Boolean,
    ): String {
        val length = (end - start).coerceAtLeast(0L)
        require(length <= Int.MAX_VALUE) { "Trecho grande demais para o editor." }
        val bytes = ByteArray(length.toInt())
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            var total = 0
            while (total < bytes.size) {
                checkCancelled(cancelled)
                val read = raf.read(bytes, total, bytes.size - total)
                if (read <= 0) break
                total += read
                onProgress(total.toLong(), length.coerceAtLeast(1L))
            }
            val actual = if (total == bytes.size) bytes else bytes.copyOf(total)
            val offset = if (start == 0L && bom.isNotEmpty() && actual.startsWithBytes(bom)) bom.size else 0
            val decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
            return decoder.decode(ByteBuffer.wrap(actual, offset, actual.size - offset)).toString()
        }
    }

    private fun findNextLineBoundary(
        file: File,
        from: Long,
        charset: Charset,
        bom: ByteArray,
        maxScan: Long,
        cancelled: () -> Boolean,
    ): Long? {
        if (isUtf16(charset)) return null
        val contentFloor = bom.size.toLong()
        val safeFrom = from.coerceAtLeast(contentFloor)
        val limit = min(file.length(), safeFrom + maxScan)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(safeFrom)
            var pos = safeFrom
            val buffer = ByteArray(8 * 1024)
            while (pos < limit) {
                checkCancelled(cancelled)
                val want = min(buffer.size.toLong(), limit - pos).toInt()
                val read = raf.read(buffer, 0, want)
                if (read <= 0) break
                for (i in 0 until read) {
                    if (buffer[i] == 0x0A.toByte()) return pos + i + 1L
                }
                pos += read
            }
        }
        return null
    }

    private fun findPreviousLineBoundary(
        file: File,
        from: Long,
        charset: Charset,
        bom: ByteArray,
        maxScan: Long,
        cancelled: () -> Boolean,
    ): Long? {
        if (isUtf16(charset)) return null
        val floor = if (bom.isNotEmpty()) bom.size.toLong() else 0L
        val lower = max(floor, from - maxScan)
        RandomAccessFile(file, "r").use { raf ->
            var end = from.coerceAtMost(file.length())
            val buffer = ByteArray(8 * 1024)
            while (end > lower) {
                checkCancelled(cancelled)
                val start = max(lower, end - buffer.size)
                val size = (end - start).toInt()
                raf.seek(start)
                raf.readFully(buffer, 0, size)
                for (i in size - 1 downTo 0) {
                    if (buffer[i] == 0x0A.toByte()) return start + i + 1L
                }
                end = start
            }
        }
        return null
    }

    private fun alignCharacterBoundary(file: File, requested: Long, charset: Charset, bom: ByteArray): Long {
        val size = file.length()
        var offset = requested.coerceIn(0L, size)
        if (offset <= bom.size.toLong()) return 0L
        if (isUtf16(charset)) {
            val relative = offset - bom.size
            if ((relative and 1L) != 0L) offset--
            return offset.coerceAtLeast(bom.size.toLong())
        }
        if (charset.name().equals("UTF-8", ignoreCase = true) && offset < size) {
            RandomAccessFile(file, "r").use { raf ->
                while (offset > bom.size) {
                    raf.seek(offset)
                    val value = raf.read()
                    if (value < 0 || value and 0xC0 != 0x80) break
                    offset--
                }
            }
        }
        return offset
    }

    private fun columnAtByteOffset(
        file: File,
        offset: Long,
        charset: Charset,
        bom: ByteArray,
        cancelled: () -> Boolean,
    ): Int {
        if (offset <= bom.size.toLong()) return 1
        val lineStart = findPreviousNewlineEndUnbounded(file, offset, charset, bom, cancelled)
            ?: bom.size.toLong()
        if (lineStart >= offset) return 1
        FileInputStream(file).use { input ->
            skipFully(input, lineStart, cancelled)
            val limited = LimitedInputStream(input, offset - lineStart)
            InputStreamReader(limited, charset).use { reader ->
                val chars = CharArray(16 * 1024)
                var count = 0L
                while (true) {
                    checkCancelled(cancelled)
                    val read = reader.read(chars)
                    if (read <= 0) break
                    count += read
                    if (count >= Int.MAX_VALUE - 1L) return Int.MAX_VALUE
                }
                return (count + 1L).toInt()
            }
        }
    }

    private fun findPreviousNewlineEndUnbounded(
        file: File,
        from: Long,
        charset: Charset,
        bom: ByteArray,
        cancelled: () -> Boolean,
    ): Long? {
        val floor = bom.size.toLong()
        if (!isUtf16(charset)) {
            RandomAccessFile(file, "r").use { raf ->
                var end = from.coerceAtMost(file.length())
                val buffer = ByteArray(32 * 1024)
                while (end > floor) {
                    checkCancelled(cancelled)
                    val start = max(floor, end - buffer.size)
                    val size = (end - start).toInt()
                    raf.seek(start)
                    raf.readFully(buffer, 0, size)
                    for (i in size - 1 downTo 0) {
                        if (buffer[i] == 0x0A.toByte()) return start + i + 1L
                    }
                    end = start
                }
            }
            return null
        }

        val little = charset.name().contains("LE", ignoreCase = true)
        RandomAccessFile(file, "r").use { raf ->
            var end = alignCharacterBoundary(file, from, charset, bom)
            val buffer = ByteArray(32 * 1024)
            while (end > floor) {
                checkCancelled(cancelled)
                var start = max(floor, end - buffer.size)
                // Mantém os pares UTF-16 alinhados em relação ao BOM.
                if (((start - floor) and 1L) != 0L) start++
                val size = (end - start).toInt()
                if (size <= 0) break
                raf.seek(start)
                raf.readFully(buffer, 0, size)
                var i = size - 2
                while (i >= 0) {
                    val a = buffer[i].toInt() and 0xFF
                    val b = buffer[i + 1].toInt() and 0xFF
                    val newline = if (little) a == 0x0A && b == 0x00 else a == 0x00 && b == 0x0A
                    if (newline) return start + i + 2L
                    i -= 2
                }
                end = start
            }
        }
        return null
    }

    private fun findLineByteOffset(
        file: File,
        requestedLine: Int,
        charset: Charset,
        bom: ByteArray,
        onProgress: (Long, Long) -> Unit,
        cancelled: () -> Boolean,
    ): Long? {
        if (requestedLine <= 1) return 0L
        val total = file.length().coerceAtLeast(1L)
        FileInputStream(file).use { input ->
            skipFully(input, bom.size.toLong(), cancelled)
            var absolute = bom.size.toLong()
            var line = 1
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            if (!isUtf16(charset)) {
                while (true) {
                    checkCancelled(cancelled)
                    val read = input.read(buffer)
                    if (read <= 0) break
                    for (i in 0 until read) {
                        if (buffer[i] == 0x0A.toByte()) {
                            line++
                            if (line == requestedLine) return absolute + i + 1L
                        }
                    }
                    absolute += read
                    onProgress(absolute.coerceAtMost(total), total)
                }
                return null
            }

            val little = charset.name().contains("LE", ignoreCase = true)
            var carry: Int? = null
            var carryPos = -1L
            while (true) {
                checkCancelled(cancelled)
                val read = input.read(buffer)
                if (read <= 0) break
                var i = 0
                if (carry != null && read > 0) {
                    val a = carry
                    val b = buffer[0].toInt() and 0xFF
                    val newline = if (little) a == 0x0A && b == 0x00 else a == 0x00 && b == 0x0A
                    if (newline) {
                        line++
                        if (line == requestedLine) return carryPos + 2L
                    }
                    carry = null
                    i = 1
                }
                while (i + 1 < read) {
                    val a = buffer[i].toInt() and 0xFF
                    val b = buffer[i + 1].toInt() and 0xFF
                    val newline = if (little) a == 0x0A && b == 0x00 else a == 0x00 && b == 0x0A
                    if (newline) {
                        line++
                        if (line == requestedLine) return absolute + i + 2L
                    }
                    i += 2
                }
                if (i < read) {
                    carry = buffer[i].toInt() and 0xFF
                    carryPos = absolute + i
                }
                absolute += read
                onProgress(absolute.coerceAtMost(total), total)
            }
        }
        return null
    }

    private fun buildPatchedFile(
        source: File,
        outputFile: File,
        window: Window,
        body: ByteArray,
        bom: ByteArray,
        cancelled: () -> Boolean,
    ) {
        FileInputStream(source).use { input ->
            FileOutputStream(outputFile).use { output ->
                copyExactly(input, output, window.startByte, cancelled)
                if (window.startByte == 0L && bom.isNotEmpty()) output.write(bom)
                output.write(body)
                skipFully(input, window.endByte - window.startByte, cancelled)
                copyRemaining(input, output, cancelled)
                output.flush()
                output.fd.sync()
            }
        }
    }

    private fun copyExactly(input: InputStream, output: FileOutputStream, bytes: Long, cancelled: () -> Boolean) {
        var remaining = bytes
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        while (remaining > 0L) {
            checkCancelled(cancelled)
            val read = input.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
            if (read <= 0) error("O arquivo terminou antes do trecho esperado.")
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun copyRemaining(input: InputStream, output: FileOutputStream, cancelled: () -> Boolean) {
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        while (true) {
            checkCancelled(cancelled)
            val read = input.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
    }

    private fun replaceAtomically(temp: File, target: File) {
        try {
            Files.move(
                temp.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            val parent = target.parentFile ?: error("Pasta de destino indisponível.")
            val backup = if (target.exists()) File(parent, ".${target.name}.large-backup-${UUID.randomUUID()}") else null
            try {
                if (backup != null) Files.move(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                backup?.delete()
            } catch (moveError: Throwable) {
                if (backup != null && backup.exists()) {
                    runCatching {
                        if (target.exists()) Files.delete(target.toPath())
                        Files.move(backup.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                throw moveError
            } finally {
                if (backup != null && backup.exists() && target.exists()) backup.delete()
            }
        }
    }

    private fun normalizeLineEndings(text: String, preferred: String): String {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        return if (preferred == "\r\n") normalized.replace("\n", "\r\n") else normalized
    }

    private fun isUtf16(charset: Charset): Boolean = charset.name().startsWith("UTF-16", ignoreCase = true)

    private fun buildPrefixTable(query: String): IntArray {
        val prefix = IntArray(query.length)
        var j = 0
        for (i in 1 until query.length) {
            while (j > 0 && !sameCharIgnoreCase(query[i], query[j])) j = prefix[j - 1]
            if (sameCharIgnoreCase(query[i], query[j])) j++
            prefix[i] = j
        }
        return prefix
    }

    private fun sameCharIgnoreCase(a: Char, b: Char): Boolean = a.equals(b, ignoreCase = true)

    private fun skipFully(input: InputStream, count: Long, cancelled: () -> Boolean) {
        var remaining = count
        val scratch = ByteArray(8 * 1024)
        while (remaining > 0L) {
            checkCancelled(cancelled)
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                val read = input.read(scratch, 0, min(scratch.size.toLong(), remaining).toInt())
                if (read <= 0) break
                remaining -= read
            }
        }
    }

    private fun checkCancelled(cancelled: () -> Boolean) {
        if (cancelled() || Thread.currentThread().isInterrupted) throw CancellationException("Operação cancelada")
    }

    private fun ByteArray.startsWithBytes(prefix: ByteArray): Boolean {
        if (prefix.size > size) return false
        for (i in prefix.indices) if (this[i] != prefix[i]) return false
        return true
    }

    private class LimitedInputStream(input: InputStream, private var remaining: Long) : FilterInputStream(input) {
        override fun read(): Int {
            if (remaining <= 0L) return -1
            val value = super.read()
            if (value >= 0) remaining--
            return value
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0L) return -1
            val allowed = min(len.toLong(), remaining).toInt()
            val read = super.read(b, off, allowed)
            if (read > 0) remaining -= read
            return read
        }
    }

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        var count: Long = 0L
            private set

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) count++
            return value
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val read = super.read(b, off, len)
            if (read > 0) count += read
            return read
        }

        override fun skip(n: Long): Long {
            val skipped = super.skip(n)
            count += skipped
            return skipped
        }
    }
}
