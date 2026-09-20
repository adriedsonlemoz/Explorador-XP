package com.exploradorxp.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import java.io.File
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

internal enum class ArchiveConflictMode {
    REPLACE,
    SKIP,
    RENAME,
}

internal enum class ArchiveSortMode {
    NAME,
    SIZE,
    TYPE,
    DATE,
}

internal data class ArchiveEntryInfo(
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val modifiedAt: Long,
    val compressionMethod: String,
    val encrypted: Boolean,
)

internal data class ZipArchiveInfo(
    val entries: List<ArchiveEntryInfo>,
    val encrypted: Boolean,
    val splitArchive: Boolean,
    val validHeaders: Boolean,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
) {
    val fileCount: Int get() = entries.count { !it.isDirectory }
    val directoryCount: Int get() = entries.count { it.isDirectory }
    val compressionRatio: Int?
        get() = if (uncompressedBytes > 0L) {
            ((1.0 - compressedBytes.toDouble() / uncompressedBytes.toDouble()) * 100.0)
                .toInt()
                .coerceIn(-999, 100)
        } else null
}

internal data class ArchiveProgress(
    val percent: Int,
    val completedEntries: Int,
    val totalEntries: Int,
    val extractedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long,
    val currentEntry: String,
)

internal data class ArchiveExtractionSummary(
    val destination: File,
    val extracted: Int,
    val extractedFiles: Int,
    val primaryExtractedFile: File?,
    val skipped: Int,
    val renamed: Int,
    val errors: Int,
    val errorMessages: List<String>,
)

internal data class ArchiveCreationSummary(
    val archive: File,
    val selectedItems: Int,
    val archivedEntries: Int,
    val inputBytes: Long,
    val outputBytes: Long,
)

private data class ArchiveSourceEntry(
    val source: File,
    val entryPath: String,
    val directory: Boolean,
)

internal class ArchivePasswordRequiredException : Exception("Este ZIP é protegido por senha.")
internal class ArchivePasswordIncorrectException : Exception("Senha incorreta ou conteúdo criptografado inválido.")
internal class ArchiveUnsafePathException(path: String) : Exception("Entrada ZIP insegura bloqueada: $path")

internal object ArchiveManager {
    suspend fun createZip(
        sources: List<File>,
        destinationParent: File,
        archiveName: String,
        onProgress: (ArchiveProgress) -> Unit = {},
    ): Result<ArchiveCreationSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val validSources = sources.distinctBy { it.absolutePath }.filter { it.exists() }
            require(validSources.isNotEmpty()) { "Nenhum item disponível para compactar." }
            require(destinationParent.exists() || destinationParent.mkdirs()) { "Não foi possível acessar a pasta de destino." }
            require(destinationParent.isDirectory) { "O destino escolhido não é uma pasta." }

            val normalizedName = normalizeArchiveFileName(archiveName)
            require(normalizedName.isNotBlank()) { "Informe um nome para o arquivo ZIP." }
            val requestedTarget = File(destinationParent, normalizedName)
            val target = uniqueFile(requestedTarget)
            val targetCanonical = target.canonicalFile

            val entries = mutableListOf<ArchiveSourceEntry>()
            val usedRoots = linkedSetOf<String>()
            val visitedDirectories = hashSetOf<String>()
            validSources.forEach { source ->
                coroutineContext.ensureActive()
                val rootName = uniqueArchiveRootName(source.name.ifBlank { "item" }, usedRoots)
                collectArchiveSourceEntries(
                    source = source,
                    entryPath = rootName,
                    outputTarget = targetCanonical,
                    destination = entries,
                    visitedDirectories = visitedDirectories,
                )
            }
            require(entries.isNotEmpty()) { "Nenhum item pôde ser preparado para compactação." }

            val totalBytes = entries.filterNot { it.directory }.sumOf { it.source.length().coerceAtLeast(0L) }
            val requiredWithMargin = totalBytes + (totalBytes / 50L).coerceAtLeast(1L * 1024L * 1024L)
            if (destinationParent.usableSpace in 1 until requiredWithMargin) {
                error(
                    "Espaço insuficiente. Necessário aproximadamente ${archiveFormatBytes(requiredWithMargin)} " +
                        "e disponível ${archiveFormatBytes(destinationParent.usableSpace)}."
                )
            }

