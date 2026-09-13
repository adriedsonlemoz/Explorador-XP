package com.exploradorxp.app

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes

class FileRepository(
    private val context: Context,
    private val prefs: PreferencesStore,
) {
    val root: File = Environment.getExternalStorageDirectory()
    val downloads: File = File(root, Environment.DIRECTORY_DOWNLOADS)

    suspend fun listDirectory(
        directory: File,
        query: String,
        sortMode: SortMode,
        showHidden: Boolean,
        foldersFirst: Boolean,
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val favorites = prefs.favorites()
        val filtered = directory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { showHidden || !isHidden(it) }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { toFileItem(it, it.absolutePath in favorites) }
            .toList()
        sort(filtered, sortMode, foldersFirst)
    }

    suspend fun favoriteItems(
        query: String,
        sortMode: SortMode,
        showHidden: Boolean,
        foldersFirst: Boolean,
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val favorites = prefs.favorites()
        val items = favorites.asSequence()
            .map(::File)
            .filter(File::exists)
            .filter { showHidden || !isHidden(it) }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { toFileItem(it, true) }
            .toList()
        sort(items, sortMode, foldersFirst)
    }

    suspend fun recentItems(query: String, sortMode: SortMode): List<FileItem> = withContext(Dispatchers.IO) {
        val favoritePaths = prefs.favorites()
        val items = prefs.recents().asSequence()
            .map(::File)
            .filter(File::exists)
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .map { toFileItem(it, it.absolutePath in favoritePaths) }
            .toList()
        if (sortMode == SortMode.DATE) items.sortedByDescending { it.createdAt } else items
    }

    fun showHidden(): Boolean = prefs.showHidden()

    fun setShowHidden(show: Boolean) = prefs.setShowHidden(show)

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

    fun storageLocations(): List<StorageLocation> {
        val locations = mutableListOf(StorageLocation("Armazenamento interno", root, removable = false))
        val seen = mutableSetOf(canonicalOrAbsolute(root))

        context.getExternalFilesDirs(null)
            .filterNotNull()
            .mapNotNull(::volumeRootFromAppExternalDir)
            .forEach { candidate ->
                val canonical = canonicalOrAbsolute(candidate)
                if (canonical !in seen && candidate.exists() && candidate.isDirectory) {
                    seen += canonical
                    val index = locations.count { it.removable } + 1
                    locations += StorageLocation(
                        label = if (index == 1) "Cartão SD" else "Cartão SD $index",
                        root = candidate,
                        removable = true,
                    )
                }
            }

        return locations
    }

    fun storageRootFor(directory: File): File {
        val directoryPath = canonicalOrAbsolute(directory)
        return storageLocations()
            .map { it.root }
            .sortedByDescending { canonicalOrAbsolute(it).length }
            .firstOrNull { candidate ->
                val rootPath = canonicalOrAbsolute(candidate)
                directoryPath == rootPath || directoryPath.startsWith(rootPath + File.separator)
            }
            ?: root
    }

    fun storageInfo(directory: File = root): StorageInfo = runCatching {
        val storageRoot = storageRootFor(directory)
        val stat = StatFs(storageRoot.absolutePath)
        StorageInfo(totalBytes = stat.totalBytes, freeBytes = stat.availableBytes)
    }.getOrDefault(StorageInfo())

    private fun volumeRootFromAppExternalDir(appDir: File): File? {
        val normalized = appDir.absolutePath.replace('\\', '/')
        val marker = "/Android/"
        val index = normalized.indexOf(marker)
        if (index <= 0) return null
        return File(normalized.substring(0, index))
    }

    private fun canonicalOrAbsolute(file: File): String =
        runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)

    private fun toFileItem(file: File, favorite: Boolean): FileItem {
        return FileItem(
            file = file,
            iconRes = FileIconMapper.iconFor(file),
            isFavorite = favorite,
            createdAt = creationTime(file),
        )
    }

    private fun creationTime(file: File): Long {
        return runCatching {
            Files.readAttributes(
                file.toPath(),
                BasicFileAttributes::class.java,
                LinkOption.NOFOLLOW_LINKS,
            ).creationTime().toMillis()
        }.getOrNull()?.takeIf { it > 0L } ?: file.lastModified()
    }

    private fun isHidden(file: File): Boolean = file.name.startsWith('.') || runCatching { file.isHidden }.getOrDefault(false)

    private fun sort(items: List<FileItem>, sortMode: SortMode, foldersFirst: Boolean): List<FileItem> {
        val detailComparator = when (sortMode) {
            SortMode.NAME -> compareBy<FileItem, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortMode.DATE -> compareByDescending<FileItem> { it.createdAt }
            SortMode.SIZE -> compareByDescending<FileItem> { it.size }
            SortMode.TYPE -> compareBy<FileItem> { it.extension }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }
        return if (foldersFirst) {
            items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.then(detailComparator))
        } else {
            items.sortedWith(detailComparator)
        }
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
