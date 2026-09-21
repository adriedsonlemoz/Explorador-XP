package com.exploradorxp.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp", "gif")
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

private sealed interface ViewerLoadState<out T> {
    data object Loading : ViewerLoadState<Nothing>
    data class Success<T>(val value: T) : ViewerLoadState<T>
    data class Error(val message: String) : ViewerLoadState<Nothing>
}

fun supportsInternalViewer(file: File): Boolean {
    val ext = file.extension.lowercase()
    return ext in imageExtensions || ext in videoExtensions || ext in audioExtensions ||
        ext in textExtensions || ext in setOf("html", "htm", "pdf", "zip", "apk") ||
        file.name.lowercase() in textFileNames
}

@Composable
fun InternalViewerScreen(
    file: File,
    externalMimeType: String? = null,
    forcedReadOnly: Boolean = false,
    folderImages: List<File> = emptyList(),
    onClose: () -> Unit,
    onOpenExternal: (File) -> Unit,
    onOpenFolder: (File) -> Unit,
) {
    var activeFilePath by rememberSaveable(file.absolutePath) { mutableStateOf(file.absolutePath) }
    var returnToArchivePath by rememberSaveable(file.absolutePath) { mutableStateOf<String?>(null) }
    val activeFile = remember(activeFilePath) { File(activeFilePath) }
    val extension = activeFile.extension.lowercase()
    val isVideo = extension in videoExtensions
    val isExternalText = remember(externalMimeType) {
        ExternalOpenSupport.normalizeMime(externalMimeType).startsWith("text/")
    }
    val isTextDocument = extension in textExtensions || extension in setOf("html", "htm") ||
        activeFile.name.lowercase() in textFileNames || isExternalText
    val isArchivePreview = remember(activeFilePath, returnToArchivePath) {
        returnToArchivePath != null || activeFile.absolutePath.contains("/archive-preview/")
    }
    var contentFullScreen by rememberSaveable(activeFilePath) { mutableStateOf(false) }
    var guardedCloseRequest by remember(activeFilePath) { mutableStateOf<(() -> Unit)?>(null) }

    BackHandler {
        when {
            contentFullScreen -> contentFullScreen = false
            returnToArchivePath != null -> {
                activeFilePath = returnToArchivePath!!
                returnToArchivePath = null
            }
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
                in imageExtensions -> ImageViewer(
                    file = activeFile,
                    folderImages = folderImages,
                    onSelectFile = { selected -> activeFilePath = selected.absolutePath },
                    onOpenExternal = { onOpenExternal(activeFile) },
                )
                in videoExtensions -> VideoPlayerViewer(
                    file = activeFile,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                )
                in audioExtensions -> MediaViewer(activeFile)
                "html", "htm" -> TextCodeEditorViewer(
                    file = activeFile,
                    forcedReadOnly = isArchivePreview || forcedReadOnly,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onClose = onClose,
                    onCloseHandlerChanged = { guardedCloseRequest = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                    onFileChanged = { activeFilePath = it.absolutePath },
                )
                "pdf" -> PdfViewer(activeFile)
                "zip" -> ArchiveZipViewer(
                    file = activeFile,
                    onPreviewFile = { preview ->
                        returnToArchivePath = activeFile.absolutePath
                        activeFilePath = preview.absolutePath
                    },
                    onOpenExternal = onOpenExternal,
                    onOpenFolder = onOpenFolder,
                )
                "apk" -> ApkViewer(activeFile) { onOpenExternal(activeFile) }
                in textExtensions -> TextCodeEditorViewer(
                    file = activeFile,
                    forcedReadOnly = isArchivePreview || forcedReadOnly,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onClose = onClose,
                    onCloseHandlerChanged = { guardedCloseRequest = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                    onFileChanged = { activeFilePath = it.absolutePath },
                )
                else -> if (activeFile.name.lowercase() in textFileNames || isExternalText) {
                    TextCodeEditorViewer(
                        file = activeFile,
                        forcedReadOnly = isArchivePreview || forcedReadOnly,
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
private fun ImageViewer(
    file: File,
    folderImages: List<File>,
    onSelectFile: (File) -> Unit,
    onOpenExternal: () -> Unit,
) {
    val key = "${file.absolutePath}:${file.lastModified()}"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<Bitmap>>(ViewerLoadState.Loading) }

    // A sequência é recebida do snapshot da pasta que já estava aberta no Explorer.
    // O visualizador não varre subpastas e não mistura imagens de outras localizações.
    val gallery = remember(folderImages, file.absolutePath) {
        val sameFolder = file.parentFile?.absolutePath
        val valid = folderImages
            .asSequence()
            .filter { it.parentFile?.absolutePath == sameFolder }
            .filter { it.extension.lowercase() in imageExtensions }
            .distinctBy { it.absolutePath }
            .toMutableList()
        if (valid.none { it.absolutePath == file.absolutePath }) valid.add(file)
        valid
    }
    val currentIndex = gallery.indexOfFirst { it.absolutePath == file.absolutePath }.coerceAtLeast(0)
    val canGoPrevious = currentIndex > 0
    val canGoNext = currentIndex in 0 until gallery.lastIndex

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

    fun previous() {
        if (canGoPrevious) onSelectFile(gallery[currentIndex - 1])
    }

    fun next() {
        if (canGoNext) onSelectFile(gallery[currentIndex + 1])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF262626))
    ) {
        var dragDistance by remember(file.absolutePath) { mutableStateOf(0f) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .pointerInput(file.absolutePath, gallery.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onHorizontalDrag = { _, amount ->
                            dragDistance += amount
                        },
                        onDragEnd = {
                            when {
                                dragDistance <= -80f -> next()
                                dragDistance >= 80f -> previous()
                            }
                            dragDistance = 0f
                        },
                        onDragCancel = { dragDistance = 0f },
                    )
                }
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

        if (gallery.size > 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF171717))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            ) {
                ViewerActionButton("◀ Anterior", enabled = canGoPrevious, onClick = ::previous)
                Text(
                    text = "${currentIndex + 1} de ${gallery.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                ViewerActionButton("Próxima ▶", enabled = canGoNext, onClick = ::next)
            }
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
private fun ApkViewer(file: File, onInstall: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeRevision by remember(file.absolutePath) { mutableIntStateOf(0) }
    var pendingInstallAfterPermission by remember(file.absolutePath) { mutableStateOf(false) }
    val key = "${file.absolutePath}:${file.lastModified()}:$resumeRevision"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<ApkInfo>>(ViewerLoadState.Loading) }

    val installerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        resumeRevision++
    }

    fun launchInstallerNow() {
        runCatching { installerLauncher.launch(apkInstallerIntent(context, file)) }.onFailure {
            Toast.makeText(context, "Não foi possível abrir o instalador do Android.", Toast.LENGTH_SHORT).show()
            onInstall()
        }
    }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (pendingInstallAfterPermission &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls())
        ) {
            pendingInstallAfterPermission = false
            launchInstallerNow()
        } else if (pendingInstallAfterPermission) {
            Toast.makeText(
                context,
                "Ative 'Permitir desta fonte' e use Voltar. O instalador abrirá automaticamente.",
                Toast.LENGTH_LONG,
            ).show()
        }
        resumeRevision++
    }

    // O Android não oferece callback no exato momento em que o usuário move o switch da tela
    // do sistema. Assim que a tela de Configurações devolve o foco ao app, este observador
    // continua a instalação automaticamente, sem exigir um segundo toque em Instalar.
    DisposableEffect(lifecycleOwner, pendingInstallAfterPermission, file.absolutePath) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                pendingInstallAfterPermission &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls())
            ) {
                pendingInstallAfterPermission = false
                launchInstallerNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun requestInstall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            pendingInstallAfterPermission = true
            Toast.makeText(
                context,
                "Ative 'Permitir desta fonte' e toque em Voltar. A instalação continuará sozinha.",
                Toast.LENGTH_LONG,
            ).show()
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            )
            unknownSourcesLauncher.launch(settingsIntent)
        } else {
            pendingInstallAfterPermission = false
            launchInstallerNow()
        }
    }

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
                val installedInfo = runCatching {
                    if (Build.VERSION.SDK_INT >= 33) {
                        pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(packageName, 0)
                    }
                }.getOrNull()
                val apkVersionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
                val installedVersionCode = installedInfo?.let {
                    if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else {
                        @Suppress("DEPRECATION")
                        it.versionCode.toLong()
                    }
                }
                val iconBitmap = runCatching {
                    appInfo?.loadIcon(pm)?.toBitmap(width = 144, height = 144, config = Bitmap.Config.ARGB_8888)
                }.getOrNull()
                ApkInfo(
                    appName = runCatching { appInfo?.loadLabel(pm)?.toString() }.getOrNull().orEmpty().ifBlank { file.nameWithoutExtension },
                    packageName = packageName,
                    versionName = info.versionName ?: "Desconhecida",
                    versionCode = apkVersionCode.toString(),
                    minSdk = appInfo?.minSdkVersion,
                    targetSdk = appInfo?.targetSdkVersion,
                    installedVersion = installedInfo?.versionName,
                    installedVersionCode = installedVersionCode,
                    canLaunchInstalled = runCatching { pm.getLaunchIntentForPackage(packageName) != null }.getOrDefault(false),
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        when (val state = loadState) {
            ViewerLoadState.Loading -> {
                Spacer(Modifier.height(20.dp))
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
                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(state.message, color = XpTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                ApkPrimaryButton(
                    label = "Tentar instalar",
                    icon = R.drawable.file_apk,
                    primary = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = ::requestInstall,
                )
            }
            is ViewerLoadState.Success -> {
                val info = state.value
                val apkCode = info.versionCode.toLongOrNull()
                val relationLabel = when {
                    info.installedVersionCode == null -> "Não instalado neste aparelho"
                    apkCode != null && info.installedVersionCode < apkCode -> "Atualização disponível"
                    apkCode != null && info.installedVersionCode == apkCode -> "Mesma versão instalada"
                    else -> "APK mais antigo que o instalado"
                }
                val installLabel = when {
                    info.installedVersionCode == null -> "Instalar"
                    apkCode != null && info.installedVersionCode < apkCode -> "Atualizar"
                    apkCode != null && info.installedVersionCode == apkCode -> "Reinstalar"
                    else -> "Instalar esta versão"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F7FC), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFC5D4E6), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    if (info.iconBitmap != null) {
                        Image(
                            bitmap = info.iconBitmap.asImageBitmap(),
                            contentDescription = info.appName,
                            modifier = Modifier.size(82.dp).clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.file_apk_large),
                            contentDescription = null,
                            modifier = Modifier.size(76.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            info.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF152A48),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            file.name,
                            color = XpTextSecondary,
                            fontSize = 10.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (info.installedVersion != null) Color(0xFFE1F3E4) else Color(0xFFE7F0FB),
                                    RoundedCornerShape(6.dp),
                                )
                                .border(
                                    1.dp,
                                    if (info.installedVersion != null) Color(0xFF82AF87) else Color(0xFFABC0D9),
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                relationLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF30475F),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFC5D4E6), RoundedCornerShape(10.dp))
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                ) {
                    ApkInfoLine("Pacote", info.packageName)
                    HorizontalDivider(color = Color(0xFFE3E9F0))
                    ApkInfoLine("Versão do APK", info.versionName)
                    if (info.installedVersion != null) {
                        HorizontalDivider(color = Color(0xFFE3E9F0))
                        ApkInfoLine("Instalada", info.installedVersion)
                    }
                    HorizontalDivider(color = Color(0xFFE3E9F0))
                    ApkInfoLine("Código", info.versionCode)
                    HorizontalDivider(color = Color(0xFFE3E9F0))
                    ApkInfoLine(
                        "Android",
                        "mín. API ${info.minSdk ?: "-"}  •  alvo API ${info.targetSdk ?: "-"}",
                    )
                    HorizontalDivider(color = Color(0xFFE3E9F0))
                    ApkInfoLine("Tamanho", formatViewerBytes(file.length()))
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F9FC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFC5D4E6), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        "Ações",
                        color = Color(0xFF183363),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ApkPrimaryButton(
                            label = installLabel,
                            icon = R.drawable.file_apk,
                            primary = true,
                            modifier = Modifier.weight(1f),
                            onClick = ::requestInstall,
                        )
                        if (info.installedVersion != null && info.canLaunchInstalled) {
                            ApkPrimaryButton(
                                label = "Abrir app",
                                icon = R.drawable.visible,
                                primary = false,
                                modifier = Modifier.weight(1f),
                            ) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(info.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    Toast.makeText(context, "O aplicativo não possui tela inicial para abrir.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Na primeira instalação, o Android pode pedir autorização para este Explorador instalar APKs. Depois de ativar, use Voltar: a instalação continua automaticamente.",
                            fontSize = 10.5.sp,
                            color = XpTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkInfoLine(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            color = Color(0xFF53657B),
            modifier = Modifier.width(108.dp),
        )
        Text(
            value,
            fontSize = 11.5.sp,
            color = Color(0xFF202B38),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ApkPrimaryButton(
    label: String,
    icon: Int,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = when {
        primary && pressed -> Color(0xFF0A54A9)
        primary -> Color(0xFF1476DF)
        pressed -> Color(0xFFDCE8F5)
        else -> Color.White
    }
    val foreground = if (primary) Color.White else Color(0xFF173A67)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .border(1.dp, if (primary) Color(0xFF0D5DB6) else Color(0xFF9DB4CF), RoundedCornerShape(7.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp),
    ) {
        CachedResourceIcon(icon, label, modifier = Modifier.size(19.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
private data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val minSdk: Int?,
    val targetSdk: Int?,
    val installedVersion: String?,
    val installedVersionCode: Long?,
    val canLaunchInstalled: Boolean,
    val iconBitmap: Bitmap?,
)

@Suppress("DEPRECATION")
private fun apkInstallerIntent(context: Context, file: File): Intent {
    require(file.exists() && file.isFile) { "O APK não existe mais." }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mime = "application/vnd.android.package-archive"

    // ACTION_VIEW passou a ser também uma entrada do próprio Explorador XP na alpha.51.
    // Para instalar, use uma ação específica que não casa com o intent-filter "Abrir com".
    val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_RETURN_RESULT, true)
    }
    if (installIntent.resolveActivity(context.packageManager) != null) return installIntent

    // Fallback para ROMs que não publicam ACTION_INSTALL_PACKAGE: escolhe explicitamente um
    // manipulador externo de APK e nunca devolve a intenção ao próprio Explorador XP.
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val handlers = context.packageManager.queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
    val external = handlers.firstOrNull { candidate ->
        candidate.activityInfo?.packageName != context.packageName &&
            ((candidate.activityInfo?.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
    } ?: handlers.firstOrNull { candidate -> candidate.activityInfo?.packageName != context.packageName }

    external?.activityInfo?.let { info ->
        viewIntent.setClassName(info.packageName, info.name)
    }
    return viewIntent
}

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
