package com.exploradorxp.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

val XpBlue = Color(0xFF0A67D8)
val XpBlueDark = Color(0xFF0752B8)
val XpBlueLight = Color(0xFF4BA4FF)
val XpBackground = Color(0xFFE9F2FC)
val XpSurface = Color(0xFFFFFFFF)
val XpBorder = Color(0xFF2E7FD3)
val XpTextSecondary = Color(0xFF36577F)
val XpSelection = Color(0xFFD9E9FB)
val XpChrome = Color(0xFFF2F4F8)
val XpChromeBorder = Color(0xFFB8C7DA)
val XpPanel = Color(0xFFF6FAFF)

private enum class FileMenuAction { OPEN, COPY, MOVE, RENAME, DELETE, SHARE, FAVORITE, PROPERTIES, SELECT }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorerScreen(
    state: ExplorerUiState,
    accessGranted: Boolean,
    onRequestAccess: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onNavigateTo: (File) -> Unit,
    onToggleSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleView: () -> Unit,
    onToggleHidden: (Boolean) -> Unit,
    onSortMode: (SortMode) -> Unit,
    onFoldersFirstChange: (Boolean) -> Unit,
    onTabChange: (ExplorerTab) -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onCopyTarget: (File) -> Unit,
    onCutTarget: (File) -> Unit,
    onPaste: () -> Unit,
    onClearClipboard: () -> Unit,
    onDelete: () -> Unit,
    onDeleteTarget: (File) -> Unit,
    onMoveToTrash: () -> Unit,
    onMoveTargetToTrash: (File) -> Unit,
    onShare: () -> Unit,
    onShareTarget: (File) -> Unit,
    onOpenTarget: (File) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectOnly: (File) -> Unit,
    onToggleSelection: (File) -> Unit,
    onToggleFavorite: (File) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRename: (File, String) -> Unit,
    onLoadTrash: () -> Unit,
    onRestoreTrashItem: (TrashItem) -> Unit,
    onDeleteTrashItem: (TrashItem) -> Unit,
    onEmptyTrash: () -> Unit,
    onAnalyzeStorage: (Boolean) -> Unit,
    onCancelStorageAnalysis: () -> Unit,
    onCancelTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNewFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var propertiesTarget by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }
    var showFolderContext by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showDeviceInfo by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var showStorageDetails by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XpBackground)
    ) {
        XpHeader(
            currentDir = state.currentDir,
            searchVisible = state.searchVisible,
            query = state.query,
            onQueryChange = onQueryChange,
            onToggleSearch = onToggleSearch,
            onNewFolder = { showNewFolder = true },
            onRefresh = onRefresh,
            canPaste = state.clipboard != null && state.tab != ExplorerTab.FAVORITES,
            onPaste = onPaste,
            onClearClipboard = onClearClipboard,
            canBack = state.canGoBack,
            canForward = state.canGoForward,
            onBack = onBack,
            onForward = onForward,
            onHome = onHome,
            onUp = onUp,
            viewMode = state.viewMode,
            onToggleView = onToggleView,
            showHidden = state.showHidden,
            onShowHiddenChange = onToggleHidden,
            sortMode = state.sortMode,
            foldersFirst = state.foldersFirst,
            onSortMode = onSortMode,
            onFoldersFirstChange = onFoldersFirstChange,
            onNavigateTo = onNavigateTo,
            storageLocations = state.storageLocations,
            onOpenDownloads = { onTabChange(ExplorerTab.DOWNLOADS) },
            onOpenFavorites = { onTabChange(ExplorerTab.FAVORITES) },
            selectionCount = state.selectedPaths.size,
            onCopySelection = onCopy,
            onCutSelection = onCut,
            onShareSelection = { onShare(); onClearSelection() },
            onDeleteSelection = { showDeleteConfirm = true },
            onRenameSelection = {
                state.selectedPaths.singleOrNull()?.let { renameTarget = File(it) }
                onClearSelection()
            },
            onPropertiesSelection = {
                state.selectedPaths.singleOrNull()?.let { propertiesTarget = File(it) }
                onClearSelection()
            },
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            onShowManual = { showManual = true },
            onShowAbout = { showAbout = true },
            onShowDeviceInfo = { showDeviceInfo = true },
            trashHasItems = state.trashHasItems,
            onOpenTrash = {
                showTrash = true
                onLoadTrash()
            },
        )

        val internalRoot = state.storageLocations.firstOrNull { !it.removable }?.root
            ?: android.os.Environment.getExternalStorageDirectory()
        val isHomePage = state.tab == ExplorerTab.FILES && samePath(state.currentDir, internalRoot)

        if (isHomePage && !state.searchVisible && state.selectedPaths.isEmpty()) {
            StorageCard(state.storageInfo) {
                showStorageDetails = true
                onAnalyzeStorage(false)
            }
        } else if (state.tab == ExplorerTab.FAVORITES && !state.searchVisible && state.selectedPaths.isEmpty()) {
            SectionTitle(title = "Favoritos", icon = R.drawable.favorites)
        }

        if (!accessGranted && state.tab != ExplorerTab.FAVORITES) {
            PermissionBanner(onRequestAccess)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.loading) {
                CircularProgressIndicator(
                    color = XpBlue,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.items.isEmpty()) {
                EmptyState(state.tab, state.query, state.currentDir)
            } else if (state.tab == ExplorerTab.FAVORITES || state.viewMode == ViewMode.LIST) {
                FileList(
                    scrollKey = "${state.tab}|${state.currentDir.absolutePath}",
                    items = state.items,
                    selectedPaths = state.selectedPaths,
                    onItemClick = onItemClick,
                    onMenuAction = { item, action ->
                        when (action) {
                            FileMenuAction.OPEN -> onOpenTarget(item.file)
                            FileMenuAction.COPY -> onCopyTarget(item.file)
                            FileMenuAction.MOVE -> onCutTarget(item.file)
                            FileMenuAction.RENAME -> renameTarget = item.file
                            FileMenuAction.DELETE -> deleteTarget = item.file
                            FileMenuAction.SHARE -> onShareTarget(item.file)
                            FileMenuAction.FAVORITE -> onToggleFavorite(item.file)
                            FileMenuAction.PROPERTIES -> propertiesTarget = item.file
                            FileMenuAction.SELECT -> onSelectOnly(item.file)
                        }
                    },
                    onLongSelect = { onToggleSelection(it.file) },
                    onBlankLongPress = { showFolderContext = true },
                )
            } else {
                FileGrid(
                    scrollKey = "${state.tab}|${state.currentDir.absolutePath}",
                    items = state.items,
                    selectedPaths = state.selectedPaths,
                    onItemClick = onItemClick,
                    onMenuAction = { item, action ->
                        when (action) {
                            FileMenuAction.OPEN -> onOpenTarget(item.file)
                            FileMenuAction.COPY -> onCopyTarget(item.file)
                            FileMenuAction.MOVE -> onCutTarget(item.file)
                            FileMenuAction.RENAME -> renameTarget = item.file
                            FileMenuAction.DELETE -> deleteTarget = item.file
                            FileMenuAction.SHARE -> onShareTarget(item.file)
                            FileMenuAction.FAVORITE -> onToggleFavorite(item.file)
                            FileMenuAction.PROPERTIES -> propertiesTarget = item.file
                            FileMenuAction.SELECT -> onSelectOnly(item.file)
                        }
                    },
                    onLongSelect = { onToggleSelection(it.file) },
                    onBlankLongPress = { showFolderContext = true },
                )
            }
        }

        ExplorerStatusBar(
            items = state.items,
            selectedPaths = state.selectedPaths,
        )
    }

    if (showNewFolder) {
        NameDialog(
            title = "Nova pasta",
            initialValue = "",
            confirmText = "Criar",
            onDismiss = { showNewFolder = false },
            onConfirm = {
                showNewFolder = false
                onCreateFolder(it)
            }
        )
    }

    renameTarget?.let { target ->
        NameDialog(
            title = "Renomear",
            initialValue = target.name,
            confirmText = "Renomear",
            onDismiss = { renameTarget = null },
            onConfirm = {
                renameTarget = null
                onRename(target, it)
            }
        )
    }

    propertiesTarget?.let { target ->
        PropertiesDialog(file = target, onDismiss = { propertiesTarget = null })
    }

    if (showDeleteConfirm) {
        DeleteChoiceDialog(
            title = "Excluir itens?",
            message = "Escolha o que fazer com ${selectedCountLabel(state.selectedPaths.size)}.",
            onDismiss = { showDeleteConfirm = false },
            onMoveToTrash = {
                showDeleteConfirm = false
                onMoveToTrash()
            },
            onDeletePermanently = {
                showDeleteConfirm = false
                onDelete()
            },
        )
    }

    deleteTarget?.let { target ->
        DeleteChoiceDialog(
            title = "Excluir item?",
            message = "Escolha o que fazer com \"${target.name}\".",
            onDismiss = { deleteTarget = null },
            onMoveToTrash = {
                deleteTarget = null
                onMoveTargetToTrash(target)
            },
            onDeletePermanently = {
                deleteTarget = null
                onDeleteTarget(target)
            },
        )
    }


    if (showFolderContext) {
        FolderContextDialog(
            canPaste = state.clipboard != null && state.tab != ExplorerTab.FAVORITES,
            canSelectAll = state.items.isNotEmpty(),
            onDismiss = { showFolderContext = false },
            onPaste = { showFolderContext = false; onPaste() },
            onNewFolder = { showFolderContext = false; showNewFolder = true },
            onSelectAll = { showFolderContext = false; onSelectAll() },
            onProperties = { showFolderContext = false; propertiesTarget = state.currentDir },
            onRefresh = { showFolderContext = false; onRefresh() },
        )
    }

    if (showManual) HelpManualDialog(onDismiss = { showManual = false })
    if (showAbout) AboutDialog(
        onDismiss = { showAbout = false },
        onOpenHelp = { showAbout = false; showManual = true },
        onOpenDeviceInfo = { showAbout = false; showDeviceInfo = true },
    )
    if (showDeviceInfo) DeviceInfoDialog(onDismiss = { showDeviceInfo = false })
    if (showTrash) {
        TrashDialog(
            items = state.trashItems,
            loading = state.trashLoading,
            onDismiss = { showTrash = false },
            onRefresh = onLoadTrash,
            onRestore = onRestoreTrashItem,
            onDeletePermanently = onDeleteTrashItem,
            onEmpty = onEmptyTrash,
        )
    }
    if (showStorageDetails) {
        StorageDetailsDialog(
            state = state.storageScan,
            fallbackInfo = state.storageInfo,
            onDismiss = {
                if (state.storageScan.analyzing) onCancelStorageAnalysis()
                showStorageDetails = false
            },
            onRefresh = { onAnalyzeStorage(true) },
            onCancel = onCancelStorageAnalysis,
            onOpenFolder = { folder ->
                showStorageDetails = false
                onNavigateTo(folder)
            },
            onOpenFile = { file ->
                showStorageDetails = false
                onOpenTarget(file)
            },
            onOpenTrash = {
                showStorageDetails = false
                showTrash = true
                onLoadTrash()
            },
        )
    }

    state.transfer?.let { transfer ->
        TransferProgressDialog(transfer = transfer, onCancel = onCancelTransfer)
    }

}

