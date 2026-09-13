package com.exploradorxp.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

private val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp", "gif")
private val videoExtensions = setOf("mp4", "m4v", "3gp", "webm", "mkv", "avi")
private val audioExtensions = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus")
private val textExtensions = setOf(
    "txt", "log", "ini", "cfg", "conf", "properties", "json", "xml", "csv", "sql",
    "css", "js", "ts", "kt", "kts", "java", "py", "sh", "bat", "yml", "yaml",
    "md", "markdown", "mds", "gradle", "toml", "env"
)

fun supportsInternalViewer(file: File): Boolean {
    val ext = file.extension.lowercase()
    return ext in imageExtensions || ext in videoExtensions || ext in audioExtensions ||
        ext in textExtensions || ext in setOf("html", "htm", "pdf", "zip", "apk")
}

@Composable
fun InternalViewerScreen(
    file: File,
    onClose: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F2E8))
    ) {
        ViewerTitleBar(file.name, onClose)
        ViewerToolbar(file, onOpenExternal)
        HorizontalDivider(color = Color(0xFFB8C7DA))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (file.extension.lowercase()) {
                in imageExtensions -> ImageViewer(file)
                in videoExtensions, in audioExtensions -> MediaViewer(file)
                "html", "htm" -> HtmlViewer(file)
                "pdf" -> PdfViewer(file)
                "zip" -> ZipViewer(file)
                "apk" -> ApkViewer(file, onOpenExternal)
                in textExtensions -> TextEditorViewer(file)
                else -> UnsupportedViewer(onOpenExternal)
            }
        }
        ViewerStatusBar(file)
    }
}

@Composable
private fun ViewerTitleBar(title: String, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF2F92F6), XpBlue, XpBlueDark)))
            .padding(horizontal = 8.dp)
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.folder_open),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClose) {
            Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
    }
}

@Composable
private fun ViewerToolbar(file: File, onOpenExternal: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(XpChrome)
            .border(1.dp, XpChromeBorder)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Visualizador",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF303030),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenExternal) { Text("Abrir com...", color = XpBlueDark, fontSize = 12.sp) }
    }
}

@Composable
private fun ImageViewer(file: File) {
    val bitmap = remember(file.absolutePath, file.lastModified()) { decodeSampledBitmap(file, 2048, 2048) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color(0xFF262626)).padding(8.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text("Não foi possível exibir a imagem.", color = Color.White)
        }
    }
}

@Composable
private fun MediaViewer(file: File) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        factory = {
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.fromFile(file))
                setOnPreparedListener { player ->
                    player.isLooping = false
                    start()
                }
            }
        },
        update = { view ->
            if (!view.isPlaying) {
                // Mantém os controles disponíveis sem reiniciar automaticamente a cada recomposição.
            }
        }
    )
}

@Composable
private fun HtmlViewer(file: File) {
    var sourceMode by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(XpPanel).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { sourceMode = !sourceMode }) {
                Text(if (sourceMode) "Visualizar página" else "Ver código-fonte", fontSize = 12.sp)
            }
        }
        if (sourceMode) {
            TextEditorViewer(file, Modifier.weight(1f))
        } else {
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = false
                        loadUrl(Uri.fromFile(file).toString())
                    }
                },
                update = { view ->
                    val wanted = Uri.fromFile(file).toString()
                    if (view.url != wanted) view.loadUrl(wanted)
                }
            )
        }
    }
}

@Composable
private fun TextEditorViewer(file: File, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var text by remember(file.absolutePath, file.lastModified()) { mutableStateOf(readTextPreview(file)) }
    val editable = file.length() <= 2_000_000L
    var editing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (editable) "" else "Arquivo grande: edição desativada; visualização limitada.") }

    Column(modifier.fillMaxSize().background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(XpPanel).padding(horizontal = 8.dp)
        ) {
            Text(
                if (editing) "Modo de edição" else "Somente leitura",
                fontSize = 12.sp,
                color = XpTextSecondary,
                modifier = Modifier.weight(1f)
            )
            TextButton(enabled = editable, onClick = {
                if (!editing) {
                    editing = true
                } else {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { file.writeText(text) } }
                        status = if (result.isSuccess) "Salvo" else "Falha ao salvar"
                        if (result.isSuccess) editing = false
                    }
                }
            }) { Text(if (editing) "Salvar" else "Editar", fontSize = 12.sp) }
            if (editing) {
                TextButton(onClick = {
                    text = readTextPreview(file)
                    editing = false
                }) { Text("Cancelar", fontSize = 12.sp) }
            }
        }
        if (status.isNotBlank()) {
            Text(status, fontSize = 12.sp, color = XpTextSecondary, modifier = Modifier.padding(horizontal = 10.dp))
        }
        if (editing) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        } else {
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF202020),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun PdfViewer(file: File) {
    val descriptor = remember(file.absolutePath) {
        runCatching { ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) }.getOrNull()
    }
    val renderer = remember(descriptor) { descriptor?.let { runCatching { PdfRenderer(it) }.getOrNull() } }
    DisposableEffect(renderer, descriptor) {
        onDispose {
            runCatching { renderer?.close() }
            runCatching { descriptor?.close() }
        }
    }

    if (renderer == null || renderer.pageCount <= 0) {
        UnsupportedMessage("Não foi possível abrir este PDF.")
        return
    }

    var pageIndex by remember { mutableIntStateOf(0) }
    val bitmap = remember(pageIndex, file.lastModified()) {
        runCatching {
            renderer.openPage(pageIndex).use { page ->
                val width = (page.width * 1.5f).toInt().coerceAtLeast(1)
                val height = (page.height * 1.5f).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { target ->
                    target.eraseColor(android.graphics.Color.WHITE)
                    page.render(target, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
        }.getOrNull()
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF5B5B5B))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().background(XpPanel).padding(4.dp)
        ) {
            TextButton(enabled = pageIndex > 0, onClick = { pageIndex-- }) { Text("◀ Anterior") }
            Text("Página ${pageIndex + 1} de ${renderer.pageCount}", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp))
            TextButton(enabled = pageIndex < renderer.pageCount - 1, onClick = { pageIndex++ }) { Text("Próxima ▶") }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun ZipViewer(file: File) {
    val scope = rememberCoroutineScope()
    val entries = remember(file.absolutePath, file.lastModified()) {
        runCatching {
            ZipFile(file).use { zip ->
                zip.entries().asSequence().map { entry ->
                    ZipEntryInfo(entry.name, entry.isDirectory, entry.size.coerceAtLeast(0L))
                }.toList()
            }
        }.getOrDefault(emptyList())
    }
    var status by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(XpPanel).padding(horizontal = 8.dp)
        ) {
            Text("${entries.size} item(ns) no arquivo", fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                scope.launch {
                    status = "Extraindo..."
                    val result = withContext(Dispatchers.IO) { extractZipSafely(file) }
                    status = result.fold(
                        onSuccess = { "Extraído para: ${it.name}" },
                        onFailure = { it.message ?: "Falha ao extrair" },
                    )
                }
            }) { Text("Extrair", fontSize = 12.sp) }
        }
        if (status.isNotBlank()) {
            Text(status, fontSize = 12.sp, color = XpTextSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries) { entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Text(if (entry.directory) "📁" else "📄", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!entry.directory) Text(formatViewerBytes(entry.size), fontSize = 12.sp, color = XpTextSecondary)
                    }
                }
                HorizontalDivider(color = Color(0xFFE0E0E0))
            }
        }
    }
}

