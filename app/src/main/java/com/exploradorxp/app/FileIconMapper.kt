package com.exploradorxp.app

import java.io.File

object FileIconMapper {
    private val extensionMap = mapOf(
        "txt" to R.drawable.file_txt,
        "log" to R.drawable.file_log,
        "rtf" to R.drawable.file_rtf,
        "md" to R.drawable.file_md,
        "doc" to R.drawable.file_doc,
        "docx" to R.drawable.file_docx,
        "odt" to R.drawable.file_odt,
        "xls" to R.drawable.file_xls,
        "xlsx" to R.drawable.file_xlsx,
        "ods" to R.drawable.file_ods,
        "csv" to R.drawable.file_csv,
        "ppt" to R.drawable.file_ppt,
        "pptx" to R.drawable.file_pptx,
        "odp" to R.drawable.file_odp,
        "pdf" to R.drawable.file_pdf,
        "jpg" to R.drawable.file_jpg,
        "jpeg" to R.drawable.file_jpg,
        "png" to R.drawable.file_png,
        "gif" to R.drawable.file_gif,
        "bmp" to R.drawable.file_bmp,
        "webp" to R.drawable.file_webp,
        "svg" to R.drawable.file_svg,
        "tif" to R.drawable.file_tif,
        "tiff" to R.drawable.file_tiff,
        "heic" to R.drawable.file_heic,
        "psd" to R.drawable.file_psd,
        "ai" to R.drawable.file_ai,
        "eps" to R.drawable.file_eps,
        "ico" to R.drawable.file_ico,
        "mp3" to R.drawable.file_mp3,
        "wav" to R.drawable.file_wav,
        "ogg" to R.drawable.file_ogg,
        "flac" to R.drawable.file_flac,
        "aac" to R.drawable.file_aac,
        "m4a" to R.drawable.file_m4a,
        "mp4" to R.drawable.file_mp4,
        "avi" to R.drawable.file_avi,
        "mkv" to R.drawable.file_mkv,
        "mov" to R.drawable.file_mov,
        "webm" to R.drawable.file_webm,
        "3gp" to R.drawable.file_3gp,
        "zip" to R.drawable.file_zip,
        "rar" to R.drawable.file_rar,
        "7z" to R.drawable.file_7z,
        "tar" to R.drawable.file_tar,
        "gz" to R.drawable.file_gz,
        "bz2" to R.drawable.file_bz2,
        "xz" to R.drawable.file_xz,
        "html" to R.drawable.file_html,
        "htm" to R.drawable.file_html,
        "css" to R.drawable.file_css,
        "js" to R.drawable.file_js,
        "json" to R.drawable.file_json,
        "xml" to R.drawable.file_xml,
        "yaml" to R.drawable.file_yaml,
        "yml" to R.drawable.file_yml,
        "php" to R.drawable.file_php,
        "py" to R.drawable.file_py,
        "java" to R.drawable.file_java,
        "kt" to R.drawable.file_kt,
        "c" to R.drawable.file_c,
        "h" to R.drawable.file_h,
        "cpp" to R.drawable.file_cpp,
        "sh" to R.drawable.file_sh,
        "bat" to R.drawable.file_bat,
        "ini" to R.drawable.file_ini,
        "sql" to R.drawable.file_sql,
        "db" to R.drawable.file_db,
        "sqlite" to R.drawable.file_sqlite,
        "apk" to R.drawable.file_apk,
        "iso" to R.drawable.file_iso,
        "exe" to R.drawable.file_exe,
        "deb" to R.drawable.file_deb,
        "rpm" to R.drawable.file_rpm,
        "dmg" to R.drawable.file_dmg,
        "bin" to R.drawable.file_bin,
        "torrent" to R.drawable.file_torrent,
        "ttf" to R.drawable.file_ttf,
        "epub" to R.drawable.file_epub,
        "mobi" to R.drawable.file_mobi,
    )

    fun iconFor(file: File): Int = iconFor(file, file.isDirectory)

    fun iconFor(file: File, isDirectory: Boolean): Int {
        if (isDirectory) return folderIconFor(file.name)
        return iconForExtension(file.extension)
    }

    fun iconForResolvedExtension(file: File, resolvedExtension: String): Int {
        if (file.isDirectory) return folderIconFor(file.name)
        return iconForExtension(resolvedExtension)
    }

    private fun iconForExtension(extension: String): Int =
        extensionMap[extension.lowercase()] ?: R.drawable.file_unknown

    private fun folderIconFor(name: String): Int = when (name.lowercase()) {
        "download", "downloads" -> R.drawable.folder_downloads
        "documents", "documentos" -> R.drawable.folder_documents
        "dcim", "pictures", "imagens", "images" -> R.drawable.folder_images
        "music", "música", "musicas", "músicas", "audio" -> R.drawable.folder_music
        "movies", "videos", "vídeos" -> R.drawable.folder_videos
        else -> R.drawable.folder
    }
}
