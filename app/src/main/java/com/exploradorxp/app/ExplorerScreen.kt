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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    var showDonation by remember { mutableStateOf(false) }
    var showDeviceInfo by remember { mutableStateOf(false) }

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
            onShowDonation = { showDonation = true },
            onShowDeviceInfo = { showDeviceInfo = true },
        )

        val internalRoot = state.storageLocations.firstOrNull { !it.removable }?.root
            ?: android.os.Environment.getExternalStorageDirectory()
        val isHomePage = state.tab == ExplorerTab.FILES && samePath(state.currentDir, internalRoot)

        if (isHomePage && !state.searchVisible && state.selectedPaths.isEmpty()) {
            StorageCard(state.storageInfo)
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
                EmptyState(state.tab, state.query)
            } else if (state.viewMode == ViewMode.LIST) {
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
        XpConfirmDialog(
            title = "Excluir item(ns)?",
            message = "Essa ação remove os itens selecionados do armazenamento.",
            confirmText = "Excluir",
            danger = true,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
        )
    }

    deleteTarget?.let { target ->
        XpConfirmDialog(
            title = "Excluir item?",
            message = "Deseja excluir ${target.name}?",
            confirmText = "Excluir",
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
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
    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showDonation) DonationDialog(onDismiss = { showDonation = false })
    if (showDeviceInfo) DeviceInfoDialog(onDismiss = { showDeviceInfo = false })

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
    onShowDonation: () -> Unit,
    onShowDeviceInfo: () -> Unit,
) {
    var fileMenu by remember { mutableStateOf(false) }
    var editMenu by remember { mutableStateOf(false) }
    var viewMenu by remember { mutableStateOf(false) }
    var favoritesMenu by remember { mutableStateOf(false) }
    var toolsMenu by remember { mutableStateOf(false) }
    var organizeMenu by remember { mutableStateOf(false) }
    var helpMenu by remember { mutableStateOf(false) }
    var addressMenu by remember { mutableStateOf(false) }

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
                    selectionCount > 0 -> "$selectionCount selecionado(s)"
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
                    modifier = Modifier.clickable(onClick = onClearSelection).padding(horizontal = 8.dp, vertical = 6.dp)
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
                    .height(26.dp)
                    .background(XpChrome)
                    .border(1.dp, XpChromeBorder)
                    .padding(horizontal = 3.dp)
            ) {
                Box {
                    XpMenuLabel("Arquivo", fileMenu) { fileMenu = true }
                    XpPopupMenu(expanded = fileMenu, onDismiss = { fileMenu = false }) {
                        XpMenuItem("Nova pasta", R.drawable.folder_new) { fileMenu = false; onNewFolder() }
                        if (canPaste) XpMenuItem("Colar", R.drawable.paste) { fileMenu = false; onPaste() }
                        XpMenuDivider()
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
                        XpMenuItem("Atualizar", R.drawable.refresh) { editMenu = false; onRefresh() }
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
                        XpMenuDivider()
                        XpMenuItem("Atualizar", R.drawable.refresh) { viewMenu = false; onRefresh() }
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
                        XpMenuDivider()
                        XpMenuItem("Atualizar", R.drawable.refresh) { toolsMenu = false; onRefresh() }
                    }
                }

                Box {
                    XpMenuLabel("Ajuda", helpMenu) { helpMenu = true }
                    XpPopupMenu(expanded = helpMenu, onDismiss = { helpMenu = false }) {
                        XpMenuItem("Manual de Ajuda", R.drawable.help) { helpMenu = false; onShowManual() }
                        XpMenuDivider()
                        XpMenuItem("Sobre", R.drawable.info) { helpMenu = false; onShowAbout() }
                        XpMenuItem("Doação", R.drawable.favorites) { helpMenu = false; onShowDonation() }
                    }
                }
            }
        }

        if (!searchMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (selectionCount > 0) 54.dp else 52.dp)
                    .background(XpChrome)
                    .border(1.dp, XpChromeBorder)
                    .padding(horizontal = 2.dp)
            ) {
                if (selectionCount > 0) {
                    XpClassicToolButton(R.drawable.copy, "Copiar", true, onCopySelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.move, "Mover", true, onCutSelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.delete, "Excluir", true, onDeleteSelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.rename, "Renomear", selectionCount == 1, onRenameSelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.share, "Compart.", true, onShareSelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.properties, "Propried.", selectionCount == 1, onPropertiesSelection, Modifier.weight(1f))
                    XpClassicToolButton(R.drawable.select_all, "Todos", true, onSelectAll, Modifier.weight(1f))
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
                        if (viewMode == ViewMode.LIST) R.drawable.view_grid else R.drawable.view_list,
                        "Exibir",
                        true,
                        onToggleView,
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
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(if (selected || pressed) Color(0xFFDCE9F8) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
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
            fontSize = 10.sp,
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
            "$count selecionado(s)",
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
private fun StorageCard(info: StorageInfo) {
    val used = formatBytes(info.usedBytes)
    val free = formatBytes(info.freeBytes)
    val total = formatBytes(info.totalBytes)
    // Anima a transição da barra ao trocar de volume (interno/SD) em vez de saltar direto ao novo valor.
    val animatedUsedFraction by animateFloatAsState(
        targetValue = info.usedFraction,
        animationSpec = tween(320),
        label = "storageUsedFraction",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(XpPanel)
            .border(1.dp, XpBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.drive_hdd),
            contentDescription = "Armazenamento interno",
            modifier = Modifier.size(38.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(9.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, XpBorder, RoundedCornerShape(8.dp))
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(animatedUsedFraction.coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(Brush.verticalGradient(listOf(Color(0xFF61E65E), Color(0xFF13A92E))))
            ) {
                Text(
                    text = "$used usados",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight((1f - animatedUsedFraction).coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(Color(0xFFE1ECF8))
            ) {
                Text(
                    text = "Livre: $free",
                    color = XpBlueDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }

        Spacer(Modifier.width(9.dp))
        Text(
            text = "Total: $total",
            color = XpBlueDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
        )
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
        modifier = Modifier.fillMaxWidth().padding(12.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(icon), null, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF16325B))
    }
}

@Composable
private fun HelpManualDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .background(Color(0xFFF8F8F2))
                .border(1.dp, XpBorder)
        ) {
            XpDialogTitle("Manual de Ajuda", onDismiss)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                HelpSection("Navegação", "Use Voltar e Avançar para percorrer o histórico, Início para retornar ao armazenamento principal e Subir para voltar uma pasta. O campo Endereço permite trocar entre os armazenamentos disponíveis.")
                HelpSection("Arquivos e pastas", "Toque para abrir. Toque e segure para entrar no modo de seleção. O botão de opções do item abre o menu contextual clássico com ações rápidas.")
                HelpSection("Copiar, mover e colar", "Selecione um ou mais itens e use Copiar ou Mover. Ao navegar até a pasta de destino, o botão Downloads é temporariamente substituído por Colar. Também é possível segurar uma área vazia e escolher Colar aqui.")
                HelpSection("Downloads", "O botão Downloads abre diretamente a pasta Download do armazenamento interno.")
                HelpSection("Favoritos", "Adicione arquivos ou pastas aos Favoritos pelo menu contextual. A lista de Favoritos fica disponível no menu Favoritos do cabeçalho.")
                HelpSection("Armazenamento", "Na página inicial são exibidos espaço usado, livre e total. O campo Endereço mostra o armazenamento interno e cartões SD detectados.")
                HelpSection("Visualizador interno", "Imagens, textos e códigos, HTML, PDF, ZIP, áudio, vídeo e APK podem ser abertos dentro do Explorador XP. Formatos ainda não suportados continuam disponíveis em Abrir com...")
                HelpSection("Organização", "Em Ferramentas > Organizar escolha Nome, Data, Tamanho ou Tipo e ative ou desative Pastas primeiro.")
                HelpSection("Arquivos ocultos", "Em Exibir é possível mostrar ou ocultar arquivos ocultos. A preferência fica salva.")
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 270.dp, max = 330.dp)
                .background(Color(0xFFF8F8F2))
                .border(1.dp, XpBorder)
        ) {
            XpDialogTitle("Sobre", onDismiss)
            Column(Modifier.padding(18.dp)) {
                Text("Explorador XP", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
                Spacer(Modifier.height(6.dp))
                Text("Versão ${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = XpTextSecondary)
                Spacer(Modifier.height(18.dp))
                Text("Desenvolvido por Adriedson Lemos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DonationDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 270.dp, max = 330.dp)
                .background(Color(0xFFF8F8F2))
                .border(1.dp, XpBorder)
        ) {
            XpDialogTitle("Doação", onDismiss)
            Column(Modifier.padding(18.dp)) {
                Text("Chave PIX", fontSize = 12.sp, color = XpTextSecondary)
                Spacer(Modifier.height(5.dp))
                Text("adriedson@outlook.com", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
            }
        }
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
        TransferKind.COPY -> "Copiando arquivos"
        TransferKind.MOVE -> "Movendo arquivos"
        TransferKind.DELETE -> "Excluindo arquivos"
    }
    val icon = when (transfer.kind) {
        TransferKind.COPY -> R.drawable.copy
        TransferKind.MOVE -> R.drawable.cut
        TransferKind.DELETE -> R.drawable.delete
    }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .background(Color(0xFFF8F8F2))
                .border(1.dp, XpBorder)
        ) {
            XpDialogTitle(title, onCancel)
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = transfer.currentName.ifBlank { "Preparando…" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    color = XpBlue,
                    trackColor = Color(0xFFDCE6F2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${transfer.done} de ${transfer.total} item(ns) • ${(animatedFraction * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = XpTextSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    XpDialogButton("Cancelar", onClick = onCancel)
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
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(6.dp))
    }
}


@Composable
internal fun XpDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    maxWidth: Int = 340,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 270.dp, max = maxWidth.dp)
                .background(Color(0xFFF8F8F2))
                .border(1.dp, Color(0xFF275B9A))
        ) {
            XpDialogTitle(title, onDismiss)
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
internal fun XpDialogButton(
    label: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 78.dp)
            .background(
                when {
                    !enabled -> Color(0xFFE5E5E5)
                    pressed -> Color(0xFFD5E7FA)
                    else -> Color(0xFFF7F7F2)
                }
            )
            .border(1.dp, if (enabled) Color(0xFF7D8FA6) else Color(0xFFB8B8B8))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> Color(0xFF999999)
                danger -> Color(0xFF9C1B12)
                else -> Color(0xFF202020)
            },
        )
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
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(10.dp))
            Text(message, fontSize = 13.sp, color = Color(0xFF303030), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            XpDialogButton("Cancelar", onClick = onDismiss)
            Spacer(Modifier.width(8.dp))
            XpDialogButton(confirmText, danger = danger, onClick = onConfirm)
        }
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
            .padding(horizontal = 8.dp, vertical = 5.dp)
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
                item.listDetailText,
                color = XpTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        var menuExpanded by remember(item.path) { mutableStateOf(false) }
        Box {
            XpIconButton(R.drawable.more, "Opções", onClick = { menuExpanded = true }, iconSize = 22, buttonSize = 32)
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
        columns = GridCells.Adaptive(minSize = 112.dp),
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
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) Color(0xFFDCEBFC) else Color.White)
                    .border(
                        if (selected) 2.dp else 1.dp,
                        if (selected) XpBlue else Color(0xFFCAD8E8),
                        RoundedCornerShape(7.dp),
                    )
                    .combinedClickable(onClick = { onItemClick(item) }, onLongClick = { onLongSelect(item) })
                    .padding(horizontal = 7.dp, vertical = 8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                ) {
                    FileVisual(item = item, size = 62.dp)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        item.name,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.gridDetailText,
                        fontSize = 11.sp,
                        color = XpTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
    val (sizeBytes, selectedCount) = remember(items, selectedPaths) {
        val selectedItems = items.filter { it.path in selectedPaths }
        val shownItems = if (selectedItems.isNotEmpty()) selectedItems else items
        shownItems.asSequence().filterNot { it.isDirectory }.sumOf { it.size } to selectedItems.size
    }
    val formattedSize = remember(sizeBytes) { formatBytes(sizeBytes) }
    val itemCount = items.size

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
            text = buildString {
                append("$itemCount ${if (itemCount == 1) "item" else "itens"}")
                if (selectedCount > 0) append("  •  $selectedCount selecionado(s)")
            },
            color = Color(0xFF303030),
            fontSize = 12.sp,
            fontWeight = if (selectedCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(XpChromeBorder))
        Text(
            text = formattedSize,
            color = Color(0xFF303030),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(start = 9.dp),
        )
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
private fun EmptyState(tab: ExplorerTab, query: String) {
    val title = when {
        query.isNotBlank() -> "Nenhum resultado para “$query”."
        tab == ExplorerTab.DOWNLOADS -> "A pasta Downloads está vazia."
        tab == ExplorerTab.FAVORITES -> "Nenhum favorito ainda."
        else -> "Esta pasta está vazia."
    }
    val subtitle = when {
        query.isNotBlank() -> "Tente pesquisar outro nome ou limpe o campo de busca."
        tab == ExplorerTab.DOWNLOADS -> "Os arquivos baixados aparecerão aqui."
        tab == ExplorerTab.FAVORITES -> "Use o menu de um arquivo ou pasta e toque em “Adicionar aos Favoritos”."
        else -> "Crie uma pasta ou copie arquivos para este local."
    }
    val icon = when {
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
            modifier = Modifier.size(78.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            color = Color(0xFF234A79),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            subtitle,
            color = XpTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
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
            PropertyLine("Tipo", if (current.isDirectory) "Pasta" else current.extension.ifBlank { "Arquivo" }.uppercase())
            if (!current.isDirectory) PropertyLine("Tamanho", formatBytes(current.sizeBytes))
            PropertyLine("Criado", formatDateTime(current.createdAt))
            PropertyLine("Modificado", formatDateTime(current.modifiedAt))
            PropertyLine("Leitura", if (current.canRead) "Sim" else "Não")
            PropertyLine("Escrita", if (current.canWrite) "Sim" else "Não")
            Spacer(Modifier.height(8.dp))
            Text("Caminho", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = XpTextSecondary)
            Text(file.absolutePath, fontSize = 11.sp, color = Color(0xFF303030))
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