@Composable
private fun XpHeader(
    currentDir: File,
    searchVisible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onNewFolder: () -> Unit,
    onRefresh: () -> Unit,
    canPaste: Boolean,
    onPaste: () -> Unit,
    onClearClipboard: () -> Unit,
    canBack: Boolean,
    canForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onUp: () -> Unit,
    viewMode: ViewMode,
    onToggleView: () -> Unit,
    showHidden: Boolean,
    onShowHiddenChange: (Boolean) -> Unit,
    sortMode: SortMode,
    foldersFirst: Boolean,
    onSortMode: (SortMode) -> Unit,
    onFoldersFirstChange: (Boolean) -> Unit,
    onNavigateTo: (File) -> Unit,
    storageLocations: List<StorageLocation>,
    onOpenDownloads: () -> Unit,
    onOpenFavorites: () -> Unit,
    selectionCount: Int,
    onCopySelection: () -> Unit,
    onCutSelection: () -> Unit,
    onShareSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onRenameSelection: () -> Unit,
    onPropertiesSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onShowManual: () -> Unit,
    onShowAbout: () -> Unit,
    onShowDeviceInfo: () -> Unit,
    trashHasItems: Boolean,
    onOpenTrash: () -> Unit,
) {
    var fileMenu by remember { mutableStateOf(false) }
    var editMenu by remember { mutableStateOf(false) }
    var viewMenu by remember { mutableStateOf(false) }
    var favoritesMenu by remember { mutableStateOf(false) }
    var toolsMenu by remember { mutableStateOf(false) }
    var organizeMenu by remember { mutableStateOf(false) }
    var helpMenu by remember { mutableStateOf(false) }
    var addressMenu by remember { mutableStateOf(false) }
    var selectionMoreMenu by remember { mutableStateOf(false) }

    val fallbackRoot = android.os.Environment.getExternalStorageDirectory()
    val effectiveLocations = storageLocations.ifEmpty {
        listOf(StorageLocation("Armazenamento interno", fallbackRoot, removable = false))
    }
    val currentLocation = remember(currentDir.absolutePath, effectiveLocations) {
        effectiveLocations
            .sortedByDescending { canonicalPathOf(it.root).length }
            .firstOrNull { location -> isInsideOrSame(currentDir, location.root) }
            ?: effectiveLocations.first()
    }
    val pathEntries = remember(currentDir.absolutePath, currentLocation.root.absolutePath) {
        buildList<Pair<String, File>> {
            add(currentLocation.label to currentLocation.root)
            val rootPath = canonicalPathOf(currentLocation.root)
            val currentPath = canonicalPathOf(currentDir)
            val relative = currentPath.removePrefix(rootPath).trim(File.separatorChar, '/')
            if (relative.isNotBlank()) {
                var cursor = currentLocation.root
                relative.split(File.separatorChar, '/').filter(String::isNotBlank).forEach { part ->
                    cursor = File(cursor, part)
                    add(part to cursor)
                }
            }
        }
    }

    val searchMode = searchVisible && selectionCount == 0
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(searchMode) {
        if (searchMode) searchFocusRequester.requestFocus()
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF2F92F6), Color(0xFF0A67D8), Color(0xFF0752B8))))
                .padding(horizontal = 7.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.folder),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = when {
                    selectionCount > 0 -> selectedCountLabel(selectionCount)
                    searchMode -> "Pesquisar"
                    else -> "Explorador XP"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selectionCount > 0) {
                Text(
                    "Cancelar",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onClearSelection).padding(horizontal = 8.dp, vertical = 5.dp)
                )
            } else if (searchMode) {
                Text(
                    "Fechar",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onToggleSearch).padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        if (selectionCount == 0 && !searchMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(29.dp)
                    .background(XpChrome)
                    .border(1.dp, XpChromeBorder)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 3.dp)
            ) {
                Box {
                    XpMenuLabel("Arquivo", fileMenu) { fileMenu = true }
                    XpPopupMenu(expanded = fileMenu, onDismiss = { fileMenu = false }) {
                        XpMenuItem("Nova pasta", R.drawable.folder_new) { fileMenu = false; onNewFolder() }
                        if (canPaste) XpMenuItem("Colar", R.drawable.paste) { fileMenu = false; onPaste() }
                        XpMenuDivider()
                        XpMenuItem("Lixeira", if (trashHasItems) R.drawable.trash_full else R.drawable.trash_empty) { fileMenu = false; onOpenTrash() }
                        XpMenuItem("Atualizar", R.drawable.refresh) { fileMenu = false; onRefresh() }
                    }
                }

                Box {
                    XpMenuLabel("Editar", editMenu) { editMenu = true }
                    XpPopupMenu(expanded = editMenu, onDismiss = { editMenu = false }) {
                        if (canPaste) {
                            XpMenuItem("Colar", R.drawable.paste) { editMenu = false; onPaste() }
                            XpMenuItem("Cancelar copiar/mover", R.drawable.close) { editMenu = false; onClearClipboard() }
                            XpMenuDivider()
                        }
                        XpMenuItem("Selecionar tudo", R.drawable.select_all) { editMenu = false; onSelectAll() }
                    }
                }

                Box {
                    XpMenuLabel("Exibir", viewMenu) { viewMenu = true }
                    XpPopupMenu(expanded = viewMenu, onDismiss = { viewMenu = false }) {
                        XpMenuItem(
                            if (viewMode == ViewMode.LIST) "Exibir como ícones" else "Exibir como lista",
                            if (viewMode == ViewMode.LIST) R.drawable.view_grid else R.drawable.view_list,
                        ) { viewMenu = false; onToggleView() }
                        XpMenuItem(
                            if (showHidden) "Ocultar arquivos ocultos" else "Mostrar arquivos ocultos",
                            R.drawable.visible,
                            checked = showHidden,
                        ) { viewMenu = false; onShowHiddenChange(!showHidden) }
                    }
                }

                Box {
                    XpMenuLabel("Favoritos", favoritesMenu) { favoritesMenu = true }
                    XpPopupMenu(expanded = favoritesMenu, onDismiss = { favoritesMenu = false }) {
                        XpMenuItem("Abrir Favoritos", R.drawable.favorites) { favoritesMenu = false; onOpenFavorites() }
                    }
                }

                Box {
                    XpMenuLabel("Ferramentas", toolsMenu) { toolsMenu = true }
                    XpPopupMenu(expanded = toolsMenu, onDismiss = { toolsMenu = false; organizeMenu = false }) {
                        Box {
                            XpMenuItem("Organizar", R.drawable.sort, trailing = "▶") { organizeMenu = true }
                            XpPopupMenu(
                                expanded = organizeMenu,
                                onDismiss = { organizeMenu = false },
                                offset = DpOffset(164.dp, (-30).dp),
                            ) {
                                SortMode.entries.forEach { mode ->
                                    val label = when (mode) {
                                        SortMode.NAME -> "Nome"
                                        SortMode.DATE -> "Data"
                                        SortMode.SIZE -> "Tamanho"
                                        SortMode.TYPE -> "Tipo"
                                    }
                                    XpMenuItem(label, checked = sortMode == mode) {
                                        organizeMenu = false
                                        toolsMenu = false
                                        onSortMode(mode)
                                    }
                                }
                                XpMenuDivider()
                                XpMenuItem("Pastas primeiro", checked = foldersFirst) {
                                    onFoldersFirstChange(!foldersFirst)
                                    organizeMenu = false
                                    toolsMenu = false
                                }
                            }
                        }
                        XpMenuDivider()
                        XpMenuItem("Informações do dispositivo", R.drawable.device_mobile) {
                            toolsMenu = false
                            onShowDeviceInfo()
                        }
                    }
                }

                Box {
                    XpMenuLabel("Ajuda", helpMenu) { helpMenu = true }
                    XpPopupMenu(expanded = helpMenu, onDismiss = { helpMenu = false }) {
                        XpMenuItem("Ajuda", R.drawable.help) { helpMenu = false; onShowManual() }
                        XpMenuDivider()
                        XpMenuItem("Sobre o Explorador XP", R.drawable.info) { helpMenu = false; onShowAbout() }
                    }
                }
            }
        }

        if (!searchMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (selectionCount > 0) 56.dp else 52.dp)
                    .background(XpChrome)
                    .border(1.dp, XpChromeBorder)
                    .padding(horizontal = 2.dp)
            ) {
                if (selectionCount > 0) {
                    XpClassicToolButton(R.drawable.copy, "Copiar", true, onCopySelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.move, "Mover", true, onCutSelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.delete, "Excluir", true, onDeleteSelection, Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        XpClassicToolButton(
                            R.drawable.more,
                            "Mais",
                            true,
                            { selectionMoreMenu = true },
                            Modifier.fillMaxWidth(),
                        )
                        XpPopupMenu(expanded = selectionMoreMenu, onDismiss = { selectionMoreMenu = false }) {
                            XpMenuItem("Renomear", R.drawable.rename, enabled = selectionCount == 1) {
                                selectionMoreMenu = false
                                onRenameSelection()
                            }
                            XpMenuItem("Compartilhar", R.drawable.share) {
                                selectionMoreMenu = false
                                onShareSelection()
                            }
                            XpMenuItem("Propriedades", R.drawable.properties, enabled = selectionCount == 1) {
                                selectionMoreMenu = false
                                onPropertiesSelection()
                            }
                            XpMenuDivider()
                            XpMenuItem("Selecionar todos", R.drawable.select_all) {
                                selectionMoreMenu = false
                                onSelectAll()
                            }
                        }
                    }
                } else {
                    XpClassicToolButton(R.drawable.back, "Voltar", canBack, onBack, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.forward, "Avançar", canForward, onForward, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.home, "Início", true, onHome, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.up, "Subir", true, onUp, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.search, "Pesquisar", true, onToggleSearch, Modifier.weight(1f))
                    if (canPaste) {
                        XpClassicToolButton(R.drawable.paste, "Colar", true, onPaste, Modifier.weight(1f))
                    } else {
                        XpClassicToolButton(R.drawable.folder_downloads, "Downloads", true, onOpenDownloads, Modifier.weight(1f))
                    }
                    XpClassicToolButton(
                        if (trashHasItems) R.drawable.trash_full else R.drawable.trash_empty,
                        "Lixeira",
                        true,
                        onOpenTrash,
                        Modifier.weight(1f),
                    )
                }
            }
        }

        if (searchMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(XpChrome)
                    .border(1.dp, XpChromeBorder)
                    .padding(horizontal = 7.dp, vertical = 5.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White)
                        .border(1.dp, Color(0xFF7F9DB9))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isBlank()) {
                        Text(
                            "Pesquisar em ${currentDir.name.ifBlank { currentLocation.label }}",
                            color = Color(0xFF777777),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF202020), fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                    )
                }
                if (query.isNotBlank()) {
                    Text(
                        "Limpar",
                        color = XpBlueDark,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onQueryChange("") }.padding(horizontal = 7.dp, vertical = 6.dp),
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(35.dp)
                    .background(XpChrome)
                    .border(1.dp, XpChromeBorder)
                    .padding(horizontal = 5.dp, vertical = 3.dp)
            ) {
                Text("Endereço", color = Color(0xFF5A5A5A), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White)
                        .border(1.dp, Color(0xFF8EA6C7))
                        .padding(horizontal = 5.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(if (currentLocation.removable) R.drawable.drive_sd else R.drawable.drive_hdd),
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    ) {
                        pathEntries.forEachIndexed { index, (label, file) ->
                            if (index > 0) {
                                Text("›", color = Color(0xFF6D7F96), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                            Text(
                                text = label,
                                color = if (index == pathEntries.lastIndex) Color(0xFF202020) else XpBlueDark,
                                fontSize = 12.sp,
                                fontWeight = if (index == pathEntries.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                modifier = Modifier.clickable(enabled = index != pathEntries.lastIndex) { onNavigateTo(file) }
                                    .padding(vertical = 3.dp),
                            )
                        }
                    }
                    Box {
                        Text(
                            text = "▾",
                            color = XpBlueDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable { addressMenu = true }.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                        XpPopupMenu(expanded = addressMenu, onDismiss = { addressMenu = false }) {
                            effectiveLocations.forEach { location ->
                                XpMenuItem(
                                    label = location.label,
                                    icon = if (location.removable) R.drawable.drive_sd else R.drawable.drive_hdd,
                                ) {
                                    addressMenu = false
                                    onNavigateTo(location.root)
                                }
                            }
                            if (pathEntries.size > 1) {
                                XpMenuDivider()
                                pathEntries.dropLast(1).forEach { (label, file) ->
                                    XpMenuItem(label, R.drawable.folder) {
                                        addressMenu = false
                                        onNavigateTo(file)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun canonicalPathOf(file: File): String =
    file.absolutePath.let { path -> if (path.length > 1) path.trimEnd(File.separatorChar) else path }

private fun samePath(a: File, b: File): Boolean = canonicalPathOf(a) == canonicalPathOf(b)

private fun isInsideOrSame(file: File, root: File): Boolean {
    val filePath = canonicalPathOf(file)
    val rootPath = canonicalPathOf(root)
    return filePath == rootPath || filePath.startsWith(rootPath + File.separator)
}

@Composable
private fun XpMenuLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Text(
        text = label,
        color = Color(0xFF202020),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(if (selected || pressed) Color(0xFFDCE9F8) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp)
    )
}

@Composable
private fun XpPopupMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        modifier = Modifier
            .background(Color(0xFFF8F8F2))
            .border(1.dp, Color(0xFF7D8FA6))
            .widthIn(min = 176.dp, max = 250.dp),
        content = content,
    )
}

@Composable
private fun XpMenuItem(
    label: String,
    icon: Int? = null,
    checked: Boolean = false,
    trailing: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(if (pressed && enabled) Color(0xFFD9E9FB) else Color.Transparent)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 7.dp),
    ) {
        Text(
            if (checked) "✓" else "",
            color = XpBlueDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(17.dp),
        )
        if (icon != null) {
            androidx.compose.foundation.Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Fit,
                alpha = if (enabled) 1f else .35f,
            )
        } else {
            Spacer(Modifier.width(20.dp))
        }
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            color = if (enabled) Color(0xFF202020) else Color(0xFF999999),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!trailing.isNullOrBlank()) {
            Text(trailing, color = Color(0xFF303030), fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun XpMenuDivider() {
    HorizontalDivider(color = Color(0xFFD2D2C8), thickness = 1.dp)
}

@Composable
private fun XpClassicToolButton(
    icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .background(if (pressed && enabled) Color(0xFFDCE9F8) else Color.Transparent)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 2.dp, horizontal = 1.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else .30f,
        )
        Text(
            text = label,
            color = if (enabled) Color(0xFF202020) else Color(0xFF999999),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun XpToolbarSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(XpChromeBorder)
    )
}

@Composable
private fun SelectionHeader(
    count: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(XpBlueLight, XpBlueDark)))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        XpIconButton(R.drawable.close, "Fechar seleção", onClose)
        Text(
            selectedCountLabel(count),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        XpIconButton(R.drawable.copy, "Copiar", onCopy)
        XpIconButton(R.drawable.cut, "Recortar", onCut)
        XpIconButton(R.drawable.share, "Compartilhar", onShare)
        XpIconButton(R.drawable.delete, "Excluir", onDelete)
    }
}

@Composable
private fun StorageCard(info: StorageInfo, onClick: () -> Unit) {
    val used = formatBytes(info.usedBytes)
    val free = formatBytes(info.freeBytes)
    val percent = (info.usedFraction * 100f).toInt().coerceIn(0, 100)
    val freePercent = 100 - percent
    val animatedUsedFraction by animateFloatAsState(
        targetValue = info.usedFraction,
        animationSpec = tween(320),
        label = "storageUsedFraction",
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .fillMaxWidth()
            .background(XpPanel)
            .border(1.dp, XpBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.drive_hdd),
                contentDescription = "Armazenamento interno",
                modifier = Modifier.size(29.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Armazenamento", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF202020), modifier = Modifier.weight(1f))
                    Text("$percent% usado", color = XpBlueDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text(
                    "$used usados • $free livres",
                    fontSize = 10.5.sp,
                    color = Color(0xFF303030),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$percent% usado • $freePercent% livre",
                    fontSize = 10.sp,
                    color = XpTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(7.dp))
            Text("Analisar ›", color = XpBlue, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFFE1ECF8)).border(1.dp, XpChromeBorder)) {
            Box(Modifier.fillMaxWidth(animatedUsedFraction.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF61E65E), Color(0xFF13A92E)))))
        }
    }
}

@Composable
private fun PermissionBanner(onRequestAccess: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF5CC))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.warning),
            contentDescription = null,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Permita acesso aos arquivos para navegar e editar o armazenamento.",
            fontSize = 13.sp,
            color = Color(0xFF5F4B00),
            modifier = Modifier.weight(1f)
        )
        XpDialogButton("Permitir", onClick = onRequestAccess)
    }
}

