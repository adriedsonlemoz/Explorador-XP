package com.exploradorxp.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesStore(application)
    private val repository = FileRepository(application, prefs)

    private val history = mutableListOf(repository.root)
    private var historyIndex = 0

    private val _uiState = MutableStateFlow(
        ExplorerUiState(
            currentDir = repository.root,
            showHidden = repository.showHidden(),
            foldersFirst = prefs.foldersFirst(),
        )
    )
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()


    // Estados de alta frequência ficam fora do ExplorerUiState principal. Assim, progresso de
    // transferência/análise e atualização da Lixeira não recompõem a árvore inteira do Explorer.
    private val _transferState = MutableStateFlow<TransferState?>(null)
    val transferState: StateFlow<TransferState?> = _transferState.asStateFlow()

    private val _transferConflict = MutableStateFlow<TransferConflict?>(null)
    val transferConflict: StateFlow<TransferConflict?> = _transferConflict.asStateFlow()
    private var transferConflictWaiter: CompletableDeferred<ConflictResolution>? = null

    private val _trashState = MutableStateFlow(TrashUiState())
    val trashState: StateFlow<TrashUiState> = _trashState.asStateFlow()

    private val _storageScanState = MutableStateFlow(StorageScanState())
    val storageScanState: StateFlow<StorageScanState> = _storageScanState.asStateFlow()

    private val _events = MutableSharedFlow<ExplorerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ExplorerEvent> = _events.asSharedFlow()

    private var refreshJob: Job? = null
    private var projectionJob: Job? = null
    private var transferJob: Job? = null
    private var navigationJob: Job? = null
    private var storageJob: Job? = null
    private var storageAnalysisJob: Job? = null
    private var trashJob: Job? = null

    private var currentSnapshotKey: String? = null
    private var currentSnapshot: List<FileItem> = emptyList()

    /** Pequeno LRU das últimas pastas para Voltar/Avançar aparecerem imediatamente. */
    private val snapshotCache = object : LinkedHashMap<String, List<FileItem>>(12, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<FileItem>>?): Boolean = size > 12
    }

    init {
        // Na primeira abertura sem permissão, não faz varredura inútil do armazenamento.
        if (hasFileAccess(application)) {
            startRefresh(useCache = true)
            loadTrash()
        }
    }

    fun refresh() {
        projectionJob?.cancel()
        snapshotCache.remove(snapshotKey(_uiState.value))
        startRefresh(useCache = false)
    }

    /**
     * Recarrega o snapshot do armazenamento. Se houver uma cópia recente no LRU,
     * ela é exibida imediatamente enquanto a leitura real é refeita em background.
     */
    private fun startRefresh(useCache: Boolean) {
        if (!hasFileAccess(getApplication())) {
            refreshJob?.cancel()
            projectionJob?.cancel()
            _uiState.update { it.copy(loading = false, items = emptyList()) }
            return
        }

        refreshJob?.cancel()
        projectionJob?.cancel()

        val stateAtStart = _uiState.value
        val key = snapshotKey(stateAtStart)
        val cached = if (useCache) snapshotCache[key] else null

        if (cached != null) {
            currentSnapshotKey = key
            currentSnapshot = cached
            _uiState.update { current ->
                if (snapshotKey(current) == key) current.copy(loading = true) else current
            }
            projectSnapshot(cached, key, stateAtStart, debounceMs = 0L, finishLoading = true)
        } else {
            _uiState.update { current ->
                if (snapshotKey(current) == key) current.copy(loading = true) else current
            }
        }

        ensureStorageMetadata(stateAtStart)

        refreshJob = viewModelScope.launch {
            // Ao voltar/avançar, o LRU já colocou a pasta na tela. Aguarda um instante antes
            // de reler o armazenamento: navegações rápidas cancelam este job antes de gerar
            // I/O inútil, enquanto Atualizar (sem cache) continua imediato.
            if (cached != null) delay(220L)

            val snapshot = try {
                when (stateAtStart.tab) {
                    ExplorerTab.FILES,
                    ExplorerTab.DOWNLOADS -> repository.directorySnapshot(stateAtStart.currentDir)
                    ExplorerTab.FAVORITES -> repository.favoriteSnapshot()
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (snapshotKey(_uiState.value) == key) {
                    _events.tryEmit(ExplorerEvent.ShowMessage(error.message ?: "Não foi possível listar os arquivos."))
                    _uiState.update { current -> if (snapshotKey(current) == key) current.copy(loading = false) else current }
                }
                return@launch
            }

            if (snapshotKey(_uiState.value) != key) return@launch

            snapshotCache[key] = snapshot
            currentSnapshotKey = key
            currentSnapshot = snapshot
            projectSnapshot(snapshot, key, _uiState.value, debounceMs = 0L, finishLoading = true)
            _uiState.update { current ->
                if (snapshotKey(current) == key) {
                    current.copy(
                        canGoBack = historyIndex > 0,
                        canGoForward = historyIndex < history.lastIndex,
                    )
                } else current
            }
        }
    }

    /** Projeta filtro/busca/ordenação em CPU, sem reler o sistema de arquivos. */
    private fun projectSnapshot(
        snapshot: List<FileItem> = currentSnapshot,
        key: String? = currentSnapshotKey,
        state: ExplorerUiState = _uiState.value,
        debounceMs: Long,
        finishLoading: Boolean = false,
    ) {
        val resolvedKey = key ?: return
        projectionJob?.cancel()
        val query = state.query
        val sortMode = state.sortMode
        val showHidden = state.showHidden
        val foldersFirst = state.foldersFirst

        projectionJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            val projected = withContext(Dispatchers.Default) {
                ExplorerItemTransforms.apply(
                    snapshot = snapshot,
                    query = query,
                    sortMode = sortMode,
                    showHidden = showHidden,
                    foldersFirst = foldersFirst,
                )
            }
            _uiState.update { current ->
                val stillSameProjection = snapshotKey(current) == resolvedKey &&
                    current.query == query &&
                    current.sortMode == sortMode &&
                    current.showHidden == showHidden &&
                    current.foldersFirst == foldersFirst
                if (stillSameProjection) current.copy(items = projected, loading = if (finishLoading) false else current.loading) else current
            }
        }
    }

    /**
     * Descoberta de volumes ocorre uma vez por sessão. Como a capacidade agora fica na
     * barra de status, o StatFs acompanha o volume da pasta atual (interno/SD) sem executar
     * a análise pesada de conteúdo, que continua totalmente sob demanda.
     */
    private fun ensureStorageMetadata(state: ExplorerUiState) {
        storageJob?.cancel()
        storageJob = viewModelScope.launch {
            val locations = if (state.storageLocations.isEmpty()) {
                withContext(Dispatchers.IO) { repository.storageLocations() }
            } else {
                state.storageLocations
            }

            val storageInfo = if (state.tab != ExplorerTab.FAVORITES) {
                withContext(Dispatchers.IO) { repository.storageInfo(state.currentDir) }
            } else null

            _uiState.update { current ->
                current.copy(
                    storageLocations = if (current.storageLocations.isEmpty()) locations else current.storageLocations,
                    storageInfo = storageInfo ?: current.storageInfo,
                )
            }
        }
    }

    fun onItemClick(item: FileItem) {
        val state = _uiState.value
        if (state.selectedPaths.isNotEmpty()) {
            toggleSelection(item.file)
            return
        }
        if (item.isDirectory) {
            navigateTo(item.file)
        } else {
            viewModelScope.launch(Dispatchers.IO) { repository.addRecent(item.file) }
            _events.tryEmit(ExplorerEvent.OpenFile(item.file, currentFolderImages(item.file)))
        }
    }

    fun toggleSelection(file: File) {
        _uiState.update { state ->
            val updated = state.selectedPaths.toMutableSet()
            if (!updated.add(file.absolutePath)) updated.remove(file.absolutePath)
            state.copy(selectedPaths = updated)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPaths = emptySet()) }
    }

    fun selectOnly(file: File) {
        _uiState.update { it.copy(selectedPaths = setOf(file.absolutePath)) }
    }

    fun navigateTo(directory: File, recordHistory: Boolean = true) {
        navigationJob?.cancel()
        navigationJob = viewModelScope.launch {
            val valid = withContext(Dispatchers.IO) { directory.exists() && directory.isDirectory }
            if (!valid) {
                _events.tryEmit(ExplorerEvent.ShowMessage("A pasta não está mais disponível."))
                return@launch
            }
            if (recordHistory) {
                while (history.lastIndex > historyIndex) history.removeAt(history.lastIndex)
                if (history.getOrNull(historyIndex)?.absolutePath != directory.absolutePath) {
                    history.add(directory)
                    historyIndex = history.lastIndex
                }
            }
            commitNavigation(directory)
        }
    }

    private fun commitNavigation(directory: File) {
        _uiState.update {
            it.copy(
                currentDir = directory,
                tab = browsingTabFor(directory),
                selectedPaths = emptySet(),
                query = "",
                canGoBack = historyIndex > 0,
                canGoForward = historyIndex < history.lastIndex,
            )
        }
        startRefresh(useCache = true)
    }

    private fun navigateHistory(targetIndex: Int) {
        if (targetIndex !in history.indices) return
        val target = history[targetIndex]
        navigationJob?.cancel()
        navigationJob = viewModelScope.launch {
            val valid = withContext(Dispatchers.IO) { target.exists() && target.isDirectory }
            if (!valid) {
                _events.tryEmit(ExplorerEvent.ShowMessage("A pasta não está mais disponível."))
                return@launch
            }
            historyIndex = targetIndex
            commitNavigation(target)
        }
    }

    fun goBack() = navigateHistory(historyIndex - 1)

    fun goForward() = navigateHistory(historyIndex + 1)

    fun goHome() = navigateTo(repository.root)

    fun goUp() {
        val state = _uiState.value
        val current = state.currentDir
        val parent = current.parentFile ?: return
        val locations = state.storageLocations.ifEmpty {
            listOf(StorageLocation("Armazenamento interno", repository.root, removable = false))
        }
        val boundary = locations
            .map { it.root }
            .filter { isInsideOrSame(current, it) }
            .maxByOrNull { normalizedAbsolutePath(it).length }
            ?: repository.root

        if (isInsideOrSame(parent, boundary)) navigateTo(parent)
    }

    fun setTab(tab: ExplorerTab) {
        when (tab) {
            ExplorerTab.FILES -> navigateTo(repository.root)
            ExplorerTab.DOWNLOADS -> {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) { repository.downloads.mkdirs() }
                    navigateTo(repository.downloads)
                }
            }
            ExplorerTab.FAVORITES -> {
                _uiState.update {
                    it.copy(
                        tab = ExplorerTab.FAVORITES,
                        selectedPaths = emptySet(),
                        query = "",
                        canGoBack = historyIndex > 0,
                        canGoForward = historyIndex < history.lastIndex,
                    )
                }
                startRefresh(useCache = true)
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        projectSnapshot(state = _uiState.value, debounceMs = 120L)
    }

    fun setSearchVisible(visible: Boolean) {
        _uiState.update { it.copy(searchVisible = visible, query = if (visible) it.query else "") }
        if (!visible) projectSnapshot(state = _uiState.value, debounceMs = 0L)
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun setSortMode(sortMode: SortMode) {
        _uiState.update { it.copy(sortMode = sortMode) }
        projectSnapshot(state = _uiState.value, debounceMs = 0L)
    }

    fun setFoldersFirst(enabled: Boolean) {
        prefs.setFoldersFirst(enabled)
        _uiState.update { it.copy(foldersFirst = enabled) }
        projectSnapshot(state = _uiState.value, debounceMs = 0L)
    }

    fun setShowHidden(show: Boolean) {
        repository.setShowHidden(show)
        _uiState.update { it.copy(showHidden = show) }
        projectSnapshot(state = _uiState.value, debounceMs = 0L)
    }

    fun copySelected() = setClipboard(selectedFiles(), ClipboardMode.COPY)
    fun cutSelected() = setClipboard(selectedFiles(), ClipboardMode.CUT)

    fun copyFile(file: File) = setClipboard(listOf(file), ClipboardMode.COPY)
    fun cutFile(file: File) = setClipboard(listOf(file), ClipboardMode.CUT)

    private fun setClipboard(files: List<File>, mode: ClipboardMode) {
        if (files.isEmpty()) return
        _uiState.update { it.copy(clipboard = ClipboardState(files, mode), selectedPaths = emptySet()) }
        _events.tryEmit(
            ExplorerEvent.ShowMessage(
                if (mode == ClipboardMode.COPY) {
                    if (files.size == 1) "1 item pronto para copiar." else "${files.size} itens prontos para copiar."
                } else {
                    if (files.size == 1) "1 item pronto para mover." else "${files.size} itens prontos para mover."
                }
            )
        )
    }

    fun clearClipboard() {
        if (_uiState.value.clipboard == null) return
        _uiState.update { it.copy(clipboard = null) }
        _events.tryEmit(ExplorerEvent.ShowMessage("Operação de copiar/mover cancelada."))
    }

    fun pasteClipboard() {
        val clipboard = _uiState.value.clipboard ?: return
        val destination = _uiState.value.currentDir
        val kind = if (clipboard.mode == ClipboardMode.CUT) TransferKind.MOVE else TransferKind.COPY
        _uiState.update { it.copy(clipboard = null) }
        runTransfer(
            kind = kind,
            successMessage = "Operação concluída.",
            failureFallback = "Falha ao colar.",
        ) { onProgress ->
            repository.paste(
                clipboard = clipboard,
                destination = destination,
                onProgress = onProgress,
                onConflict = ::requestTransferConflict,
            )
        }
    }

    /** Exclusão permanente, usada apenas após escolha explícita do usuário. */
    fun deleteFile(file: File) {
        runTransfer(
            kind = TransferKind.DELETE,
            successMessage = "Item apagado permanentemente.",
            failureFallback = "Falha ao apagar permanentemente.",
        ) { onProgress -> repository.delete(listOf(file), onProgress) }
    }

    fun moveFileToTrash(file: File) {
        runTransfer(
            kind = TransferKind.MOVE,
            successMessage = "Item movido para a Lixeira.",
            failureFallback = "Falha ao mover para a Lixeira.",
            refreshTrash = true,
        ) { onProgress -> repository.moveToTrash(listOf(file), onProgress) }
    }

    fun shareFile(file: File) {
        viewModelScope.launch {
            val shareable = withContext(Dispatchers.IO) { file.exists() && file.isFile }
            if (shareable) _events.tryEmit(ExplorerEvent.ShareFiles(listOf(file)))
        }
    }

    fun openFileOrFolder(file: File) {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) { file.exists() to file.isDirectory }
            if (!status.first) {
                _events.tryEmit(ExplorerEvent.ShowMessage("O item não está mais disponível."))
            } else if (status.second) {
                navigateTo(file)
            } else {
                withContext(Dispatchers.IO) { repository.addRecent(file) }
                _events.tryEmit(ExplorerEvent.OpenFile(file, currentFolderImages(file)))
            }
        }
    }

    private fun currentFolderImages(openedFile: File): List<File> {
        val state = _uiState.value
        if (state.tab == ExplorerTab.FAVORITES || openedFile.extension.lowercase() !in imageExtensions) return emptyList()
        val parentPath = openedFile.parentFile?.absolutePath ?: return listOf(openedFile)
        if (state.currentDir.absolutePath != parentPath || currentSnapshotKey != snapshotKey(state)) return listOf(openedFile)

        val orderedItems = if (state.query.isBlank()) {
            state.items
        } else {
            // A busca pode esconder outras fotos da mesma pasta. Para a galeria, refaz somente
            // a projeção em memória sem o texto de busca; nenhuma leitura de disco é necessária.
            ExplorerItemTransforms.apply(
                snapshot = currentSnapshot,
                query = "",
                sortMode = state.sortMode,
                showHidden = state.showHidden,
                foldersFirst = false,
            )
        }
        return orderedItems.asSequence()
            .filter { !it.isDirectory && it.extension in imageExtensions }
            .map(FileItem::file)
            .toList()
    }

    fun selectAllVisible() {
        val paths = _uiState.value.items.map { it.path }.toSet()
        _uiState.update { it.copy(selectedPaths = paths) }
    }

    fun deleteSelected() {
        val files = selectedFiles()
        if (files.isEmpty()) return
        val count = files.size
        runTransfer(
            kind = TransferKind.DELETE,
            successMessage = if (count == 1) "1 item apagado permanentemente." else "$count itens apagados permanentemente.",
            failureFallback = "Falha ao apagar permanentemente.",
        ) { onProgress -> repository.delete(files, onProgress) }
    }

    fun moveSelectedToTrash() {
        val files = selectedFiles()
        if (files.isEmpty()) return
        val count = files.size
        runTransfer(
            kind = TransferKind.MOVE,
            successMessage = if (count == 1) "1 item movido para a Lixeira." else "$count itens movidos para a Lixeira.",
            failureFallback = "Falha ao mover para a Lixeira.",
            refreshTrash = true,
        ) { onProgress -> repository.moveToTrash(files, onProgress) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(_uiState.value.currentDir, name)
                .onSuccess {
                    invalidateAllSnapshots()
                    _events.emit(ExplorerEvent.ShowMessage("Pasta criada."))
                    startRefresh(useCache = false)
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao criar pasta.")) }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            repository.createFile(_uiState.value.currentDir, name)
                .onSuccess { file ->
                    invalidateAllSnapshots()
                    _events.emit(ExplorerEvent.ShowMessage("Arquivo criado."))
                    startRefresh(useCache = false)
                    if (supportsInternalViewer(file)) _events.emit(ExplorerEvent.OpenFile(file))
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao criar arquivo.")) }
        }
    }

    fun rename(file: File, newName: String) {
        viewModelScope.launch {
            repository.rename(file, newName)
                .onSuccess {
                    invalidateAllSnapshots()
                    clearSelection()
                    _events.emit(ExplorerEvent.ShowMessage("Item renomeado."))
                    startRefresh(useCache = false)
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao renomear.")) }
        }
    }

    fun toggleFavorite(file: File) {
        viewModelScope.launch {
            val added = withContext(Dispatchers.IO) { repository.toggleFavorite(file) }
            val path = file.absolutePath

            snapshotCache.keys.toList().forEach { key ->
                val snapshot = snapshotCache[key].orEmpty()
                snapshotCache[key] = when {
                    key == FAVORITES_SNAPSHOT_KEY && !added -> snapshot.filterNot { it.path == path }
                    else -> snapshot.map { item -> if (item.path == path) item.copy(isFavorite = added) else item }
                }
            }
            snapshotCache.remove(FAVORITES_SNAPSHOT_KEY)

            if (_uiState.value.tab == ExplorerTab.FAVORITES && !added) {
                currentSnapshot = currentSnapshot.filterNot { it.path == path }
            } else {
                currentSnapshot = currentSnapshot.map { item -> if (item.path == path) item.copy(isFavorite = added) else item }
            }
            currentSnapshotKey?.let { key -> projectSnapshot(currentSnapshot, key, _uiState.value, debounceMs = 0L) }
            _events.tryEmit(ExplorerEvent.ShowMessage(if (added) "Adicionado aos favoritos." else "Removido dos favoritos."))
        }
    }

    fun loadTrash() {
        if (!hasFileAccess(getApplication())) return
        trashJob?.cancel()
        trashJob = viewModelScope.launch {
            _trashState.update { it.copy(loading = true) }
            val items = runCatching { repository.trashSnapshot() }
                .onFailure { _events.tryEmit(ExplorerEvent.ShowMessage(it.message ?: "Não foi possível abrir a Lixeira.")) }
                .getOrDefault(emptyList())
            _trashState.value = TrashUiState(items = items, loading = false)
        }
    }

    fun restoreTrashItem(item: TrashItem) {
        runTransfer(
            kind = TransferKind.MOVE,
            successMessage = "Item restaurado.",
            failureFallback = "Falha ao restaurar o item.",
            refreshTrash = true,
        ) { onProgress -> repository.restoreTrash(listOf(item), onProgress) }
    }

    fun permanentlyDeleteTrashItem(item: TrashItem) {
        runTransfer(
            kind = TransferKind.DELETE,
            successMessage = "Item apagado permanentemente.",
            failureFallback = "Falha ao apagar o item da Lixeira.",
            refreshTrash = true,
        ) { onProgress -> repository.permanentlyDeleteTrash(listOf(item), onProgress) }
    }

    fun emptyTrash() {
        if (_trashState.value.items.isEmpty()) return
        runTransfer(
            kind = TransferKind.DELETE,
            successMessage = "Lixeira esvaziada.",
            failureFallback = "Falha ao esvaziar a Lixeira.",
            refreshTrash = true,
        ) { onProgress -> repository.emptyTrash(onProgress) }
    }

    fun analyzeStorage(force: Boolean = false) {
        if (!hasFileAccess(getApplication())) return
        val current = _storageScanState.value
        if (!force && current.analysis != null && !current.analyzing) return
        storageAnalysisJob?.cancel()
        val locations = _uiState.value.storageLocations
        val target = locations.firstOrNull { !it.removable }?.root ?: repository.root
        storageAnalysisJob = viewModelScope.launch {
            _storageScanState.value = StorageScanState(analyzing = true)
            val result = repository.analyzeStorage(target) { count ->
                _storageScanState.update { state ->
                    state.copy(analyzing = true, scannedFiles = count, error = null)
                }
            }
            result
                .onSuccess { analysis ->
                    _storageScanState.value = StorageScanState(analyzing = false, scannedFiles = analysis.scannedFiles, analysis = analysis)
                }
                .onFailure { error ->
                    if (error is CancellationException) {
                        _storageScanState.update { it.copy(analyzing = false) }
                    } else {
                        _storageScanState.value = StorageScanState(analyzing = false, error = error.message ?: "Falha ao analisar o armazenamento.")
                    }
                }
        }
    }

    fun cancelStorageAnalysis() {
        storageAnalysisJob?.cancel()
    }

    fun shareSelected() {
        val selected = selectedFiles()
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) { selected.filter { it.exists() && it.isFile } }
            if (files.isNotEmpty()) _events.tryEmit(ExplorerEvent.ShareFiles(files))
        }
    }

    fun selectedFiles(): List<File> = _uiState.value.selectedPaths.map(::File)

    fun fileByPath(path: String): File? = File(path)

    /** Cancela a cópia/mover/exclusão em andamento, se houver. */
    fun cancelTransfer() {
        transferConflictWaiter?.cancel()
        transferConflictWaiter = null
        _transferConflict.value = null
        transferJob?.cancel()
    }

    fun resolveTransferConflict(decision: ConflictDecision, applyToAll: Boolean) {
        val waiter = transferConflictWaiter ?: return
        if (!waiter.isCompleted) waiter.complete(ConflictResolution(decision, applyToAll))
        transferConflictWaiter = null
        _transferConflict.value = null
    }

    private suspend fun requestTransferConflict(conflict: TransferConflict): ConflictResolution {
        val waiter = CompletableDeferred<ConflictResolution>()
        transferConflictWaiter = waiter
        _transferConflict.value = conflict
        return try {
            waiter.await()
        } finally {
            if (transferConflictWaiter === waiter) transferConflictWaiter = null
            if (_transferConflict.value == conflict) _transferConflict.value = null
        }
    }

    /**
     * Executa uma cópia/mover/exclusão relatando progresso em [transferState],
     * cancelável a qualquer momento via [cancelTransfer]. Uma transferência nova cancela
     * automaticamente qualquer uma ainda em andamento.
     */
    private fun runTransfer(
        kind: TransferKind,
        successMessage: String,
        failureFallback: String,
        refreshTrash: Boolean = false,
        operation: suspend ((TransferProgress) -> Unit) -> Result<Unit>,
    ) {
        transferJob?.cancel()
        transferConflictWaiter?.cancel()
        transferConflictWaiter = null
        _transferConflict.value = null
        transferJob = viewModelScope.launch {
            _transferState.value = TransferState(kind, done = 0, total = 1, currentName = "")
            var firstByteAtNs = 0L
            val result = operation { progress ->
                if (progress.bytesDone > 0L && firstByteAtNs == 0L) firstByteAtNs = System.nanoTime()
                val elapsedSeconds = if (firstByteAtNs == 0L) 0.0 else
                    ((System.nanoTime() - firstByteAtNs).coerceAtLeast(1L) / 1_000_000_000.0)
                val bytesPerSecond = if (elapsedSeconds > 0.15 && progress.bytesDone > 0L)
                    (progress.bytesDone / elapsedSeconds).toLong().coerceAtLeast(0L) else 0L
                val remainingBytes = (progress.bytesTotal - progress.bytesDone).coerceAtLeast(0L)
                val etaSeconds = if (bytesPerSecond > 0L && remainingBytes > 0L)
                    ((remainingBytes + bytesPerSecond - 1) / bytesPerSecond) else null
                _transferState.value = TransferState(
                    kind = kind,
                    done = progress.done,
                    total = progress.total,
                    currentName = progress.currentName,
                    bytesDone = progress.bytesDone,
                    bytesTotal = progress.bytesTotal,
                    bytesPerSecond = bytesPerSecond,
                    etaSeconds = etaSeconds,
                )
            }
            transferConflictWaiter = null
            _transferConflict.value = null
            _transferState.value = null
            result
                .onSuccess {
                    invalidateAllSnapshots()
                    clearSelection()
                    _events.tryEmit(ExplorerEvent.ShowMessage(successMessage))
                    startRefresh(useCache = false)
                    if (refreshTrash) loadTrash()
                }
                .onFailure { error ->
                    val message = if (error is CancellationException) {
                        "Operação cancelada."
                    } else {
                        error.message ?: failureFallback
                    }
                    invalidateAllSnapshots()
                    _events.tryEmit(ExplorerEvent.ShowMessage(message))
                    startRefresh(useCache = false)
                    if (refreshTrash) loadTrash()
                }
        }
    }

    private fun invalidateAllSnapshots() {
        snapshotCache.clear()
        currentSnapshotKey = null
        currentSnapshot = emptyList()
    }

    private fun snapshotKey(state: ExplorerUiState): String = when (state.tab) {
        ExplorerTab.FAVORITES -> FAVORITES_SNAPSHOT_KEY
        ExplorerTab.FILES,
        ExplorerTab.DOWNLOADS -> "${state.tab.name}|${normalizedAbsolutePath(state.currentDir)}"
    }

    private fun browsingTabFor(directory: File): ExplorerTab {
        val dirPath = normalizedAbsolutePath(directory)
        val downloadsPath = normalizedAbsolutePath(repository.downloads)
        return if (dirPath == downloadsPath || dirPath.startsWith(downloadsPath + File.separator)) {
            ExplorerTab.DOWNLOADS
        } else {
            ExplorerTab.FILES
        }
    }

    private fun normalizedAbsolutePath(file: File): String = file.absolutePath.let { path -> if (path.length > 1) path.trimEnd(File.separatorChar) else path }

    private fun sameAbsolutePath(a: File, b: File): Boolean = normalizedAbsolutePath(a) == normalizedAbsolutePath(b)

    private fun isInsideOrSame(file: File, root: File): Boolean {
        val filePath = normalizedAbsolutePath(file)
        val rootPath = normalizedAbsolutePath(root)
        return filePath == rootPath || filePath.startsWith(rootPath + File.separator)
    }

    companion object {
        private const val FAVORITES_SNAPSHOT_KEY = "FAVORITES"
    }
}
