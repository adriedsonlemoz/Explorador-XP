package com.exploradorxp.app

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.PriorityQueue
import java.util.UUID
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
     * A pasta interna da Lixeira nunca é exposta na navegação, mesmo com ocultos visíveis.
     */
    suspend fun directorySnapshot(directory: File): List<FileItem> = withContext(Dispatchers.IO) {
        require(directory.exists() && directory.isDirectory) { "A pasta não está mais disponível." }
        val favorites = prefs.favorites()
        val children = directory.listFiles().orEmpty()
        buildList(children.size) {
            children.forEachIndexed { index, file ->
                // listFiles()/metadados são I/O bloqueante. Em pastas muito grandes, checar o
                // cancelamento em lotes evita que uma navegação antiga continue competindo com
                // a pasta que o usuário acabou de abrir.
                if ((index and 63) == 0) coroutineContext.ensureActive()
                if (!isInternalTrashArtifact(file)) {
                    add(toFileItem(file, file.absolutePath in favorites))
                }
            }
        }
    }

    /** Snapshot dos favoritos ainda existentes. Também captura metadados fora da UI. */
    suspend fun favoriteSnapshot(): List<FileItem> = withContext(Dispatchers.IO) {
        prefs.favorites().asSequence()
            .map(::File)
            .filter(File::exists)
            .filterNot { isInsideManagedTrash(it) || isInternalTrashArtifact(it) }
            .map { file -> toFileItem(file, true) }
            .toList()
    }

    suspend fun recentItems(query: String, sortMode: SortMode): List<FileItem> = withContext(Dispatchers.IO) {
        val favoritePaths = prefs.favorites()
        val items = prefs.recents().asSequence()
            .map(::File)
            .filter(File::exists)
            .filterNot { isInsideManagedTrash(it) || isInternalTrashArtifact(it) }
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

    suspend fun createFile(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(name.isNotBlank()) { "Informe um nome para o arquivo." }
            require('/' !in name && '\\' !in name) { "O nome não pode conter separadores de caminho." }
            require('\u0000' !in name) { "O nome contém um caractere inválido." }
            val file = File(parent, name.trim())
            require(!file.exists()) { "Já existe um item com esse nome." }
            check(file.createNewFile()) { "Não foi possível criar o arquivo." }
            file
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(newName.isNotBlank()) { "Informe um novo nome." }
            require('/' !in newName && '\\' !in newName) { "O nome não pode conter separadores de caminho." }
            require(!isInsideManagedTrash(file)) { "Itens da Lixeira devem ser restaurados antes de renomear." }
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

    /** Move itens para a Lixeira oculta do mesmo volume sempre que possível. */
    suspend fun moveToTrash(files: List<File>, onProgress: (TransferProgress) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val validFiles = files.filter { it.exists() }
            require(validFiles.isNotEmpty()) { "Nenhum item disponível para mover para a Lixeira." }
            val total = validFiles.sumOf { countEntries(it) }.coerceAtLeast(1)
            var done = 0
            val ticker = ProgressTicker(total, onProgress)

            validFiles.forEach { source ->
                coroutineContext.ensureActive()
                require(!isInsideManagedTrash(source)) { "O item já está na Lixeira." }
                val entryCount = countEntries(source)
                val trashRoot = managedTrashRoot(storageRootFor(source))
                check(trashRoot.mkdirs() || trashRoot.isDirectory) { "Não foi possível preparar a Lixeira." }
                val container = File(trashRoot, "${System.currentTimeMillis()}-${UUID.randomUUID()}")
                check(container.mkdirs()) { "Não foi possível criar a entrada da Lixeira." }
                val target = File(container, source.name)
                val metadata = JSONObject()
                    .put("originalPath", source.absolutePath)
                    .put("originalName", source.name)
                    .put("deletedAt", System.currentTimeMillis())
                    .put("size", if (source.isFile) source.length() else 0L)
                    .put("isDirectory", source.isDirectory)
                File(container, TRASH_INFO_FILE).writeText(metadata.toString(), Charsets.UTF_8)

                if (source.renameTo(target)) {
                    done += entryCount
                    ticker.report(done, source.name)
                } else {
                    try {
                        copyRecursively(source, target) { name ->
                            done++
                            ticker.report(done, name)
                        }
                        check(deleteRecursively(source)) { "O item foi copiado para a Lixeira, mas não foi possível remover a origem." }
                    } catch (error: Throwable) {
                        deleteRecursively(container)
                        throw error
                    }
                }
            }
            ticker.reportFinal(done, "")
        }
    }

    suspend fun trashSnapshot(): List<TrashItem> = withContext(Dispatchers.IO) {
        storageLocations().flatMap { location ->
            val trashRoot = managedTrashRoot(location.root)
            trashRoot.listFiles().orEmpty().mapNotNull(::readTrashItem)
        }.sortedByDescending(TrashItem::deletedAt)
    }

    suspend fun restoreTrash(items: List<TrashItem>, onProgress: (TransferProgress) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = items.filter { it.trashedFile.exists() }
            require(existing.isNotEmpty()) { "Nenhum item disponível para restaurar." }
            val total = existing.sumOf { countEntries(it.trashedFile) }.coerceAtLeast(1)
            var done = 0
            val ticker = ProgressTicker(total, onProgress)

            existing.forEach { item ->
                coroutineContext.ensureActive()
                val original = File(item.originalPath)
                val parent = original.parentFile ?: root
                check(parent.mkdirs() || parent.isDirectory) { "Não foi possível recriar a pasta original." }
                val target = if (!original.exists()) original else uniqueRestoreTarget(parent, original.name)
                val entryCount = countEntries(item.trashedFile)
                if (item.trashedFile.renameTo(target)) {
                    done += entryCount
                    ticker.report(done, target.name)
                } else {
                    try {
                        copyRecursively(item.trashedFile, target) { name ->
                            done++
                            ticker.report(done, name)
                        }
                        check(deleteRecursively(item.trashedFile)) { "O item foi restaurado, mas não foi possível limpar a cópia da Lixeira." }
                    } catch (error: Throwable) {
                        // Evita deixar uma restauração parcial/duplicada quando o fallback falha.
                        deleteRecursively(target)
                        throw error
                    }
                }
                item.trashedFile.parentFile?.let { deleteRecursively(it) }
            }
            pruneEmptyManagedTrashRoots()
            ticker.reportFinal(done, "")
        }
    }

    suspend fun permanentlyDeleteTrash(items: List<TrashItem>, onProgress: (TransferProgress) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = items.filter { it.trashedFile.exists() }
            val total = existing.sumOf { countEntries(it.trashedFile) }.coerceAtLeast(1)
            var done = 0
            val ticker = ProgressTicker(total, onProgress)
            existing.forEach { item ->
                check(deleteRecursively(item.trashedFile) { name ->
                    done++
                    ticker.report(done, name)
                }) { "Não foi possível apagar ${item.name}." }
                item.trashedFile.parentFile?.let { deleteRecursively(it) }
            }
            pruneEmptyManagedTrashRoots()
            ticker.reportFinal(done, "")
        }
    }

    suspend fun emptyTrash(onProgress: (TransferProgress) -> Unit = {}): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val roots = storageLocations().map { managedTrashRoot(it.root) }.filter(File::exists)
            val entries = roots.flatMap { it.listFiles().orEmpty().toList() }
            val total = entries.sumOf(::countEntries).coerceAtLeast(1)
            var done = 0
            val ticker = ProgressTicker(total, onProgress)
            entries.forEach { entry ->
                coroutineContext.ensureActive()
                check(deleteRecursively(entry) { name ->
                    done++
                    ticker.report(done, name)
                }) { "Não foi possível remover ${entry.name} da Lixeira." }
            }
            // Remove também a pasta gerenciada vazia para garantir que nenhum resíduo físico
            // do Explorador XP permaneça após “Esvaziar Lixeira”. Ela será recriada quando necessário.
            roots.forEach { root -> if (root.exists() && root.listFiles().orEmpty().isEmpty()) root.delete() }
            ticker.reportFinal(done.coerceAtLeast(if (entries.isEmpty()) 1 else done), "")
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

    /** Varredura sob demanda; não é executada durante a abertura normal do Explorer. */
    suspend fun analyzeStorage(
        directory: File = root,
        onProgress: (Int) -> Unit = {},
    ): Result<StorageAnalysis> = withContext(Dispatchers.IO) {
        runCatching {
            val storageRoot = storageRootFor(directory)
            val info = storageInfo(storageRoot)
            val categories = linkedMapOf<String, MutableCategory>()
            val folders = linkedMapOf<String, MutableFolder>()
            val largest = PriorityQueue<StorageFileSummary>(compareBy { it.bytes })
            val queue = ArrayDeque<ScanNode>()
            storageRoot.listFiles().orEmpty().forEach { child ->
                if (!isInternalTrashArtifact(child)) {
                    queue.add(ScanNode(child, if (child.isDirectory) child else null))
                }
            }

            var scannedFiles = 0
            var scannedBytes = 0L
            while (queue.isNotEmpty()) {
                coroutineContext.ensureActive()
                val node = queue.removeFirst()
                val file = node.file
                if (file.isDirectory) {
                    file.listFiles().orEmpty().forEach { child ->
                        if (!isInternalTrashArtifact(child)) queue.add(ScanNode(child, node.topFolder ?: file))
                    }
                    continue
                }
                if (!file.isFile) continue

                val bytes = runCatching { file.length() }.getOrDefault(0L).coerceAtLeast(0L)
                scannedFiles++
                scannedBytes += bytes
                val (categoryKey, categoryLabel) = FileTypeClassifier.storageCategory(file.extension)
                val category = categories.getOrPut(categoryKey) { MutableCategory(categoryLabel) }
                category.bytes += bytes
                category.count++

                node.topFolder?.let { top ->
                    val folder = folders.getOrPut(top.absolutePath) { MutableFolder(top) }
                    folder.bytes += bytes
                    folder.count++
                }

                val summary = StorageFileSummary(file, bytes, FileTypeClassifier.labelFor(file, false))
                if (largest.size < LARGE_FILE_LIMIT) {
                    largest.add(summary)
                } else if (bytes > (largest.peek()?.bytes ?: 0L)) {
                    largest.poll()
                    largest.add(summary)
                }

                if (scannedFiles % 200 == 0) onProgress(scannedFiles)
            }
            onProgress(scannedFiles)

            val managedTrash = managedTrashRoot(storageRoot)
            val trashItems = managedTrash.listFiles().orEmpty().filter(File::isDirectory)
            val trashBytes = trashItems.sumOf { directorySizeBytes(it) }

            StorageAnalysis(
                root = storageRoot,
                storageInfo = info,
                categories = categories.map { (key, value) ->
                    StorageCategorySummary(key, value.label, value.bytes, value.count)
                }.sortedByDescending { it.bytes },
                topFolders = folders.values
                    .map { StorageFolderSummary(it.folder, it.bytes, it.count) }
                    .sortedByDescending { it.bytes }
                    .take(12),
                largeFiles = largest.toList().sortedByDescending { it.bytes },
                scannedFiles = scannedFiles,
                scannedBytes = scannedBytes,
                trashBytes = trashBytes,
                trashItemCount = trashItems.size,
                completedAt = System.currentTimeMillis(),
            )
        }
    }

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

    private fun managedTrashRoot(storageRoot: File): File = File(storageRoot, TRASH_DIR_NAME)

    private fun isManagedTrashDirectory(file: File): Boolean = file.isDirectory && file.name == TRASH_DIR_NAME

    /**
     * Artefatos de lixeira não devem vazar para o Explorer nem para a análise de armazenamento.
     * Além da pasta gerenciada pelo app, alguns provedores/MediaStore expõem nomes internos
     * como .$recycle_bin$ e .trashed-*. Eles são apenas ocultados; o app não os apaga.
     */
    private fun isInternalTrashArtifact(file: File): Boolean {
        if (isManagedTrashDirectory(file)) return true
        val name = file.name.lowercase()
        return name == ".\$recycle_bin\$" ||
            name == ".recycle_bin\$" ||
            name == "\$recycle.bin" ||
            name.startsWith(".trashed-") ||
            name.startsWith(".trash-")
    }

    private fun isInsideManagedTrash(file: File): Boolean {
        val path = normalizedAbsolutePath(file)
        return storageLocations().any { location ->
            val trashPath = normalizedAbsolutePath(managedTrashRoot(location.root))
            path == trashPath || path.startsWith(trashPath + File.separator)
        }
    }

    private fun readTrashItem(container: File): TrashItem? = runCatching {
        if (!container.isDirectory) return@runCatching null
        val metadataFile = File(container, TRASH_INFO_FILE)
        if (!metadataFile.isFile) return@runCatching null
        val metadata = JSONObject(metadataFile.readText(Charsets.UTF_8))
        val trashedFile = container.listFiles().orEmpty().firstOrNull { it.name != TRASH_INFO_FILE } ?: return@runCatching null
        val originalPath = metadata.optString("originalPath")
        if (originalPath.isBlank()) return@runCatching null
        val isDirectory = metadata.optBoolean("isDirectory", trashedFile.isDirectory)
        TrashItem(
            id = container.name,
            trashedFile = trashedFile,
            originalPath = originalPath,
            originalName = metadata.optString("originalName")
                .ifBlank { File(originalPath).name }
                .ifBlank { trashedFile.name },
            deletedAt = metadata.optLong("deletedAt", container.lastModified()),
            size = metadata.optLong("size", 0L).takeIf { it > 0L }
                ?: directorySizeBytes(trashedFile),
            isDirectory = isDirectory,
            typeLabel = FileTypeClassifier.labelFor(File(originalPath), isDirectory),
            iconRes = FileIconMapper.iconFor(File(originalPath), isDirectory),
        )
    }.getOrNull()

    private fun toFileItem(file: File, favorite: Boolean): FileItem {
        val isDirectory = file.isDirectory
        val name = file.name.ifBlank { file.absolutePath }
        val extension = if (isDirectory) "" else file.extension.lowercase()
        val size = if (isDirectory) 0L else file.length()
        val modifiedAt = file.lastModified()
        val hidden = name.startsWith('.') || runCatching { file.isHidden }.getOrDefault(false)
        val typeLabel = FileTypeClassifier.labelFor(file, isDirectory)
        return FileItem(
            file = file,
            iconRes = FileIconMapper.iconFor(file, isDirectory),
            name = name,
            isDirectory = isDirectory,
            isHidden = hidden,
            size = size,
            modifiedAt = modifiedAt,
            extension = extension,
            typeLabel = typeLabel,
            listDetailText = FileDisplayFormatter.listDetail(modifiedAt, size, isDirectory, extension),
            gridDetailText = FileDisplayFormatter.gridDetail(size, isDirectory, extension),
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

    private fun uniqueRestoreTarget(parent: File, originalName: String): File {
        var candidate = File(parent, originalName)
        if (!candidate.exists()) return candidate
        val dot = originalName.lastIndexOf('.')
        val base = if (dot > 0) originalName.substring(0, dot) else originalName
        val ext = if (dot > 0) originalName.substring(dot) else ""
        var index = 1
        while (candidate.exists()) {
            val suffix = if (index == 1) " (restaurado)" else " (restaurado $index)"
            candidate = File(parent, "$base$suffix$ext")
            index++
        }
        return candidate
    }

    private fun pruneEmptyManagedTrashRoots() {
        storageLocations().map { managedTrashRoot(it.root) }.forEach { trashRoot ->
            if (trashRoot.exists() && trashRoot.listFiles().orEmpty().isEmpty()) trashRoot.delete()
        }
    }

    private fun directorySizeBytes(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return runCatching { file.length() }.getOrDefault(0L).coerceAtLeast(0L)
        return file.listFiles().orEmpty().sumOf(::directorySizeBytes)
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

    private data class ScanNode(val file: File, val topFolder: File?)
    private data class MutableCategory(val label: String, var bytes: Long = 0L, var count: Int = 0)
    private data class MutableFolder(val folder: File, var bytes: Long = 0L, var count: Int = 0)

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

    companion object {
        private const val TRASH_DIR_NAME = ".ExploradorXP_Lixeira"
        private const val TRASH_INFO_FILE = ".trashinfo.json"
        private const val LARGE_FILE_LIMIT = 20
    }
}