@Composable
private fun SectionTitle(title: String, icon: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(icon), null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF16325B))
    }
}

@Composable
private fun HelpManualDialog(onDismiss: () -> Unit) {
    var expandedTopic by remember { mutableStateOf("Começando") }
    val topics = listOf(
        "Começando" to "Use a barra superior para voltar, avançar, ir ao Início, subir uma pasta, pesquisar, abrir Downloads e acessar a Lixeira. Toque em um arquivo para abrir e segure para selecionar.",
        "Navegação" to "O campo Endereço funciona como caminho navegável. Toque em uma parte do caminho para voltar diretamente até ela. A seta ao lado do endereço permite trocar entre armazenamentos detectados.",
        "Arquivos e pastas" to "A lista e a grade mostram o tipo do arquivo, tamanho e outras informações úteis. O botão de opções abre ações como copiar, mover, renomear, excluir, compartilhar, favoritar e ver propriedades.",
        "Copiar e mover" to "Selecione itens e use Copiar ou Mover. Depois navegue até o destino e use Colar. Quando há algo na área de transferência, o botão Downloads é temporariamente substituído por Colar.",
        "Lixeira" to "Ao excluir, escolha entre Mover para a Lixeira e Apagar permanentemente. A pasta interna e artefatos de lixeira do sistema ficam ocultos da navegação comum. Na Lixeira você pode restaurar, apagar definitivamente ou esvaziar tudo.",
        "Armazenamento" to "Toque no cartão de armazenamento para analisar espaço total, usado e livre. As porcentagens por categoria usam somente os arquivos acessíveis analisados; áreas protegidas do Android podem não entrar na soma. A Lixeira aparece separadamente.",
        "Pesquisa" to "Use Pesquisar para filtrar rapidamente os itens da pasta atual. Ao entrar na busca, a interface é compactada para dar mais espaço aos resultados e ao teclado.",
        "Favoritos" to "Adicione arquivos ou pastas aos Favoritos pelo menu de opções. A lista fica disponível no menu Favoritos do cabeçalho.",
        "Visualizadores" to "Imagens, textos e códigos, HTML, PDF, ZIP, áudio, vídeo e APK podem abrir dentro do Explorador XP. O player de vídeo oferece progresso, ±10 s, velocidade, tela cheia, Ajustar/Preencher e retomada de posição. Se um formato falhar ou não for suportado, use Abrir com outro aplicativo.",
        "Arquivos ocultos" to "No menu Exibir é possível mostrar ou ocultar arquivos ocultos. A pasta interna usada pela Lixeira continua protegida e não aparece na navegação comum.",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .safeDrawingPadding()
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F2))
                    .border(1.dp, XpBorder)
            ) {
                XpDialogTitle("Ajuda", onDismiss)
            Text(
                "Escolha um assunto para ver somente a explicação necessária.",
                fontSize = 12.sp,
                color = XpTextSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                topics.forEach { (title, text) ->
                    HelpTopic(
                        title = title,
                        text = text,
                        expanded = expandedTopic == title,
                        onClick = { expandedTopic = if (expandedTopic == title) "" else title },
                    )
                    Spacer(Modifier.height(6.dp))
                }
                }
            }
        }
    }
}

