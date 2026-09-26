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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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

    // Gate cooperativo de pausa. O trabalho pesado consulta este estado entre arquivos e
    // também entre blocos de cópia, então Pausar não cancela nem reinicia a operação.
    private val transferPaused = MutableStateFlow(false)
    @Volatile private var transferPauseStartedAtNs: Long = 0L
    @Volatile private var transferPausedAccumulatedNs: Long = 0L

    private val _trashState = MutableStateFlow(TrashUiState())
    val trashState: StateFlow<TrashUiState> = _trashState.asStateFlow()

    private val _storageScanState = MutableStateFlow(StorageScanState())
    val storageScanState: StateFlow<StorageScanState> = _storageScanState.asStateFlow()

    private val _advancedSearchState = MutableStateFlow(AdvancedSearchState())
    val advancedSearchState: StateFlow<AdvancedSearchState> = _advancedSearchState.asStateFlow()

    private val _events = MutableSharedFlow<ExplorerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ExplorerEvent> = _events.asSharedFlow()

    private var refreshJob: Job? = null
    private var projectionJob: Job? = null
    private var transferJob: Job? = null
    private var transferGeneration: Long = 0L
    private var navigationJob: Job? = null
    private var storageJob: Job? = null
    private var storageAnalysisJob: Job? = null
    private var trashJob: Job? = null
    private var advancedSearchJob: Job? = null
    private var advancedSearchGeneration: Long = 0L
    private var directoryRefreshJob: Job? = null
    private val currentDirectoryObserver = CurrentDirectoryObserver(::onCurrentDirectoryChanged)

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
        if (_advancedSearchState.value.active) {
            runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
        }
    }

    /**
     * Recarrega o snapshot do armazenamento. Se houver uma cópia recente no LRU,
     * ela é exibida imediatamente enquanto a leitura real é refeita em background.
     */
    private fun startRefresh(useCache: Boolean) {
        if (!hasFileAccess(getApplication())) {
            refreshJob?.cancel()
            projectionJob?.cancel()
            currentDirectoryObserver.stop()
            _uiState.update { it.copy(loading = false, items = emptyList()) }
            return
        }

        refreshJob?.cancel()
        projectionJob?.cancel()

        val stateAtStart = _uiState.value
        syncCurrentDirectoryObserver(stateAtStart)
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
        advancedSearchJob?.cancel()
        advancedSearchGeneration++
        _advancedSearchState.update { it.copy(active = false, running = false, scannedItems = 0, matchedItems = 0, results = emptyList(), error = null, cancelled = false) }
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
                advancedSearchJob?.cancel()
                advancedSearchGeneration++
                _advancedSearchState.update { it.copy(active = false, running = false, scannedItems = 0, matchedItems = 0, results = emptyList(), error = null, cancelled = false) }
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
        if (_advancedSearchState.value.active) {
            runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 260L)
        } else {
            projectSnapshot(state = _uiState.value, debounceMs = 120L)
        }
    }

    fun setSearchVisible(visible: Boolean) {
        _uiState.update { it.copy(searchVisible = visible, query = if (visible) it.query else "") }
        if (!visible) {
            advancedSearchJob?.cancel()
            advancedSearchGeneration++
            _advancedSearchState.update { it.copy(active = false, running = false, scannedItems = 0, matchedItems = 0, results = emptyList(), error = null, cancelled = false) }
            projectSnapshot(state = _uiState.value, debounceMs = 0L)
        }
    }

    fun applyAdvancedSearchFilters(filters: AdvancedSearchFilters, sortMode: SortMode = _uiState.value.sortMode) {
        val normalized = filters.copy(extension = AdvancedSearchMatcher.normalizeExtension(filters.extension))
        val min = normalized.minSizeBytes
        val max = normalized.maxSizeBytes
        if (min != null && max != null && min > max) {
            _events.tryEmit(ExplorerEvent.ShowMessage("O tamanho mínimo não pode ser maior que o máximo."))
            return
        }
        _uiState.update { it.copy(sortMode = sortMode) }
        runAdvancedSearch(normalized, debounceMs = 0L)
    }

    fun cancelAdvancedSearch() {
        if (advancedSearchJob?.isActive != true) return
        advancedSearchGeneration++
        advancedSearchJob?.cancel()
        _advancedSearchState.update { it.copy(running = false, cancelled = true) }
    }

    fun useSimpleSearch() {
        advancedSearchJob?.cancel()
        advancedSearchGeneration++
        _advancedSearchState.update { it.copy(active = false, running = false, scannedItems = 0, matchedItems = 0, results = emptyList(), error = null, cancelled = false) }
        projectSnapshot(state = _uiState.value, debounceMs = 0L)
    }

    private fun runAdvancedSearch(filters: AdvancedSearchFilters, debounceMs: Long) {
        if (!hasFileAccess(getApplication())) return
        advancedSearchJob?.cancel()
        val generation = ++advancedSearchGeneration
        val stateAtStart = _uiState.value
        val root = stateAtStart.currentDir
        val query = stateAtStart.query
        val tab = stateAtStart.tab
        val previousSearch = _advancedSearchState.value
        _advancedSearchState.value = AdvancedSearchState(
            active = true,
            running = true,
            scannedItems = if (debounceMs > 0L) previousSearch.scannedItems else 0,
            matchedItems = if (debounceMs > 0L) previousSearch.results.size else 0,
            results = if (debounceMs > 0L) previousSearch.results else emptyList(),
            filters = filters,
        )
        advancedSearchJob = viewModelScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            if (generation != advancedSearchGeneration) return@launch
            try {
                if (tab == ExplorerTab.FAVORITES) {
                    val snapshot = withContext(Dispatchers.IO) { repository.favoriteSnapshot() }
                    val now = System.currentTimeMillis()
                    val filtered = withContext(Dispatchers.Default) {
                        ExplorerItemTransforms.apply(
                            snapshot = snapshot.filter { item ->
                                AdvancedSearchMatcher.matches(
                                    name = item.name,
                                    extension = item.extension,
                                    isDirectory = item.isDirectory,
                                    size = item.size,
                                    modifiedAt = item.modifiedAt,
                                    query = query,
                                    filters = filters.copy(includeSubfolders = false),
                                    nowMillis = now,
                                )
                            },
                            query = "",
                            sortMode = stateAtStart.sortMode,
                            showHidden = stateAtStart.showHidden,
                            foldersFirst = stateAtStart.foldersFirst,
                        )
                    }
                    if (generation == advancedSearchGeneration) {
                        _advancedSearchState.value = AdvancedSearchState(
                            active = true,
                            running = false,
                            scannedItems = snapshot.size,
                            matchedItems = filtered.size,
                            results = filtered,
                            filters = filters,
                        )
                    }
                } else {
                    val result = repository.advancedSearch(
                        directory = root,
                        query = query,
                        filters = filters,
                        sortMode = stateAtStart.sortMode,
                        showHidden = stateAtStart.showHidden,
                        foldersFirst = stateAtStart.foldersFirst,
                    ) { scanned, results ->
                        if (generation == advancedSearchGeneration) {
                            _advancedSearchState.value = AdvancedSearchState(
                                active = true,
                                running = true,
                                scannedItems = scanned,
                                matchedItems = results.size,
                                results = results,
                                filters = filters,
                            )
                        }
                    }
                    if (generation != advancedSearchGeneration) return@launch
                    result
                        .onSuccess { results ->
                            _advancedSearchState.update { current ->
                                current.copy(running = false, matchedItems = results.size, results = results, error = null, cancelled = false)
                            }
                        }
                        .onFailure { error ->
                            if (error is CancellationException) {
                                _advancedSearchState.update { it.copy(running = false, cancelled = true) }
                            } else {
                                _advancedSearchState.update { it.copy(running = false, error = error.message ?: "Falha ao pesquisar.") }
                            }
                        }
                }
            } catch (error: CancellationException) {
                if (generation == advancedSearchGeneration) _advancedSearchState.update { it.copy(running = false, cancelled = true) }
            } catch (error: Throwable) {
                if (generation == advancedSearchGeneration) _advancedSearchState.update { it.copy(running = false, error = error.message ?: "Falha ao pesquisar.") }
            }
        }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun setSortMode(sortMode: SortMode) {
        _uiState.update { it.copy(sortMode = sortMode) }
        if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
        else projectSnapshot(state = _uiState.value, debounceMs = 0L)
    }

    fun setFoldersFirst(enabled: Boolean) {
        prefs.setFoldersFirst(enabled)
        _uiState.update { it.copy(foldersFirst = enabled) }
        if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
        else projectSnapshot(state = _uiState.value, debounceMs = 0L)
    }

    fun setShowHidden(show: Boolean) {
        repository.setShowHidden(show)
        _uiState.update { it.copy(showHidden = show) }
        if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
        else projectSnapshot(state = _uiState.value, debounceMs = 0L)
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
                awaitIfPaused = ::awaitTransferResumed,
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
        ) { onProgress -> repository.delete(listOf(file), onProgress, ::awaitTransferResumed) }
    }

    fun moveFileToTrash(file: File) {
        runTransfer(
            kind = TransferKind.MOVE,
            successMessage = "Item movido para a Lixeira.",
            failureFallback = "Falha ao mover para a Lixeira.",
            refreshTrash = true,
        ) { onProgress -> repository.moveToTrash(listOf(file), onProgress, ::awaitTransferResumed) }
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

        val advancedResults = _advancedSearchState.value.takeIf { it.active }?.results
        if (advancedResults != null) {
            return advancedResults.asSequence()
                .filter { !it.isDirectory && it.extension in imageExtensions }
                .map(FileItem::file)
                .toList()
                .ifEmpty { listOf(openedFile) }
        }

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
        val advanced = _advancedSearchState.value
        val visible = if (advanced.active) advanced.results else _uiState.value.items
        val paths = visible.map { it.path }.toSet()
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
        ) { onProgress -> repository.delete(files, onProgress, ::awaitTransferResumed) }
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
        ) { onProgress -> repository.moveToTrash(files, onProgress, ::awaitTransferResumed) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(_uiState.value.currentDir, name)
                .onSuccess {
                    invalidateAllSnapshots()
                    _events.emit(ExplorerEvent.ShowMessage("Pasta criada."))
                    startRefresh(useCache = false)
                    if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
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
                    if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
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
                    if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
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
        ) { onProgress -> repository.restoreTrash(listOf(item), onProgress, ::awaitTransferResumed) }
    }

    fun permanentlyDeleteTrashItem(item: TrashItem) {
        runTransfer(
            kind = TransferKind.DELETE,
            successMessage = "Item apagado permanentemente.",
            failureFallback = "Falha ao apagar o item da Lixeira.",
            refreshTrash = true,
        ) { onProgress -> repository.permanentlyDeleteTrash(listOf(item), onProgress, ::awaitTransferResumed) }
    }

    fun emptyTrash() {
        if (_trashState.value.items.isEmpty()) return
        runTransfer(
            kind = TransferKind.DELETE,
            successMessage = "Lixeira esvaziada.",
            failureFallback = "Falha ao esvaziar a Lixeira.",
            refreshTrash = true,
        ) { onProgress -> repository.emptyTrash(onProgress, ::awaitTransferResumed) }
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

    /** Pausa cooperativamente a operação atual sem perder o ponto já processado. */
    fun pauseTransfer() {
        if (transferJob?.isActive != true || transferPaused.value) return
        transferPaused.value = true
        transferPauseStartedAtNs = System.nanoTime()
        _transferState.update { state -> state?.copy(isPaused = true) }
    }

    /** Continua a operação exatamente do checkpoint em que ela foi pausada. */
    fun resumeTransfer() {
        if (!transferPaused.value) return
        val now = System.nanoTime()
        val started = transferPauseStartedAtNs
        if (started > 0L) {
            transferPausedAccumulatedNs += (now - started).coerceAtLeast(0L)
        }
        transferPauseStartedAtNs = 0L
        transferPaused.value = false
        _transferState.update { state -> state?.copy(isPaused = false) }
    }

    /** Cancela a cópia/mover/exclusão em andamento, se houver. */
    fun cancelTransfer() {
        transferConflictWaiter?.cancel()
        transferConflictWaiter = null
        _transferConflict.value = null
        transferJob?.cancel()
        transferPaused.value = false
        transferPauseStartedAtNs = 0L
        transferPausedAccumulatedNs = 0L
    }

    private suspend fun awaitTransferResumed() {
        transferPaused.filter { paused -> !paused }.first()
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
        val runId = ++transferGeneration
        transferConflictWaiter?.cancel()
        transferConflictWaiter = null
        _transferConflict.value = null
        transferPaused.value = false
        transferPauseStartedAtNs = 0L
        transferPausedAccumulatedNs = 0L
        transferJob = viewModelScope.launch {
            _transferState.value = TransferState(kind, done = 0, total = 1, currentName = "", isPaused = false)
            var firstByteAtNs = 0L
            var pausedAtFirstByteNs = 0L
            val result = try {
                operation { progress ->
                val nowNs = System.nanoTime()
                if (progress.bytesDone > 0L && firstByteAtNs == 0L) {
                    firstByteAtNs = nowNs
                    pausedAtFirstByteNs = transferPausedAccumulatedNs
                }
                val pausedSinceFirstByteNs =
                    (transferPausedAccumulatedNs - pausedAtFirstByteNs).coerceAtLeast(0L)
                val pausedNowNs = if (transferPauseStartedAtNs > firstByteAtNs && firstByteAtNs > 0L)
                    (nowNs - transferPauseStartedAtNs).coerceAtLeast(0L) else 0L
                val activeElapsedNs = if (firstByteAtNs == 0L) 0L else
                    (nowNs - firstByteAtNs - pausedSinceFirstByteNs - pausedNowNs).coerceAtLeast(1L)
                val elapsedSeconds = activeElapsedNs / 1_000_000_000.0
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
                        isPaused = transferPaused.value,
                        currentItemIndex = progress.currentItemIndex,
                        currentItemCount = progress.currentItemCount,
                        currentItemDone = progress.currentItemDone,
                        currentItemTotal = progress.currentItemTotal,
                        currentItemBytesDone = progress.currentItemBytesDone,
                        currentItemBytesTotal = progress.currentItemBytesTotal,
                        queueItems = progress.queueItems,
                    )
                }
            } catch (error: Throwable) {
                Result.failure(error)
            }

            // Uma operação cancelada porque outra começou não pode limpar o estado da nova.
            if (runId != transferGeneration) return@launch

            transferConflictWaiter = null
            _transferConflict.value = null
            _transferState.value = null
            transferPaused.value = false
            transferPauseStartedAtNs = 0L
            transferPausedAccumulatedNs = 0L
            result
                .onSuccess {
                    invalidateAllSnapshots()
                    clearSelection()
                    _events.tryEmit(ExplorerEvent.ShowMessage(successMessage))
                    startRefresh(useCache = false)
                    if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
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
                    if (_advancedSearchState.value.active) runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
                    if (refreshTrash) loadTrash()
                }
        }
    }

    private fun syncCurrentDirectoryObserver(state: ExplorerUiState = _uiState.value) {
        if (state.tab == ExplorerTab.FAVORITES || !hasFileAccess(getApplication())) {
            currentDirectoryObserver.stop()
        } else {
            currentDirectoryObserver.watch(state.currentDir)
        }
    }

    private fun onCurrentDirectoryChanged() {
        // FileObserver pode emitir CREATE e CLOSE_WRITE para o mesmo download. Consolida
        // os eventos para evitar várias releituras enquanto o arquivo ainda está sendo gravado.
        viewModelScope.launch {
            directoryRefreshJob?.cancel()
            directoryRefreshJob = viewModelScope.launch directoryRefresh@{
                delay(DIRECTORY_REFRESH_DEBOUNCE_MS)
                val state = _uiState.value
                if (state.tab == ExplorerTab.FAVORITES || !hasFileAccess(getApplication())) return@directoryRefresh

                snapshotCache.remove(snapshotKey(state))
                startRefresh(useCache = false)
                if (_advancedSearchState.value.active) {
                    runAdvancedSearch(_advancedSearchState.value.filters, debounceMs = 0L)
                }
            }
        }
    }

    override fun onCleared() {
        directoryRefreshJob?.cancel()
        currentDirectoryObserver.stop()
        super.onCleared()
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
        private const val DIRECTORY_REFRESH_DEBOUNCE_MS = 350L
    }
}
