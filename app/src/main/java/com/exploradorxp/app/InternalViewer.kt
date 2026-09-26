package com.exploradorxp.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

internal val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp", "gif", "heic")
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
    if (ext in imageExtensions || ext in videoExtensions || ext in audioExtensions ||
        ext in textExtensions || ext in setOf("html", "htm", "pdf", "zip", "apk") ||
        file.name.lowercase() in textFileNames
    ) return true

    if (!FileContentDetector.needsContentDetection(file)) return false
    return FileContentDetector.detectExtension(file) != null
}

@Composable
fun InternalViewerScreen(
    file: File,
    externalMimeType: String? = null,
    forcedReadOnly: Boolean = false,
    externalOrigin: ExternalOpenOrigin? = null,
    folderImages: List<File> = emptyList(),
    onClose: () -> Unit,
    onOpenExternal: (File) -> Unit,
    onOpenFolder: (File) -> Unit,
    onShareFile: (File) -> Unit,
    onMoveToTrash: (File) -> Unit,
) {
    var activeFilePath by rememberSaveable(file.absolutePath) { mutableStateOf(file.absolutePath) }
    var returnToArchivePath by rememberSaveable(file.absolutePath) { mutableStateOf<String?>(null) }
    val activeFile = remember(activeFilePath) { File(activeFilePath) }
    val physicalExtension = activeFile.extension.lowercase()
    val extension = remember(activeFilePath, activeFile.length(), activeFile.lastModified()) {
        if (FileContentDetector.needsContentDetection(activeFile)) {
            FileContentDetector.detectExtension(activeFile) ?: physicalExtension
        } else {
            physicalExtension
        }
    }
    val typeDetectedFromContent = extension.isNotBlank() && extension != physicalExtension
    val isVideo = extension in videoExtensions
    val isExternalText = remember(externalMimeType) {
        ExternalOpenSupport.normalizeMime(externalMimeType).startsWith("text/")
    }
    val isTextDocument = extension in textExtensions || extension in setOf("html", "htm") ||
        activeFile.name.lowercase() in textFileNames || isExternalText
    val isArchivePreview = remember(activeFilePath, returnToArchivePath) {
        returnToArchivePath != null || activeFile.absolutePath.contains("/archive-preview/")
    }
    var contentFullScreen by rememberSaveable(file.absolutePath) { mutableStateOf(false) }
    var guardedCloseRequest by remember(activeFilePath) { mutableStateOf<(() -> Unit)?>(null) }
    var removedImagePaths by remember(file.absolutePath) { mutableStateOf<Set<String>>(emptySet()) }

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
            ViewerTitleBar(activeFile, extension, requestClose)
            ViewerToolbar(activeFile, extension, typeDetectedFromContent) { onOpenExternal(activeFile) }
            HorizontalDivider(color = Color(0xFFB8C7DA))
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                extension in imageExtensions -> ImageViewer(
                    file = activeFile,
                    folderImages = folderImages.filterNot { it.absolutePath in removedImagePaths },
                    fullScreen = contentFullScreen,
                    canModify = !forcedReadOnly && !isArchivePreview,
                    onFullScreenChange = { contentFullScreen = it },
                    onSelectFile = { selected -> activeFilePath = selected.absolutePath },
                    onOpenExternal = { onOpenExternal(activeFile) },
                    onShare = { onShareFile(activeFile) },
                    onMoveToTrash = { target ->
                        removedImagePaths = removedImagePaths + target.absolutePath
                        onMoveToTrash(target)
                    },
                    onClose = requestClose,
                )
                extension in videoExtensions -> VideoPlayerViewer(
                    file = activeFile,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                )
                extension in audioExtensions -> MediaViewer(activeFile)
                extension == "pdf" -> PdfViewer(activeFile)
                extension == "zip" -> ArchiveZipViewer(
                    file = activeFile,
                    onPreviewFile = { preview ->
                        returnToArchivePath = activeFile.absolutePath
                        activeFilePath = preview.absolutePath
                    },
                    onOpenExternal = onOpenExternal,
                    onOpenFolder = onOpenFolder,
                )
                extension == "apk" -> ApkViewer(activeFile) { onOpenExternal(activeFile) }
                isTextDocument -> TextCodeEditorViewer(
                    file = activeFile,
                    forcedReadOnly = isArchivePreview || forcedReadOnly,
                    externalOrigin = externalOrigin,
                    fullScreen = contentFullScreen,
                    onFullScreenChange = { contentFullScreen = it },
                    onClose = onClose,
                    onCloseHandlerChanged = { guardedCloseRequest = it },
                    onOpenExternal = { onOpenExternal(activeFile) },
                    onFileChanged = { activeFilePath = it.absolutePath },
                )
                else -> UnsupportedViewer { onOpenExternal(activeFile) }
            }
        }
        if (!contentFullScreen) ViewerStatusBar(activeFile, extension, typeDetectedFromContent)
    }
}

