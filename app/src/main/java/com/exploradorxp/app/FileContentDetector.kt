package com.exploradorxp.app

import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

/**
 * Detecção leve usada somente quando o nome do arquivo não informa um tipo interno suportado.
 * A extensão continua sendo a fonte primária; o conteúdo é consultado como fallback para arquivos
 * sem extensão, renomeados ou recebidos com um nome genérico.
 */
internal object FileContentDetector {
    private const val HEADER_BYTES = 8192
    private const val CACHE_MISS = "<none>"
    private val cache = ConcurrentHashMap<String, String>()

    private val internalExtensions = setOf(
        "jpg", "jpeg", "png", "bmp", "webp", "gif",
        "mp4", "m4v", "3gp", "webm", "mkv", "avi", "mov",
        "mp3", "wav", "m4a", "aac", "ogg", "flac", "opus",
        "txt", "log", "ini", "cfg", "conf", "properties", "json", "xml", "csv", "sql",
        "css", "js", "mjs", "cjs", "ts", "tsx", "jsx", "kt", "kts", "java", "py", "sh", "bash",
        "bat", "cmd", "ps1", "yml", "yaml", "md", "markdown", "mds", "gradle", "toml", "env",
        "php", "rb", "go", "rs", "swift", "dart", "c", "h", "hpp", "cpp", "cc", "vue", "svelte",
        "tex", "gitignore", "gitattributes", "editorconfig", "html", "htm",
        "pdf", "zip", "apk",
    )

    fun needsContentDetection(file: File): Boolean {
        val extension = file.extension.lowercase()
        if (extension in internalExtensions) return false
        val genericExtensions = setOf("bin", "dat", "tmp", "download", "file")
        return extension.isBlank() || extension in genericExtensions || !FileTypeClassifier.isKnownExtension(extension)
    }

    /** Retorna uma extensão virtual compatível com os visualizadores internos. */
    fun detectExtension(file: File): String? {
        if (!file.exists() || !file.isFile || file.length() <= 0L) return null
        val key = "${file.absolutePath}|${file.length()}|${file.lastModified()}"
        cache[key]?.let { cached -> return cached.takeUnless { it == CACHE_MISS } }

        val detected = detectUncached(file)
        if (cache.size > 128) cache.clear()
        cache[key] = detected ?: CACHE_MISS
        return detected
    }

    private fun detectUncached(file: File): String? {
        val header = readHeader(file) ?: return null
        if (looksLikeZip(header)) return detectZipSubtype(file)
        if (startsWithAscii(header, "%PDF-")) return "pdf"
        if (header.startsWithBytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "png"
        if (header.startsWithBytes(0xFF, 0xD8, 0xFF)) return "jpg"
        if (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a")) return "gif"
        if (header.startsWithBytes(0x42, 0x4D)) return "bmp"
        if (header.size >= 12 && startsWithAscii(header, "RIFF") && asciiAt(header, 8, "WEBP")) return "webp"
        if (looksLikeText(header)) return "txt"
        return null
    }

    private fun detectZipSubtype(file: File): String = runCatching {
        ZipFile(file).use { zip ->
            val hasManifest = zip.getEntry("AndroidManifest.xml") != null
            val hasDex = zip.getEntry("classes.dex") != null || zip.getEntry("classes2.dex") != null
            val hasResources = zip.getEntry("resources.arsc") != null
            if (hasManifest && (hasDex || hasResources)) "apk" else "zip"
        }
    }.getOrDefault("zip")

    private fun readHeader(file: File): ByteArray? = runCatching {
        FileInputStream(file).use { input ->
            val target = ByteArray(minOf(HEADER_BYTES.toLong(), file.length()).toInt())
            var total = 0
            while (total < target.size) {
                val read = input.read(target, total, target.size - total)
                if (read <= 0) break
                total += read
            }
            if (total == target.size) target else target.copyOf(total)
        }
    }.getOrNull()

    private fun looksLikeZip(bytes: ByteArray): Boolean =
        bytes.startsWithBytes(0x50, 0x4B, 0x03, 0x04) ||
            bytes.startsWithBytes(0x50, 0x4B, 0x05, 0x06) ||
            bytes.startsWithBytes(0x50, 0x4B, 0x07, 0x08)

    private fun looksLikeText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        // BOMs comuns: UTF-8, UTF-16 LE e UTF-16 BE.
        if (bytes.startsWithBytes(0xEF, 0xBB, 0xBF) || bytes.startsWithBytes(0xFF, 0xFE) || bytes.startsWithBytes(0xFE, 0xFF)) {
            return true
        }

        var printable = 0
        var suspicious = 0
        bytes.forEach { raw ->
            val value = raw.toInt() and 0xFF
            when {
                value == 0 -> return false
                value == 9 || value == 10 || value == 13 -> printable++
                value in 0x20..0x7E || value >= 0x80 -> printable++
                else -> suspicious++
            }
        }
        return printable >= 16 && suspicious * 20 <= bytes.size
    }

    private fun startsWithAscii(bytes: ByteArray, value: String): Boolean = asciiAt(bytes, 0, value)

    private fun asciiAt(bytes: ByteArray, offset: Int, value: String): Boolean {
        if (offset < 0 || bytes.size < offset + value.length) return false
        value.forEachIndexed { index, char ->
            if ((bytes[offset + index].toInt() and 0xFF) != char.code) return false
        }
        return true
    }

    private fun ByteArray.startsWithBytes(vararg expected: Int): Boolean {
        if (size < expected.size) return false
        expected.forEachIndexed { index, value ->
            if ((this[index].toInt() and 0xFF) != value) return false
        }
        return true
    }
}
