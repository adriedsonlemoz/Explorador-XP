package com.exploradorxp.app

/** Configurações compartilhadas entre as galerias de fotos e vídeos. */
internal enum class MediaGallerySort(val label: String) {
    RECENT("Mais recentes"), OLDEST("Mais antigos"), LARGEST("Maior tamanho"),
    SMALLEST("Menor tamanho"), NAME("Nome A–Z")
}

internal enum class MediaGalleryOrigin(val label: String) {
    ALL("Todas as pastas"), CAMERA("Câmera"), SCREENSHOTS("Capturas de tela"),
    DOWNLOADS("Downloads"), OTHER("Outras pastas")
}

/** Apenas classifica pela pasta: nunca acessa dados privados de outros aplicativos. */
internal object MediaGalleryRules {
    fun isInDirectory(path: String, directory: String): Boolean =
        java.io.File(path).parentFile?.absolutePath == java.io.File(directory).absolutePath


    fun matches(path: String, origin: MediaGalleryOrigin): Boolean {
        if (origin == MediaGalleryOrigin.ALL) return true
        val normalized = "/" + path.replace('\\', '/').lowercase().trim('/') + "/"
        val camera = normalized.contains("/dcim/camera/") || normalized.contains("/camera/")
        val screenshots = normalized.contains("/screenshots/") || normalized.contains("/screen captures/")
        val downloads = normalized.contains("/download/") || normalized.contains("/downloads/")
        return when (origin) {
            MediaGalleryOrigin.ALL -> true
            MediaGalleryOrigin.CAMERA -> camera
            MediaGalleryOrigin.SCREENSHOTS -> screenshots
            MediaGalleryOrigin.DOWNLOADS -> downloads
            MediaGalleryOrigin.OTHER -> !camera && !screenshots && !downloads
        }
    }
}