@Composable
private fun HelpTopic(
    title: String,
    text: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (expanded) Color(0xFFEAF3FF) else Color.White)
            .border(1.dp, if (expanded) XpBorder else Color(0xFFCBD7E5))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (expanded) "▼" else "▶", color = XpBlueDark, fontSize = 11.sp)
            Spacer(Modifier.width(7.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
        }
        if (expanded) {
            Spacer(Modifier.height(7.dp))
            Text(text, fontSize = 12.sp, color = Color(0xFF303030), lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenDeviceInfo: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .safeDrawingPadding()
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp)
                    .heightIn(max = maxHeight)
                    .background(Color(0xFFF8F8F2))
                    .border(1.dp, XpBorder)
            ) {
                XpDialogTitle("Sobre o Explorador XP", onDismiss)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Explorador XP", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
                            Text(
                                "Gerenciador de arquivos Android com identidade inspirada no Windows XP.",
                                fontSize = 12.sp,
                                color = Color(0xFF303030),
                                lineHeight = 16.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    AboutSectionCard("Informações", R.drawable.info) {
                        PropertyLine("Versão", BuildConfig.VERSION_NAME)
                        PropertyLine("versionCode", BuildConfig.VERSION_CODE.toString())
                        PropertyLine("Desenvolvedor", "Adriedson Lemos")
                    }

                    Spacer(Modifier.height(10.dp))
                    AboutSectionCard("Apoiar o projeto", R.drawable.favorites) {
                        Text("PIX", fontSize = 10.5.sp, color = XpTextSecondary)
                        Text(
                            "adriedson@outlook.com",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF202020),
                        )
                        Spacer(Modifier.height(8.dp))
                        XpDialogButton(
                            label = if (copied) "Chave copiada" else "Copiar chave",
                            iconRes = if (copied) R.drawable.check else R.drawable.copy,
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("PIX Explorador XP", "adriedson@outlook.com"))
                                copied = true
                            },
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    AboutSectionCard("Novidades desta versão", R.drawable.file_new) {
                        listOf(
                            "Player interno de vídeo migrado para Media3/ExoPlayer, mantendo o restante dos visualizadores intacto.",
                            "Novos controles: progresso e duração, ±10 s, velocidades de 0.5x a 2x, reiniciar, Ajustar/Preencher e tela cheia.",
                            "Controles somem durante a reprodução, a posição pode ser retomada e erros oferecem abertura externa com mensagem clara.",
                            "Rotação e fechamento preservam melhor o estado e liberam corretamente os recursos do player.",
                        ).forEach { change ->
                            Text("• $change", fontSize = 11.5.sp, color = Color(0xFF303030), lineHeight = 15.sp, modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    AboutSectionCard("Atalhos úteis", R.drawable.help) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                            XpDialogButton("Ajuda", modifier = Modifier.weight(1f), iconRes = R.drawable.help, onClick = onOpenHelp)
                            XpDialogButton("Informações técnicas", modifier = Modifier.weight(1f), iconRes = R.drawable.device_mobile, onClick = onOpenDeviceInfo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSectionCard(
    title: String,
    icon: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0xFFCAD8E8))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(painterResource(icon), null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
        }
        Spacer(Modifier.height(7.dp))
        content()
    }
}

@Composable
private fun TransferProgressDialog(transfer: TransferState, onCancel: () -> Unit) {
    val animatedFraction by animateFloatAsState(
        targetValue = transfer.fraction,
        animationSpec = tween(180),
        label = "transferFraction",
    )
    val title = when (transfer.kind) {
        TransferKind.COPY -> "Copiando ${itemCountLabel(transfer.total)}"
        TransferKind.MOVE -> "Movendo ${itemCountLabel(transfer.total)}"
        TransferKind.DELETE -> "Excluindo ${itemCountLabel(transfer.total)}"
    }
    val icon = when (transfer.kind) {
        TransferKind.COPY -> R.drawable.copy
        TransferKind.MOVE -> R.drawable.move
        TransferKind.DELETE -> R.drawable.delete
    }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .safeDrawingPadding()
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 270.dp, max = 320.dp)
                    .background(Color(0xFFF8F8F2))
                    .border(1.dp, XpBorder)
            ) {
            XpDialogTitle(title, onCancel)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(29.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = transfer.currentName.ifBlank { "Preparando…" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (transfer.done >= transfer.total) 1f else animatedFraction },
                    color = XpBlue,
                    trackColor = Color(0xFFDCE6F2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${transfer.done.coerceAtMost(transfer.total)} de ${transfer.total} • ${(transfer.fraction * 100f).toInt().coerceIn(0, 100)}%",
                    fontSize = 12.sp,
                    color = XpTextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Cancelar", iconRes = R.drawable.close, onClick = onCancel)
                }
            }
        }
    }
    }
}

