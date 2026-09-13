package com.exploradorxp.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
            storageInfo = repository.storageInfo(),
            storageLocations = repository.storageLocations(),
        )
    )
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExplorerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ExplorerEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val state = _uiState.value
            val items = runCatching {
                when (state.tab) {
                    ExplorerTab.FILES,
                    ExplorerTab.DOWNLOADS -> repository.listDirectory(
                        state.currentDir,
                        state.query,
                        state.sortMode,
                        state.showHidden,
                    )
                    ExplorerTab.FAVORITES -> repository.favoriteItems(
                        state.query,
                        state.sortMode,
                        state.showHidden,
                    )
                }
            }.getOrElse {
                _events.tryEmit(ExplorerEvent.ShowMessage(it.message ?: "Não foi possível listar os arquivos."))
                emptyList()
            }
            _uiState.update {
                it.copy(
                    items = items,
                    loading = false,
                    storageInfo = repository.storageInfo(state.currentDir),
                    storageLocations = repository.storageLocations(),
                    canGoBack = historyIndex > 0,
                    canGoForward = historyIndex < history.lastIndex,
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
            repository.addRecent(item.file)
            _events.tryEmit(ExplorerEvent.OpenFile(item.file))
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
        if (!directory.exists() || !directory.isDirectory) {
            _events.tryEmit(ExplorerEvent.ShowMessage("A pasta não está mais disponível."))
            return
        }
        if (recordHistory) {
            while (history.lastIndex > historyIndex) history.removeLast()
            if (history.getOrNull(historyIndex)?.absolutePath != directory.absolutePath) {
                history.add(directory)
                historyIndex = history.lastIndex
            }
        }
        _uiState.update {
            it.copy(
                currentDir = directory,
                tab = browsingTabFor(directory),
                selectedPaths = emptySet(),
                query = "",
            )
        }
        refresh()
    }

    fun goBack() {
        if (historyIndex <= 0) return
        historyIndex--
        navigateTo(history[historyIndex], recordHistory = false)
    }

    fun goForward() {
        if (historyIndex >= history.lastIndex) return
        historyIndex++
        navigateTo(history[historyIndex], recordHistory = false)
    }

    fun goHome() = navigateTo(repository.root)

    fun goUp() {
        val current = _uiState.value.currentDir
        val boundary = repository.storageRootFor(current)
        val parent = current.parentFile ?: return
        val parentPath = runCatching { parent.canonicalPath }.getOrDefault(parent.absolutePath)
        val boundaryPath = runCatching { boundary.canonicalPath }.getOrDefault(boundary.absolutePath)
        if (parentPath == boundaryPath || parentPath.startsWith(boundaryPath + File.separator)) {
            navigateTo(parent)
        }
    }

    fun setTab(tab: ExplorerTab) {
        when (tab) {
            ExplorerTab.FILES -> navigateTo(repository.root)
            ExplorerTab.DOWNLOADS -> {
                repository.downloads.mkdirs()
                navigateTo(repository.downloads)
            }
            ExplorerTab.FAVORITES -> {
                _uiState.update { it.copy(tab = ExplorerTab.FAVORITES, selectedPaths = emptySet(), query = "") }
                refresh()
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        refresh()
    }

    fun setSearchVisible(visible: Boolean) {
        _uiState.update { it.copy(searchVisible = visible, query = if (visible) it.query else "") }
        if (!visible) refresh()
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
        }
    }

    fun setSortMode(sortMode: SortMode) {
        _uiState.update { it.copy(sortMode = sortMode) }
        refresh()
    }

    fun setShowHidden(show: Boolean) {
        repository.setShowHidden(show)
        _uiState.update { it.copy(showHidden = show) }
        refresh()
    }

    fun copySelected() = setClipboard(selectedFiles(), ClipboardMode.COPY)
    fun cutSelected() = setClipboard(selectedFiles(), ClipboardMode.CUT)

    fun copyFile(file: File) = setClipboard(listOf(file), ClipboardMode.COPY)
    fun cutFile(file: File) = setClipboard(listOf(file), ClipboardMode.CUT)

    private fun setClipboard(files: List<File>, mode: ClipboardMode) {
        val existing = files.filter(File::exists)
        if (existing.isEmpty()) return
        _uiState.update { it.copy(clipboard = ClipboardState(existing, mode), selectedPaths = emptySet()) }
        _events.tryEmit(
            ExplorerEvent.ShowMessage(
                if (mode == ClipboardMode.COPY) "${existing.size} item(ns) pronto(s) para copiar."
                else "${existing.size} item(ns) pronto(s) para mover."
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
        viewModelScope.launch {
            repository.paste(clipboard, destination)
                .onSuccess {
                    _uiState.update { it.copy(clipboard = null) }
                    _events.emit(ExplorerEvent.ShowMessage("Operação concluída."))
                    refresh()
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao colar.")) }
        }
    }

    fun deleteFile(file: File) {
        if (!file.exists()) return
        viewModelScope.launch {
            repository.delete(listOf(file))
                .onSuccess {
                    _uiState.update { it.copy(selectedPaths = it.selectedPaths - file.absolutePath) }
                    _events.emit(ExplorerEvent.ShowMessage("Item excluído."))
                    refresh()
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao excluir.")) }
        }
    }

    fun shareFile(file: File) {
        if (file.exists() && file.isFile) _events.tryEmit(ExplorerEvent.ShareFiles(listOf(file)))
    }

    fun openFileOrFolder(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            navigateTo(file)
        } else {
            repository.addRecent(file)
            _events.tryEmit(ExplorerEvent.OpenFile(file))
        }
    }

    fun selectAllVisible() {
        val paths = _uiState.value.items.map { it.file.absolutePath }.toSet()
        _uiState.update { it.copy(selectedPaths = paths) }
    }

    fun deleteSelected() {
        val files = selectedFiles()
        if (files.isEmpty()) return
        viewModelScope.launch {
            repository.delete(files)
                .onSuccess {
                    clearSelection()
                    _events.emit(ExplorerEvent.ShowMessage("${files.size} item(ns) excluído(s)."))
                    refresh()
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao excluir.")) }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(_uiState.value.currentDir, name)
                .onSuccess {
                    _events.emit(ExplorerEvent.ShowMessage("Pasta criada."))
                    refresh()
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao criar pasta.")) }
        }
    }

    fun rename(file: File, newName: String) {
        viewModelScope.launch {
            repository.rename(file, newName)
                .onSuccess {
                    clearSelection()
                    _events.emit(ExplorerEvent.ShowMessage("Item renomeado."))
                    refresh()
                }
                .onFailure { _events.emit(ExplorerEvent.ShowMessage(it.message ?: "Falha ao renomear.")) }
        }
    }

    fun toggleFavorite(file: File) {
        val added = repository.toggleFavorite(file)
        _events.tryEmit(ExplorerEvent.ShowMessage(if (added) "Adicionado aos favoritos." else "Removido dos favoritos."))
        refresh()
    }

    fun shareSelected() {
        val files = selectedFiles().filter(File::isFile)
        if (files.isNotEmpty()) _events.tryEmit(ExplorerEvent.ShareFiles(files))
    }

    fun selectedFiles(): List<File> = _uiState.value.selectedPaths.map(::File).filter(File::exists)

    fun fileByPath(path: String): File? = File(path).takeIf(File::exists)

    private fun browsingTabFor(directory: File): ExplorerTab {
        val dirPath = runCatching { directory.canonicalPath }.getOrDefault(directory.absolutePath)
        val downloadsPath = runCatching { repository.downloads.canonicalPath }.getOrDefault(repository.downloads.absolutePath)
        return if (dirPath == downloadsPath || dirPath.startsWith(downloadsPath + File.separator)) {
            ExplorerTab.DOWNLOADS
        } else {
            ExplorerTab.FILES
        }
    }
}
