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
internal enum class ExplorerQuickDestination { PHOTOS, VIDEOS, DOWNLOADS, NETWORK, TRASH }
internal enum class MediaShortcutKind { PHOTOS, VIDEOS }

@Composable
internal fun ExplorerQuickAccessBar(
    active: ExplorerQuickDestination?,
    onNavigate: (ExplorerQuickDestination) -> Unit,
) {
    val tabs = listOf(
        Triple(ExplorerQuickDestination.PHOTOS, "Fotos", R.drawable.xp_folder_image),
        Triple(ExplorerQuickDestination.VIDEOS, "Vídeos", R.drawable.xp_folder_video),
        Triple(ExplorerQuickDestination.DOWNLOADS, "Downloads", R.drawable.folder_downloads),
        Triple(ExplorerQuickDestination.NETWORK, "Rede", R.drawable.device_mobile),
        Triple(ExplorerQuickDestination.TRASH, "Lixeira", R.drawable.trash_empty),
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(XpChrome)
            .border(1.dp, XpChromeBorder)
            .height(58.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (destination, label, drawable) ->
            val selected = active == destination
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(if (selected) XpSelection else Color.Transparent)
                    .clickable { onNavigate(destination) }
                    .padding(vertical = 5.dp, horizontal = 1.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painter = painterResource(drawable), contentDescription = label,
                    modifier = Modifier.size(25.dp))
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 10.sp, maxLines = 1,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) XpBlueDark else Color(0xFF253B56),
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** O MediaStore indexa arquivos em diferentes pastas; não limita o atalho à DCIM/Movies. */
internal data class IndexedMediaFile(val file: File, val modifiedSeconds: Long, val sizeBytes: Long)

internal object MediaShortcutIndex {
    const val PAGE_SIZE = 96

    fun load(context: Context, kind: MediaShortcutKind, page: Int = 0): List<IndexedMediaFile> {
        val uri = when (kind) {
            MediaShortcutKind.PHOTOS -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaShortcutKind.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val dataColumn = MediaStore.MediaColumns.DATA
        val modifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
        val sizeColumn = MediaStore.MediaColumns.SIZE
        val results = ArrayList<IndexedMediaFile>(PAGE_SIZE)
        // Consulta executada somente em Dispatchers.IO; limitar iteração evita sobrecarga da UI.
        context.contentResolver.query(
            uri, arrayOf(dataColumn, modifiedColumn, sizeColumn), null, null, "$modifiedColumn DESC"
        )?.use { cursor ->
            val pathIndex = cursor.getColumnIndex(dataColumn)
            val dateIndex = cursor.getColumnIndex(modifiedColumn)
            val sizeIndex = cursor.getColumnIndex(sizeColumn)
            if (pathIndex < 0) return emptyList()
            // Paginar no cursor, sem materializar milhares de objetos em RAM.
            if (page > 0 && !cursor.moveToPosition(page * PAGE_SIZE - 1)) return emptyList()
            while (cursor.moveToNext() && results.size < PAGE_SIZE) {
                val path = cursor.getString(pathIndex)?.takeIf { it.isNotBlank() } ?: continue
                val file = File(path)
                if (!file.isFile || !file.canRead() || isPrivateExploradorCache(file)) continue
                val timestamp = if (dateIndex >= 0 && !cursor.isNull(dateIndex)) cursor.getLong(dateIndex) else 0L
                val bytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else file.length()
                results.add(IndexedMediaFile(file, timestamp, bytes.coerceAtLeast(0L)))
            }
        }
        return results
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
    var error by remember(kind, accessGranted) { mutableStateOf<String?>(null) }
    var gridView by remember(kind) { mutableStateOf(kind == MediaShortcutKind.PHOTOS) }
    var selectMode by remember(kind) { mutableStateOf(false) }
    var selected by remember(kind) { mutableStateOf<Set<String>>(emptySet()) }
    var notice by remember { mutableStateOf("") }
    LaunchedEffect(kind, accessGranted, page) {
        if (accessGranted && hasMore) {
            loading = true
            try {
                val next = withContext(Dispatchers.IO) { MediaShortcutIndex.load(context, kind, page) }
                entries = if (page == 0) next else (entries + next).distinctBy { it.file.absolutePath }
                hasMore = next.size == MediaShortcutIndex.PAGE_SIZE
                error = null
            } catch (exception: Exception) {
                error = exception.localizedMessage ?: "Não foi possível carregar a mídia."
            } finally { loading = false }
        }
    }
    val pickDestination = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val source = entries.filter { it.file.absolutePath in selected }.map { it.file }
        if (uri != null && source.isNotEmpty()) scope.launch {
            copying = true; notice = "Copiando arquivos selecionados..."
            try {
                val result = withContext(Dispatchers.IO) {
                    val dest = DocumentFile.fromTreeUri(context, uri) ?: kotlin.error("Pasta de destino indisponível")
                    require(dest.canWrite()) { "Pasta sem permissão de escrita." }
                    var copied = 0; var skipped = 0
                    for (file in source) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        if (!file.isFile || !file.canRead() || dest.findFile(file.name) != null) { skipped++; continue }
                        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"
                        val document = dest.createFile(mime, file.name) ?: kotlin.error("Não foi possível criar ${file.name}")
                        try {
                            FileInputStream(file).use { input ->
                                (context.contentResolver.openOutputStream(document.uri) ?: kotlin.error("Destino indisponível"))
                                    .use { output -> input.copyTo(output, 64 * 1024) }
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
        val source = entries.filter { it.file.absolutePath in selected }.map { it.file }
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
    val title = if (kind == MediaShortcutKind.PHOTOS) "Galeria de fotos" else "Galeria de vídeos"
    fun tap(item: IndexedMediaFile) {
        if (selectMode) {
            selected = if (item.file.absolutePath in selected) selected - item.file.absolutePath
                else selected + item.file.absolutePath
        } else onOpenFile(item.file)
    }
    Column(Modifier.fillMaxSize().background(XpBackground)) {
        Row(Modifier.fillMaxWidth().background(XpBlueDark).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹  Voltar", modifier = Modifier.clickable(onClick = onDismiss), color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text("${entries.size}${if (hasMore) "+" else ""}", color = Color.White, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { gridView = !gridView }, enabled = !copying) { Text(if (gridView) "Lista" else "Grade") }
            OutlinedButton(onClick = { selectMode = !selectMode; selected = emptySet() }, enabled = !copying) {
                Text(if (selectMode) "Cancelar seleção" else "Selecionar")
            }
        }
        if (selectMode) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickDestination.launch(null) }, enabled = selected.isNotEmpty() && !copying,
                    modifier = Modifier.weight(1f)) { Text("Copiar (${selected.size})", maxLines = 1) }
                OutlinedButton(onClick = { shareSelected() }, enabled = selected.isNotEmpty() && !copying,
                    modifier = Modifier.weight(1f)) { Text("Enviar", maxLines = 1) }
            }
        }
        if (notice.isNotBlank()) Text(notice, Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 11.sp, color = XpBlueDark)
        when {
            !accessGranted -> Column(Modifier.padding(18.dp)) {
                Text("Autorize o acesso aos arquivos para listar $title.")
                Text("Conceder acesso", Modifier.clickable(onClick = onRequestAccess).padding(10.dp), color = XpBlueDark)
            }
            entries.isEmpty() && loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null && entries.isEmpty() -> Text(error ?: "", Modifier.padding(18.dp), color = Color(0xFFA42222))
            entries.isEmpty() -> Text("Nenhum arquivo encontrado no índice de mídia do Android.", Modifier.padding(18.dp))
            else -> if (gridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp), modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(entries, key = { it.file.absolutePath }) { item ->
                        Column(Modifier.fillMaxWidth().combinedClickable(
                            onClick = { tap(item) },
                            onLongClick = { selectMode = true; selected = selected + item.file.absolutePath }
                        ), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                GalleryFileThumbnail(item.file, kind == MediaShortcutKind.VIDEOS, 104.dp)
                                if (selectMode) Checkbox(checked = item.file.absolutePath in selected,
                                    onCheckedChange = { tap(item) }, modifier = Modifier.align(Alignment.TopEnd))
                            }
                            Text(item.file.name, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(galleryFileSize(item.sizeBytes), fontSize = 10.sp, color = XpTextSecondary)
                        }
                    }
                    if (hasMore) item {
                        OutlinedButton(onClick = { page++ }, enabled = !loading && !copying) {
                            Text(if (loading) "Carregando..." else "Mais fotos/vídeos")
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(entries, key = { it.file.absolutePath }) { item ->
                        Row(Modifier.fillMaxWidth().combinedClickable(
                            onClick = { tap(item) }, onLongClick = {
                                selectMode = true; selected = selected + item.file.absolutePath
                            }).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (selectMode) Checkbox(checked = item.file.absolutePath in selected,
                                onCheckedChange = { tap(item) })
                            GalleryFileThumbnail(item.file, kind == MediaShortcutKind.VIDEOS, 55.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.file.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${galleryFileSize(item.sizeBytes)} • ${item.file.parentFile?.name ?: "Armazenamento"}",
                                    fontSize = 10.sp, color = XpTextSecondary)
                            }
                        }
                    }
                    if (hasMore) item {
                        OutlinedButton(onClick = { page++ }, enabled = !loading && !copying) {
                            Text(if (loading) "Carregando..." else "Carregar mais")
                        }
                    }
                }
            }
        }
    }
}