            val temp = File(destinationParent, ".${target.name}.compactando-${System.nanoTime()}")
            var processedBytes = 0L
            var completed = 0
            val startedAt = System.nanoTime()
            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(temp))).use { zipOut ->
                    zipOut.setLevel(Deflater.DEFAULT_COMPRESSION)
                    entries.forEach { item ->
                        coroutineContext.ensureActive()
                        val zipPath = item.entryPath.replace(File.separatorChar, '/').trimStart('/') + if (item.directory) "/" else ""
                        val entry = ZipEntry(zipPath).apply {
                            if (item.source.lastModified() > 0L) time = item.source.lastModified()
                        }
                        zipOut.putNextEntry(entry)
                        if (!item.directory) {
                            BufferedInputStream(FileInputStream(item.source)).use { input ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                                while (true) {
                                    coroutineContext.ensureActive()
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    zipOut.write(buffer, 0, read)
                                    processedBytes += read
                                    emitProgress(
                                        onProgress,
                                        completed,
                                        entries.size,
                                        processedBytes,
                                        totalBytes,
                                        startedAt,
                                        item.entryPath,
                                    )
                                }
                            }
                        }
                        zipOut.closeEntry()
                        completed++
                        emitProgress(
                            onProgress,
                            completed,
                            entries.size,
                            processedBytes,
                            totalBytes,
                            startedAt,
                            item.entryPath,
                        )
                    }
                }
                coroutineContext.ensureActive()
                require(temp.renameTo(target)) { "Não foi possível finalizar o arquivo ZIP." }
                emitProgress(onProgress, entries.size, entries.size, totalBytes, totalBytes, startedAt, "Concluído")
                ArchiveCreationSummary(
                    archive = target,
                    selectedItems = validSources.size,
                    archivedEntries = entries.size,
                    inputBytes = totalBytes,
                    outputBytes = target.length().coerceAtLeast(0L),
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                runCatching { temp.delete() }
                throw cancelled
            } catch (error: Throwable) {
                runCatching { temp.delete() }
                throw error
            }
        }
    }

    suspend fun readZip(file: File): Result<ZipArchiveInfo> = withContext(Dispatchers.IO) {
        runCatching {
            require(file.exists() && file.isFile) { "Arquivo ZIP não encontrado." }
            val zip = ZipFile(file)
            val headers = zip.fileHeaders
            val entries = headers.mapNotNull { header -> header.toEntryInfoOrNull() }
            ZipArchiveInfo(
                entries = entries,
                encrypted = zip.isEncrypted,
                splitArchive = zip.isSplitArchive,
                validHeaders = zip.isValidZipFile,
                compressedBytes = entries.filterNot { it.isDirectory }.sumOf { it.compressedSize.coerceAtLeast(0L) },
                uncompressedBytes = entries.filterNot { it.isDirectory }.sumOf { it.size.coerceAtLeast(0L) },
            )
        }
    }

    suspend fun extract(
        zipFile: File,
        destinationParent: File,
        createNamedFolder: Boolean,
        selectedPaths: Set<String>? = null,
        conflictMode: ArchiveConflictMode = ArchiveConflictMode.RENAME,
        password: CharArray? = null,
        onProgress: (ArchiveProgress) -> Unit = {},
    ): Result<ArchiveExtractionSummary> = withContext(Dispatchers.IO) {
        runCatching {
            require(destinationParent.exists() || destinationParent.mkdirs()) { "Não foi possível acessar a pasta de destino." }
            require(destinationParent.isDirectory) { "O destino escolhido não é uma pasta." }

            val zip = ZipFile(zipFile, password)
            if (zip.isEncrypted && (password == null || password.isEmpty())) throw ArchivePasswordRequiredException()
            val allHeaders = zip.fileHeaders
            val headers = headersForSelection(allHeaders, selectedPaths)
            require(headers.isNotEmpty()) { "Nenhum item selecionado para extrair." }

            val baseDestination = if (createNamedFolder) {
                val base = zipFile.nameWithoutExtension.ifBlank { "extraido" }
                val requested = File(destinationParent, base)
                if (requested.exists() && conflictMode == ArchiveConflictMode.RENAME) uniqueDirectory(requested) else requested
            } else {
                destinationParent
            }
            if (!baseDestination.exists() && !baseDestination.mkdirs()) {
                error("Não foi possível criar a pasta de extração.")
            }

            val totalBytes = headers.filterNot { it.isDirectory }.sumOf { it.uncompressedSize.coerceAtLeast(0L) }
            val requiredWithMargin = totalBytes + (totalBytes / 50L).coerceAtLeast(1L * 1024L * 1024L)
            if (destinationParent.usableSpace in 1 until requiredWithMargin) {
                error("Espaço insuficiente. Necessário aproximadamente ${archiveFormatBytes(requiredWithMargin)} e disponível ${archiveFormatBytes(destinationParent.usableSpace)}.")
            }

            var extracted = 0
            var extractedFiles = 0
            var primaryExtractedFile: File? = null
            var skipped = 0
            var renamed = 0
            var errors = 0
            val errorMessages = mutableListOf<String>()
            var completed = 0
            var copiedBytes = 0L
            val startedAt = System.nanoTime()
            val totalEntries = headers.size

            headers.forEach { header ->
                coroutineContext.ensureActive()
                val rawName = header.fileName.orEmpty()
                val safeRelative = safeEntryPath(rawName)
                if (safeRelative.isBlank()) {
                    completed++
                    return@forEach
                }

                try {
                    var target = safeTarget(baseDestination, safeRelative)
                    if (header.isDirectory) {
                        if (target.exists() && !target.isDirectory) {
                            when (conflictMode) {
                                ArchiveConflictMode.SKIP -> {
                                    skipped++
                                    completed++
                                    emitProgress(onProgress, completed, totalEntries, copiedBytes, totalBytes, startedAt, rawName)
                                    return@forEach
                                }
                                ArchiveConflictMode.REPLACE -> {
                                    require(target.delete()) { "Não foi possível substituir ${target.name}." }
                                }
                                ArchiveConflictMode.RENAME -> {
                                    target = uniqueDirectory(target)
                                    renamed++
                                }
                            }
                        }
                        if (!target.exists()) require(target.mkdirs()) { "Não foi possível criar a pasta ${target.name}." }
                        extracted++
                    } else {
                        val parent = target.parentFile
                        if (parent != null && !parent.exists()) require(parent.mkdirs()) { "Não foi possível criar a pasta ${parent.name}." }
                        if (target.exists()) {
                            when (conflictMode) {
                                ArchiveConflictMode.SKIP -> {
                                    skipped++
                                    completed++
                                    emitProgress(
                                        onProgress, completed, totalEntries, copiedBytes, totalBytes,
                                        startedAt, rawName,
                                    )
                                    return@forEach
                                }
                                ArchiveConflictMode.REPLACE -> {
                                    if (target.isDirectory) target.deleteRecursively() else target.delete()
                                }
                                ArchiveConflictMode.RENAME -> {
                                    target = uniqueFile(target)
                                    renamed++
                                }
                            }
                        }

                        try {
                            zip.getInputStream(header).use { input ->
                                FileOutputStream(target).use { output ->
                                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                                    while (true) {
                                        coroutineContext.ensureActive()
                                        val read = input.read(buffer)
                                        if (read < 0) break
                                        output.write(buffer, 0, read)
                                        copiedBytes += read
                                        emitProgress(
                                            onProgress, completed, totalEntries, copiedBytes, totalBytes,
                                            startedAt, rawName,
                                        )
                                    }
                                    output.fd.sync()
                                }
                            }
                            extracted++
                            extractedFiles++
                            if (primaryExtractedFile == null) primaryExtractedFile = target
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            // Remove o arquivo incompleto criado pela operação cancelada. Em modo
                            // Substituir o original já não pode ser recuperado, por isso não fingimos rollback.
                            runCatching { if (target.isFile) target.delete() }
                            throw cancelled
                        } catch (writeError: Throwable) {
                            runCatching { if (target.isFile) target.delete() }
                            throw writeError
                        }
                    }
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    if (looksLikePasswordError(t, zip.isEncrypted)) throw ArchivePasswordIncorrectException()
                    errors++
                    if (errorMessages.size < 8) errorMessages += "${rawName.ifBlank { "item" }}: ${t.message ?: "falha ao extrair"}"
                }
                completed++
                emitProgress(onProgress, completed, totalEntries, copiedBytes, totalBytes, startedAt, rawName)
            }

            emitProgress(onProgress, totalEntries, totalEntries, totalBytes, totalBytes, startedAt, "Concluído")

            ArchiveExtractionSummary(
                destination = baseDestination,
                extracted = extracted,
                extractedFiles = extractedFiles,
                primaryExtractedFile = primaryExtractedFile,
                skipped = skipped,
                renamed = renamed,
                errors = errors,
                errorMessages = errorMessages,
            )
        }
    }

    suspend fun verifyIntegrity(
        zipFile: File,
        password: CharArray? = null,
        onProgress: (ArchiveProgress) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val zip = ZipFile(zipFile, password)
            if (zip.isEncrypted && (password == null || password.isEmpty())) throw ArchivePasswordRequiredException()
            require(zip.isValidZipFile) { "Cabeçalhos ZIP inválidos." }
            val headers = zip.fileHeaders.filterNot { it.isDirectory }
            val totalBytes = headers.sumOf { it.uncompressedSize.coerceAtLeast(0L) }
            var readBytes = 0L
            val startedAt = System.nanoTime()
            headers.forEachIndexed { index, header ->
                coroutineContext.ensureActive()
                try {
                    zip.getInputStream(header).use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            readBytes += count
                            emitProgress(onProgress, index, headers.size, readBytes, totalBytes, startedAt, header.fileName.orEmpty())
                        }
                    }
                } catch (t: Throwable) {
                    if (looksLikePasswordError(t, zip.isEncrypted)) throw ArchivePasswordIncorrectException()
                    throw t
                }
                emitProgress(onProgress, index + 1, headers.size, readBytes, totalBytes, startedAt, header.fileName.orEmpty())
            }
        }
    }

    suspend fun extractEntryToCache(
        zipFile: File,
        entryPath: String,
        cacheRoot: File,
        password: CharArray? = null,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val zip = ZipFile(zipFile, password)
            if (zip.isEncrypted && (password == null || password.isEmpty())) throw ArchivePasswordRequiredException()
            val header = zip.getFileHeader(entryPath) ?: error("Item não encontrado no ZIP.")
            require(!header.isDirectory) { "Não é possível visualizar uma pasta." }
            val safeName = File(safeEntryPath(header.fileName.orEmpty())).name.ifBlank { "arquivo" }
            val previewDir = File(cacheRoot, "archive-preview").apply {
                if (!exists()) mkdirs()
            }
            val target = uniqueFile(File(previewDir, safeName))
            try {
                zip.getInputStream(header).use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            } catch (t: Throwable) {
                if (looksLikePasswordError(t, zip.isEncrypted)) throw ArchivePasswordIncorrectException()
                throw t
            }
            target
        }
    }

    internal fun requiredBytes(info: ZipArchiveInfo, selectedPaths: Set<String>? = null): Long {
        if (selectedPaths.isNullOrEmpty()) return info.uncompressedBytes
        val normalized = selectedPaths.map(::normalizeEntryPath)
        return info.entries
            .filterNot { it.isDirectory }
            .filter { entry -> normalized.any { selected -> entry.path == selected || entry.path.startsWith("$selected/") } }
            .sumOf { it.size.coerceAtLeast(0L) }
    }
}

