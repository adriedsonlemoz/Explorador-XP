package com.exploradorxp.app

import java.io.File

internal data class ArchiveBrowserItem(
    val path: String,
    val name: String,
    val directory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val modifiedAt: Long,
    val source: ArchiveEntryInfo?,
    val descendantFiles: Int = 0,
    val descendantDirectories: Int = 0,
)

internal data class ArchiveBrowserIndex(
    val itemsByPath: Map<String, ArchiveBrowserItem>,
    val childrenByParent: Map<String, List<ArchiveBrowserItem>>,
    val searchableItems: List<ArchiveBrowserItem>,
    val totalFiles: Int,
    val totalDirectories: Int,
)

private data class MutableArchiveStats(
    var files: Int = 0,
    var directories: Int = 0,
    var size: Long = 0L,
    var compressedSize: Long = 0L,
    var modifiedAt: Long = 0L,
)

internal fun buildArchiveBrowserIndex(entries: List<ArchiveEntryInfo>): ArchiveBrowserIndex {
    val sourcesByPath = linkedMapOf<String, ArchiveEntryInfo?>()
    val directories = linkedSetOf<String>()

    fun ensureDirectory(path: String, explicit: ArchiveEntryInfo? = null) {
        if (path.isBlank()) return
        directories += path
        if (path !in sourcesByPath || explicit != null) {
            sourcesByPath[path] = explicit ?: sourcesByPath[path]
        }
    }

    entries.forEach { entry ->
        val path = normalizeEntryPath(entry.path)
        if (path.isBlank()) return@forEach
        val parts = path.split('/').filter { it.isNotBlank() }
        val directoryDepth = if (entry.isDirectory) parts.size else (parts.size - 1).coerceAtLeast(0)
        for (depth in 1..directoryDepth) {
            val directoryPath = parts.take(depth).joinToString("/")
            ensureDirectory(directoryPath, entry.takeIf { entry.isDirectory && directoryPath == path })
        }
        if (!entry.isDirectory) sourcesByPath[path] = entry
    }

    val stats = hashMapOf<String, MutableArchiveStats>()
    stats[""] = MutableArchiveStats()
    directories.forEach { stats.getOrPut(it) { MutableArchiveStats() } }

    fun ancestors(parentPath: String): List<String> {
        if (parentPath.isBlank()) return listOf("")
        val parts = parentPath.split('/').filter { it.isNotBlank() }
        val result = ArrayList<String>(parts.size + 1)
        result += ""
        for (depth in 1..parts.size) result += parts.take(depth).joinToString("/")
        return result
    }

    directories.forEach { path ->
        val source = sourcesByPath[path]
        val parent = path.substringBeforeLast('/', "")
        ancestors(parent).forEach { ancestor ->
            val stat = stats.getOrPut(ancestor) { MutableArchiveStats() }
            stat.directories += 1
            stat.modifiedAt = maxOf(stat.modifiedAt, source?.modifiedAt ?: 0L)
        }
        source?.let { explicit ->
            val own = stats.getOrPut(path) { MutableArchiveStats() }
            own.modifiedAt = maxOf(own.modifiedAt, explicit.modifiedAt)
        }
    }

    sourcesByPath.forEach { (path, source) ->
        if (path in directories || source == null || source.isDirectory) return@forEach
        val parent = path.substringBeforeLast('/', "")
        ancestors(parent).forEach { ancestor ->
            val stat = stats.getOrPut(ancestor) { MutableArchiveStats() }
            stat.files += 1
            stat.size += source.size.coerceAtLeast(0L)
            stat.compressedSize += source.compressedSize.coerceAtLeast(0L)
            stat.modifiedAt = maxOf(stat.modifiedAt, source.modifiedAt)
        }
    }

    val items = linkedMapOf<String, ArchiveBrowserItem>()
    sourcesByPath.forEach { (path, source) ->
        val directory = path in directories
        val stat = stats[path]
        val item = if (directory) {
            ArchiveBrowserItem(
                path = path,
                name = path.substringAfterLast('/'),
                directory = true,
                size = stat?.size ?: 0L,
                compressedSize = stat?.compressedSize ?: 0L,
                modifiedAt = maxOf(source?.modifiedAt ?: 0L, stat?.modifiedAt ?: 0L),
                source = source,
                descendantFiles = stat?.files ?: 0,
                descendantDirectories = stat?.directories ?: 0,
            )
        } else {
            val entry = source ?: return@forEach
            ArchiveBrowserItem(
                path = path,
                name = path.substringAfterLast('/'),
                directory = false,
                size = entry.size,
                compressedSize = entry.compressedSize,
                modifiedAt = entry.modifiedAt,
                source = entry,
            )
        }
        items[path] = item
    }

    val children = linkedMapOf<String, MutableList<ArchiveBrowserItem>>()
    items.values.forEach { item ->
        val parent = item.path.substringBeforeLast('/', "")
        children.getOrPut(parent) { mutableListOf() } += item
    }

    return ArchiveBrowserIndex(
        itemsByPath = items,
        childrenByParent = children.mapValues { it.value.toList() },
        searchableItems = items.values.toList(),
        totalFiles = items.values.count { !it.directory },
        totalDirectories = items.values.count { it.directory },
    )
}

internal fun buildArchiveBrowserItems(
    index: ArchiveBrowserIndex,
    currentPath: String,
    query: String,
    sortMode: ArchiveSortMode,
    ascending: Boolean,
): List<ArchiveBrowserItem> {
    val normalizedCurrent = normalizeEntryPath(currentPath)
    val candidates = if (query.isBlank()) {
        index.childrenByParent[normalizedCurrent].orEmpty()
    } else {
        index.searchableItems.filter { item ->
            item.name.contains(query, ignoreCase = true) || item.path.contains(query, ignoreCase = true)
        }
    }

    val comparator = Comparator<ArchiveBrowserItem> { left, right ->
        if (left.directory != right.directory) {
            if (left.directory) -1 else 1
        } else {
            val primary = when (sortMode) {
                ArchiveSortMode.NAME -> left.name.compareTo(right.name, ignoreCase = true)
                ArchiveSortMode.SIZE -> left.size.compareTo(right.size)
                ArchiveSortMode.TYPE -> File(left.name).extension.compareTo(File(right.name).extension, ignoreCase = true)
                ArchiveSortMode.DATE -> left.modifiedAt.compareTo(right.modifiedAt)
            }
            val stable = if (primary != 0) primary else left.name.compareTo(right.name, ignoreCase = true)
            if (ascending) stable else -stable
        }
    }
    return candidates.sortedWith(comparator)
}
