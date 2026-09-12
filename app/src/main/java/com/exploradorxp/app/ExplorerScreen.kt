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
val XpBackground = Color(0xFFEAF4FF)
val XpSurface = Color(0xFFFFFFFF)
val XpBorder = Color(0xFF78A9E6)
val XpTextSecondary = Color(0xFF3E5682)
val XpSelection = Color(0xFFD8EAFE)

@Composable
fun ExplorerScreen(
    state: ExplorerUiState,
    accessGranted: Boolean,
    onRequestAccess: () -> Unit,
    onBack: () -> Unit,
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
    var showMore by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XpBackground)
    ) {
        if (state.selectedPaths.isEmpty()) {
            XpHeader(
                searchVisible = state.searchVisible,
                query = state.query,
                onQueryChange = onQueryChange,
                onToggleSearch = onToggleSearch,
                showMore = showMore,
                onShowMore = { showMore = it },
                onNewFolder = { showNewFolder = true },
                onRefresh = onRefresh,
                canPaste = state.clipboard != null && state.tab != ExplorerTab.FAVORITES,
                onPaste = onPaste,
                onSort = { showSort = true },
                canBack = state.canGoBack,
                onBack = onBack,
                onUp = onUp,
                viewMode = state.viewMode,
                onToggleView = onToggleView,
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
            CompactLocationBar(
                currentDir = state.currentDir,
                showHidden = state.showHidden,
                onShowHiddenChange = onToggleHidden,
                onNavigateTo = onNavigateTo,
            )
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

        BottomNavigation(
            selected = state.tab,
            onTabChange = onTabChange,
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
    searchVisible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    showMore: Boolean,
    onShowMore: (Boolean) -> Unit,
    onNewFolder: () -> Unit,
    onRefresh: () -> Unit,
    canPaste: Boolean,
    onPaste: () -> Unit,
    onSort: () -> Unit,
    canBack: Boolean,
    onBack: () -> Unit,
    onUp: () -> Unit,
    viewMode: ViewMode,
    onToggleView: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(XpBlueLight, XpBlue, XpBlueDark)))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        XpHeaderButton(
            icon = R.drawable.back,
            contentDescription = "Voltar",
            enabled = canBack,
            onClick = onBack,
        )
        Spacer(Modifier.width(4.dp))
        XpHeaderButton(
            icon = R.drawable.up,
            contentDescription = "Subir",
            enabled = true,
            onClick = onUp,
        )
        Spacer(Modifier.width(4.dp))
        XpHeaderButton(
            icon = if (viewMode == ViewMode.LIST) R.drawable.view_grid else R.drawable.view_list,
            contentDescription = "Exibir",
            enabled = true,
            onClick = onToggleView,
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.folder),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(7.dp))
        if (searchVisible) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Pesquisar") },
                modifier = Modifier.weight(1f).height(44.dp),
            )
        } else {
            Text(
                text = "Explorador",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        XpIconButton(icon = R.drawable.search, contentDescription = "Pesquisar", onClick = onToggleSearch, iconSize = 25, buttonSize = 38)
        Box {
            XpIconButton(icon = R.drawable.more, contentDescription = "Mais", onClick = { onShowMore(true) }, iconSize = 23, buttonSize = 38)
            DropdownMenu(expanded = showMore, onDismissRequest = { onShowMore(false) }) {
                DropdownMenuItem(text = { Text("Nova pasta") }, onClick = {
                    onShowMore(false); onNewFolder()
                })
                DropdownMenuItem(text = { Text("Atualizar") }, onClick = {
                    onShowMore(false); onRefresh()
                })
                DropdownMenuItem(text = { Text("Ordenar") }, onClick = {
                    onShowMore(false); onSort()
                })
                if (canPaste) {
                    DropdownMenuItem(text = { Text("Colar") }, onClick = {
                        onShowMore(false); onPaste()
                    })
                }
            }
        }
    }
}

@Composable
private fun XpHeaderButton(
    icon: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x332B8BF0))
            .border(1.dp, Color(0x99B9DDFF), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(27.dp),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else .35f,
        )
    }
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
private fun CompactLocationBar(
    currentDir: File,
    showHidden: Boolean,
    onShowHiddenChange: (Boolean) -> Unit,
    onNavigateTo: (File) -> Unit,
) {
    val root = android.os.Environment.getExternalStorageDirectory()
    val segments = remember(currentDir.absolutePath) {
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White)
                .border(1.dp, XpBorder, RoundedCornerShape(9.dp))
                .padding(horizontal = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.drive_hdd),
                    contentDescription = null,
                    modifier = Modifier.size(25.dp)
                )
                Spacer(Modifier.width(7.dp))
                segments.forEachIndexed { index, pair ->
                    Text(
                        text = pair.first,
                        color = Color(0xFF132A55),
                        fontWeight = if (index == segments.lastIndex) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.clickable { onNavigateTo(pair.second) }
                    )
                    if (index != segments.lastIndex) {
                        Text("  ›  ", color = XpBlueDark, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    }
                }
            }
            Text("▾", color = XpBlueDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFE6F1FF))
                .border(1.dp, XpBorder, RoundedCornerShape(9.dp))
                .clickable { onShowHiddenChange(!showHidden) }
                .padding(horizontal = 8.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.visible),
                contentDescription = null,
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text("Ocultos", color = Color(0xFF183363), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            HiddenToggleIndicator(checked = showHidden)
        }
    }
}

@Composable
private fun HiddenToggleIndicator(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) Color(0xFF2E80D8) else Color(0xFF9CAFC7))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        )
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
            .background(Color(0xFFE4F0FF))
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
                .border(1.dp, Color(0xFF7891B2), RoundedCornerShape(8.dp))
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
                    .background(Color(0xFFD8E2EF))
            ) {
                Text(
                    text = "Livre: $free",
                    color = Color(0xFF183363),
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
            color = Color(0xFF183363),
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
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, XpBorder, RoundedCornerShape(12.dp))
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
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
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
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            XpIconButton(R.drawable.more, "Opções", onClick = { menuExpanded = true }, iconSize = 26)
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
        columns = GridCells.Adaptive(minSize = 105.dp),
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
                    .padding(10.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    item.name,
                    fontSize = 13.sp,
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
private fun BottomNavigation(selected: ExplorerTab, onTabChange: (ExplorerTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFDCEBFF))
            .border(1.dp, XpBorder)
            .padding(4.dp)
    ) {
        BottomNavItem(
            label = "Arquivos",
            icon = R.drawable.folder,
            selected = selected == ExplorerTab.FILES,
            onClick = { onTabChange(ExplorerTab.FILES) },
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            label = "Downloads",
            icon = R.drawable.folder_downloads,
            selected = selected == ExplorerTab.DOWNLOADS,
            onClick = { onTabChange(ExplorerTab.DOWNLOADS) },
            modifier = Modifier.weight(1f)
        )
        BottomNavItem(
            label = "Favoritos",
            icon = R.drawable.favorites,
            selected = selected == ExplorerTab.FAVORITES,
            onClick = { onTabChange(ExplorerTab.FAVORITES) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BottomNavItem(label: String, icon: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFBFD9FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(icon), null, modifier = Modifier.size(32.dp))
        Text(label, color = if (selected) XpBlueDark else Color(0xFF405274), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
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