internal fun normalizeArchiveFileName(value: String): String {
    val cleaned = value.trim()
        .replace('/', '_')
        .replace('\\', '_')
        .replace(Regex("[\u0000-\u001F]"), "")
        .trim('.', ' ')
    if (cleaned.isBlank()) return ""
    return if (cleaned.endsWith(".zip", ignoreCase = true)) cleaned else "$cleaned.zip"
}

private fun uniqueArchiveRootName(name: String, used: MutableSet<String>): String {
    if (used.add(name.lowercase())) return name
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0) name.substring(0, dot) else name
    val ext = if (dot > 0) name.substring(dot) else ""
    var index = 1
    while (true) {
        val candidate = "$base ($index)$ext"
        if (used.add(candidate.lowercase())) return candidate
        index++
    }
}

private suspend fun collectArchiveSourceEntries(
    source: File,
    entryPath: String,
    outputTarget: File,
    destination: MutableList<ArchiveSourceEntry>,
    visitedDirectories: MutableSet<String>,
) {
    coroutineContext.ensureActive()
    val canonical = runCatching { source.canonicalFile }.getOrElse { source.absoluteFile }
    if (canonical.path == outputTarget.path) return
    if (source.isDirectory) {
        if (!visitedDirectories.add(canonical.path)) return
        destination += ArchiveSourceEntry(source, entryPath, directory = true)
        val children = source.listFiles()?.sortedWith(
            compareBy<File>({ !it.isDirectory }, { it.name.lowercase() })
        ).orEmpty()
        for (child in children) {
            collectArchiveSourceEntries(
                source = child,
                entryPath = "$entryPath/${child.name}",
                outputTarget = outputTarget,
                destination = destination,
                visitedDirectories = visitedDirectories,
            )
        }
    } else if (source.isFile) {
        destination += ArchiveSourceEntry(source, entryPath, directory = false)
    }
}