@Composable
private fun ViewerTitleBar(file: File, resolvedExtension: String, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF2F92F6), XpBlue, XpBlueDark)))
            .padding(horizontal = 7.dp)
    ) {
        CachedResourceIcon(
            resId = FileIconMapper.iconForResolvedExtension(file, resolvedExtension),
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
private fun ViewerToolbar(
    file: File,
    resolvedExtension: String,
    detectedFromContent: Boolean,
    onOpenExternal: () -> Unit,
) {
    val typeLabel = remember(file.absolutePath, resolvedExtension, detectedFromContent) {
        val base = if (resolvedExtension.isBlank()) {
            FileTypeClassifier.labelFor(file, false)
        } else {
            FileTypeClassifier.labelForExtension(resolvedExtension)
        }
        if (detectedFromContent) "$base • detectado pelo conteúdo" else base
    }
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
    fullScreen: Boolean,
    canModify: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onSelectFile: (File) -> Unit,
    onOpenExternal: () -> Unit,
    onShare: () -> Unit,
    onMoveToTrash: (File) -> Unit,
    onClose: () -> Unit,
) {
    val key = "${file.absolutePath}:${file.lastModified()}"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<Bitmap>>(ViewerLoadState.Loading) }
    var scale by remember(file.absolutePath) { mutableFloatStateOf(1f) }
    var offsetX by remember(file.absolutePath) { mutableFloatStateOf(0f) }
    var offsetY by remember(file.absolutePath) { mutableFloatStateOf(0f) }
    var quarterTurns by remember(file.absolutePath) { mutableIntStateOf(0) }
    var controlsVisible by remember(file.absolutePath) { mutableStateOf(true) }
    var showInfo by remember(file.absolutePath) { mutableStateOf(false) }
    var confirmTrash by remember(file.absolutePath) { mutableStateOf(false) }

    // A sequência continua limitada ao snapshot da pasta que estava aberta no Explorer.
    // Nada aqui percorre subpastas ou mistura imagens de outras localizações.
    val gallery = remember(folderImages, file.absolutePath) {
        val sameFolder = file.parentFile?.absolutePath
        val valid = folderImages
            .asSequence()
            .filter { it.parentFile?.absolutePath == sameFolder }
            .filter { it.extension.lowercase() in imageExtensions }
            .filter { it.exists() && it.isFile }
            .distinctBy { it.absolutePath }
            .toMutableList()
        if (file.exists() && valid.none { it.absolutePath == file.absolutePath }) valid.add(file)
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
                    val bitmap = decodeSampledBitmap(file, 2200, 2200)
                    if (bitmap != null) ViewerLoadState.Success(bitmap)
                    else ViewerLoadState.Error("O formato ou a codificação desta imagem não pôde ser exibido internamente.")
                }
            }
        }
    }

    LaunchedEffect(fullScreen, controlsVisible, file.absolutePath) {
        if (fullScreen && controlsVisible) {
            delay(2800)
            controlsVisible = false
        }
    }

    val bitmap = (loadState as? ViewerLoadState.Success)?.value
    DisposableEffect(bitmap) {
        onDispose {
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        }
    }

    fun resetTransform() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun previous() {
        if (canGoPrevious) onSelectFile(gallery[currentIndex - 1])
    }

    fun next() {
        if (canGoNext) onSelectFile(gallery[currentIndex + 1])
    }

    fun trashCurrent() {
        val replacement = ImageViewerLogic.replacementSourceIndex(currentIndex, gallery.size)
            ?.let(gallery::getOrNull)
        if (replacement != null) onSelectFile(replacement) else onClose()
        onMoveToTrash(file)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202020))
    ) {
        var dragDistance by remember(file.absolutePath) { mutableStateOf(0f) }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(if (fullScreen) 0.dp else 8.dp)
                // Pinça com dois dedos: zoom e deslocamento. Um dedo continua reservado
                // para a navegação horizontal entre as imagens da pasta.
                .pointerInput(file.absolutePath) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var gestureActive = true
                        while (gestureActive) {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val nextScale = ImageViewerLogic.clampScale(scale * zoomChange)
                                scale = nextScale
                                if (nextScale > 1.01f) {
                                    offsetX += panChange.x
                                    offsetY += panChange.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) change.consume()
                                }
                            }
                            gestureActive = event.changes.any { it.pressed }
                        }
                    }
                }
                .pointerInput(file.absolutePath, scale, fullScreen) {
                    detectTapGestures(
                        onTap = {
                            if (fullScreen) controlsVisible = !controlsVisible
                        },
                        onDoubleTap = {
                            val nextScale = ImageViewerLogic.doubleTapScale(scale)
                            scale = nextScale
                            if (nextScale <= 1.01f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                            controlsVisible = true
                        },
                    )
                }
                .pointerInput(file.absolutePath, gallery.size, scale) {
                    if (scale <= 1.01f) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragDistance = 0f },
                            onHorizontalDrag = { _, amount -> dragDistance += amount },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                            rotationZ = quarterTurns * 90f,
                        ),
                    contentScale = ContentScale.Fit,
                )
            }

            if (controlsVisible && loadState is ViewerLoadState.Success<*>) {
                Text(
                    text = "${(scale * 100).toInt()}%${if (quarterTurns != 0) "  •  ${quarterTurns * 90}°" else ""}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0x99000000), RoundedCornerShape(5.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        if (controlsVisible) {
            if (gallery.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF171717))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    ViewerActionButton("◀ Anterior", enabled = canGoPrevious, onClick = ::previous)
                    Text(
                        text = "${currentIndex + 1} / ${gallery.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    ViewerActionButton("Próxima ▶", enabled = canGoNext, onClick = ::next)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF171717))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                if (scale > 1.01f) ViewerActionButton("Ajustar") { resetTransform() }
                ViewerActionButton("↻ Girar") {
                    quarterTurns = (quarterTurns + 1) % 4
                    resetTransform()
                }
                ViewerActionButton("Info") { showInfo = true }
                ViewerActionButton("Compartilhar", onClick = onShare)
                ViewerActionButton(if (fullScreen) "Sair tela cheia" else "Tela cheia") {
                    controlsVisible = true
                    onFullScreenChange(!fullScreen)
                }
                ViewerActionButton("Lixeira", enabled = canModify) { confirmTrash = true }
            }
        }
    }

    if (showInfo) {
        ImageInfoDialog(file = file, onDismiss = { showInfo = false })
    }
    if (confirmTrash) {
        ImageTrashDialog(
            file = file,
            onDismiss = { confirmTrash = false },
            onConfirm = {
                confirmTrash = false
                trashCurrent()
            },
        )
    }
}

