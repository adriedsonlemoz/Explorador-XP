package com.exploradorxp.app

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileRepository(private val prefs: PreferencesStore) {
    val root: File = Environment.getExternalStorageDirectory()

    suspend fun listDirectory(
        directory: File,
        query: String,
        sortMode: SortMode,
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val favorites = prefs.favorites()
        val filtered = directory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { FileItem(it, FileIconMapper.iconFor(it), it.absolutePath in favorites) }
            .toList()
        sort(filtered, sortMode)
    }

    suspend fun favoriteItems(query: String, sortMode: SortMode): List<FileItem> = withContext(Dispatchers.IO) {
        val favorites = prefs.favorites()
        val items = favorites.asSequence()
            .map(::File)
            .filter(File::exists)
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { FileItem(it, FileIconMapper.iconFor(it), true) }
            .toList()
        sort(items, sortMode)
    }

    suspend fun recentItems(query: String, sortMode: SortMode): List<FileItem> = withContext(Dispatchers.IO) {
        val favoritePaths = prefs.favorites()
        val items = prefs.recents().asSequence()
            .map(::File)
            .filter(File::exists)
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { FileItem(it, FileIconMapper.iconFor(it), it.absolutePath in favoritePaths) }
            .toList()
        if (sortMode == SortMode.DATE) items.sortedByDescending { it.lastModified } else items
    }

    suspend fun createFolder(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(name.isNotBlank()) { "Informe um nome para a pasta." }
            require('/' !in name && '\\' !in name) { "O nome não pode conter separadores de caminho." }
            val folder = File(parent, name.trim())
            require(!folder.exists()) { "Já existe um item com esse nome." }
            check(folder.mkdirs()) { "Não foi possível criar a pasta." }
            folder
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(newName.isNotBlank()) { "Informe um novo nome." }
            require('/' !in newName && '\\' !in newName) { "O nome não pode conter separadores de caminho." }
            val target = File(file.parentFile, newName.trim())
            require(!target.exists()) { "Já existe um item com esse nome." }
            check(file.renameTo(target)) { "Não foi possível renomear o item." }
            target
        }
    }

    suspend fun delete(files: List<File>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            files.forEach { file ->
                check(deleteRecursively(file)) { "Não foi possível excluir ${file.name}." }
            }
        }
    }

    suspend fun paste(clipboard: ClipboardState, destination: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            clipboard.files.forEach { source ->
                require(source.exists()) { "${source.name} não existe mais." }
                if (clipboard.mode == ClipboardMode.CUT && source.parentFile?.canonicalPath == destination.canonicalPath) {
                    return@forEach
                }
                if (source.isDirectory) {
                    val sourcePath = source.canonicalPath + File.separator
                    val destinationPath = destination.canonicalPath + File.separator
                    require(!destinationPath.startsWith(sourcePath)) { "Não é possível copiar uma pasta para dentro dela mesma." }
                }
                val target = uniqueTarget(destination, source.name)
                if (clipboard.mode == ClipboardMode.CUT && source.renameTo(target)) {
                    // Fast-path move on the same volume.
                } else {
                    copyRecursively(source, target)
                    if (clipboard.mode == ClipboardMode.CUT) {
                        check(deleteRecursively(source)) { "O item foi copiado, mas não foi possível remover a origem." }
                    }
                }
            }
        }
    }

    fun toggleFavorite(file: File): Boolean = prefs.toggleFavorite(file)
    fun addRecent(file: File) = prefs.addRecent(file)

    fun storageInfo(): StorageInfo = runCatching {
        val stat = StatFs(root.absolutePath)
        StorageInfo(totalBytes = stat.totalBytes, freeBytes = stat.availableBytes)
    }.getOrDefault(StorageInfo())

    private fun sort(items: List<FileItem>, sortMode: SortMode): List<FileItem> {
        val directoryFirst = compareByDescending<FileItem> { it.isDirectory }
        val detailComparator = when (sortMode) {
            SortMode.NAME -> compareBy<FileItem, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortMode.DATE -> compareByDescending<FileItem> { it.lastModified }
            SortMode.SIZE -> compareByDescending<FileItem> { it.size }
            SortMode.TYPE -> compareBy<FileItem> { it.extension }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }
        return items.sortedWith(directoryFirst.then(detailComparator))
    }

    private fun uniqueTarget(parent: File, originalName: String): File {
        var candidate = File(parent, originalName)
        if (!candidate.exists()) return candidate
        val dot = originalName.lastIndexOf('.')
        val base = if (dot > 0) originalName.substring(0, dot) else originalName
        val ext = if (dot > 0) originalName.substring(dot) else ""
        var index = 1
        while (candidate.exists()) {
            candidate = File(parent, "$base ($index)$ext")
            index++
        }
        return candidate
    }

    private fun copyRecursively(source: File, target: File) {
        if (source.isDirectory) {
            check(target.mkdirs() || target.isDirectory) { "Não foi possível criar ${target.name}." }
            source.listFiles().orEmpty().forEach { child ->
                copyRecursively(child, File(target, child.name))
            }
        } else {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            target.setLastModified(source.lastModified())
        }
    }

    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) file.listFiles().orEmpty().forEach { if (!deleteRecursively(it)) return false }
        return file.delete() || !file.exists()
    }
}