private fun FileHeader.toEntryInfoOrNull(): ArchiveEntryInfo? {
    val path = normalizeEntryPath(fileName.orEmpty())
    if (path.isBlank()) return null
    return ArchiveEntryInfo(
        path = path,
        isDirectory = isDirectory,
        size = uncompressedSize.coerceAtLeast(0L),
        compressedSize = compressedSize.coerceAtLeast(0L),
        modifiedAt = dosDateTimeToMillis(lastModifiedTime),
        compressionMethod = compressionMethod?.name ?: "DESCONHECIDO",
        encrypted = isEncrypted,
    )
}

private fun headersForSelection(headers: List<FileHeader>, selectedPaths: Set<String>?): List<FileHeader> {
    if (selectedPaths.isNullOrEmpty()) return headers
    val normalized = selectedPaths.map(::normalizeEntryPath)
    return headers.filter { header ->
        val path = normalizeEntryPath(header.fileName.orEmpty())
        normalized.any { selected -> path == selected || path.startsWith("$selected/") }
    }
}

internal fun normalizeEntryPath(path: String): String = path
    .replace('\\', '/')
    .trim()
    .trimStart('/')
    .trimEnd('/')

internal fun safeEntryPath(path: String): String {
    val normalized = normalizeEntryPath(path)
    val parts = normalized.split('/').filter { it.isNotBlank() && it != "." }
    if (parts.any { it == ".." }) throw ArchiveUnsafePathException(path)
    if (path.startsWith('/') || path.startsWith('\\') || Regex("^[A-Za-z]:").containsMatchIn(path)) {
        throw ArchiveUnsafePathException(path)
    }
    return parts.joinToString(File.separator)
}

