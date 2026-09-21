package com.exploradorxp.app

/**
 * Define exatamente o que o Explorador XP anuncia ao Android como abertura externa suportada.
 * Não usamos MIME coringa nem application/octet-stream para evitar aparecer em arquivos que o app não trata.
 */
object ExternalOpenSupport {
    private val supportedExtensions = setOf(
        // Imagens exibidas internamente.
        "jpg", "jpeg", "png", "bmp", "webp", "gif",
        // Vídeo e áudio tratados pelos visualizadores internos.
        "mp4", "m4v", "3gp", "webm", "mkv", "avi", "mov",
        "mp3", "wav", "m4a", "aac", "ogg", "flac", "opus",
        // Texto/código editável pelo editor interno.
        "txt", "log", "ini", "cfg", "conf", "properties", "json", "xml", "csv", "sql",
        "css", "js", "mjs", "cjs", "ts", "tsx", "jsx", "kt", "kts", "java", "py", "sh", "bash",
        "bat", "cmd", "ps1", "yml", "yaml", "md", "markdown", "mds", "gradle", "toml", "env",
        "php", "rb", "go", "rs", "swift", "dart", "c", "h", "hpp", "cpp", "cc", "vue", "svelte",
        "tex", "gitignore", "gitattributes", "editorconfig", "html", "htm",
        // Visualizadores dedicados.
        "pdf", "zip", "apk",
    )

    private val supportedExactNames = setOf(
        "makefile", "dockerfile", "readme", "license", ".gitignore", ".gitattributes", ".editorconfig",
    )

    private val supportedMimeTypes = setOf(
        "application/pdf",
        "application/zip",
        "application/x-zip",
        "application/x-zip-compressed",
        "application/vnd.android.package-archive",
        "application/x-android-package-archive",
        "application/json",
        "application/xml",
        "application/xhtml+xml",
        "application/javascript",
        "application/ecmascript",
        "application/yaml",
        "application/x-yaml",
        "application/toml",
        "application/sql",
        "application/x-sh",
        "application/x-httpd-php",
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/bmp",
        "image/x-ms-bmp",
        "image/webp",
        "video/mp4",
        "video/x-m4v",
        "video/3gpp",
        "video/webm",
        "video/x-matroska",
        "video/x-msvideo",
        "video/quicktime",
        "audio/mpeg",
        "audio/wav",
        "audio/x-wav",
        "audio/mp4",
        "audio/x-m4a",
        "audio/aac",
        "audio/ogg",
        "application/ogg",
        "audio/flac",
        "audio/opus",
    )

    fun isCompatible(fileName: String?, mimeType: String?): Boolean {
        val normalizedName = fileName.orEmpty().substringAfterLast('/').lowercase()
        val extension = normalizedName.substringAfterLast('.', "")
        val mime = normalizeMime(mimeType)
        return normalizedName in supportedExactNames ||
            extension in supportedExtensions ||
            mime.startsWith("text/") ||
            mime in supportedMimeTypes
    }

    fun normalizeMime(mimeType: String?): String =
        mimeType.orEmpty().substringBefore(';').trim().lowercase()

    fun preferredExtensionForMime(mimeType: String?): String? = when (normalizeMime(mimeType)) {
        "application/pdf" -> "pdf"
        "application/zip", "application/x-zip", "application/x-zip-compressed" -> "zip"
        "application/vnd.android.package-archive", "application/x-android-package-archive" -> "apk"
        "application/json" -> "json"
        "application/xml" -> "xml"
        "application/xhtml+xml" -> "html"
        "application/javascript", "application/ecmascript" -> "js"
        "application/yaml", "application/x-yaml" -> "yaml"
        "application/toml" -> "toml"
        "application/sql" -> "sql"
        "application/x-sh" -> "sh"
        "application/x-httpd-php" -> "php"
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/bmp", "image/x-ms-bmp" -> "bmp"
        "image/webp" -> "webp"
        "video/mp4" -> "mp4"
        "video/x-m4v" -> "m4v"
        "video/3gpp" -> "3gp"
        "video/webm" -> "webm"
        "video/x-matroska" -> "mkv"
        "video/x-msvideo" -> "avi"
        "video/quicktime" -> "mov"
        "audio/mpeg" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/mp4", "audio/x-m4a" -> "m4a"
        "audio/aac" -> "aac"
        "audio/ogg", "application/ogg" -> "ogg"
        "audio/flac" -> "flac"
        "audio/opus" -> "opus"
        else -> if (normalizeMime(mimeType).startsWith("text/")) "txt" else null
    }

    fun hasSupportedExtension(fileName: String): Boolean {
        val normalizedName = fileName.substringAfterLast('/').lowercase()
        val extension = normalizedName.substringAfterLast('.', "")
        return normalizedName in supportedExactNames || extension in supportedExtensions
    }
}
