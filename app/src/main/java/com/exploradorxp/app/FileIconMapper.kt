package com.exploradorxp.app

import java.io.File

/**
 * Mapeia arquivos para um conjunto pequeno e coerente de ícones vetoriais.
 *
 * A extensão continua sendo usada para escolher a categoria visual, mas o app não
 * mantém mais um PNG diferente para cada formato. Isso reduz o APK, elimina
 * decodificação de bitmap para ícones e mantém a interface consistente.
 */
object FileIconMapper {
    private val text = setOf("txt", "log", "rtf", "md", "ini")
    private val documents = setOf("doc", "docx", "odt")
    private val sheets = setOf("xls", "xlsx", "ods", "csv")
    private val presentations = setOf("ppt", "pptx", "odp")
    private val images = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tif", "tiff", "heic", "psd", "ai", "eps", "ico")
    private val audio = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")
    private val video = setOf("mp4", "avi", "mkv", "mov", "webm", "3gp")
    private val archives = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
    private val code = setOf("html", "htm", "css", "js", "json", "xml", "yaml", "yml", "php", "py", "java", "kt", "c", "h", "cpp", "sh", "bat")
    private val databases = setOf("sql", "db", "sqlite")
    private val apps = setOf("apk", "exe", "deb", "rpm")
    private val disks = setOf("iso", "dmg", "bin", "torrent")
    private val ebooks = setOf("epub", "mobi")

    fun iconFor(file: File): Int = iconFor(file, file.isDirectory)

    fun iconFor(file: File, isDirectory: Boolean): Int {
        if (isDirectory) return folderIconFor(file.name)
        return iconForExtension(file.extension)
    }

    fun iconForResolvedExtension(file: File, resolvedExtension: String): Int {
        if (file.isDirectory) return folderIconFor(file.name)
        return iconForExtension(resolvedExtension)
    }

    internal fun iconForExtension(extension: String): Int = when (extension.lowercase()) {
        in text -> R.drawable.xp_file_text
        in documents -> R.drawable.xp_file_document
        in sheets -> R.drawable.xp_file_sheet
        in presentations -> R.drawable.xp_file_presentation
        "pdf" -> R.drawable.xp_file_pdf
        in images -> R.drawable.xp_file_image
        in audio -> R.drawable.xp_file_audio
        in video -> R.drawable.xp_file_video
        in archives -> R.drawable.xp_file_archive
        in code -> R.drawable.xp_file_code
        in databases -> R.drawable.xp_file_database
        in apps -> R.drawable.xp_file_app
        in disks -> R.drawable.xp_file_disk
        "ttf", "otf" -> R.drawable.xp_file_font
        in ebooks -> R.drawable.xp_file_ebook
        else -> R.drawable.xp_file_unknown
    }

    internal fun folderIconFor(name: String): Int = when (name.lowercase()) {
        "download", "downloads" -> R.drawable.xp_folder_download
        "documents", "documentos" -> R.drawable.xp_folder_document
        "dcim", "pictures", "imagens", "images" -> R.drawable.xp_folder_image
        "music", "música", "musicas", "músicas", "audio" -> R.drawable.xp_folder_music
        "movies", "videos", "vídeos" -> R.drawable.xp_folder_video
        else -> R.drawable.xp_folder
    }
}
