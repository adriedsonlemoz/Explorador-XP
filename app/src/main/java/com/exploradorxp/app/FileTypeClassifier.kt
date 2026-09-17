package com.exploradorxp.app

import java.io.File

/** Fonte única para nomes legíveis de tipos e categorias usadas na análise de armazenamento. */
object FileTypeClassifier {
    private val labels = mapOf(
        "txt" to "Arquivo de texto", "log" to "Arquivo de log", "rtf" to "Documento RTF", "md" to "Markdown",
        "doc" to "Documento Word", "docx" to "Documento Word", "odt" to "Documento OpenDocument",
        "xls" to "Planilha Excel", "xlsx" to "Planilha Excel", "ods" to "Planilha OpenDocument", "csv" to "Planilha CSV",
        "ppt" to "Apresentação PowerPoint", "pptx" to "Apresentação PowerPoint", "odp" to "Apresentação OpenDocument",
        "pdf" to "Documento PDF",
        "jpg" to "Imagem JPEG", "jpeg" to "Imagem JPEG", "png" to "Imagem PNG", "gif" to "Imagem GIF",
        "bmp" to "Imagem BMP", "webp" to "Imagem WebP", "svg" to "Imagem SVG", "tif" to "Imagem TIFF",
        "tiff" to "Imagem TIFF", "heic" to "Imagem HEIC", "psd" to "Imagem Photoshop", "ai" to "Arquivo Illustrator",
        "eps" to "Imagem EPS", "ico" to "Ícone",
        "mp3" to "Áudio MP3", "wav" to "Áudio WAV", "ogg" to "Áudio OGG", "flac" to "Áudio FLAC",
        "aac" to "Áudio AAC", "m4a" to "Áudio M4A",
        "mp4" to "Vídeo MP4", "avi" to "Vídeo AVI", "mkv" to "Vídeo MKV", "mov" to "Vídeo MOV",
        "webm" to "Vídeo WebM", "3gp" to "Vídeo 3GP",
        "zip" to "Arquivo ZIP", "rar" to "Arquivo RAR", "7z" to "Arquivo 7-Zip", "tar" to "Arquivo TAR",
        "gz" to "Arquivo GZip", "bz2" to "Arquivo BZip2", "xz" to "Arquivo XZ",
        "html" to "Documento HTML", "htm" to "Documento HTML", "css" to "Folha de estilo CSS",
        "js" to "Código JavaScript", "mjs" to "Módulo JavaScript", "cjs" to "Módulo CommonJS",
        "ts" to "Código TypeScript", "tsx" to "Código TSX", "jsx" to "Código JSX",
        "json" to "Arquivo JSON", "xml" to "Arquivo XML", "yaml" to "Arquivo YAML",
        "yml" to "Arquivo YAML", "php" to "Código PHP", "py" to "Código Python", "java" to "Código Java",
        "kt" to "Código Kotlin", "kts" to "Script Kotlin", "c" to "Código C", "h" to "Cabeçalho C/C++", "cpp" to "Código C++",
        "sh" to "Script Shell", "bat" to "Script do Windows", "ini" to "Arquivo de configuração",
        "properties" to "Arquivo de propriedades", "toml" to "Arquivo TOML", "gradle" to "Script Gradle",
        "sql" to "Script SQL", "db" to "Banco de dados", "sqlite" to "Banco de dados SQLite",
        "apk" to "Aplicativo Android (APK)", "iso" to "Imagem de disco ISO", "exe" to "Executável do Windows",
        "deb" to "Pacote Debian", "rpm" to "Pacote RPM", "dmg" to "Imagem de disco macOS", "bin" to "Arquivo binário",
        "torrent" to "Arquivo Torrent", "ttf" to "Fonte TrueType", "epub" to "Livro EPUB", "mobi" to "Livro MOBI",
    )

    private val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tif", "tiff", "heic", "psd", "ai", "eps", "ico")
    private val videoExtensions = setOf("mp4", "avi", "mkv", "mov", "webm", "3gp")
    private val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "aac", "m4a")
    private val documentExtensions = setOf("txt", "log", "rtf", "md", "doc", "docx", "odt", "xls", "xlsx", "ods", "csv", "ppt", "pptx", "odp", "pdf", "html", "htm", "epub", "mobi")
    private val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

    fun labelFor(file: File, isDirectory: Boolean = file.isDirectory): String =
        if (isDirectory) "Pasta de arquivos" else labelForExtension(file.extension)

    fun labelForExtension(extension: String): String {
        val ext = extension.lowercase()
        return labels[ext] ?: if (ext.isBlank()) "Arquivo sem extensão" else "Arquivo ${ext.uppercase()}"
    }

    fun storageCategory(extension: String): Pair<String, String> {
        val ext = extension.lowercase()
        return when {
            ext in imageExtensions -> "images" to "Imagens"
            ext in videoExtensions -> "videos" to "Vídeos"
            ext in audioExtensions -> "audio" to "Áudio"
            ext in documentExtensions -> "documents" to "Documentos"
            ext == "apk" -> "apps" to "Aplicativos/APK"
            ext in archiveExtensions -> "archives" to "Compactados"
            else -> "other" to "Outros"
        }
    }
}
