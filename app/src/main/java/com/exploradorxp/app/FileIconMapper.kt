package com.exploradorxp.app

import java.io.File

enum class FileIconKind {
    FOLDER,
    DOWNLOADS_FOLDER,
    DOCUMENT,
    PDF,
    SPREADSHEET,
    PRESENTATION,
    IMAGE,
    AUDIO,
    VIDEO,
    ARCHIVE,
    CODE,
    DATABASE,
    APK,
    FONT,
    DISK_IMAGE,
    TORRENT,
    UNKNOWN,
}

data class FileVisual(
    val kind: FileIconKind,
    val badge: String? = null,
)

/**
 * Mapeamento leve por categoria. Em vez de manter um PNG para cada extensão,
 * a UI desenha um vetor por família e uma etiqueta curta (PDF, ZIP, DOCX...).
 */
object FileIconMapper {
    private val documents = setOf("txt", "log", "rtf", "md", "doc", "docx", "odt", "epub", "mobi")
    private val spreadsheets = setOf("xls", "xlsx", "ods", "csv")
    private val presentations = setOf("ppt", "pptx", "odp")
    private val images = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tif", "tiff", "heic", "psd", "ai", "eps", "ico")
    private val audio = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")
    private val video = setOf("mp4", "avi", "mkv", "mov", "webm", "3gp")
    private val archives = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    private val code = setOf("html", "htm", "css", "js", "json", "xml", "yaml", "yml", "php", "py", "java", "kt", "c", "h", "cpp", "sh", "bat", "ini")
    private val databases = setOf("sql", "db", "sqlite")
    private val diskImages = setOf("iso", "dmg", "bin", "deb", "rpm", "exe")

    fun iconFor(file: File): FileVisual = iconFor(file, file.isDirectory)

    fun iconFor(file: File, isDirectory: Boolean): FileVisual {
        if (isDirectory) {
            return when (file.name.lowercase()) {
                "download", "downloads" -> FileVisual(FileIconKind.DOWNLOADS_FOLDER)
                else -> FileVisual(FileIconKind.FOLDER)
            }
        }

        val ext = file.extension.lowercase()
        val kind = when {
            ext == "pdf" -> FileIconKind.PDF
            ext in documents -> FileIconKind.DOCUMENT
            ext in spreadsheets -> FileIconKind.SPREADSHEET
            ext in presentations -> FileIconKind.PRESENTATION
            ext in images -> FileIconKind.IMAGE
            ext in audio -> FileIconKind.AUDIO
            ext in video -> FileIconKind.VIDEO
            ext in archives -> FileIconKind.ARCHIVE
            ext in code -> FileIconKind.CODE
            ext in databases -> FileIconKind.DATABASE
            ext == "apk" -> FileIconKind.APK
            ext == "ttf" || ext == "otf" -> FileIconKind.FONT
            ext in diskImages -> FileIconKind.DISK_IMAGE
            ext == "torrent" -> FileIconKind.TORRENT
            else -> FileIconKind.UNKNOWN
        }
        return FileVisual(kind, badgeFor(ext))
    }

    private fun badgeFor(extension: String): String? {
        if (extension.isBlank()) return null
        return when (extension) {
            "jpeg" -> "JPG"
            "tiff" -> "TIF"
            "sqlite" -> "DB"
            else -> extension.uppercase().take(5)
        }
    }
}
