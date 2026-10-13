package com.exploradorxp.app

/** Os filtros se aplicam ao conjunto limitado dos maiores arquivos retornados pela varredura. */
internal enum class LargeFileFilter { ALL, IMAGES, VIDEOS, APK, ARCHIVES, GAMES, OTHER }
internal object StorageDisplayRules {
    fun filterLargeFiles(files: List<StorageFileSummary>, filter: LargeFileFilter): List<StorageFileSummary> =
        files.filter { entry ->
            val type = FileTypeClassifier.storageCategory(entry.file.extension).first
            when (filter) {
                LargeFileFilter.ALL -> true
                LargeFileFilter.IMAGES -> type == "images"
                LargeFileFilter.VIDEOS -> type == "videos"
                LargeFileFilter.APK -> type == "apps"
                LargeFileFilter.ARCHIVES -> type == "archives"
                LargeFileFilter.GAMES -> type == "game_data"
                LargeFileFilter.OTHER -> type !in setOf("images", "videos", "apps", "archives", "game_data")
            }
        }.sortedWith(compareByDescending<StorageFileSummary> { it.bytes }.thenBy { it.file.name })
}
