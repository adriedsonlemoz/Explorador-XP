package com.exploradorxp.app

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

class FileRepository(
    private val context: Context,
    private val prefs: PreferencesStore,
) {
    val root: File = Environment.getExternalStorageDirectory()
    val downloads: File = File(root, Environment.DIRECTORY_DOWNLOADS)

    @Volatile
    private var cachedStorageLocations: List<StorageLocation>? = null

    /**
     * Lê o diretório apenas uma vez e captura todos os metadados necessários para a UI.
     * Busca, filtro e ordenação são aplicados depois sobre esse snapshot em memória.
     */
    suspend fun directorySnapshot(directory: File): List<FileItem> = withContext(Dispatchers.IO) {
        require(directory.exists() && directory.isDirectory) { "A pasta não está mais disponível." }
        val favorites = prefs.favorites()
        directory.listFiles()
            .orEmpty()
            .map { file -> toFileItem(file, file.absolutePath in favorites) }
    }

    /** Snapshot dos favoritos ainda existentes. Também captura metadados fora da UI. */
    suspend fun favoriteSnapshot(): List<FileItem> = withContext(Dispatchers.IO) {
        prefs.favorites().asSequence()
            .map(::File)
            .filter(File::exists)
            .map { file -> toFileItem(file, true) }
            .toList()
    }

    suspend fun recentItems(query: String, sortMode: SortMode): List<FileItem> = withContext(Dispatchers.IO) {
        val favoritePaths = prefs.favorites()
        val items = prefs.recents().asSequence()
            .map(::File)
            .filter(File::exists)
            .map { toFileItem(it, it.absolutePath in favoritePaths) }
            .toList()
        ExplorerItemTransforms.apply(
            snapshot = items,
            query = query,
            sortMode = sortMode,
            showHidden = true,
            foldersFirst = false,
        )
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

    suspend fun delete(files: List<File>, onProgress: (TransferProgress) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val total = files.sumOf { countEntries(it) }.coerceAtLeast(1)
            var done = 0
            val ticker = ProgressTicker(total, onProgress)
            files.forEach { file ->
                coroutineContext.ensureActive()
                check(deleteRecursively(file) { name ->
                    done++
                    ticker.report(done, name)
                }) { "Não foi possível excluir ${file.name}." }
            }
            ticker.reportFinal(done, "")
        }
    }

    suspend fun paste(
        clipboard: ClipboardState,
        destination: File,
        onProgress: (TransferProgress) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val total = clipboard.files.sumOf { countEntries(it) }.coerceAtLeast(1)
            var done = 0
            val ticker = ProgressTicker(total, onProgress)
            clipboard.files.forEach { source ->
                coroutineContext.ensureActive()
                require(source.exists()) { "${source.name} não existe mais." }
                if (clipboard.mode == ClipboardMode.CUT && sameAbsolutePath(source.parentFile, destination)) {
                    return@forEach
                }
                if (source.isDirectory) {
                    val sourcePath = normalizedAbsolutePath(source) + File.separator
                    val destinationPath = normalizedAbsolutePath(destination) + File.separator
                    require(!destinationPath.startsWith(sourcePath)) { "Não é possível copiar uma pasta para dentro dela mesma." }
                }
                val target = uniqueTarget(destination, source.name)
                if (clipboard.mode == ClipboardMode.CUT && source.renameTo(target)) {
                    // Fast-path move no mesmo volume: sem cópia byte a byte, então
                    // o progresso avança de uma vez para todo o subconteúdo movido.
                    done += countEntries(target)
                    ticker.report(done, target.name)
                } else {
                    copyRecursively(source, target) { name ->
                        done++
                        ticker.report(done, name)
                    }
                    if (clipboard.mode == ClipboardMode.CUT) {
                        check(deleteRecursively(source)) { "O item foi copiado, mas não foi possível remover a origem." }
                    }
                }
            }
            ticker.reportFinal(done, "")
        }
    }

    fun toggleFavorite(file: File): Boolean = prefs.toggleFavorite(file)
    fun addRecent(file: File) = prefs.addRecent(file)

    /**
     * Localizações de armazenamento são estáveis durante a sessão. Fazemos a descoberta
     * uma única vez para evitar canonicalPath/exists/isDirectory repetidos a cada pasta.
     */
    fun storageLocations(): List<StorageLocation> {
        cachedStorageLocations?.let { return it }
        return synchronized(this) {
            cachedStorageLocations?.let { return@synchronized it }
            val locations = mutableListOf(StorageLocation("Armazenamento interno", root, removable = false))
            val seen = mutableSetOf(normalizedAbsolutePath(root))

            context.getExternalFilesDirs(null)
                .filterNotNull()
                .mapNotNull(::volumeRootFromAppExternalDir)
                .forEach { candidate ->
                    val normalized = normalizedAbsolutePath(candidate)
                    if (normalized !in seen && candidate.exists() && candidate.isDirectory) {
                        seen += normalized
                        val index = locations.count { it.removable } + 1
                        locations += StorageLocation(
                            label = if (index == 1) "Cartão SD" else "Cartão SD $index",
                            root = candidate,
                            removable = true,
                        )
                    }
                }

            locations.toList().also { cachedStorageLocations = it }
        }
    }

    fun storageRootFor(directory: File): File {
        val directoryPath = normalizedAbsolutePath(directory)
        return storageLocations()
            .map { it.root }
            .sortedByDescending { normalizedAbsolutePath(it).length }
            .firstOrNull { candidate ->
                val rootPath = normalizedAbsolutePath(candidate)
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

    private fun normalizedAbsolutePath(file: File): String =
        file.absolutePath.let { path -> if (path.length > 1) path.trimEnd(File.separatorChar) else path }

    private fun sameAbsolutePath(a: File?, b: File): Boolean =
        a != null && normalizedAbsolutePath(a) == normalizedAbsolutePath(b)

    private fun toFileItem(file: File, favorite: Boolean): FileItem {
        // Toda leitura de metadados acontece aqui, em Dispatchers.IO, uma única vez.
        val isDirectory = file.isDirectory
        val name = file.name.ifBlank { file.absolutePath }
        val extension = if (isDirectory) "" else file.extension.lowercase()
        val size = if (isDirectory) 0L else file.length()
        val modifiedAt = file.lastModified()
        val hidden = name.startsWith('.') || runCatching { file.isHidden }.getOrDefault(false)
        return FileItem(
            file = file,
            icon = FileIconMapper.iconFor(file, isDirectory),
            name = name,
            isDirectory = isDirectory,
            isHidden = hidden,
            size = size,
            modifiedAt = modifiedAt,
            extension = extension,
            listDetailText = FileDisplayFormatter.listDetail(modifiedAt, size, isDirectory),
            gridDetailText = FileDisplayFormatter.gridDetail(modifiedAt),
            isFavorite = favorite,
        )
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

    private suspend fun copyRecursively(source: File, target: File, onEntry: (String) -> Unit) {
        coroutineContext.ensureActive()
        if (source.isDirectory) {
            check(target.mkdirs() || target.isDirectory) { "Não foi possível criar ${target.name}." }
            onEntry(target.name)
            source.listFiles().orEmpty().forEach { child ->
                copyRecursively(child, File(target, child.name), onEntry)
            }
        } else {
            target.parentFile?.mkdirs()
            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            target.setLastModified(source.lastModified())
            onEntry(target.name)
        }
    }

    private fun deleteRecursively(file: File, onEntry: (String) -> Unit = {}): Boolean {
        if (file.isDirectory) file.listFiles().orEmpty().forEach { if (!deleteRecursively(it, onEntry)) return false }
        val removed = file.delete() || !file.exists()
        if (removed) onEntry(file.name)
        return removed
    }

    /** Conta pastas e arquivos (incluindo a própria raiz) para estimar o total de uma transferência. */
    private fun countEntries(file: File): Int {
        return if (file.isDirectory) {
            1 + file.listFiles().orEmpty().sumOf { countEntries(it) }
        } else {
            1
        }
    }

    /**
     * Agrupa atualizações de progresso para não sobrecarregar a UI a cada arquivo processado
     * em transferências grandes: emite no máximo a cada ~80 ms, sempre garantindo a emissão final.
     */
    private class ProgressTicker(
        private val total: Int,
        private val onProgress: (TransferProgress) -> Unit,
    ) {
        private var lastEmitMs = 0L

        fun report(done: Int, currentName: String) {
            val now = System.currentTimeMillis()
            if (now - lastEmitMs >= 80L || done >= total) {
                lastEmitMs = now
                onProgress(TransferProgress(done.coerceAtMost(total), total, currentName))
            }
        }

        fun reportFinal(done: Int, currentName: String) {
            onProgress(TransferProgress(done.coerceAtMost(total), total, currentName))
        }
    }
}
