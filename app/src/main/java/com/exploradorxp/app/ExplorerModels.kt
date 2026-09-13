package com.exploradorxp.app

import java.io.File

enum class ViewMode { LIST, GRID }
enum class ExplorerTab { FILES, DOWNLOADS, FAVORITES }
enum class SortMode { NAME, DATE, SIZE, TYPE }
enum class ClipboardMode { COPY, CUT }

data class FileItem(
    val file: File,
    val iconRes: Int,
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
) {
    val name: String get() = file.name.ifBlank { file.absolutePath }
    val isDirectory: Boolean get() = file.isDirectory
    val size: Long get() = if (file.isFile) file.length() else 0L
    val lastModified: Long get() = file.lastModified()
    val extension: String get() = file.extension.lowercase()
}

data class ClipboardState(
    val files: List<File>,
    val mode: ClipboardMode,
)

data class StorageLocation(
    val label: String,
    val root: File,
    val removable: Boolean = false,
)

data class StorageInfo(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
}

data class ExplorerUiState(
    val currentDir: File,
    val items: List<FileItem> = emptyList(),
    val loading: Boolean = false,
    val query: String = "",
    val searchVisible: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortMode: SortMode = SortMode.NAME,
    val tab: ExplorerTab = ExplorerTab.FILES,
    val selectedPaths: Set<String> = emptySet(),
    val clipboard: ClipboardState? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val showHidden: Boolean = false,
    val storageInfo: StorageInfo = StorageInfo(),
    val storageLocations: List<StorageLocation> = emptyList(),
)

sealed interface ExplorerEvent {
    data class OpenFile(val file: File) : ExplorerEvent
    data class ShareFiles(val files: List<File>) : ExplorerEvent
    data class ShowMessage(val message: String) : ExplorerEvent
}