@Composable
private fun ApkViewer(file: File, onInstall: () -> Unit) {
    val context = LocalContext.current
    val info = remember(file.absolutePath, file.lastModified()) {
        runCatching {
            val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            packageInfo?.let {
                ApkInfo(
                    packageName = it.packageName ?: "Desconhecido",
                    versionName = it.versionName ?: "Desconhecida",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode.toString() else it.versionCode.toString(),
                )
            }
        }.getOrNull()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.file_apk),
            contentDescription = null,
            modifier = Modifier.size(86.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(file.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(18.dp))
        InfoLine("Pacote", info?.packageName ?: "Não identificado")
        InfoLine("Versão", info?.versionName ?: "Não identificada")
        InfoLine("Código", info?.versionCode ?: "-")
        InfoLine("Tamanho", formatViewerBytes(file.length()))
        Spacer(Modifier.height(20.dp))
        Button(onClick = onInstall) { Text("Instalar / Abrir com sistema") }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(86.dp))
        Text(value, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun UnsupportedViewer(onOpenExternal: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text("Este formato ainda não possui visualização interna.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onOpenExternal) { Text("Abrir com outro aplicativo") }
    }
}

@Composable
private fun UnsupportedMessage(message: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text(message) }
}

@Composable
private fun ViewerStatusBar(file: File) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(26.dp).background(XpChrome).border(1.dp, XpChromeBorder).padding(horizontal = 8.dp)
    ) {
        Text(file.extension.uppercase().ifBlank { "ARQUIVO" }, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(formatViewerBytes(file.length()), fontSize = 11.sp)
    }
}

private data class ZipEntryInfo(val name: String, val directory: Boolean, val size: Long)
private data class ApkInfo(val packageName: String, val versionName: String, val versionCode: String)

private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > reqWidth * 2 || bounds.outHeight / sample > reqHeight * 2) sample *= 2
        BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) })
    }.getOrNull()
}

private fun readTextPreview(file: File, maxChars: Int = 2_000_000): String {
    return runCatching {
        file.bufferedReader().use { reader ->
            val buffer = CharArray(8192)
            val out = StringBuilder()
            while (out.length < maxChars) {
                val count = reader.read(buffer, 0, minOf(buffer.size, maxChars - out.length))
                if (count <= 0) break
                out.append(buffer, 0, count)
            }
            if (reader.read() >= 0) out.append("\n\n[Arquivo grande: visualização limitada aos primeiros ${maxChars} caracteres]")
            out.toString()
        }
    }.getOrElse { "Não foi possível ler este arquivo.\n${it.message.orEmpty()}" }
}

private fun extractZipSafely(zipFile: File): Result<File> = runCatching {
    val parent = zipFile.parentFile ?: error("Pasta de destino indisponível.")
    val baseName = zipFile.nameWithoutExtension.ifBlank { "extraido" }
    var destination = File(parent, baseName)
    var counter = 1
    while (destination.exists()) {
        destination = File(parent, "$baseName ($counter)")
        counter++
    }
    check(destination.mkdirs()) { "Não foi possível criar a pasta de extração." }
    val destinationPath = destination.canonicalPath + File.separator
    ZipFile(zipFile).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            val target = File(destination, entry.name)
            val targetPath = target.canonicalPath
            require(targetPath == destination.canonicalPath || targetPath.startsWith(destinationPath)) { "Entrada ZIP inválida." }
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
        }
    }
    destination
}

private fun formatViewerBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    do {
        value /= 1024.0
        index++
    } while (value >= 1024 && index < units.lastIndex)
    return String.format(java.util.Locale.getDefault(), "%.1f %s", value, units[index])
}