@Composable
private fun XpDialogTitle(title: String, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Brush.verticalGradient(listOf(XpBlueLight, XpBlueDark)))
            .padding(horizontal = 10.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFF36B58), Color(0xFFB92318))))
                .border(1.dp, Color.White)
                .clickable(onClick = onDismiss),
        ) {
            Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}


@Composable
internal fun XpDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    maxWidth: Int = 340,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .safeDrawingPadding()
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 270.dp, max = maxWidth.dp)
                    .heightIn(max = maxHeight)
                    .background(Color(0xFFF8F8F2))
                    .border(1.dp, Color(0xFF275B9A))
            ) {
                XpDialogTitle(title, onDismiss)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
internal fun XpDialogButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    iconRes: Int? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 32.dp)
            .widthIn(min = 78.dp)
            .background(
                when {
                    !enabled -> Color(0xFFE5E5E5)
                    danger && pressed -> Color(0xFFFFDAD5)
                    danger -> Color(0xFFFFF0EE)
                    pressed -> Color(0xFFD5E7FA)
                    else -> Color(0xFFF7F7F2)
                }
            )
            .border(1.dp, when {
                !enabled -> Color(0xFFB8B8B8)
                danger -> Color(0xFFC34B40)
                else -> Color(0xFF7D8FA6)
            })
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            iconRes?.let {
                androidx.compose.foundation.Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                label,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = when {
                    !enabled -> Color(0xFF999999)
                    danger -> Color(0xFF9C1B12)
                    else -> Color(0xFF202020)
                },
            )
        }
    }
}

@Composable
private fun XpConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    XpDialogFrame(title = title, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.Top) {
            androidx.compose.foundation.Image(
                painter = painterResource(if (danger) R.drawable.warning else R.drawable.info),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(10.dp))
            Text(message, fontSize = 13.sp, color = Color(0xFF303030), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            XpDialogButton("Cancelar", iconRes = R.drawable.close, onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            XpDialogButton(confirmText, danger = danger, iconRes = if (danger) R.drawable.delete else R.drawable.check, onClick = onConfirm)
        }
    }
}

@Composable
private fun DeleteChoiceDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit,
) {
    XpDialogFrame(title = title, onDismiss = onDismiss, maxWidth = 650) {
        Row(verticalAlignment = Alignment.Top) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.trash_full),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(message, fontSize = 13.sp, color = Color(0xFF303030))
                Spacer(Modifier.height(4.dp))
                Text("Mover para a Lixeira permite restaurar depois.", fontSize = 11.sp, color = XpTextSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 540.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Mover para a Lixeira", modifier = Modifier.weight(1f), iconRes = R.drawable.trash_full, onClick = onMoveToTrash)
                    XpDialogButton("Apagar permanentemente", modifier = Modifier.weight(1f), danger = true, iconRes = R.drawable.delete, onClick = onDeletePermanently)
                    XpDialogButton("Cancelar", modifier = Modifier.weight(1f), iconRes = R.drawable.close, onClick = onDismiss)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Mover para a Lixeira", modifier = Modifier.fillMaxWidth(), iconRes = R.drawable.trash_full, onClick = onMoveToTrash)
                    XpDialogButton("Apagar permanentemente", modifier = Modifier.fillMaxWidth(), danger = true, iconRes = R.drawable.delete, onClick = onDeletePermanently)
                    XpDialogButton("Cancelar", modifier = Modifier.fillMaxWidth(), iconRes = R.drawable.close, onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun TrashDialog(
    items: List<TrashItem>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onRestore: (TrashItem) -> Unit,
    onDeletePermanently: (TrashItem) -> Unit,
    onEmpty: () -> Unit,
) {
    var deleteCandidate by remember { mutableStateOf<TrashItem?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .safeDrawingPadding()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F2))
                    .border(1.dp, XpBorder)
            ) {
                XpDialogTitle("Lixeira", onDismiss)
            Column(modifier = Modifier.fillMaxWidth().background(XpChrome)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(if (items.isEmpty()) R.drawable.trash_empty else R.drawable.trash_full),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val trashBytes = items.sumOf { it.size }
                        Text("${itemCountLabel(items.size)} • ${formatBytes(trashBytes)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Restaure itens ou apague-os definitivamente.", fontSize = 11.sp, color = XpTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 7.dp),
                ) {
                    XpDialogButton("Atualizar", enabled = !loading, iconRes = R.drawable.refresh, onClick = onRefresh)
                    Spacer(Modifier.width(6.dp))
                    XpDialogButton("Esvaziar Lixeira", enabled = items.isNotEmpty() && !loading, danger = true, iconRes = R.drawable.delete) { confirmEmpty = true }
                }
            }
            HorizontalDivider(color = XpChromeBorder)

            when {
                loading -> LoadingPanelInline("Carregando Lixeira…")
                items.isEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    androidx.compose.foundation.Image(painterResource(R.drawable.trash_empty), null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("A Lixeira está vazia", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
                    Text("Arquivos enviados para a Lixeira aparecerão aqui.", fontSize = 12.sp, color = XpTextSecondary)
                }
                else -> LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
                    items(items, key = { it.id }) { item ->
                        TrashItemRow(
                            item = item,
                            onRestore = { onRestore(item) },
                            onDelete = { deleteCandidate = item },
                        )
                        HorizontalDivider(color = Color(0xFFD8E1ED))
                    }
                }
            }
        }
    }
    }

    deleteCandidate?.let { item ->
        XpConfirmDialog(
            title = "Apagar permanentemente?",
            message = "\"${item.name}\" será removido de forma definitiva e não poderá ser restaurado.",
            confirmText = "Apagar",
            danger = true,
            onDismiss = { deleteCandidate = null },
            onConfirm = {
                deleteCandidate = null
                onDeletePermanently(item)
            },
        )
    }
    if (confirmEmpty) {
        XpConfirmDialog(
            title = "Esvaziar Lixeira?",
            message = "Todos os itens da Lixeira serão apagados permanentemente.",
            confirmText = "Esvaziar",
            danger = true,
            onDismiss = { confirmEmpty = false },
            onConfirm = {
                confirmEmpty = false
                onEmpty()
            },
        )
    }
}

@Composable
private fun TrashItemRow(
    item: TrashItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(item.id) { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(item.typeLabel)
                    if (!item.isDirectory || item.size > 0L) append(" • ${formatBytes(item.size)}")
                    append(" • ${formatTrashDate(item.deletedAt)}")
                },
                fontSize = 10.sp,
                color = XpTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                compactOriginalPath(item.originalPath),
                fontSize = 9.5.sp,
                color = Color(0xFF66788F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        XpDialogButton("Restaurar", iconRes = R.drawable.move, onClick = onRestore)
        Spacer(Modifier.width(3.dp))
        Box {
            XpIconButton(R.drawable.more, "Mais ações", onClick = { menuExpanded = true }, iconSize = 20, buttonSize = 30)
            XpPopupMenu(expanded = menuExpanded, onDismiss = { menuExpanded = false }) {
                XpMenuItem("Apagar permanentemente", R.drawable.delete) {
                    menuExpanded = false
                    onDelete()
                }
            }
        }
    }
}

