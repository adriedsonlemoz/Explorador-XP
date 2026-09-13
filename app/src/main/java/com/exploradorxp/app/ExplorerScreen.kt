package com.exploradorxp.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun ExplorerScreen(
    state: ExplorerUiState,
    accessGranted: Boolean,
    onRequestAccess: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onNavigateTo: (File) -> Unit,
    onToggleSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleView: () -> Unit,
    onToggleHidden: (Boolean) -> Unit,
    onSortMode: (SortMode) -> Unit,
    onTabChange: (ExplorerTab) -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectOnly: (File) -> Unit,
    onToggleFavorite: (File) -> Unit,
    onCreateFolder: (String) -> Unit,
    onRename: (File, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNewFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var propertiesTarget by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XpBackground)
    ) {
        if (state.selectedPaths.isEmpty()) {
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
                onSort = { showSort = true },
                canBack = state.canGoBack,
                canForward = state.canGoForward,
                onBack = onBack,
                onForward = onForward,
                onUp = onUp,
                viewMode = state.viewMode,
                onToggleView = onToggleView,
                showHidden = state.showHidden,
                onShowHiddenChange = onToggleHidden,
                onNavigateTo = onNavigateTo,
                onOpenDownloads = { onTabChange(ExplorerTab.DOWNLOADS) },
                onOpenFavorites = { onTabChange(ExplorerTab.FAVORITES) },
            )
        } else {
            SelectionHeader(
                count = state.selectedPaths.size,
                onClose = onClearSelection,
                onCopy = onCopy,
                onCut = onCut,
                onShare = onShare,
                onDelete = { showDeleteConfirm = true },
            )
        }

        if (state.tab != ExplorerTab.FAVORITES) {
            StorageCard(state.storageInfo)
        } else {
            SectionTitle(title = "Favoritos", icon = R.drawable.favorites)
        }

        if (!accessGranted && state.tab != ExplorerTab.FAVORITES) {
            PermissionBanner(onRequestAccess)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.loading) {
                CircularProgressIndicator(
                    color = XpBlue,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.items.isEmpty()) {
                EmptyState(state.tab, state.query)
            } else if (state.viewMode == ViewMode.LIST) {
                FileList(
                    items = state.items,
                    selectedPaths = state.selectedPaths,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                    onSelectOnly = onSelectOnly,
                    onRename = { renameTarget = it },
                    onProperties = { propertiesTarget = it },
                    onToggleFavorite = onToggleFavorite,
                )
            } else {
                FileGrid(
                    items = state.items,
                    selectedPaths = state.selectedPaths,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
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
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir item(ns)?") },
            text = { Text("Essa ação remove os itens selecionados do armazenamento.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Excluir", color = Color(0xFFB00020)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    if (showSort) {
        SortDialog(
            current = state.sortMode,
            onDismiss = { showSort = false },
            onSelect = {
                showSort = false
                onSortMode(it)
            }
        )
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
    onSort: () -> Unit,
    canBack: Boolean,
    canForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    viewMode: ViewMode,
    onToggleView: () -> Unit,
    showHidden: Boolean,
    onShowHiddenChange: (Boolean) -> Unit,
    onNavigateTo: (File) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    var fileMenu by remember { mutableStateOf(false) }
    var editMenu by remember { mutableStateOf(false) }
    var viewMenu by remember { mutableStateOf(false) }
    var favoritesMenu by remember { mutableStateOf(false) }
    var toolsMenu by remember { mutableStateOf(false) }
    var helpMenu by remember { mutableStateOf(false) }
    var addressMenu by remember { mutableStateOf(false) }

    val root = android.os.Environment.getExternalStorageDirectory()
    val pathEntries = remember(currentDir.absolutePath) {
        buildList<Pair<String, File>> {
            add("Armazenamento interno" to root)
            val relative = currentDir.absolutePath.removePrefix(root.absolutePath).trim('/')
            if (relative.isNotBlank()) {
                var cursor = root
                relative.split('/').filter(String::isNotBlank).forEach { part ->
                    cursor = File(cursor, part)
                    add(part to cursor)
                }
            }
        }
    }
    val displayPath = pathEntries.joinToString("  ›  ") { it.first }

    Column(Modifier.fillMaxWidth()) {
        // Barra de título clássica do Explorer XP
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF2F92F6), Color(0xFF0A67D8), Color(0xFF0752B8))))
                .padding(horizontal = 7.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.folder),
                contentDescription = null,
                modifier = Modifier.size(27.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Explorador",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            XpWindowControl("—", Color(0xFF2C78D1))
            Spacer(Modifier.width(2.dp))
            XpWindowControl("□", Color(0xFF2C78D1))
            Spacer(Modifier.width(2.dp))
            XpWindowControl("×", Color(0xFFE65236))
        }

        // Menu clássico: Arquivo / Editar / Exibir / Favoritos / Ferramentas / Ajuda
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(27.dp)
                .background(XpChrome)
                .border(1.dp, XpChromeBorder)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            Box {
                XpMenuLabel("Arquivo", fileMenu) { fileMenu = true }
                DropdownMenu(expanded = fileMenu, onDismissRequest = { fileMenu = false }) {
                    DropdownMenuItem(text = { Text("Nova pasta") }, onClick = { fileMenu = false; onNewFolder() })
                    if (canPaste) DropdownMenuItem(text = { Text("Colar") }, onClick = { fileMenu = false; onPaste() })
                    DropdownMenuItem(text = { Text("Atualizar") }, onClick = { fileMenu = false; onRefresh() })
                }
            }

            Box {
                XpMenuLabel("Editar", editMenu) { editMenu = true }
                DropdownMenu(expanded = editMenu, onDismissRequest = { editMenu = false }) {
                    if (canPaste) DropdownMenuItem(text = { Text("Colar") }, onClick = { editMenu = false; onPaste() })
                    DropdownMenuItem(text = { Text("Atualizar") }, onClick = { editMenu = false; onRefresh() })
                }
            }

            Box {
                XpMenuLabel("Exibir", viewMenu) { viewMenu = true }
                DropdownMenu(expanded = viewMenu, onDismissRequest = { viewMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (viewMode == ViewMode.LIST) "Exibir como ícones" else "Exibir como lista") },
                        onClick = { viewMenu = false; onToggleView() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (showHidden) "Ocultar arquivos ocultos ✓" else "Mostrar arquivos ocultos") },
                        onClick = { viewMenu = false; onShowHiddenChange(!showHidden) },
                    )
                    DropdownMenuItem(text = { Text("Ordenar por...") }, onClick = { viewMenu = false; onSort() })
                    DropdownMenuItem(text = { Text("Atualizar") }, onClick = { viewMenu = false; onRefresh() })
                }
            }

            Box {
                XpMenuLabel("Favoritos", favoritesMenu) { favoritesMenu = true }
                DropdownMenu(expanded = favoritesMenu, onDismissRequest = { favoritesMenu = false }) {
                    DropdownMenuItem(text = { Text("Abrir Favoritos") }, onClick = { favoritesMenu = false; onOpenFavorites() })
                }
            }

            Box {
                XpMenuLabel("Ferramentas", toolsMenu) { toolsMenu = true }
                DropdownMenu(expanded = toolsMenu, onDismissRequest = { toolsMenu = false }) {
                    DropdownMenuItem(text = { Text("Ordenar") }, onClick = { toolsMenu = false; onSort() })
                    DropdownMenuItem(text = { Text("Atualizar") }, onClick = { toolsMenu = false; onRefresh() })
                }
            }

            Box {
                XpMenuLabel("Ajuda", helpMenu) { helpMenu = true }
                DropdownMenu(expanded = helpMenu, onDismissRequest = { helpMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Explorador XP 0.1.0-alpha.5") },
                        enabled = false,
                        onClick = {},
                    )
                }
            }
        }

        // Barra de ferramentas compacta no padrão do Explorer XP
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp)
                .background(XpChrome)
                .border(1.dp, XpChromeBorder)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 5.dp)
        ) {
            XpClassicToolButton(R.drawable.back, "Voltar", canBack, onBack)
            XpClassicToolButton(R.drawable.forward, "Avançar", canForward, onForward)
            XpToolbarSeparator()
            XpClassicToolButton(R.drawable.up, "Subir", true, onUp)
            XpClassicToolButton(R.drawable.search, "Pesquisar", true, onToggleSearch)
            XpClassicToolButton(R.drawable.folder_downloads, "Downloads", true, onOpenDownloads)
            XpClassicToolButton(
                if (viewMode == ViewMode.LIST) R.drawable.view_grid else R.drawable.view_list,
                "Exibir",
                true,
                onToggleView,
            )
        }

        // Barra de endereço fina, como no Windows Explorer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(XpChrome)
                .border(1.dp, XpChromeBorder)
                .padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            Text("Endereço", color = Color(0xFF5A5A5A), fontSize = 10.sp)
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
                    painter = painterResource(if (currentDir == root) R.drawable.drive_hdd else R.drawable.folder),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(5.dp))
                if (searchVisible) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isBlank()) {
                            Text("Pesquisar nesta pasta", color = Color(0xFF777777), fontSize = 11.sp)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(color = Color(0xFF202020), fontSize = 11.sp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Text(
                        text = displayPath,
                        color = Color(0xFF202020),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Box {
                    Text(
                        text = "▾",
                        color = XpBlueDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { addressMenu = true }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                    DropdownMenu(expanded = addressMenu, onDismissRequest = { addressMenu = false }) {
                        pathEntries.forEach { (label, file) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { addressMenu = false; onNavigateTo(file) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XpWindowControl(symbol: String, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = .75f), color)))
            .border(1.dp, Color.White.copy(alpha = .8f), RoundedCornerShape(4.dp))
    ) {
        Text(symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun XpMenuLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color(0xFF202020),
        fontSize = 11.sp,
        modifier = Modifier
            .background(if (selected) Color(0xFFDCE9F8) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
    )
}

@Composable
private fun XpClassicToolButton(
    icon: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(57.dp)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 3.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(icon),
            contentDescription = label,
            modifier = Modifier.size(27.dp),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else .30f,
        )
        Text(
            text = label,
            color = if (enabled) Color(0xFF202020) else Color(0xFF999999),
            fontSize = 9.sp,
            maxLines = 1,
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
                    .weight(info.usedFraction.coerceAtLeast(0.001f))
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
                    .weight((1f - info.usedFraction).coerceAtLeast(0.001f))
                    .fillMaxHeight()
                    .background(Color(0xFFE1ECF8))
            ) {
                Text(
                    text = "Livre: $free",
                    color = XpBlueDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
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
        Button(onClick = onRequestAccess) { Text("Permitir") }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileList(
    items: List<FileItem>,
    selectedPaths: Set<String>,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onSelectOnly: (File) -> Unit,
    onRename: (File) -> Unit,
    onProperties: (File) -> Unit,
    onToggleFavorite: (File) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White)
            .border(1.dp, XpBorder, RoundedCornerShape(5.dp))
    ) {
        items(items, key = { it.file.absolutePath }) { item ->
            FileListRow(
                item = item,
                selected = item.file.absolutePath in selectedPaths,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                onSelectOnly = { onSelectOnly(item.file) },
                onRename = { onRename(item.file) },
                onProperties = { onProperties(item.file) },
                onToggleFavorite = { onToggleFavorite(item.file) },
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
    onLongClick: () -> Unit,
    onSelectOnly: () -> Unit,
    onRename: () -> Unit,
    onProperties: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) XpSelection else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.isFavorite) {
                    androidx.compose.foundation.Image(
                        painterResource(R.drawable.folder_favorite),
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                if (item.isDirectory) "${formatDate(item.createdAt)}  •  ${formatTime(item.createdAt)}  •  Pasta de arquivos"
                else "${formatDate(item.createdAt)}  •  ${formatTime(item.createdAt)}  •  ${formatBytes(item.size)}",
                color = XpTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            XpIconButton(R.drawable.more, "Opções", onClick = { menuExpanded = true }, iconSize = 21, buttonSize = 34)
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text(if (item.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos") }, onClick = {
                    menuExpanded = false; onToggleFavorite()
                })
                DropdownMenuItem(text = { Text("Renomear") }, onClick = {
                    menuExpanded = false; onRename()
                })
                DropdownMenuItem(text = { Text("Propriedades") }, onClick = {
                    menuExpanded = false; onProperties()
                })
                DropdownMenuItem(text = { Text("Selecionar") }, onClick = {
                    menuExpanded = false; onSelectOnly()
                })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGrid(
    items: List<FileItem>,
    selectedPaths: Set<String>,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(8.dp),
    ) {
        items(items, key = { it.file.absolutePath }) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (item.file.absolutePath in selectedPaths) XpSelection else Color.White)
                    .border(1.dp, XpBorder, RoundedCornerShape(10.dp))
                    .combinedClickable(onClick = { onItemClick(item) }, onLongClick = { onItemLongClick(item) })
                    .padding(7.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${formatDate(item.createdAt)} • ${formatTime(item.createdAt)}",
                    fontSize = 10.sp,
                    color = XpTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExplorerStatusBar(
    items: List<FileItem>,
    selectedPaths: Set<String>,
) {
    val selectedItems = items.filter { it.file.absolutePath in selectedPaths }
    val hasSelection = selectedItems.isNotEmpty()
    val shownItems = if (hasSelection) selectedItems else items
    val sizeBytes = shownItems.asSequence()
        .filterNot { it.isDirectory }
        .sumOf { it.size }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(27.dp)
            .background(XpChrome)
            .border(1.dp, XpChromeBorder)
            .padding(horizontal = 7.dp)
    ) {
        Text(
            text = if (hasSelection) {
                "${selectedItems.size} selecionado(s)"
            } else {
                "${items.size} ${if (items.size == 1) "objeto" else "objetos"}"
            },
            color = Color(0xFF303030),
            fontSize = 10.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(XpChromeBorder)
        )

        Text(
            text = formatBytes(sizeBytes),
            color = Color(0xFF303030),
            fontSize = 10.sp,
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(buttonSize.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(if (query.isNotBlank()) R.drawable.search else R.drawable.folder_open),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                query.isNotBlank() -> "Nenhum resultado para “$query”."
                tab == ExplorerTab.DOWNLOADS -> "A pasta Downloads está vazia."
                tab == ExplorerTab.FAVORITES -> "Nenhum favorito ainda."
                else -> "Esta pasta está vazia."
            },
            color = XpTextSecondary,
            fontSize = 16.sp,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Nome") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onConfirm(value) }) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun PropertiesDialog(file: File, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Propriedades") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painterResource(FileIconMapper.iconFor(file)),
                        null,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(file.name, fontWeight = FontWeight.Bold)
                }
                Text("Tipo: ${if (file.isDirectory) "Pasta" else file.extension.ifBlank { "Arquivo" }.uppercase()}")
                if (file.isFile) Text("Tamanho: ${formatBytes(file.length())}")
                Text("Criado: ${formatDateTime(fileCreationTime(file))}")
                Text("Modificado: ${formatDateTime(file.lastModified())}")
                Text("Caminho: ${file.absolutePath}", fontSize = 12.sp, color = XpTextSecondary)
                Text("Leitura: ${if (file.canRead()) "Sim" else "Não"} • Escrita: ${if (file.canWrite()) "Sim" else "Não"}")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun SortDialog(current: SortMode, onDismiss: () -> Unit, onSelect: (SortMode) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ordenar por") },
        text = {
            Column {
                SortMode.entries.forEach { mode ->
                    TextButton(onClick = { onSelect(mode) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            when (mode) {
                                SortMode.NAME -> "Nome"
                                SortMode.DATE -> "Data"
                                SortMode.SIZE -> "Tamanho"
                                SortMode.TYPE -> "Tipo"
                            } + if (mode == current) "  ✓" else ""
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun formatDate(time: Long): String {
    if (time <= 0L) return "Data desconhecida"
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("pt", "BR")).format(Date(time))
}

private fun formatTime(time: Long): String {
    if (time <= 0L) return "--:--"
    return DateFormat.getTimeInstance(DateFormat.SHORT, Locale("pt", "BR")).format(Date(time))
}

private fun formatDateTime(time: Long): String {
    if (time <= 0L) return "Data desconhecida"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("pt", "BR")).format(Date(time))
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
    return if (value >= 100) String.format(Locale("pt", "BR"), "%.0f %s", value, units[index])
    else String.format(Locale("pt", "BR"), "%.1f %s", value, units[index])
}
