package com.exploradorxp.app

import androidx.compose.runtime.Immutable
import java.io.File

enum class ViewMode { LIST, GRID }
enum class ExplorerTab { FILES, DOWNLOADS, FAVORITES }
enum class SortMode { NAME, DATE, SIZE, TYPE }
enum class ClipboardMode { COPY, CUT }
enum class TransferKind { COPY, MOVE, DELETE }
enum class TransferQueueItemStatus { PENDING, RUNNING, DONE, SKIPPED, ERROR }

@Immutable
data class TransferQueueItem(
    val path: String,
    val name: String,
    val status: TransferQueueItemStatus = TransferQueueItemStatus.PENDING,
)

enum class SearchItemKind { ALL, FILES, FOLDERS }
enum class SearchFileType { ALL, IMAGES, VIDEOS, AUDIO, DOCUMENTS, APKS, ARCHIVES, OTHER }
enum class SearchDateRange { ANY, TODAY, LAST_7_DAYS, LAST_30_DAYS }

@Immutable
data class AdvancedSearchFilters(
    val includeSubfolders: Boolean = false,
    val itemKind: SearchItemKind = SearchItemKind.ALL,
    val fileType: SearchFileType = SearchFileType.ALL,
    val extension: String = "",
    val minSizeBytes: Long? = null,
    val maxSizeBytes: Long? = null,
    val dateRange: SearchDateRange = SearchDateRange.ANY,
) {
    val hasRestrictions: Boolean
        get() = includeSubfolders ||
            itemKind != SearchItemKind.ALL ||
            fileType != SearchFileType.ALL ||
            extension.isNotBlank() ||
            minSizeBytes != null ||
            maxSizeBytes != null ||
            dateRange != SearchDateRange.ANY
}

@Immutable
data class AdvancedSearchState(
    val active: Boolean = false,
    val running: Boolean = false,
    val scannedItems: Int = 0,
    val matchedItems: Int = 0,
    val results: List<FileItem> = emptyList(),
    val filters: AdvancedSearchFilters = AdvancedSearchFilters(),
    val error: String? = null,
    val cancelled: Boolean = false,
)

/** Progresso relatado pelo [FileRepository] durante cópia, mover ou exclusão. */
data class TransferProgress(
    val done: Int,
    val total: Int,
    val currentName: String,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L,
    val currentItemIndex: Int = 0,
    val currentItemCount: Int = 0,
    val currentItemDone: Int = 0,
    val currentItemTotal: Int = 0,
    val currentItemBytesDone: Long = 0L,
    val currentItemBytesTotal: Long = 0L,
    val queueItems: List<TransferQueueItem> = emptyList(),
)

enum class ConflictDecision { REPLACE, SKIP, KEEP_BOTH }

data class ConflictResolution(
    val decision: ConflictDecision,
    val applyToAll: Boolean = false,
)

@Immutable
data class TransferConflict(
    val sourcePath: String,
    val targetPath: String,
    val sourceIsDirectory: Boolean,
    val targetIsDirectory: Boolean,
) {
    val sourceName: String get() = File(sourcePath).name
    val targetName: String get() = File(targetPath).name
}

/** Estado exibido na UI enquanto uma transferência está em andamento. */
@Immutable
data class TransferState(
    val kind: TransferKind,
    val done: Int,
    val total: Int,
    val currentName: String,
    val bytesDone: Long = 0L,
    val bytesTotal: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val etaSeconds: Long? = null,
    val isPaused: Boolean = false,
    val currentItemIndex: Int = 0,
    val currentItemCount: Int = 0,
    val currentItemDone: Int = 0,
    val currentItemTotal: Int = 0,
    val currentItemBytesDone: Long = 0L,
    val currentItemBytesTotal: Long = 0L,
    val queueItems: List<TransferQueueItem> = emptyList(),
) {
    val fraction: Float
        get() = if (bytesTotal > 0L) {
            (bytesDone.toDouble() / bytesTotal.toDouble()).toFloat().coerceIn(0f, 1f)
        } else if (total > 0) {
            (done.toFloat() / total).coerceIn(0f, 1f)
        } else 0f

    val currentItemFraction: Float
        get() = if (currentItemBytesTotal > 0L) {
            (currentItemBytesDone.toDouble() / currentItemBytesTotal.toDouble()).toFloat().coerceIn(0f, 1f)
        } else if (currentItemTotal > 0) {
            (currentItemDone.toFloat() / currentItemTotal).coerceIn(0f, 1f)
        } else 0f
}

@Immutable
data class FileItem(
    val file: File,
    val iconRes: Int,
    val name: String,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val size: Long,
    val modifiedAt: Long,
    val extension: String,
    val typeLabel: String,
    val listDetailText: String,
    val gridDetailText: String,
    val isFavorite: Boolean = false,
) {
    // Caminho absoluto não consulta o sistema de arquivos e serve como chave estável da UI.
    val path: String get() = file.absolutePath

    // Mantido com o mesmo nome usado por telas secundárias.
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

@Immutable
data class StorageInfo(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
}

/** Item mantido pela Lixeira. O arquivo físico fica em uma pasta oculta gerenciada pelo app. */
data class TrashItem(
    val id: String,
    val trashedFile: File,
    val originalPath: String,
    val originalName: String,
    val deletedAt: Long,
    val size: Long,
    val isDirectory: Boolean,
    val typeLabel: String,
    val iconRes: Int,
) {
    val name: String
        get() = originalName.ifBlank { File(originalPath).name }.ifBlank { trashedFile.name }
}


@Immutable
data class TrashUiState(
    val items: List<TrashItem> = emptyList(),
    val loading: Boolean = false,
) {
    val hasItems: Boolean get() = items.isNotEmpty()
}

data class StorageCategorySummary(
    val key: String,
    val label: String,
    val bytes: Long,
    val fileCount: Int,
)

data class StorageFolderSummary(
    val folder: File,
    val bytes: Long,
    val fileCount: Int,
)

data class StorageFileSummary(
    val file: File,
    val bytes: Long,
    val typeLabel: String,
)

data class StorageAnalysis(
    val root: File,
    val storageInfo: StorageInfo,
    val categories: List<StorageCategorySummary>,
    val topFolders: List<StorageFolderSummary>,
    val largeFiles: List<StorageFileSummary>,
    val scannedFiles: Int,
    val scannedBytes: Long,
    val inaccessibleDirectories: Int = 0,
    val trashBytes: Long = 0L,
    val trashItemCount: Int = 0,
    val completedAt: Long,
)

@Immutable
data class StorageScanState(
    val analyzing: Boolean = false,
    val scannedFiles: Int = 0,
    val analysis: StorageAnalysis? = null,
    val error: String? = null,
)

@Immutable
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
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val showHidden: Boolean = false,
    val storageInfo: StorageInfo = StorageInfo(),
    val storageLocations: List<StorageLocation> = emptyList(),
)

sealed interface ExplorerEvent {
    data class OpenFile(
        val file: File,
        val folderImages: List<File> = emptyList(),
    ) : ExplorerEvent
    data class ShareFiles(val files: List<File>) : ExplorerEvent
    data class ShowMessage(val message: String) : ExplorerEvent
}