@Composable
private fun StorageDetailsDialog(
    state: StorageScanState,
    fallbackInfo: StorageInfo,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    onOpenFolder: (File) -> Unit,
    onOpenFile: (File) -> Unit,
    onOpenTrash: () -> Unit,
) {
    val analysis = state.analysis
    val info = analysis?.storageInfo ?: fallbackInfo
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .safeDrawingPadding()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F8F2))
                    .border(1.dp, XpBorder)
            ) {
                XpDialogTitle("Armazenamento", onDismiss)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().background(XpChrome).padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                androidx.compose.foundation.Image(painterResource(R.drawable.drive_storage), null, modifier = Modifier.size(31.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Armazenamento interno", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF202020))
                    Text(
                        "${formatBytes(info.usedBytes)} usados • ${formatBytes(info.freeBytes)} livres",
                        fontSize = 11.sp,
                        color = XpTextSecondary,
                    )
                    Text(
                        "${storageUsedPercent(info)}% usado • ${100 - storageUsedPercent(info)}% livre • ${formatBytes(info.totalBytes)} total",
                        fontSize = 10.5.sp,
                        color = Color(0xFF66788F),
                    )
                }
                if (state.analyzing) XpDialogButton("Cancelar", iconRes = R.drawable.close, onClick = onCancel)
                else XpDialogButton(if (analysis == null) "Analisar" else "Atualizar análise", iconRes = R.drawable.drive_storage, onClick = onRefresh)
            }
            HorizontalDivider(color = XpChromeBorder)

            if (state.analyzing && analysis == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    CircularProgressIndicator(color = XpBlue)
                    Spacer(Modifier.height(12.dp))
                    Text("Analisando armazenamento…", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
                    Spacer(Modifier.height(4.dp))
                    Text("${state.scannedFiles} arquivos verificados", fontSize = 12.sp, color = XpTextSecondary)
                    Spacer(Modifier.height(5.dp))
                    Text("A análise roda somente nesta tela e pode ser cancelada.", fontSize = 11.sp, color = Color(0xFF66788F))
                }
            } else if (state.error != null && analysis == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(20.dp),
                ) {
                    androidx.compose.foundation.Image(painterResource(R.drawable.warning), null, modifier = Modifier.size(45.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(state.error, fontSize = 13.sp, color = Color(0xFF7D2A20), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    XpDialogButton("Tentar novamente", onClick = onRefresh)
                }
            } else if (analysis != null) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp)
                ) {
                    StorageSummaryCard(info)
                    Spacer(Modifier.height(14.dp))
                    StorageSectionHeader("Por tipo de arquivo", "${formatBytes(analysis.scannedBytes)} acessíveis")
                    Text(
                        "As porcentagens abaixo consideram apenas os arquivos que o Android permitiu ao Explorador XP analisar.",
                        fontSize = 10.5.sp,
                        color = Color(0xFF66788F),
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                    val categorizedBytes = analysis.scannedBytes.coerceAtLeast(1L)
                    analysis.categories.forEach { category ->
                        StorageCategoryRow(category, categorizedBytes)
                    }
                    if (analysis.trashItemCount > 0 || analysis.trashBytes > 0L) {
                        Spacer(Modifier.height(10.dp))
                        StorageTrashRow(analysis.trashItemCount, analysis.trashBytes, onOpenTrash)
                    }

                    Spacer(Modifier.height(16.dp))
                    StorageSectionHeader("Pastas que mais ocupam espaço", "Toque para abrir")
                    if (analysis.topFolders.isEmpty()) {
                        Text("Nenhuma pasta pôde ser analisada.", fontSize = 12.sp, color = XpTextSecondary)
                    } else {
                        analysis.topFolders.forEachIndexed { index, folder ->
                            StorageFolderRow(index + 1, folder) { onOpenFolder(folder.folder) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    StorageSectionHeader("Arquivos grandes", "Toque para abrir")
                    if (analysis.largeFiles.isEmpty()) {
                        Text("Nenhum arquivo encontrado.", fontSize = 12.sp, color = XpTextSecondary)
                    } else {
                        analysis.largeFiles.forEachIndexed { index, file ->
                            StorageLargeFileRow(index + 1, file) { onOpenFile(file.file) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Algumas áreas protegidas do Android podem não permitir leitura completa. Por isso, a soma das categorias pode ser menor que o espaço usado pelo sistema.",
                        fontSize = 10.sp,
                        color = Color(0xFF66788F),
                    )
                }
                if (state.analyzing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF6D9)).padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        CircularProgressIndicator(color = XpBlue, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Atualizando… ${state.scannedFiles} arquivos", fontSize = 11.sp, color = Color(0xFF5F4B00))
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    Text("Toque em Analisar para localizar pastas e arquivos que mais ocupam espaço.", fontSize = 13.sp, color = XpTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
                }
            }
            }
        }
    }
}
@Composable
private fun StorageSummaryCard(info: StorageInfo) {
    val usedPercent = storageUsedPercent(info)
    val freePercent = 100 - usedPercent
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFFCAD8E8)).padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Espaço total", fontSize = 10.5.sp, color = XpTextSecondary)
                Text(formatBytes(info.totalBytes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF202020))
            }
            Text("$usedPercent% usado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "${formatBytes(info.usedBytes)} usados • ${formatBytes(info.freeBytes)} livres",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF303030),
        )
        Text(
            "$usedPercent% usado • $freePercent% livre",
            fontSize = 11.sp,
            color = XpTextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0xFFE1ECF8)).border(1.dp, XpChromeBorder)) {
            Box(Modifier.fillMaxWidth(info.usedFraction.coerceIn(0f, 1f)).fillMaxHeight().background(Color(0xFF39B54A)))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "Esta barra representa o armazenamento total informado pelo Android.",
            fontSize = 9.5.sp,
            color = Color(0xFF66788F),
        )
    }
}

private fun storageUsedPercent(info: StorageInfo): Int =
    (info.usedFraction.coerceIn(0f, 1f) * 100f).toInt().coerceIn(0, 100)

@Composable
private fun StorageSectionHeader(title: String, hint: String) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = XpBlueDark, modifier = Modifier.weight(1f))
        Text(hint, fontSize = 10.sp, color = XpTextSecondary)
    }
}

@Composable
private fun StorageCategoryRow(category: StorageCategorySummary, totalBytes: Long) {
    val fraction = (category.bytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    val percent = (fraction * 100f).toInt().coerceIn(0, 100)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(category.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("$percent% dos acessíveis • ${category.fileCount} • ${formatBytes(category.bytes)}", fontSize = 10.5.sp, color = XpTextSecondary)
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFFE3EAF2))) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(XpBlueLight))
        }
    }
}

@Composable
private fun StorageFolderRow(index: Int, folder: StorageFolderSummary, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        Text("$index", fontSize = 11.sp, color = XpTextSecondary, modifier = Modifier.width(22.dp))
        androidx.compose.foundation.Image(painterResource(FileIconMapper.iconFor(folder.folder, true)), null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.folder.name.ifBlank { folder.folder.absolutePath }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${folder.fileCount} arquivos", fontSize = 10.sp, color = XpTextSecondary)
        }
        Text(formatBytes(folder.bytes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
    }
}

@Composable
private fun StorageLargeFileRow(index: Int, summary: StorageFileSummary, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        Text("$index", fontSize = 11.sp, color = XpTextSecondary, modifier = Modifier.width(22.dp))
        androidx.compose.foundation.Image(painterResource(FileIconMapper.iconFor(summary.file, false)), null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(summary.file.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(summary.typeLabel, fontSize = 10.sp, color = XpTextSecondary)
        }
        Text(formatBytes(summary.bytes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
    }
}

@Composable
private fun LoadingPanelInline(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
    ) {
        CircularProgressIndicator(color = XpBlue)
        Spacer(Modifier.height(9.dp))
        Text(message, fontSize = 12.sp, color = XpTextSecondary)
    }
}

@Composable
private fun HelpSection(title: String, text: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
    Spacer(Modifier.height(3.dp))
    Text(text, fontSize = 12.sp, color = Color(0xFF303030))
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun FileContextDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit,
    onProperties: () -> Unit,
    onSelect: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 270.dp)
                .background(Color(0xFFF8F8F2))
                .border(1.dp, Color(0xFF7D8FA6))
                .padding(vertical = 3.dp)
        ) {
            ContextActionRow(item.iconRes, "Abrir", onOpen)
            HorizontalDivider(color = Color(0xFFD2D2C8))
            ContextActionRow(R.drawable.copy, "Copiar", onCopy)
            ContextActionRow(R.drawable.move, "Mover", onMove)
            ContextActionRow(R.drawable.rename, "Renomear", onRename)
            ContextActionRow(R.drawable.delete, "Excluir", onDelete)
            if (!item.isDirectory) ContextActionRow(R.drawable.share, "Compartilhar", onShare)
            HorizontalDivider(color = Color(0xFFD2D2C8))
            ContextActionRow(
                if (item.isFavorite) R.drawable.favorites else R.drawable.folder_favorite,
                if (item.isFavorite) "Remover dos Favoritos" else "Adicionar aos Favoritos",
                onFavorite,
            )
            ContextActionRow(R.drawable.properties, "Propriedades", onProperties)
            HorizontalDivider(color = Color(0xFFD2D2C8))
            ContextActionRow(R.drawable.select_all, "Selecionar", onSelect)
        }
    }
}