private fun safeTarget(root: File, relativePath: String): File {
    val canonicalRoot = root.canonicalFile
    val target = File(canonicalRoot, relativePath).canonicalFile
    val rootPrefix = canonicalRoot.path + File.separator
    if (target.path != canonicalRoot.path && !target.path.startsWith(rootPrefix)) {
        throw ArchiveUnsafePathException(relativePath)
    }
    return target
}

internal fun uniqueFile(original: File): File {
    if (!original.exists()) return original
    val name = original.name
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0) name.substring(0, dot) else name
    val ext = if (dot > 0) name.substring(dot) else ""
    var index = 1
    var candidate: File
    do {
        candidate = File(original.parentFile, "$base ($index)$ext")
        index++
    } while (candidate.exists())
    return candidate
}

private fun uniqueDirectory(original: File): File {
    if (!original.exists()) return original
    var index = 1
    var candidate: File
    do {
        candidate = File(original.parentFile, "${original.name} ($index)")
        index++
    } while (candidate.exists())
    return candidate
}

private fun emitProgress(
    callback: (ArchiveProgress) -> Unit,
    completed: Int,
    totalEntries: Int,
    copiedBytes: Long,
    totalBytes: Long,
    startedAtNanos: Long,
    currentEntry: String,
) {
    val elapsedSeconds = ((System.nanoTime() - startedAtNanos) / 1_000_000_000.0).coerceAtLeast(0.05)
    val percent = when {
        totalBytes > 0L -> ((copiedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
        totalEntries > 0 -> ((completed * 100) / totalEntries).coerceIn(0, 100)
        else -> 100
    }
    callback(
        ArchiveProgress(
            percent = percent,
            completedEntries = completed.coerceAtMost(totalEntries),
            totalEntries = totalEntries,
            extractedBytes = copiedBytes,
            totalBytes = totalBytes,
            speedBytesPerSecond = (copiedBytes / elapsedSeconds).toLong().coerceAtLeast(0L),
            currentEntry = currentEntry,
        )
    )
}

private fun looksLikePasswordError(t: Throwable, encrypted: Boolean): Boolean {
    if (!encrypted) return false
    if (t is ArchivePasswordRequiredException || t is ArchivePasswordIncorrectException) return true
    if (t is ZipException && t.type == ZipException.Type.WRONG_PASSWORD) return true
    val text = generateSequence(t) { it.cause }
        .joinToString(" ") { it.message.orEmpty() }
        .lowercase()
    return text.contains("password") || text.contains("senha") || text.contains("decrypt") || text.contains("crc")
}

private fun dosDateTimeToMillis(value: Long): Long {
    if (value <= 0L) return 0L
    return runCatching {
        val time = value.toInt()
        val second = (time and 0x1F) * 2
        val minute = (time shr 5) and 0x3F
        val hour = (time shr 11) and 0x1F
        val day = (time shr 16) and 0x1F
        val month = ((time shr 21) and 0x0F) - 1
        val year = ((time shr 25) and 0x7F) + 1980
        Calendar.getInstance().apply {
            set(Calendar.MILLISECOND, 0)
            set(year, month.coerceIn(0, 11), day.coerceAtLeast(1), hour, minute, second)
        }.timeInMillis
    }.getOrDefault(0L)
}

internal fun archiveFormatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    do {
        value /= 1024.0
        index++
    } while (value >= 1024.0 && index < units.lastIndex)
    return String.format(java.util.Locale.getDefault(), "%.1f %s", value, units[index])
}
