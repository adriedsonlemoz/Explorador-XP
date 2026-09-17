package com.exploradorxp.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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

private const val TEXT_PREVIEW_MAX_CHARS = 350_000
private const val TEXT_EDIT_MAX_BYTES = 750_000L
private const val ZIP_PREVIEW_MAX_ENTRIES = 3_000

private sealed interface ViewerLoadState<out T> {
    data object Loading : ViewerLoadState<Nothing>
    data class Success<T>(val value: T) : ViewerLoadState<T>
    data class Error(val message: String) : ViewerLoadState<Nothing>
}

private data class TextPreview(val text: String, val truncated: Boolean)
private data class ZipPreview(val entries: List<ZipEntryInfo>, val truncated: Boolean)

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
        ViewerToolbar(onOpenExternal)
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
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
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
private fun ViewerToolbar(onOpenExternal: () -> Unit) {
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
        TextButton(onClick = onOpenExternal) {
            Text("Abrir com...", color = XpBlueDark, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ImageViewer(file: File) {
    val key = "${file.absolutePath}:${file.lastModified()}"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<Bitmap>>(ViewerLoadState.Loading) }

    LaunchedEffect(key) {
        loadState = ViewerLoadState.Loading
        loadState = withContext(Dispatchers.IO) {
            val bitmap = decodeSampledBitmap(file, 1800, 1800)
            if (bitmap != null) ViewerLoadState.Success(bitmap)
            else ViewerLoadState.Error("Não foi possível exibir a imagem.")
        }
    }

    val bitmap = (loadState as? ViewerLoadState.Success)?.value
    DisposableEffect(bitmap) {
        onDispose {
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color(0xFF262626)).padding(8.dp)
    ) {
        when (val state = loadState) {
            ViewerLoadState.Loading -> CircularProgressIndicator(color = Color.White)
            is ViewerLoadState.Error -> Text(state.message, color = Color.White)
            is ViewerLoadState.Success -> Image(
                bitmap = state.value.asImageBitmap(),
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
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
        update = { /* O VideoView mantém o estado sem reiniciar em recomposições. */ }
    )
}

@Composable
private fun HtmlViewer(file: File) {
    var sourceMode by remember(file.absolutePath) { mutableStateOf(false) }
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
                        settings.loadsImagesAutomatically = true
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
    val key = "${file.absolutePath}:${file.lastModified()}"
    val scope = rememberCoroutineScope()
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<TextPreview>>(ViewerLoadState.Loading) }
    var editing by remember(file.absolutePath) { mutableStateOf(false) }
    var status by remember(file.absolutePath) { mutableStateOf("") }
    var editView by remember(file.absolutePath) { mutableStateOf<EditText?>(null) }
    val editableBySize = file.length() <= TEXT_EDIT_MAX_BYTES

    LaunchedEffect(key) {
        editing = false
        loadState = ViewerLoadState.Loading
        loadState = withContext(Dispatchers.IO) {
            runCatching { readTextPreview(file, TEXT_PREVIEW_MAX_CHARS) }
                .fold(
                    onSuccess = { ViewerLoadState.Success(it) },
                    onFailure = { ViewerLoadState.Error("Não foi possível ler este arquivo.\n${it.message.orEmpty()}") },
                )
        }
    }

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
            val currentPreview = (loadState as? ViewerLoadState.Success<TextPreview>)?.value
            val canEdit = editableBySize && currentPreview?.truncated == false
            TextButton(
                enabled = canEdit,
                onClick = {
                    if (!editing) {
                        editing = true
                        status = ""
                    } else {
                        val newText = editView?.text?.toString()
                            ?: (loadState as? ViewerLoadState.Success<TextPreview>)?.value?.text
                            ?: ""
                        scope.launch {
                            status = "Salvando..."
                            val result = withContext(Dispatchers.IO) { runCatching { file.writeText(newText) } }
                            if (result.isSuccess) {
                                loadState = ViewerLoadState.Success(TextPreview(newText, false))
                                status = "Salvo"
                                editing = false
                            } else {
                                status = result.exceptionOrNull()?.message ?: "Falha ao salvar"
                            }
                        }
                    }
                }
            ) { Text(if (editing) "Salvar" else "Editar", fontSize = 12.sp) }
            if (editing) {
                TextButton(onClick = {
                    editing = false
                    editView = null
                    status = ""
                }) { Text("Cancelar", fontSize = 12.sp) }
            }
        }

        val preview = (loadState as? ViewerLoadState.Success<TextPreview>)?.value
        val infoText = when {
            status.isNotBlank() -> status
            preview?.truncated == true -> "Arquivo grande: mostrando os primeiros ${TEXT_PREVIEW_MAX_CHARS / 1000} mil caracteres."
            !editableBySize -> "Arquivo grande: edição desativada para manter o visualizador responsivo."
            else -> ""
        }
        if (infoText.isNotBlank()) {
            Text(
                infoText,
                fontSize = 12.sp,
                color = XpTextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = loadState) {
                ViewerLoadState.Loading -> LoadingPanel("Carregando texto...")
                is ViewerLoadState.Error -> UnsupportedMessage(state.message)
                is ViewerLoadState.Success -> {
                    if (editing) {
                        EditableTextView(
                            initialText = state.value.text,
                            onViewReady = { editView = it },
                        )
                    } else {
                        ReadOnlyTextView(state.value.text)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyTextView(text: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val padding = (10 * context.resources.displayMetrics.density).toInt()
            TextView(context).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                setTextColor(android.graphics.Color.rgb(32, 32, 32))
                typeface = Typeface.MONOSPACE
                textSize = 13f
                gravity = Gravity.TOP or Gravity.START
                setPadding(padding, padding, padding, padding)
                setTextIsSelectable(true)
                movementMethod = ScrollingMovementMethod.getInstance()
                isVerticalScrollBarEnabled = true
                this.text = text
                tag = text
            }
        },
        update = { view ->
            if (view.tag !== text) {
                view.text = text
                view.tag = text
            }
        }
    )
}

@Composable
private fun EditableTextView(initialText: String, onViewReady: (EditText) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val padding = (10 * context.resources.displayMetrics.density).toInt()
            EditText(context).apply {
                setBackgroundColor(android.graphics.Color.WHITE)
                setTextColor(android.graphics.Color.rgb(32, 32, 32))
                typeface = Typeface.MONOSPACE
                textSize = 13f
                gravity = Gravity.TOP or Gravity.START
                setPadding(padding, padding, padding, padding)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setHorizontallyScrolling(false)
                isVerticalScrollBarEnabled = true
                setText(initialText)
                setSelection(text.length)
                onViewReady(this)
            }
        },
        update = { view -> onViewReady(view) },
    )
}

@Composable
private fun PdfViewer(file: File) {
    val key = "${file.absolutePath}:${file.lastModified()}"
    var pageIndex by remember(file.absolutePath) { mutableIntStateOf(0) }
    var pageCount by remember(file.absolutePath) { mutableIntStateOf(0) }
    var pageState by remember(key) { mutableStateOf<ViewerLoadState<PdfPageData>>(ViewerLoadState.Loading) }

    LaunchedEffect(key, pageIndex) {
        pageState = ViewerLoadState.Loading
        val result = withContext(Dispatchers.IO) {
            runCatching { renderPdfPage(file, pageIndex) }.fold(
                onSuccess = { ViewerLoadState.Success(it) },
                onFailure = { ViewerLoadState.Error(it.message ?: "Falha ao renderizar a página.") },
            )
        }
        if (result is ViewerLoadState.Success) pageCount = result.value.pageCount
        pageState = result
    }

    val pageBitmap = (pageState as? ViewerLoadState.Success<PdfPageData>)?.value?.bitmap
    DisposableEffect(pageBitmap) {
        onDispose {
            if (pageBitmap != null && !pageBitmap.isRecycled) pageBitmap.recycle()
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF5B5B5B))) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().background(XpPanel).padding(4.dp)
        ) {
            TextButton(enabled = pageIndex > 0, onClick = { pageIndex-- }) { Text("◀ Anterior") }
            Text(
                if (pageCount > 0) "Página ${pageIndex + 1} de $pageCount" else "Abrindo PDF...",
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            TextButton(enabled = pageCount > 0 && pageIndex < pageCount - 1, onClick = { pageIndex++ }) { Text("Próxima ▶") }
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
            when (val state = pageState) {
                ViewerLoadState.Loading -> CircularProgressIndicator(color = Color.White)
                is ViewerLoadState.Error -> Text(state.message, color = Color.White)
                is ViewerLoadState.Success -> Image(
                    state.value.bitmap.asImageBitmap(),
                    null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun ZipViewer(file: File) {
    val scope = rememberCoroutineScope()
    val key = "${file.absolutePath}:${file.lastModified()}"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<ZipPreview>>(ViewerLoadState.Loading) }
    var status by remember(file.absolutePath) { mutableStateOf("") }

    LaunchedEffect(key) {
        loadState = ViewerLoadState.Loading
        loadState = withContext(Dispatchers.IO) {
            runCatching { readZipPreview(file) }.fold(
                onSuccess = { ViewerLoadState.Success(it) },
                onFailure = { ViewerLoadState.Error(it.message ?: "Não foi possível ler o ZIP.") },
            )
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        val preview = (loadState as? ViewerLoadState.Success<ZipPreview>)?.value
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(XpPanel).padding(horizontal = 8.dp)
        ) {
            Text(
                when {
                    preview == null -> "Conteúdo do arquivo"
                    preview.truncated -> "${preview.entries.size}+ itens no arquivo"
                    else -> "${preview.entries.size} item(ns) no arquivo"
                },
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                enabled = loadState is ViewerLoadState.Success,
                onClick = {
                    scope.launch {
                        status = "Extraindo..."
                        val result = withContext(Dispatchers.IO) { extractZipSafely(file) }
                        status = result.fold(
                            onSuccess = { "Extraído para: ${it.name}" },
                            onFailure = { it.message ?: "Falha ao extrair" },
                        )
                    }
                }
            ) { Text("Extrair", fontSize = 12.sp) }
        }
        if (status.isNotBlank()) {
            Text(status, fontSize = 12.sp, color = XpTextSecondary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = loadState) {
                ViewerLoadState.Loading -> LoadingPanel("Lendo conteúdo...")
                is ViewerLoadState.Error -> UnsupportedMessage(state.message)
                is ViewerLoadState.Success -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.value.entries) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
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
    }
}

@Composable
private fun ApkViewer(file: File, onInstall: () -> Unit) {
    val context = LocalContext.current
    val key = "${file.absolutePath}:${file.lastModified()}"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<ApkInfo?>>(ViewerLoadState.Loading) }

    LaunchedEffect(key) {
        loadState = withContext(Dispatchers.IO) {
            runCatching {
                val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                packageInfo?.let {
                    ApkInfo(
                        packageName = it.packageName ?: "Desconhecido",
                        versionName = it.versionName ?: "Desconhecida",
                        versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) it.longVersionCode.toString() else it.versionCode.toString(),
                    )
                }
            }.fold(
                onSuccess = { ViewerLoadState.Success(it) },
                onFailure = { ViewerLoadState.Error(it.message ?: "Não foi possível analisar o APK.") },
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Android,
            contentDescription = null,
            tint = Color(0xFF20A84A),
            modifier = Modifier.size(86.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(file.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(18.dp))
        when (val state = loadState) {
            ViewerLoadState.Loading -> CircularProgressIndicator(color = XpBlue)
            is ViewerLoadState.Error -> Text(state.message, color = XpTextSecondary)
            is ViewerLoadState.Success -> {
                val info = state.value
                InfoLine("Pacote", info?.packageName ?: "Não identificado")
                InfoLine("Versão", info?.versionName ?: "Não identificada")
                InfoLine("Código", info?.versionCode ?: "-")
            }
        }
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
private fun LoadingPanel(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = XpBlue)
        Spacer(Modifier.height(10.dp))
        Text(message, fontSize = 12.sp, color = XpTextSecondary)
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
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(message)
    }
}

@Composable
private fun ViewerStatusBar(file: File) {
    val extension = remember(file.absolutePath) { file.extension.uppercase().ifBlank { "ARQUIVO" } }
    val size = remember(file.absolutePath, file.lastModified()) { file.length() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(26.dp).background(XpChrome).border(1.dp, XpChromeBorder).padding(horizontal = 8.dp)
    ) {
        Text(extension, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(formatViewerBytes(size), fontSize = 11.sp)
    }
}

private data class PdfPageData(val bitmap: Bitmap, val pageCount: Int)
private data class ZipEntryInfo(val name: String, val directory: Boolean, val size: Long)
private data class ApkInfo(val packageName: String, val versionName: String, val versionCode: String)

private fun renderPdfPage(file: File, pageIndex: Int): PdfPageData {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            require(renderer.pageCount > 0) { "PDF sem páginas." }
            require(pageIndex in 0 until renderer.pageCount) { "Página inválida." }
            renderer.openPage(pageIndex).use { page ->
                val scale = 1.35f
                val width = (page.width * scale).toInt().coerceAtLeast(1)
                val height = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return PdfPageData(bitmap, renderer.pageCount)
            }
        }
    }
}

private fun decodeSampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (bounds.outWidth / sample > reqWidth * 2 || bounds.outHeight / sample > reqHeight * 2) sample *= 2
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }.getOrNull()
}

private fun readTextPreview(file: File, maxChars: Int): TextPreview {
    file.bufferedReader().use { reader ->
        val buffer = CharArray(8192)
        val out = StringBuilder(minOf(maxChars, 64 * 1024))
        while (out.length < maxChars) {
            val count = reader.read(buffer, 0, minOf(buffer.size, maxChars - out.length))
            if (count <= 0) break
            out.append(buffer, 0, count)
        }
        val truncated = reader.read() >= 0
        return TextPreview(out.toString(), truncated)
    }
}

private fun readZipPreview(file: File): ZipPreview {
    ZipFile(file).use { zip ->
        val entries = ArrayList<ZipEntryInfo>(minOf(zip.size(), ZIP_PREVIEW_MAX_ENTRIES))
        val enumeration = zip.entries()
        var truncated = false
        while (enumeration.hasMoreElements()) {
            if (entries.size >= ZIP_PREVIEW_MAX_ENTRIES) {
                truncated = true
                break
            }
            val entry = enumeration.nextElement()
            entries += ZipEntryInfo(entry.name, entry.isDirectory, entry.size.coerceAtLeast(0L))
        }
        return ZipPreview(entries, truncated)
    }
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
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
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