@Composable
private fun FolderContextDialog(
    canPaste: Boolean,
    canSelectAll: Boolean,
    onDismiss: () -> Unit,
    onPaste: () -> Unit,
    onNewFolder: () -> Unit,
    onSelectAll: () -> Unit,
    onProperties: () -> Unit,
    onRefresh: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 210.dp, max = 260.dp)
                .background(Color(0xFFF8F8F2))
                .border(1.dp, Color(0xFF7D8FA6))
                .padding(vertical = 3.dp)
        ) {
            if (canPaste) ContextActionRow(R.drawable.paste, "Colar aqui", onPaste)
            ContextActionRow(R.drawable.folder_new, "Nova pasta", onNewFolder)
            if (canSelectAll) ContextActionRow(R.drawable.select_all, "Selecionar tudo", onSelectAll)
            HorizontalDivider(color = Color(0xFFD2D2C8))
            ContextActionRow(R.drawable.refresh, "Atualizar", onRefresh)
            ContextActionRow(R.drawable.properties, "Propriedades", onProperties)
        }
    }
}

@Composable
private fun XpFileDropdownMenu(
    item: FileItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (FileMenuAction) -> Unit,
) {
    XpPopupMenu(
        expanded = expanded,
        onDismiss = onDismiss,
    ) {
        XpContextMenuItem(item.iconRes, "Abrir") { onAction(FileMenuAction.OPEN) }
        HorizontalDivider(color = Color(0xFFD2D2C8))
        XpContextMenuItem(R.drawable.copy, "Copiar") { onAction(FileMenuAction.COPY) }
        XpContextMenuItem(R.drawable.move, "Mover") { onAction(FileMenuAction.MOVE) }
        XpContextMenuItem(R.drawable.rename, "Renomear") { onAction(FileMenuAction.RENAME) }
        XpContextMenuItem(R.drawable.delete, "Excluir") { onAction(FileMenuAction.DELETE) }
        if (!item.isDirectory) XpContextMenuItem(R.drawable.share, "Compartilhar") { onAction(FileMenuAction.SHARE) }
        HorizontalDivider(color = Color(0xFFD2D2C8))
        XpContextMenuItem(
            if (item.isFavorite) R.drawable.favorites else R.drawable.folder_favorite,
            if (item.isFavorite) "Remover dos Favoritos" else "Adicionar aos Favoritos",
        ) { onAction(FileMenuAction.FAVORITE) }
        XpContextMenuItem(R.drawable.properties, "Propriedades") { onAction(FileMenuAction.PROPERTIES) }
        HorizontalDivider(color = Color(0xFFD2D2C8))
        XpContextMenuItem(R.drawable.select_all, "Selecionar") { onAction(FileMenuAction.SELECT) }
    }
}

@Composable
private fun XpContextMenuItem(icon: Int, label: String, onClick: () -> Unit) {
    XpMenuItem(label = label, icon = icon, onClick = onClick)
}

