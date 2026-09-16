package com.exploradorxp.app

import java.io.File

enum class ViewMode { LIST, GRID }
enum class ExplorerTab { FILES, DOWNLOADS, FAVORITES }
enum class SortMode { NAME, DATE, SIZE, TYPE }
enum class ClipboardMode { COPY, CUT }
enum class TransferKind { COPY, MOVE, DELETE }

/** Progresso relatado pelo [FileRepository] durante cópia, mover ou exclusão. */
data class TransferProgress(
    val done: Int,
    val total: Int,
    val currentName: String,
)

/** Estado exibido na UI enquanto uma transferência está em andamento. */
data class TransferState(
    val kind: TransferKind,
    val done: Int,
    val total: Int,
    val currentName: String,
) {
    val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
}

data class FileItem(
    val file: File,
    val iconRes: Int,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedAt: Long,
    val extension: String,
    val isFavorite: Boolean = false,
) {
    // Mantido com o mesmo nome usado pela interface para evitar leituras extras do sistema de arquivos.
    val createdAt: Long get() = modifiedAt
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
    val foldersFirst: Boolean = true,
    val tab: ExplorerTab = ExplorerTab.FILES,
    val selectedPaths: Set<String> = emptySet(),
    val clipboard: ClipboardState? = null,
    val transfer: TransferState? = null,
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
