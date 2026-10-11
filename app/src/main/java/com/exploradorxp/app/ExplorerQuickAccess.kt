package com.exploradorxp.app

import android.content.Context
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
internal data class IndexedMediaFile(val file: File, val modifiedSeconds: Long)

internal object MediaShortcutIndex {
    const val MAX_RESULTS = 600

    fun load(context: Context, kind: MediaShortcutKind): List<IndexedMediaFile> {
        val uri = when (kind) {
            MediaShortcutKind.PHOTOS -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaShortcutKind.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val dataColumn = MediaStore.MediaColumns.DATA
        val modifiedColumn = MediaStore.MediaColumns.DATE_MODIFIED
        val results = ArrayList<IndexedMediaFile>(MAX_RESULTS)
        // Consulta executada somente em Dispatchers.IO; limitar iteração evita sobrecarga da UI.
        context.contentResolver.query(
            uri, arrayOf(dataColumn, modifiedColumn), null, null, "$modifiedColumn DESC"
        )?.use { cursor ->
            val pathIndex = cursor.getColumnIndex(dataColumn)
            val dateIndex = cursor.getColumnIndex(modifiedColumn)
            if (pathIndex < 0) return emptyList()
            while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                val path = cursor.getString(pathIndex)?.takeIf { it.isNotBlank() } ?: continue
                val file = File(path)
                if (!file.isFile || !file.canRead() || isPrivateExploradorCache(file)) continue
                val timestamp = if (dateIndex >= 0 && !cursor.isNull(dateIndex)) cursor.getLong(dateIndex) else 0L
                results.add(IndexedMediaFile(file, timestamp))
            }
        }
        return results
    }

    internal fun isPrivateExploradorCache(file: File): Boolean =
        file.toPath().any { segment ->
            segment.toString().equals(".ExploradorXP_Lixeira", ignoreCase = true)
        }
}

@Composable
internal fun MediaShortcutScreen(
    kind: MediaShortcutKind,
    accessGranted: Boolean,
    onRequestAccess: () -> Unit,
    onDismiss: () -> Unit,
    onOpenFile: (File) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    BackHandler(onBack = onDismiss)
    var loading by remember(kind, accessGranted) { mutableStateOf(accessGranted) }
    var entries by remember(kind, accessGranted) { mutableStateOf(emptyList<IndexedMediaFile>()) }
    var error by remember(kind, accessGranted) { mutableStateOf<String?>(null) }
    LaunchedEffect(kind, accessGranted) {
        if (accessGranted) {
            loading = true
            try { entries = withContext(Dispatchers.IO) { MediaShortcutIndex.load(context, kind) } }
            catch (exception: Exception) { error = exception.localizedMessage ?: "Não foi possível carregar a mídia." }
            finally { loading = false }
        }
    }
    val title = if (kind == MediaShortcutKind.PHOTOS) "Fotos" else "Vídeos"
    Column(Modifier.fillMaxSize().background(XpBackground)) {
        Row(Modifier.fillMaxWidth().background(XpBlueDark).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("‹  Voltar", modifier = Modifier.clickable(onClick = onDismiss), color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Text("${entries.size}${if (entries.size == MediaShortcutIndex.MAX_RESULTS) "+" else ""}",
                color = Color.White, fontSize = 12.sp)
        }
        when {
            !accessGranted -> Column(Modifier.padding(18.dp)) {
                Text("Autorize o acesso aos arquivos para listar $title.")
                Text("Conceder acesso", modifier = Modifier.clickable(onClick = onRequestAccess).padding(10.dp), color = XpBlueDark)
            }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Text(error ?: "", modifier = Modifier.padding(18.dp), color = Color(0xFFA42222))
            entries.isEmpty() -> Text("Nenhum arquivo encontrado no índice de mídia do Android.", modifier = Modifier.padding(18.dp))
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(entries, key = { it.file.absolutePath }) { item ->
                    Row(Modifier.fillMaxWidth().clickable { onOpenFile(item.file) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(if (kind == MediaShortcutKind.PHOTOS) R.drawable.xp_file_image else R.drawable.xp_file_video),
                            contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.file.name, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text("${item.file.parentFile?.name ?: "Armazenamento"} • " +
                                (if (item.modifiedSeconds > 0L) DateFormat.getDateInstance(DateFormat.SHORT).format(Date(item.modifiedSeconds * 1000)) else ""),
                                fontSize = 10.sp, color = XpTextSecondary)
                        }
                    }
                    androidx.compose.material3.HorizontalDivider(color = XpChromeBorder)
                }
            }
        }
    }
}
