package com.exploradorxp.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

private val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp", "gif")
private val videoExtensions = setOf("mp4", "m4v", "3gp", "webm", "mkv", "avi", "mov")
private val audioExtensions = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus")
internal val textExtensions = setOf(
    "txt", "log", "ini", "cfg", "conf", "properties", "json", "xml", "csv", "sql",
    "css", "js", "mjs", "cjs", "ts", "tsx", "jsx", "kt", "kts", "java", "py", "sh", "bash",
    "bat", "cmd", "ps1", "yml", "yaml", "md", "markdown", "mds", "gradle", "toml", "env",
    "php", "rb", "go", "rs", "swift", "dart", "c", "h", "hpp", "cpp", "cc", "vue", "svelte",
    "tex", "properties", "gitignore", "gitattributes", "editorconfig"
)

private val textFileNames = setOf(
    "makefile", "dockerfile", "readme", "license", ".gitignore", ".gitattributes", ".editorconfig"
)

private const val ZIP_PREVIEW_MAX_ENTRIES = 3_000

private sealed interface ViewerLoadState<out T> {
    data object Loading : ViewerLoadState<Nothing>
    data class Success<T>(val value: T) : ViewerLoadState<T>
    data class Error(val message: String) : ViewerLoadState<Nothing>
}

private data class ZipPreview(val entries: List<ZipEntryInfo>, val truncated: Boolean)

fun supportsInternalViewer(file: File): Boolean {
    val ext = file.extension.lowercase()
    return ext in imageExtensions || ext in videoExtensions || ext in audioExtensions ||
        ext in textExtensions || ext in setOf("html", "htm", "pdf", "zip", "apk") ||
        file.name.lowercase() in textFileNames
}