@Composable
private fun ImageInfoDialog(file: File, onDismiss: () -> Unit) {
    var details by remember(file.absolutePath, file.lastModified()) {
        mutableStateOf<ViewerLoadState<ImageDetails>>(ViewerLoadState.Loading)
    }
    LaunchedEffect(file.absolutePath, file.lastModified()) {
        details = ViewerLoadState.Loading
        details = withContext(Dispatchers.IO) {
            runCatching { readImageDetails(file) }.fold(
                onSuccess = { ViewerLoadState.Success(it) },
                onFailure = { ViewerLoadState.Error(it.message ?: "Não foi possível ler as informações da imagem.") },
            )
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F4EC), RoundedCornerShape(10.dp))
                .border(1.dp, XpChromeBorder, RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            Text("Informações da imagem", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
            Spacer(Modifier.height(8.dp))
            when (val state = details) {
                ViewerLoadState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = XpBlue)
                    Spacer(Modifier.width(10.dp))
                    Text("Lendo metadados...", fontSize = 12.sp, color = XpTextSecondary)
                }
                is ViewerLoadState.Error -> Text(state.message, fontSize = 12.sp, color = Color(0xFF7A1E1E))
                is ViewerLoadState.Success -> {
                    val d = state.value
                    LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                        item { ImageInfoSection("Arquivo") }
                        item { ImageInfoLine("Nome", file.name) }
                        item { ImageInfoLine("Pasta", file.parentFile?.absolutePath ?: "—") }
                        item { ImageInfoLine("Tipo", FileTypeClassifier.labelFor(file, false)) }
                        item { ImageInfoLine("Tamanho", formatViewerBytes(file.length())) }
                        item { ImageInfoLine("Resolução", if (d.width > 0 && d.height > 0) "${d.width} × ${d.height} px" else "Não informada") }
                        item { ImageInfoLine("Modificado", d.modified) }
                        if (d.hasExif) {
                            item { Spacer(Modifier.height(6.dp)); ImageInfoSection("EXIF") }
                            d.cameraMake?.let { item { ImageInfoLine("Fabricante", it) } }
                            d.cameraModel?.let { item { ImageInfoLine("Câmera", it) } }
                            d.dateTaken?.let { item { ImageInfoLine("Capturada em", it) } }
                            d.orientation?.let { item { ImageInfoLine("Orientação", it) } }
                            d.iso?.let { item { ImageInfoLine("ISO", it) } }
                            d.exposure?.let { item { ImageInfoLine("Exposição", it) } }
                            d.aperture?.let { item { ImageInfoLine("Abertura", it) } }
                            d.focalLength?.let { item { ImageInfoLine("Distância focal", it) } }
                            d.software?.let { item { ImageInfoLine("Software", it) } }
                        } else {
                            item {
                                Text(
                                    "Nenhum metadado EXIF legível foi encontrado.",
                                    fontSize = 11.sp,
                                    color = XpTextSecondary,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                ViewerActionButton("Fechar", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ImageTrashDialog(file: File, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F4EC), RoundedCornerShape(10.dp))
                .border(1.dp, XpChromeBorder, RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            Text("Mover para a Lixeira?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
            Spacer(Modifier.height(8.dp))
            Text(
                "${file.name}\n\nO arquivo poderá ser restaurado pela Lixeira do Explorador XP.",
                fontSize = 12.sp,
                color = Color(0xFF303030),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { ViewerActionButton("Cancelar", onClick = onDismiss) }
                Box(Modifier.weight(1f)) { ViewerActionButton("Mover", onClick = onConfirm) }
            }
        }
    }
}

@Composable
private fun ImageInfoSection(title: String) {
    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = XpBlueDark, modifier = Modifier.padding(vertical = 3.dp))
}

@Composable
private fun ImageInfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF555555), modifier = Modifier.width(105.dp))
        Text(value, fontSize = 11.sp, color = Color(0xFF202020), modifier = Modifier.weight(1f))
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
    var permissionsExpanded by rememberSaveable(file.absolutePath) { mutableStateOf(false) }
    var technicalExpanded by rememberSaveable(file.absolutePath) { mutableStateOf(false) }
    val key = "${file.absolutePath}:${file.lastModified()}:$resumeRevision"
    var loadState by remember(key) { mutableStateOf<ViewerLoadState<ApkInfo>>(ViewerLoadState.Loading) }

    val installerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        resumeRevision++
    }

    val appDetailsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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

    fun openUnknownSourcesSettings(continueInstall: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (continueInstall) launchInstallerNow()
            return
        }
        pendingInstallAfterPermission = continueInstall
        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )
        runCatching { unknownSourcesLauncher.launch(settingsIntent) }.onFailure {
            pendingInstallAfterPermission = false
            Toast.makeText(context, "Não foi possível abrir a permissão de instalação.", Toast.LENGTH_SHORT).show()
        }
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
            Toast.makeText(
                context,
                "Ative 'Permitir desta fonte' e use Voltar. A instalação continuará sozinha.",
                Toast.LENGTH_LONG,
            ).show()
            openUnknownSourcesSettings(continueInstall = true)
        } else {
            pendingInstallAfterPermission = false
            launchInstallerNow()
        }
    }

    LaunchedEffect(key) {
        loadState = withContext(Dispatchers.IO) {
            runCatching { inspectApk(context, file) }.fold(
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
                Text("Analisando APK, assinatura e compatibilidade...", color = XpTextSecondary, fontSize = 12.sp)
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
                val relationLabel = when (info.versionRelation) {
                    ApkVersionRelation.NOT_INSTALLED -> "Novo aplicativo"
                    ApkVersionRelation.UPGRADE -> "Atualização disponível"
                    ApkVersionRelation.SAME -> "Mesma versão instalada"
                    ApkVersionRelation.DOWNGRADE -> "Versão anterior ao app instalado"
                }
                val installLabel = when (info.versionRelation) {
                    ApkVersionRelation.NOT_INSTALLED -> "Instalar"
                    ApkVersionRelation.UPGRADE -> "Atualizar"
                    ApkVersionRelation.SAME -> "Reinstalar"
                    ApkVersionRelation.DOWNGRADE -> "Instalar versão antiga"
                }
                val sourceAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                    context.packageManager.canRequestPackageInstalls()
                val signatureText = when (info.signatureRelation) {
                    ApkSignatureRelation.NOT_APPLICABLE -> "APK assinado"
                    ApkSignatureRelation.MATCH -> "Compatível com o app instalado"
                    ApkSignatureRelation.MISMATCH -> "Assinatura diferente do app instalado"
                    ApkSignatureRelation.UNKNOWN -> "Não foi possível comparar"
                }
                val blockedReason = when {
                    !info.androidCompatible -> "Este APK exige Android API ${info.minSdk}; o aparelho usa API ${Build.VERSION.SDK_INT}."
                    !info.abiCompatible -> "O APK não possui biblioteca nativa compatível com ${info.deviceAbis.joinToString(", ")}."
                    info.signatureRelation == ApkSignatureRelation.MISMATCH -> "O Android não permite atualizar um pacote instalado com outra assinatura. Abra Gerenciar app e remova a versão atual antes de instalar esta, se for realmente desejado."
                    info.versionRelation == ApkVersionRelation.DOWNGRADE -> "O versionCode deste APK é menor que o instalado. O Android normalmente bloqueia downgrade direto; use Gerenciar app para remover a versão atual antes de instalar esta versão antiga."
                    else -> null
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
                                    when {
                                        blockedReason != null -> Color(0xFFFFEEE6)
                                        info.installedVersion != null -> Color(0xFFE1F3E4)
                                        else -> Color(0xFFE7F0FB)
                                    },
                                    RoundedCornerShape(6.dp),
                                )
                                .border(
                                    1.dp,
                                    when {
                                        blockedReason != null -> Color(0xFFD89A78)
                                        info.installedVersion != null -> Color(0xFF82AF87)
                                        else -> Color(0xFFABC0D9)
                                    },
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                if (blockedReason != null) "Instalação bloqueada" else relationLabel,
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
                    Text("Versões", color = Color(0xFF183363), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    ApkInfoLine("APK", "${info.versionName}  (${info.versionCode})")
                    if (info.installedVersion != null && info.installedVersionCode != null) {
                        HorizontalDivider(color = Color(0xFFE3E9F0))
                        ApkInfoLine("Instalada", "${info.installedVersion}  (${info.installedVersionCode})")
                    }
                    HorizontalDivider(color = Color(0xFFE3E9F0))
                    ApkInfoLine("Situação", relationLabel)
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F9FC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFC5D4E6), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text("Compatibilidade e segurança", color = Color(0xFF183363), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    ApkStatusLine(
                        label = "Android",
                        value = if (info.androidCompatible) "Compatível • aparelho API ${Build.VERSION.SDK_INT}" else "Incompatível • requer API ${info.minSdk}",
                        ok = info.androidCompatible,
                    )
                    ApkStatusLine(
                        label = "Processador",
                        value = when {
                            !info.hasNativeLibraries -> "Compatível • sem bibliotecas nativas"
                            info.abiCompatible -> "Compatível • ${formatAbiList(info.apkAbis)}"
                            else -> "Incompatível • ${formatAbiList(info.apkAbis)}"
                        },
                        ok = info.abiCompatible,
                    )
                    ApkStatusLine(
                        label = "Assinatura",
                        value = signatureText,
                        ok = info.signatureRelation != ApkSignatureRelation.MISMATCH,
                        neutral = info.signatureRelation == ApkSignatureRelation.UNKNOWN || info.signatureRelation == ApkSignatureRelation.NOT_APPLICABLE,
                    )
                    ApkStatusLine(
                        label = "Fonte",
                        value = if (sourceAllowed) "Permitida para o Explorador XP" else "Autorização necessária",
                        ok = sourceAllowed,
                        neutral = !sourceAllowed,
                    )
                }

                if (blockedReason != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF2E9), RoundedCornerShape(9.dp))
                            .border(1.dp, Color(0xFFE4A47F), RoundedCornerShape(9.dp))
                            .padding(10.dp),
                    ) {
                        CachedResourceIcon(R.drawable.warning, "Aviso", modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(8.dp))
                        Text(blockedReason, fontSize = 11.sp, color = Color(0xFF6B3A22), lineHeight = 15.sp, modifier = Modifier.weight(1f))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { technicalExpanded = !technicalExpanded }.padding(vertical = 2.dp),
                    ) {
                        Text("Detalhes técnicos", color = Color(0xFF183363), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (technicalExpanded) "▲" else "▼", fontSize = 11.sp, color = XpBlueDark)
                    }
                    if (technicalExpanded) {
                        Spacer(Modifier.height(6.dp))
                        ApkInfoLine("Pacote", info.packageName)
                        HorizontalDivider(color = Color(0xFFE3E9F0))
                        ApkInfoLine("Android", "mín. API ${info.minSdk ?: "-"}  •  alvo API ${info.targetSdk ?: "-"}")
                        if (info.installedTargetSdk != null) {
                            HorizontalDivider(color = Color(0xFFE3E9F0))
                            ApkInfoLine("Alvo instalado", "API ${info.installedTargetSdk}")
                        }
                        HorizontalDivider(color = Color(0xFFE3E9F0))
                        ApkInfoLine("Arquiteturas", formatAbiList(info.apkAbis))
                        HorizontalDivider(color = Color(0xFFE3E9F0))
                        ApkInfoLine("Aparelho", info.deviceAbis.joinToString(", ").ifBlank { "Não informado" })
                        HorizontalDivider(color = Color(0xFFE3E9F0))
                        ApkInfoLine("Tamanho", formatViewerBytes(file.length()))
                        if (info.signerDigests.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFE3E9F0))
                            ApkInfoLine("SHA-256 APK", info.signerDigests.joinToString("\n"))
                        }
                        if (!info.installedSignerDigests.isNullOrEmpty()) {
                            HorizontalDivider(color = Color(0xFFE3E9F0))
                            ApkInfoLine("SHA-256 atual", info.installedSignerDigests.joinToString("\n"))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFC5D4E6), RoundedCornerShape(10.dp))
                        .clickable { permissionsExpanded = !permissionsExpanded }
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Permissões solicitadas (${info.requestedPermissions.size})",
                                color = Color(0xFF183363),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (info.dangerousPermissionCount > 0) {
                                Text(
                                    "${info.dangerousPermissionCount} marcadas pelo Android como sensíveis",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF8B4B24),
                                )
                            }
                        }
                        Text(if (permissionsExpanded) "▲" else "▼", fontSize = 11.sp, color = XpBlueDark)
                    }
                    if (permissionsExpanded) {
                        Spacer(Modifier.height(7.dp))
                        if (info.requestedPermissions.isEmpty()) {
                            Text("Nenhuma permissão declarada no manifesto do APK.", fontSize = 11.sp, color = XpTextSecondary)
                        } else {
                            info.requestedPermissions.forEachIndexed { index, permission ->
                                if (index > 0) HorizontalDivider(color = Color(0xFFE9EDF2))
                                ApkPermissionLine(permission)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F9FC), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFC5D4E6), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Text("Ações", color = Color(0xFF183363), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (info.installedVersion != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            ApkPrimaryButton(
                                label = installLabel,
                                icon = R.drawable.file_apk,
                                primary = true,
                                enabled = info.canAttemptInstall,
                                modifier = Modifier.weight(1f),
                                onClick = ::requestInstall,
                            )
                            ApkPrimaryButton(
                                label = "Abrir",
                                icon = R.drawable.visible,
                                primary = false,
                                enabled = info.canLaunchInstalled,
                                modifier = Modifier.weight(1f),
                            ) {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(info.packageName)
                                if (launchIntent != null) context.startActivity(launchIntent)
                                else Toast.makeText(context, "O aplicativo não possui tela inicial para abrir.", Toast.LENGTH_SHORT).show()
                            }
                            ApkPrimaryButton(
                                label = "Gerenciar",
                                icon = R.drawable.settings,
                                primary = false,
                                modifier = Modifier.weight(1f),
                            ) {
                                val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${info.packageName}"))
                                runCatching { appDetailsLauncher.launch(details) }.onFailure {
                                    Toast.makeText(context, "Não foi possível abrir as informações do aplicativo.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        ApkPrimaryButton(
                            label = installLabel,
                            icon = R.drawable.file_apk,
                            primary = true,
                            enabled = info.canAttemptInstall,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = ::requestInstall,
                        )
                    }
                    if (!sourceAllowed && info.canAttemptInstall) {
                        Spacer(Modifier.height(7.dp))
                        ApkPrimaryButton(
                            label = "Permitir instalação nesta fonte",
                            icon = R.drawable.unlocked,
                            primary = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) { openUnknownSourcesSettings(continueInstall = false) }
                    }
                    if (!sourceAllowed && info.canAttemptInstall) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ao tocar em instalar, o Explorador XP abre a autorização do Android. Depois de ativar a opção e usar Voltar, a instalação continua automaticamente.",
                            fontSize = 10.5.sp,
                            color = XpTextSecondary,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkStatusLine(
    label: String,
    value: String,
    ok: Boolean,
    neutral: Boolean = false,
) {
    val icon = when {
        neutral -> R.drawable.info
        ok -> R.drawable.check
        else -> R.drawable.warning
    }
    val valueColor = when {
        neutral -> Color(0xFF53657B)
        ok -> Color(0xFF356B3D)
        else -> Color(0xFF9A4D28)
    }
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        CachedResourceIcon(icon, null, modifier = Modifier.size(17.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(7.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF53657B), modifier = Modifier.width(86.dp))
        Text(value, fontSize = 11.sp, color = valueColor, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ApkPermissionLine(permission: ApkPermission) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        CachedResourceIcon(
            if (permission.dangerous) R.drawable.warning else R.drawable.check,
            null,
            modifier = Modifier.size(16.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(permission.label, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF202B38))
            Text(
                permission.name,
                fontSize = 9.5.sp,
                color = XpTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (permission.dangerous) {
            Text("sensível", fontSize = 9.5.sp, color = Color(0xFF9A4D28), fontWeight = FontWeight.SemiBold)
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
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background = when {
        !enabled -> Color(0xFFE5E8EC)
        primary && pressed -> Color(0xFF0A54A9)
        primary -> Color(0xFF1476DF)
        pressed -> Color(0xFFDCE8F5)
        else -> Color.White
    }
    val foreground = when {
        !enabled -> Color(0xFF8A929C)
        primary -> Color.White
        else -> Color(0xFF173A67)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .border(
                1.dp,
                if (!enabled) Color(0xFFBFC5CC) else if (primary) Color(0xFF0D5DB6) else Color(0xFF9DB4CF),
                RoundedCornerShape(7.dp),
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
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
private fun ViewerStatusBar(file: File, resolvedExtension: String, detectedFromContent: Boolean) {
    val extension = remember(file.absolutePath, resolvedExtension, detectedFromContent) {
        val base = if (resolvedExtension.isBlank()) {
            FileTypeClassifier.labelFor(file, false)
        } else {
            FileTypeClassifier.labelForExtension(resolvedExtension)
        }
        if (detectedFromContent) "$base (detectado)" else base
    }
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

private data class ImageDetails(
    val width: Int,
    val height: Int,
    val modified: String,
    val cameraMake: String?,
    val cameraModel: String?,
    val dateTaken: String?,
    val orientation: String?,
    val iso: String?,
    val exposure: String?,
    val aperture: String?,
    val focalLength: String?,
    val software: String?,
) {
    val hasExif: Boolean
        get() = listOf(cameraMake, cameraModel, dateTaken, orientation, iso, exposure, aperture, focalLength, software)
            .any { !it.isNullOrBlank() }
}

@Suppress("DEPRECATION")
private fun readImageDetails(file: File): ImageDetails {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
    fun attr(tag: String): String? = exif?.getAttribute(tag)?.trim()?.takeIf { it.isNotEmpty() }
    val orientation = attr(ExifInterface.TAG_ORIENTATION)?.toIntOrNull()?.let(::imageOrientationLabel)
    val modified = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(file.lastModified()))
    return ImageDetails(
        width = bounds.outWidth.coerceAtLeast(0),
        height = bounds.outHeight.coerceAtLeast(0),
        modified = modified,
        cameraMake = attr(ExifInterface.TAG_MAKE),
        cameraModel = attr(ExifInterface.TAG_MODEL),
        dateTaken = attr(ExifInterface.TAG_DATETIME_ORIGINAL) ?: attr(ExifInterface.TAG_DATETIME),
        orientation = orientation,
        iso = attr(ExifInterface.TAG_ISO_SPEED_RATINGS),
        exposure = attr(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${formatExifNumber(it)} s" },
        aperture = attr(ExifInterface.TAG_F_NUMBER)?.let { "f/${formatExifNumber(it)}" },
        focalLength = attr(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${formatExifNumber(it)} mm" },
        software = attr(ExifInterface.TAG_SOFTWARE),
    )
}

private fun formatExifNumber(raw: String): String {
    val parts = raw.split('/', limit = 2)
    if (parts.size != 2) return raw
    val numerator = parts[0].toDoubleOrNull() ?: return raw
    val denominator = parts[1].toDoubleOrNull()?.takeIf { it != 0.0 } ?: return raw
    val value = numerator / denominator
    return if (value >= 1.0) String.format(java.util.Locale.getDefault(), "%.1f", value).trimEnd('0').trimEnd('.')
    else String.format(java.util.Locale.getDefault(), "%.4f", value).trimEnd('0').trimEnd('.')
}

private fun imageOrientationLabel(value: Int): String = when (value) {
    ExifInterface.ORIENTATION_NORMAL -> "Normal"
    ExifInterface.ORIENTATION_ROTATE_90 -> "90°"
    ExifInterface.ORIENTATION_ROTATE_180 -> "180°"
    ExifInterface.ORIENTATION_ROTATE_270 -> "270°"
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Espelhada horizontalmente"
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Espelhada verticalmente"
    ExifInterface.ORIENTATION_TRANSPOSE -> "Transposta"
    ExifInterface.ORIENTATION_TRANSVERSE -> "Transversa"
    else -> "Não informada"
}

private data class PdfPageData(val bitmap: Bitmap, val pageCount: Int)
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
