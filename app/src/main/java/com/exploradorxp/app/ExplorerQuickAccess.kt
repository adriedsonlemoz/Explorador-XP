package com.exploradorxp.app

import android.content.Context
import android.net.Uri
import android.content.Intent
import android.content.ClipData
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import java.io.FileInputStream
import androidx.activity.compose.BackHandler
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Barra persistente para o explorador e todas as páginas internas. */
internal enum class ExplorerQuickDestination { PHOTOS, VIDEOS, DOWNLOADS, NETWORK, TRASH, TOOLS }
internal enum class MediaShortcutKind { PHOTOS, VIDEOS }

@Composable
internal fun ExplorerQuickAccessBar(
    active: ExplorerQuickDestination?,
    onNavigate: (ExplorerQuickDestination) -> Unit,
    trashCount: Int? = null,
    trashSize: String? = null,
) {
    val tabs = listOf(
        Triple(ExplorerQuickDestination.PHOTOS, "Fotos", R.drawable.xp_folder_image),
        Triple(ExplorerQuickDestination.VIDEOS, "Vídeos", R.drawable.xp_folder_video),
        Triple(ExplorerQuickDestination.DOWNLOADS, "Downloads", R.drawable.folder_downloads),
        Triple(ExplorerQuickDestination.NETWORK, "Rede", R.drawable.device_mobile),
        Triple(ExplorerQuickDestination.TRASH, "Lixeira", if ((trashCount ?: 0) > 0) R.drawable.trash_full else R.drawable.trash_empty),
        Triple(ExplorerQuickDestination.TOOLS, "Ferramentas", R.drawable.settings),
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(XpChrome)
            .border(1.dp, XpChromeBorder)
            .height(72.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (destination, label, drawable) ->
            val selected = active == destination
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(if (selected) XpSelection else Color.Transparent)
                    .clickable { onNavigate(destination) }
                    .padding(vertical = 3.dp, horizontal = 1.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painter = painterResource(drawable), contentDescription = label,
                    modifier = Modifier.size(23.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 9.sp, maxLines = 1,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) XpBlueDark else Color(0xFF253B56),
                    overflow = TextOverflow.Ellipsis)
                if (destination == ExplorerQuickDestination.TRASH) {
                    Text(
                        text = trashCount?.let { if (it == 1) "1 item" else "$it itens" } ?: "—",
                        fontSize = 8.sp, lineHeight = 9.sp, maxLines = 1,
                        color = XpTextSecondary, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = trashSize ?: "—",
                        fontSize = 8.sp, lineHeight = 9.sp, maxLines = 1,
                        color = XpTextSecondary, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Índice de mídia paginado: a seleção total percorre apenas metadados, nunca thumbnails. */
internal data class IndexedMediaFile(
    val file: File,
    val modifiedSeconds: Long,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMillis: Long = 0L,
)

internal data class MediaGalleryPage(val entries: List<IndexedMediaFile>, val hasMore: Boolean)

internal object MediaShortcutIndex {
    const val PAGE_SIZE = 96

    private fun mediaUri(kind: MediaShortcutKind): Uri = when (kind) {
        MediaShortcutKind.PHOTOS -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        MediaShortcutKind.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    private fun sortClause(sort: MediaGallerySort): String {
        val column = when (sort) {
            MediaGallerySort.RECENT, MediaGallerySort.OLDEST -> MediaStore.MediaColumns.DATE_MODIFIED
            MediaGallerySort.LARGEST, MediaGallerySort.SMALLEST -> MediaStore.MediaColumns.SIZE
            MediaGallerySort.NAME -> MediaStore.MediaColumns.DISPLAY_NAME
        }
        val direction = when (sort) {
            MediaGallerySort.OLDEST, MediaGallerySort.SMALLEST, MediaGallerySort.NAME -> "ASC"
            else -> "DESC"
        }
        return "$column $direction, ${MediaStore.MediaColumns._ID} DESC"
    }

    private suspend fun queryMedia(
        context: Context, kind: MediaShortcutKind, sort: MediaGallerySort,
        consumer: (IndexedMediaFile) -> Boolean,
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
        ) + if (kind == MediaShortcutKind.VIDEOS) arrayOf(MediaStore.Video.VideoColumns.DURATION)
            else emptyArray()
        context.contentResolver.query(mediaUri(kind), projection, null, null, sortClause(sort))?.use { cursor ->
            val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val dateIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val widthIndex = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIndex = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationIndex = if (kind == MediaShortcutKind.VIDEOS)
                cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1
            if (pathIndex < 0) return
            while (cursor.moveToNext()) {
                kotlin.coroutines.coroutineContext.ensureActive()
                val path = cursor.getString(pathIndex)?.takeIf { it.isNotBlank() } ?: continue
                val file = File(path)
                if (!file.isFile || !file.canRead() || isPrivateExploradorCache(file)) continue
                fun longAt(index: Int): Long = if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else 0L
                val media = IndexedMediaFile(file, longAt(dateIndex), longAt(sizeIndex).coerceAtLeast(0),
                    longAt(widthIndex).toInt().coerceAtLeast(0),
                    longAt(heightIndex).toInt().coerceAtLeast(0),
                    longAt(durationIndex).coerceAtLeast(0))
                if (!consumer(media)) break
            }
        }
    }

    suspend fun loadPage(
        context: Context, kind: MediaShortcutKind, page: Int,
        sort: MediaGallerySort, origin: MediaGalleryOrigin,
    ): MediaGalleryPage {
        require(page >= 0)
        val found = ArrayList<IndexedMediaFile>(PAGE_SIZE + 1)
        var skipped = 0L
        val offset = page.toLong() * PAGE_SIZE
        queryMedia(context, kind, sort) { media ->
            if (!MediaGalleryRules.matches(media.file.absolutePath, origin)) return@queryMedia true
            if (skipped < offset) { skipped++; return@queryMedia true }
            found.add(media)
            found.size <= PAGE_SIZE
        }
        return MediaGalleryPage(found.take(PAGE_SIZE), found.size > PAGE_SIZE)
    }

    /** Os caminhos são coletados sem carregar imagens; suporta seleção além da primeira página. */
    suspend fun allMatchingPaths(
        context: Context, kind: MediaShortcutKind,
        sort: MediaGallerySort, origin: MediaGalleryOrigin,
    ): Set<String> {
        val paths = LinkedHashSet<String>()
        queryMedia(context, kind, sort) { media ->
            if (MediaGalleryRules.matches(media.file.absolutePath, origin)) paths.add(media.file.absolutePath)
            true
        }
        return paths
    }

    internal fun isPrivateExploradorCache(file: File): Boolean =
        file.toPath().any { segment ->
            segment.toString().equals(".ExploradorXP_Lixeira", ignoreCase = true)
        }
}

internal fun galleryFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

internal fun galleryDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).coerceAtLeast(0)
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, (seconds / 60) % 60, seconds % 60)
        else "%02d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun GalleryToolbarAction(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true, onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(38.dp)) {
            Icon(icon, contentDescription = label, tint = if (enabled) XpBlueDark else XpTextSecondary,
                modifier = Modifier.size(22.dp))
        }
        Text(label, fontSize = 10.sp, color = XpTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaShortcutScreen(
    kind: MediaShortcutKind,
    accessGranted: Boolean,
    onRequestAccess: () -> Unit,
    onDismiss: () -> Unit,
    onOpenFile: (File) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onDismiss)
    var entries by remember(kind, accessGranted) { mutableStateOf(emptyList<IndexedMediaFile>()) }
    var page by remember(kind, accessGranted) { mutableStateOf(0) }
    var hasMore by remember(kind, accessGranted) { mutableStateOf(true) }
    var loading by remember(kind, accessGranted) { mutableStateOf(accessGranted) }
    var copying by remember { mutableStateOf(false) }
    var selectingAll by remember { mutableStateOf(false) }
    var error by remember(kind, accessGranted) { mutableStateOf<String?>(null) }
    var gridView by remember(kind) { mutableStateOf(kind == MediaShortcutKind.PHOTOS) }
    var selectMode by remember(kind) { mutableStateOf(false) }
    var selected by remember(kind) { mutableStateOf<Set<String>>(emptySet()) }
    var notice by remember { mutableStateOf("") }
    var sort by remember(kind) { mutableStateOf(MediaGallerySort.RECENT) }
    var origin by remember(kind) { mutableStateOf(MediaGalleryOrigin.ALL) }
    var sortMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    fun reloadGallery() {
        page = 0; entries = emptyList(); hasMore = true; error = null
        selected = emptySet(); refreshKey++
    }

    LaunchedEffect(kind, accessGranted, sort, origin, page, refreshKey) {
        if (accessGranted && (page == 0 || hasMore)) {
            loading = true
            try {
                val next = withContext(Dispatchers.IO) {
                    MediaShortcutIndex.loadPage(context, kind, page, sort, origin)
                }
                entries = if (page == 0) next.entries
                    else (entries + next.entries).distinctBy { it.file.absolutePath }
                hasMore = next.hasMore
                error = null
            } catch (exception: Exception) {
                error = exception.localizedMessage ?: "Não foi possível carregar a mídia."
            } finally { loading = false }
        }
    }

    val pickDestination = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        // selected pode conter arquivos ainda não carregados em tela após Selecionar tudo.
        val source = selected.map(::File)
        if (uri != null && source.isNotEmpty()) scope.launch {
            copying = true; notice = "Copiando ${source.size} arquivo(s)..."
            try {
                val result = withContext(Dispatchers.IO) {
                    val dest = DocumentFile.fromTreeUri(context, uri) ?: kotlin.error("Pasta de destino indisponível")
                    require(dest.canWrite()) { "Pasta sem permissão de escrita." }
                    var copied = 0; var skipped = 0
                    for (file in source) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        if (!file.isFile || !file.canRead() || dest.findFile(file.name) != null) {
                            skipped++; continue
                        }
                        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
                            ?: "application/octet-stream"
                        val document = dest.createFile(mime, file.name)
                            ?: kotlin.error("Não foi possível criar ${file.name}")
                        try {
                            FileInputStream(file).use { input ->
                                (context.contentResolver.openOutputStream(document.uri)
                                    ?: kotlin.error("Destino indisponível")).use { output ->
                                    input.copyTo(output, 64 * 1024)
                                }
                            }
                            copied++
                        } catch (e: Exception) { runCatching { document.delete() }; throw e }
                    }
                    "$copied copiado(s), $skipped ignorado(s). Arquivos existentes não foram substituídos."
                }
                notice = result; selected = emptySet(); selectMode = false
            } catch (e: Exception) { notice = "Falha na cópia: ${e.localizedMessage}" }
            finally { copying = false }
        }
    }
    fun shareSelected() {
        // Limite para evitar TransactionTooLargeException na intent com centenas de URIs.
        if (selected.size > 100) {
            notice = "Para enviar, selecione no máximo 100 arquivos por vez. Copiar aceita todos."
            return
        }
        val source = selected.map(::File).filter { it.isFile && it.canRead() }
        if (source.isEmpty()) return
        runCatching {
            val uris = ArrayList(source.map {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            })
            val intent = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                type = if (kind == MediaShortcutKind.PHOTOS) "image/*" else "video/*"
                if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris.first())
                else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                clipData = ClipData.newUri(context.contentResolver, "Mídia selecionada", uris.first()).also { clip ->
                    uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar ${source.size} arquivo(s)"))
        }.onFailure { notice = "Falha ao compartilhar: ${it.localizedMessage}" }
    }
    fun updateAllSelection(invert: Boolean) {
        scope.launch {
            selectingAll = true
            try {
                val paths = withContext(Dispatchers.IO) {
                    MediaShortcutIndex.allMatchingPaths(context, kind, sort, origin)
                }
                selected = if (invert) paths - selected else paths
                notice = "${selected.size} arquivo(s) selecionado(s) no filtro atual."
            } catch (e: Exception) {
                notice = "Falha ao atualizar seleção: ${e.localizedMessage}"
            } finally { selectingAll = false }
        }
    }
    val title = if (kind == MediaShortcutKind.PHOTOS) "Galeria de fotos" else "Galeria de vídeos"
    fun tap(item: IndexedMediaFile) {
        if (selectMode) {
            selected = if (item.file.absolutePath in selected) selected - item.file.absolutePath
                else selected + item.file.absolutePath
        } else onOpenFile(item.file)
    }

    Column(Modifier.fillMaxSize().background(XpBackground)) {
        Row(Modifier.fillMaxWidth().background(XpBlueDark).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Voltar", modifier = Modifier.clickable(onClick = onDismiss), color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(if (selectMode) "${selected.size} selecionado(s)" else title,
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(if (selectMode) "" else "${entries.size}${if (hasMore) "+" else ""}",
                color = Color.White, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (selectMode) {
                GalleryToolbarAction("Fechar", Icons.Rounded.Close, !copying && !selectingAll) {
                    selected = emptySet(); selectMode = false
                }
                GalleryToolbarAction("Tudo", Icons.Rounded.SelectAll, !copying && !selectingAll) { updateAllSelection(false) }
                GalleryToolbarAction("Limpar", Icons.Rounded.CheckBox, !copying && !selectingAll) { selected = emptySet() }
                GalleryToolbarAction("Inverter", Icons.Rounded.CheckCircle, !copying && !selectingAll) {
                    updateAllSelection(true)
                }
                Box {
                    GalleryToolbarAction("Mais", Icons.Rounded.MoreVert, !copying) { moreMenu = true }
                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                        DropdownMenuItem(text = { Text("Atualizar galeria") }, onClick = {
                            moreMenu = false; reloadGallery()
                        })
                    }
                }
            } else {
                GalleryToolbarAction(if (gridView) "Lista" else "Grade",
                    if (gridView) Icons.Rounded.ViewList else Icons.Rounded.GridView, !copying) {
                    gridView = !gridView
                }
                GalleryToolbarAction("Selecionar", Icons.Rounded.CheckBox, !copying) {
                    selectMode = true; selected = emptySet()
                }
                Box {
                    GalleryToolbarAction("Ordenar", Icons.Rounded.Sort, !copying) { sortMenu = true }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        MediaGallerySort.values().forEach { option ->
                            DropdownMenuItem(text = { Text(option.label + if (sort == option) "  ✓" else "") },
                                onClick = {
                                    sort = option; sortMenu = false; reloadGallery()
                                })
                        }
                    }
                }
                Box {
                    GalleryToolbarAction("Filtrar", Icons.Rounded.FilterList, !copying) { filterMenu = true }
                    DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                        MediaGalleryOrigin.values().forEach { option ->
                            DropdownMenuItem(text = { Text(option.label + if (origin == option) "  ✓" else "") },
                                onClick = {
                                    origin = option; filterMenu = false; reloadGallery()
                                })
                        }
                    }
                }
                Box {
                    GalleryToolbarAction("Mais", Icons.Rounded.MoreVert, !copying) { moreMenu = true }
                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                        DropdownMenuItem(text = { Text("Atualizar") }, onClick = {
                            moreMenu = false; reloadGallery()
                        })
                        DropdownMenuItem(text = { Text("Informações") }, onClick = {
                            moreMenu = false
                            notice = "${entries.size} carregado(s) • ${sort.label} • ${origin.label}. Use Carregar mais para outras páginas."
                        })
                    }
                }
            }
        }
        if (selectMode) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { pickDestination.launch(null) }, enabled = selected.isNotEmpty() && !copying && !selectingAll,
                    modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar (${selected.size})", maxLines = 1)
                }
                OutlinedButton(onClick = { shareSelected() }, enabled = selected.isNotEmpty() && !copying && !selectingAll,
                    modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Enviar")
                }
            }
        }
        if (origin != MediaGalleryOrigin.ALL && !selectMode) {
            Text("Filtro: ${origin.label}", Modifier.padding(start = 14.dp, bottom = 4.dp),
                color = XpBlueDark, fontSize = 11.sp)
        }
        if (loading || selectingAll || copying) {
            Text(when { copying -> "Copiando..."; selectingAll -> "Selecionando todas as páginas..."; else -> "Carregando..." },
                Modifier.padding(horizontal = 12.dp, vertical = 2.dp), fontSize = 11.sp, color = XpTextSecondary)
        }
        if (notice.isNotBlank()) Text(notice, Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 11.sp, color = XpBlueDark)
        when {
            !accessGranted -> Column(Modifier.padding(18.dp)) {
                Text("Autorize o acesso aos arquivos para listar $title.")
                Text("Conceder acesso", Modifier.clickable(onClick = onRequestAccess).padding(10.dp), color = XpBlueDark)
            }
            entries.isEmpty() && loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null && entries.isEmpty() -> Text(error ?: "", Modifier.padding(18.dp), color = Color(0xFFA42222))
            entries.isEmpty() -> Text("Nenhum arquivo encontrado para este filtro.", Modifier.padding(18.dp))
            else -> if (gridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(entries, key = { it.file.absolutePath }) { item ->
                        val chosen = item.file.absolutePath in selected
                        Column(Modifier.fillMaxWidth().combinedClickable(
                            onClick = { tap(item) },
                            onLongClick = { selectMode = true; selected = selected + item.file.absolutePath }
                        ), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.border(if (chosen) 2.dp else 0.dp,
                                if (chosen) XpBlueDark else Color.Transparent,
                                androidx.compose.foundation.shape.RoundedCornerShape(7.dp))) {
                                GalleryFileThumbnail(item.file, kind == MediaShortcutKind.VIDEOS, 104.dp)
                                if (kind == MediaShortcutKind.VIDEOS && item.durationMillis > 0L)
                                    Text(galleryDuration(item.durationMillis),
                                        Modifier.align(Alignment.BottomEnd)
                                            .background(Color(0xCC111B2B)).padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 10.sp, color = Color.White)
                                if (selectMode)
                                    Icon(if (chosen) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = if (chosen) "Selecionado" else "Não selecionado",
                                        tint = if (chosen) XpBlueDark else Color.White,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(24.dp))
                            }
                            Text(item.file.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(galleryFileSize(item.sizeBytes), fontSize = 10.sp, color = XpTextSecondary)
                        }
                    }
                    if (hasMore) item {
                        OutlinedButton(onClick = { page++ }, enabled = !loading && !copying && !selectingAll) {
                            Text(if (loading) "Carregando..." else "Mais arquivos")
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(entries, key = { it.file.absolutePath }) { item ->
                        val chosen = item.file.absolutePath in selected
                        Row(Modifier.fillMaxWidth().background(if (chosen) XpSelection else Color.Transparent)
                            .combinedClickable(onClick = { tap(item) }, onLongClick = {
                                selectMode = true; selected = selected + item.file.absolutePath
                            }).padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            if (selectMode) Checkbox(checked = chosen, onCheckedChange = { tap(item) })
                            Box {
                                GalleryFileThumbnail(item.file, kind == MediaShortcutKind.VIDEOS, 55.dp)
                                if (kind == MediaShortcutKind.VIDEOS && item.durationMillis > 0L)
                                    Text(galleryDuration(item.durationMillis),
                                        Modifier.align(Alignment.BottomEnd).background(Color(0xCC111B2B))
                                            .padding(horizontal = 2.dp), color = Color.White, fontSize = 9.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.file.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val dimensions = if (item.width > 0 && item.height > 0)
                                    " • ${item.width}×${item.height}" else ""
                                Text("${galleryFileSize(item.sizeBytes)}$dimensions • ${item.file.parentFile?.name ?: "Armazenamento"}",
                                    fontSize = 10.sp, color = XpTextSecondary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (hasMore) item {
                        OutlinedButton(onClick = { page++ }, enabled = !loading && !copying && !selectingAll) {
                            Text(if (loading) "Carregando..." else "Carregar mais")
                        }
                    }
                }
            }
        }
    }
}