@Composable
fun InternalViewerScreen(
    file: File,
    onClose: () -> Unit,
    onOpenExternal: (File) -> Unit,
) {
    var activeFilePath by rememberSaveable(file.absolutePath) { mutableStateOf(file.absolutePath) }
    val activeFile = remember(activeFilePath) { File(activeFilePath) }
    val extension = activeFile.extension.lowercase()
    val isVideo = extension in videoExtensions
    val isTextDocument = extension in textExtensions || extension in setOf("html", "htm") ||
        activeFile.name.lowercase() in textFileNames
    var contentFullScreen by rememberSaveable(activeFilePath) { mutableStateOf(false) }
    var guardedCloseRequest by remember(activeFilePath) { mutableStateOf<(() -> Unit)?>(null) }

    BackHandler {
        when {
            contentFullScreen -> contentFullScreen = false
            isTextDocument && guardedCloseRequest != null -> guardedCloseRequest?.invoke()
            else -> onClose()
        }
    }

    val requestClose: () -> Unit = {
        if (isTextDocument && guardedCloseRequest != null) {
            guardedCloseRequest?.invoke()
            Unit
        } else {
            onClose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isVideo) Color.Black else Color(0xFFF6F2E8))
    ) {
        if (!contentFullScreen) {
            ViewerTitleBar(activeFile, requestClose)
            ViewerToolbar(activeFile) { onOpenExternal(activeFile) }
            HorizontalDivider(color = Color(0xFFB8C7DA))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (extension) {
                in imageExtensions -> ImageViewer(activeFile) { onOpenExternal(activeFile) }
                in videoExtensions -> VideoPlayerViewer(
                    file = activeFile,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                )
                in audioExtensions -> MediaViewer(activeFile)
                "html", "htm" -> TextCodeEditorViewer(
                    file = activeFile,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onClose = onClose,
                    onCloseHandlerChanged = { guardedCloseRequest = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                    onFileChanged = { activeFilePath = it.absolutePath },
                )
                "pdf" -> PdfViewer(activeFile)
                "zip" -> ZipViewer(activeFile)
                "apk" -> ApkViewer(activeFile) { onOpenExternal(activeFile) }
                in textExtensions -> TextCodeEditorViewer(
                    file = activeFile,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onClose = onClose,
                    onCloseHandlerChanged = { guardedCloseRequest = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                    onFileChanged = { activeFilePath = it.absolutePath },
                )
                else -> if (activeFile.name.lowercase() in textFileNames) {
                    TextCodeEditorViewer(
                        file = activeFile,
                        fullScreen = contentFullScreen,
                        onFullScreenChange = { contentFullScreen = it },
                        onClose = onClose,
                        onCloseHandlerChanged = { guardedCloseRequest = it },
                        onOpenExternal = { onOpenExternal(activeFile) },
                        onFileChanged = { activeFilePath = it.absolutePath },
                    )
                } else {
                    UnsupportedViewer { onOpenExternal(activeFile) }
                }
            }
        }
        if (!contentFullScreen) ViewerStatusBar(activeFile)
    }
}

@Composable
private fun ViewerTitleBar(file: File, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF2F92F6), XpBlue, XpBlueDark)))
            .padding(horizontal = 7.dp)
    ) {
        CachedResourceIcon(
            resId = FileIconMapper.iconFor(file),
            contentDescription = null,
            modifier = Modifier.size(25.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = file.name,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(25.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFF36B58), Color(0xFFB92318))))
                .border(1.dp, Color.White)
                .clickable(onClick = onClose),
        ) {
            Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ViewerToolbar(file: File, onOpenExternal: () -> Unit) {
    val typeLabel = remember(file.absolutePath) { FileTypeClassifier.labelFor(file, false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(39.dp)
            .background(XpChrome)
            .border(1.dp, XpChromeBorder)
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = "$typeLabel  •  ${formatViewerBytes(file.length())}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF303030),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ViewerActionButton("Abrir com...", onClick = onOpenExternal)
    }
}

@Composable
private fun ViewerActionButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 74.dp)
            .background(
                color = when {
                    !enabled -> Color(0xFFF0F2F4)
                    pressed -> XpControlPressed
                    else -> XpControlBackground
                },
                shape = RoundedCornerShape(4.dp),
            )
            .border(1.dp, if (enabled) XpControlBorder else Color(0xFFD3D9E0), RoundedCornerShape(4.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
    ) {
        Text(
            label,
            color = if (enabled) Color(0xFF202020) else Color(0xFF999999),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun ImageViewer(file: File, onOpenExternal: () -> Unit) {
    val key = "${file.absolutePath}:${file.lastModified()}"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<Bitmap>>(ViewerLoadState.Loading) }

    LaunchedEffect(key) {
        loadState = ViewerLoadState.Loading
        loadState = withContext(Dispatchers.IO) {
            when {
                !file.exists() -> ViewerLoadState.Error("O arquivo não existe mais.")
                !file.canRead() -> ViewerLoadState.Error("O Explorador XP não conseguiu acessar esta imagem.")
                file.length() <= 0L -> ViewerLoadState.Error("A imagem está vazia ou corrompida.")
                else -> {
                    val bitmap = decodeSampledBitmap(file, 1800, 1800)
                    if (bitmap != null) ViewerLoadState.Success(bitmap)
                    else ViewerLoadState.Error("O formato ou a codificação desta imagem não pôde ser exibido internamente.")
                }
            }
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
            is ViewerLoadState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(20.dp),
            ) {
                Text(state.message, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                ViewerActionButton("Abrir com outro aplicativo", onClick = onOpenExternal)
            }
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
            modifier = Modifier.fillMaxWidth().height(40.dp).background(XpPanel).padding(horizontal = 6.dp)
        ) {
            ViewerActionButton("◀ Anterior", enabled = pageIndex > 0) { pageIndex-- }
            Text(
                if (pageCount > 0) "Página ${pageIndex + 1} de $pageCount" else "Abrindo PDF...",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            ViewerActionButton("Próxima ▶", enabled = pageCount > 0 && pageIndex < pageCount - 1) { pageIndex++ }
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
            modifier = Modifier.fillMaxWidth().height(40.dp).background(XpPanel).padding(horizontal = 8.dp)
        ) {
            Text(
                when {
                    preview == null -> "Conteúdo do arquivo"
                    preview.truncated -> "${preview.entries.size}+ itens no arquivo"
                    else -> if (preview.entries.size == 1) "1 item no arquivo" else "${preview.entries.size} itens no arquivo"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            ViewerActionButton(
                label = "Extrair",
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
            )
        }
        if (status.isNotBlank()) {
            Text(
                status,
                fontSize = 11.sp,
                color = XpTextSecondary,
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF8D9)).padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(28.dp).background(Color(0xFFEAF2FB)).border(1.dp, Color(0xFFC9D8E8)).padding(horizontal = 10.dp),
        ) {
            Text("Nome", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Tamanho", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(78.dp))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = loadState) {
                ViewerLoadState.Loading -> LoadingPanel("Lendo conteúdo...")
                is ViewerLoadState.Error -> UnsupportedMessage(state.message)
                is ViewerLoadState.Success -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.value.entries) { entry ->
                        val cleanPath = entry.name.trimEnd('/')
                        val displayName = cleanPath.substringAfterLast('/').ifBlank { entry.name }
                        val parentPath = cleanPath.substringBeforeLast('/', "")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            CachedResourceIcon(
                                resId = FileIconMapper.iconFor(File(cleanPath), entry.directory),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (parentPath.isNotBlank()) {
                                    Text(parentPath, fontSize = 10.sp, color = XpTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Text(
                                if (entry.directory) "Pasta" else formatViewerBytes(entry.size),
                                fontSize = 11.sp,
                                color = XpTextSecondary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(78.dp),
                            )
                        }
                        HorizontalDivider(color = Color(0xFFE0E6EE))
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
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<ApkInfo>>(ViewerLoadState.Loading) }

    LaunchedEffect(key) {
        loadState = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val info = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_META_DATA)
                    ?: error("O Android não conseguiu ler os metadados deste APK.")
                val appInfo = info.applicationInfo?.also {
                    it.sourceDir = file.absolutePath
                    it.publicSourceDir = file.absolutePath
                }
                val packageName = info.packageName
                val installedInfo = runCatching { pm.getPackageInfo(packageName, 0) }.getOrNull()
                val iconBitmap = runCatching {
                    appInfo?.loadIcon(pm)?.toBitmap(width = 144, height = 144, config = Bitmap.Config.ARGB_8888)
                }.getOrNull()
                ApkInfo(
                    appName = runCatching { appInfo?.loadLabel(pm)?.toString() }.getOrNull().orEmpty().ifBlank { file.nameWithoutExtension },
                    packageName = packageName,
                    versionName = info.versionName ?: "Desconhecida",
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode.toString() else info.versionCode.toString(),
                    minSdk = appInfo?.minSdkVersion,
                    targetSdk = appInfo?.targetSdkVersion,
                    installedVersion = installedInfo?.versionName,
                    iconBitmap = iconBitmap,
                )
            }.fold(
                onSuccess = { ViewerLoadState.Success(it) },
                onFailure = { ViewerLoadState.Error(it.message ?: "Não foi possível analisar o APK.") },
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
    ) {
        when (val state = loadState) {
            ViewerLoadState.Loading -> {
                CircularProgressIndicator(color = XpBlue)
                Spacer(Modifier.height(10.dp))
                Text("Lendo informações do APK...", color = XpTextSecondary, fontSize = 12.sp)
            }
            is ViewerLoadState.Error -> {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.file_apk_large),
                    contentDescription = null,
                    modifier = Modifier.size(82.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                Text(state.message, color = XpTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            is ViewerLoadState.Success -> {
                val info = state.value
                if (info.iconBitmap != null) {
                    Image(
                        bitmap = info.iconBitmap.asImageBitmap(),
                        contentDescription = info.appName,
                        modifier = Modifier.size(92.dp).clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.file_apk_large),
                        contentDescription = null,
                        modifier = Modifier.size(86.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(info.appName, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                Text(file.name, color = XpTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .background(if (info.installedVersion != null) Color(0xFFE2F4E4) else Color(0xFFEAF2FB))
                        .border(1.dp, if (info.installedVersion != null) Color(0xFF78A87B) else Color(0xFFA9BED5))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (info.installedVersion != null) "Instalado • versão ${info.installedVersion}" else "Não instalado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF30475F),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(XpPanel)
                        .border(1.dp, XpChromeBorder)
                        .padding(12.dp),
                ) {
                    InfoLine("Pacote", info.packageName)
                    InfoLine("Versão", info.versionName)
                    InfoLine("Código", info.versionCode)
                    InfoLine("Android mín.", info.minSdk?.let { "API $it" } ?: "-")
                    InfoLine("Android alvo", info.targetSdk?.let { "API $it" } ?: "-")
                    InfoLine("Tamanho", formatViewerBytes(file.length()))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        ViewerActionButton("Instalar / Abrir com sistema", onClick = onInstall)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(92.dp))
        Text(value, fontSize = 12.sp, color = Color(0xFF303030), modifier = Modifier.weight(1f))
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
        ViewerActionButton("Abrir com outro aplicativo", onClick = onOpenExternal)
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
    val extension = remember(file.absolutePath) { FileTypeClassifier.labelFor(file, false) }
    val size = remember(file.absolutePath, file.lastModified()) { file.length() }
    val parent = remember(file.absolutePath) { file.parentFile?.name.orEmpty() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(28.dp).background(XpChrome).border(1.dp, XpChromeBorder).padding(horizontal = 8.dp)
    ) {
        Text(
            text = if (parent.isBlank()) extension else "$parent  •  $extension",
            fontSize = 11.sp,
            color = Color(0xFF303030),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.height(18.dp).width(1.dp).background(XpChromeBorder))
        Text(formatViewerBytes(size), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}

private data class PdfPageData(val bitmap: Bitmap, val pageCount: Int)
private data class ZipEntryInfo(val name: String, val directory: Boolean, val size: Long)
private data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val minSdk: Int?,
    val targetSdk: Int?,
    val installedVersion: String?,
    val iconBitmap: Bitmap?,
)

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