@Composable
private fun ContextActionRow(icon: Int, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pressed) XpSelection else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(icon), null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(9.dp))
        Text(label, color = Color(0xFF202020), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ContextActionCell(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(icon), null, modifier = Modifier.size(27.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = if (danger) Color(0xFF9C1B12) else Color(0xFF202020),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileList(
    scrollKey: String,
    items: List<FileItem>,
    selectedPaths: Set<String>,
    onItemClick: (FileItem) -> Unit,
    onMenuAction: (FileItem, FileMenuAction) -> Unit,
    onLongSelect: (FileItem) -> Unit,
    onBlankLongPress: () -> Unit,
) {
    val iconsToWarm = remember(items) { items.asSequence().take(32).map { it.iconRes }.distinct().toList() }
    PreloadResourceIcons(iconsToWarm)

    // Estado de rolagem próprio por pasta/aba: reinicia no topo ao navegar,
    // em vez de manter a posição da listagem anterior.
    val listState = remember(scrollKey) { LazyListState() }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White)
            .border(1.dp, XpBorder, RoundedCornerShape(5.dp))
            .combinedClickable(onClick = {}, onLongClick = onBlankLongPress)
    ) {
        items(items, key = { it.path }) { item ->
            FileListRow(
                item = item,
                selected = item.path in selectedPaths,
                onClick = { onItemClick(item) },
                onLongSelect = { onLongSelect(item) },
                onMenuAction = { action -> onMenuAction(item, action) },
            )
            HorizontalDivider(color = Color(0xFFD8E1ED), thickness = 1.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListRow(
    item: FileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongSelect: () -> Unit,
    onMenuAction: (FileMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFDCEBFC) else Color.Transparent)
            .border(if (selected) 1.dp else 0.dp, if (selected) XpBlue else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongSelect)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp)) {
            FileVisual(item = item, size = 38.dp)
            if (selected) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.check),
                    contentDescription = "Selecionado",
                    modifier = Modifier.align(Alignment.BottomEnd).size(17.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.isFavorite) {
                    androidx.compose.foundation.Image(
                        painterResource(R.drawable.folder_favorite),
                        null,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Text(
                text = if (item.isDirectory) item.typeLabel else "${item.typeLabel} • ${formatBytes(item.size)}",
                color = Color(0xFF4E6077),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${FileDisplayFormatter.date(item.modifiedAt)} • ${FileDisplayFormatter.time(item.modifiedAt)}",
                color = XpTextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        var menuExpanded by remember(item.path) { mutableStateOf(false) }
        Box {
            XpIconButton(R.drawable.more, "Opções", onClick = { menuExpanded = true }, iconSize = 22, buttonSize = 30)
            XpFileDropdownMenu(
                item = item,
                expanded = menuExpanded,
                onDismiss = { menuExpanded = false },
                onAction = { action -> menuExpanded = false; onMenuAction(action) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGrid(
    scrollKey: String,
    items: List<FileItem>,
    selectedPaths: Set<String>,
    onItemClick: (FileItem) -> Unit,
    onMenuAction: (FileItem, FileMenuAction) -> Unit,
    onLongSelect: (FileItem) -> Unit,
    onBlankLongPress: () -> Unit,
) {
    val iconsToWarm = remember(items) { items.asSequence().take(32).map { it.iconRes }.distinct().toList() }
    PreloadResourceIcons(iconsToWarm)

    val gridState = remember(scrollKey) { LazyGridState() }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(7.dp)
            .combinedClickable(onClick = {}, onLongClick = onBlankLongPress),
    ) {
        items(items, key = { it.path }) { item ->
            val selected = item.path in selectedPaths
            var menuExpanded by remember(item.path) { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (selected) Color(0xFFDCEBFC) else Color.White)
                    .border(
                        if (selected) 2.dp else 1.dp,
                        if (selected) XpBlue else Color(0xFFCAD8E8),
                        RoundedCornerShape(3.dp),
                    )
                    .combinedClickable(onClick = { onItemClick(item) }, onLongClick = { onLongSelect(item) })
                    .padding(horizontal = 7.dp, vertical = 7.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                ) {
                    FileVisual(item = item, size = 62.dp)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        item.name,
                        fontSize = 12.5.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.gridDetailText,
                        fontSize = 10.5.sp,
                        color = XpTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(3.dp))
                }

                if (selected) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.check),
                        contentDescription = "Selecionado",
                        modifier = Modifier.align(Alignment.TopStart).size(18.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    XpIconButton(
                        R.drawable.more,
                        "Opções",
                        onClick = { menuExpanded = true },
                        iconSize = 20,
                        buttonSize = 28,
                    )
                    XpFileDropdownMenu(
                        item = item,
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onAction = { action -> menuExpanded = false; onMenuAction(item, action) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplorerStatusBar(
    items: List<FileItem>,
    selectedPaths: Set<String>,
) {
    val selectedItems = remember(items, selectedPaths) { items.filter { it.path in selectedPaths } }
    val sizeBytes = remember(items, selectedItems) {
        val shownItems = if (selectedItems.isNotEmpty()) selectedItems else items
        shownItems.asSequence().filterNot { it.isDirectory }.sumOf { it.size }
    }
    val formattedSize = remember(sizeBytes) { formatBytes(sizeBytes) }
    val itemCount = items.size
    val folderCount = remember(items) { items.count { it.isDirectory } }
    val fileCount = itemCount - folderCount
    val single = selectedItems.singleOrNull()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(XpChrome)
            .border(1.dp, XpChromeBorder)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = when {
                single != null -> "1 selecionado • ${single.typeLabel}"
                selectedItems.isNotEmpty() -> if (sizeBytes > 0L) "${selectedItems.size} selecionados • $formattedSize" else "${selectedItems.size} selecionados"
                else -> "$itemCount ${if (itemCount == 1) "item" else "itens"} • $folderCount ${if (folderCount == 1) "pasta" else "pastas"} • $fileCount arquivos"
            },
            color = Color(0xFF303030),
            fontSize = 12.sp,
            fontWeight = if (selectedItems.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val trailing = when {
            single?.isDirectory == true -> "Pasta"
            sizeBytes > 0L -> formattedSize
            else -> null
        }
        if (trailing != null) {
            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(XpChromeBorder))
            Text(
                text = trailing,
                color = Color(0xFF303030),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(start = 9.dp),
            )
        }
    }
}

@Composable
private fun XpToolbarButton(
    icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFDCEBFF))))
            .border(1.dp, XpBorder, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(icon), null, modifier = Modifier.size(34.dp), alpha = if (enabled) 1f else .35f)
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (enabled) Color(0xFF183363) else Color.Gray, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun XpIconButton(icon: Int, contentDescription: String, onClick: () -> Unit, iconSize: Int = 30, buttonSize: Int = 44) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(buttonSize.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (pressed) Color(0xFFDCE9F8) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun EmptyState(tab: ExplorerTab, query: String, currentDir: File) {
    val restricted = query.isBlank() && tab != ExplorerTab.FAVORITES && isAndroidRestrictedDirectory(currentDir)
    val title = when {
        query.isNotBlank() -> "Nenhum resultado para “$query”."
        restricted -> "Acesso limitado pelo Android"
        tab == ExplorerTab.DOWNLOADS -> "A pasta Downloads está vazia."
        tab == ExplorerTab.FAVORITES -> "Nenhum favorito ainda."
        else -> "Esta pasta está vazia."
    }
    val subtitle = when {
        query.isNotBlank() -> "Tente pesquisar outro nome ou limpe o campo de busca."
        restricted -> "Versões recentes do Android restringem o acesso direto ao conteúdo desta pasta. Ela pode conter arquivos mesmo quando o Explorador XP não consegue listá-los."
        tab == ExplorerTab.DOWNLOADS -> "Os arquivos baixados aparecerão aqui."
        tab == ExplorerTab.FAVORITES -> "Use o menu de um arquivo ou pasta e toque em “Adicionar aos Favoritos”."
        else -> "Crie uma pasta ou copie arquivos para este local."
    }
    val icon = when {
        restricted -> R.drawable.locked
        query.isNotBlank() -> R.drawable.search_large
        tab == ExplorerTab.FAVORITES -> R.drawable.favorites
        else -> R.drawable.folder_open_large
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(28.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color(0xFF234A79), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = XpTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NameDialog(
    title: String,
    initialValue: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    XpDialogFrame(title = title, onDismiss = onDismiss) {
        Text("Nome", color = XpTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color.White)
                .border(1.dp, Color(0xFF7F9DB9))
                .padding(horizontal = 8.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(color = Color(0xFF202020), fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            XpDialogButton("Cancelar", onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            XpDialogButton(
                label = confirmText,
                enabled = value.isNotBlank(),
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
            )
        }
    }
}

private data class FilePropertiesInfo(
    val isDirectory: Boolean,
    val extension: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val canRead: Boolean,
    val canWrite: Boolean,
)

@Composable
private fun PropertiesDialog(file: File, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var info by remember(file.absolutePath) { mutableStateOf<FilePropertiesInfo?>(null) }
    LaunchedEffect(file.absolutePath) {
        info = withContext(Dispatchers.IO) {
            FilePropertiesInfo(
                isDirectory = file.isDirectory,
                extension = file.extension,
                sizeBytes = if (file.isFile) file.length() else 0L,
                createdAt = fileCreationTime(file),
                modifiedAt = file.lastModified(),
                canRead = file.canRead(),
                canWrite = file.canWrite(),
            )
        }
    }

    XpDialogFrame(title = "Propriedades", onDismiss = onDismiss, maxWidth = 370) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CachedResourceIcon(
                resId = FileIconMapper.iconFor(file),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(file.parentFile?.name.orEmpty(), color = XpTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(12.dp))
        XpMenuDivider()
        Spacer(Modifier.height(10.dp))
        val current = info
        if (current == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = XpBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Carregando detalhes…", color = XpTextSecondary, fontSize = 13.sp)
            }
        } else {
            PropertyLine("Tipo", FileTypeClassifier.labelFor(file, current.isDirectory))
            if (!current.isDirectory) PropertyLine("Tamanho", formatBytes(current.sizeBytes))
            PropertyLine("Criado", formatDateTime(current.createdAt))
            PropertyLine("Modificado", formatDateTime(current.modifiedAt))
            PropertyLine("Leitura", if (current.canRead) "Sim" else "Não")
            PropertyLine("Escrita", if (current.canWrite) "Sim" else "Não")
            Spacer(Modifier.height(8.dp))
            Text("Caminho", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = XpTextSecondary)
            Text(file.absolutePath, fontSize = 11.sp, color = Color(0xFF303030), maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                XpDialogButton("Copiar nome") { copyTextToClipboard(context, "Nome do arquivo", file.name) }
                XpDialogButton("Copiar caminho") { copyTextToClipboard(context, "Caminho", file.absolutePath) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            XpDialogButton("Fechar", onClick = onDismiss)
        }
    }
}

@Composable
private fun PropertyLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("$label:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(88.dp))
        Text(value, fontSize = 12.sp, color = Color(0xFF303030), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SortDialog(current: SortMode, onDismiss: () -> Unit, onSelect: (SortMode) -> Unit) {
    XpDialogFrame(title = "Ordenar por", onDismiss = onDismiss) {
        SortMode.entries.forEach { mode ->
            val label = when (mode) {
                SortMode.NAME -> "Nome"
                SortMode.DATE -> "Data"
                SortMode.SIZE -> "Tamanho"
                SortMode.TYPE -> "Tipo"
            }
            ContextActionRow(
                icon = R.drawable.sort,
                label = if (mode == current) "✓  $label" else label,
                onClick = { onSelect(mode) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            XpDialogButton("Cancelar", onClick = onDismiss)
        }
    }
}

private fun selectedCountLabel(count: Int): String = if (count == 1) "1 selecionado" else "$count selecionados"

private fun itemCountLabel(count: Int): String = if (count == 1) "1 item" else "$count itens"

private fun isAndroidRestrictedDirectory(directory: File): Boolean {
    val path = directory.absolutePath.replace('\\', '/').trimEnd('/')
    return path.endsWith("/Android/data", ignoreCase = true) || path.endsWith("/Android/obb", ignoreCase = true)
}

private fun compactOriginalPath(path: String): String {
    val normalized = path.replace('\\', '/')
    val marker = "/storage/emulated/0/"
    return normalized.substringAfter(marker, normalized).ifBlank { normalized }
}

private fun formatTrashDate(time: Long): String = "Excluído em ${formatDate(time)} ${formatTime(time)}"

private fun copyTextToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

@Composable
private fun StorageTrashRow(itemCount: Int, bytes: Long, onOpen: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE0C987))
            .clickable(onClick = onOpen)
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        androidx.compose.foundation.Image(painterResource(R.drawable.trash_full), null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Lixeira", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4710))
            Text("${itemCountLabel(itemCount)} • ${formatBytes(bytes)}", fontSize = 10.5.sp, color = Color(0xFF78642F))
        }
        Text("Abrir ›", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
    }
}

private fun formatDate(time: Long): String {
    if (time <= 0L) return "Data desconhecida"
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("pt-BR")).format(Date(time))
}

private fun formatTime(time: Long): String {
    if (time <= 0L) return "--:--"
    return DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag("pt-BR")).format(Date(time))
}

private fun formatDateTime(time: Long): String {
    if (time <= 0L) return "Data desconhecida"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.forLanguageTag("pt-BR")).format(Date(time))
}

private fun fileCreationTime(file: File): Long {
    return runCatching {
        java.nio.file.Files.readAttributes(
            file.toPath(),
            java.nio.file.attribute.BasicFileAttributes::class.java,
            java.nio.file.LinkOption.NOFOLLOW_LINKS,
        ).creationTime().toMillis()
    }.getOrNull()?.takeIf { it > 0L } ?: file.lastModified()
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (value >= 100) String.format(Locale.forLanguageTag("pt-BR"), "%.0f %s", value, units[index])
    else String.format(Locale.forLanguageTag("pt-BR"), "%.1f %s", value, units[index])
}
