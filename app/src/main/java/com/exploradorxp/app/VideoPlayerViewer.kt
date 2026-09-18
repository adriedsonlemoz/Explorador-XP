package com.exploradorxp.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.abs

private const val VIDEO_POSITION_PREFS = "explorador_xp_video_positions"
private const val VIDEO_POSITION_SAVE_INTERVAL_MS = 5_000L
private const val VIDEO_CONTROL_HIDE_DELAY_MS = 3_200L
private const val VIDEO_SEEK_STEP_MS = 10_000L

private val videoSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerViewer(
    file: File,
    fullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onOpenExternal: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    SystemBarsForVideoFullscreen(fullScreen)

    val initialProblem = remember(file.absolutePath, file.length(), file.lastModified()) {
        when {
            !file.exists() -> "O arquivo de vídeo não existe mais neste local."
            !file.canRead() -> "O Explorador XP não tem acesso de leitura a este vídeo."
            file.length() <= 0L -> "Este vídeo está vazio ou parece estar corrompido."
            else -> null
        }
    }

    if (initialProblem != null) {
        VideoFailurePanel(message = initialProblem, onOpenExternal = onOpenExternal)
        return
    }

    val positionKey = remember(file.absolutePath, file.length(), file.lastModified()) {
        "${file.absolutePath}|${file.length()}|${file.lastModified()}"
    }
    val storedPosition = remember(positionKey) { loadVideoPosition(context, positionKey) }
    var requestedPlayWhenReady by rememberSaveable(file.absolutePath) { mutableStateOf(true) }
    var playbackSpeed by rememberSaveable(file.absolutePath) { mutableStateOf(1f) }
    var fillMode by rememberSaveable(file.absolutePath) { mutableStateOf(false) }

    val player = remember(file.absolutePath, file.length(), file.lastModified()) {
        ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                if (storedPosition > 0L) seekTo(storedPosition)
                setPlaybackSpeed(playbackSpeed)
                prepare()
                playWhenReady = requestedPlayWhenReady
            }
    }

    var controlsVisible by remember(file.absolutePath) { mutableStateOf(true) }
    var isPlaying by remember(file.absolutePath) { mutableStateOf(false) }
    var playbackState by remember(file.absolutePath) { mutableIntStateOf(Player.STATE_IDLE) }
    var currentPosition by remember(file.absolutePath) { mutableLongStateOf(storedPosition) }
    var duration by remember(file.absolutePath) { mutableLongStateOf(0L) }
    var infoVisible by remember(file.absolutePath) { mutableStateOf(false) }
    var speedMenuVisible by remember(file.absolutePath) { mutableStateOf(false) }
    var errorMessage by remember(file.absolutePath) { mutableStateOf<String?>(null) }
    var videoWidth by remember(file.absolutePath) { mutableIntStateOf(0) }
    var videoHeight by remember(file.absolutePath) { mutableIntStateOf(0) }
    var draggingProgress by remember(file.absolutePath) { mutableStateOf(false) }
    var draggedPosition by remember(file.absolutePath) { mutableFloatStateOf(storedPosition.toFloat()) }
    var lastSavedPosition by remember(file.absolutePath) { mutableLongStateOf(storedPosition) }

    DisposableEffect(player, positionKey, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
                if (!value) controlsVisible = true
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                val playerDuration = player.duration
                if (playerDuration != C.TIME_UNSET && playerDuration > 0L) duration = playerDuration
                if (state == Player.STATE_ENDED) {
                    controlsVisible = true
                    requestedPlayWhenReady = false
                    clearVideoPosition(context, positionKey)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = friendlyVideoError(error)
                controlsVisible = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            if (event == Lifecycle.Event.ON_STOP) {
                saveVideoPosition(context, positionKey, player.currentPosition, player.duration)
                player.pause()
            }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            saveVideoPosition(context, positionKey, player.currentPosition, player.duration)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, positionKey) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            val playerDuration = player.duration
            if (playerDuration != C.TIME_UNSET && playerDuration > 0L) duration = playerDuration
            if (!draggingProgress) draggedPosition = currentPosition.toFloat()

            if (player.isPlaying && abs(currentPosition - lastSavedPosition) >= VIDEO_POSITION_SAVE_INTERVAL_MS) {
                saveVideoPosition(context, positionKey, currentPosition, playerDuration)
                lastSavedPosition = currentPosition
            }
            delay(250L)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, infoVisible, speedMenuVisible, errorMessage) {
        if (controlsVisible && isPlaying && !infoVisible && !speedMenuVisible && errorMessage == null) {
            delay(VIDEO_CONTROL_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    val isBuffering = playbackState == Player.STATE_BUFFERING
    val typeLabel = remember(file.absolutePath) { FileTypeClassifier.labelFor(file, false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val compactLandscape = maxHeight < 360.dp

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext: Context ->
                PlayerView(viewContext).apply {
                    useController = false
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.player = player
                }
            },
            update = { playerView: PlayerView ->
                playerView.player = player
                playerView.resizeMode = if (fillMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
        )

        val tapSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = tapSource,
                    indication = null,
                    onClick = {
                        controlsVisible = !controlsVisible
                        if (!controlsVisible) {
                            infoVisible = false
                            speedMenuVisible = false
                        }
                    },
                )
        )

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center).size(38.dp),
            )
        }

        if (controlsVisible && errorMessage == null) {
            VideoControls(
                modifier = Modifier.fillMaxSize(),
                compactLandscape = compactLandscape,
                file = file,
                typeLabel = typeLabel,
                player = player,
                isPlaying = isPlaying,
                playbackState = playbackState,
                currentPosition = currentPosition,
                duration = duration,
                draggingProgress = draggingProgress,
                draggedPosition = draggedPosition,
                onDraggingProgressChange = { draggingProgress = it },
                onDraggedPositionChange = { draggedPosition = it },
                playbackSpeed = playbackSpeed,
                onRequestedPlayChange = { requestedPlayWhenReady = it },
                onPlaybackSpeedChange = { speed ->
                    playbackSpeed = speed
                    player.setPlaybackSpeed(speed)
                    speedMenuVisible = false
                    controlsVisible = true
                },
                speedMenuVisible = speedMenuVisible,
                onSpeedMenuVisibleChange = { speedMenuVisible = it },
                fillMode = fillMode,
                onFillModeChange = { fillMode = it },
                fullScreen = fullScreen,
                onFullScreenChange = onFullScreenChange,
                infoVisible = infoVisible,
                onInfoVisibleChange = { infoVisible = it },
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                onOpenExternal = {
                    requestedPlayWhenReady = false
                    player.pause()
                    saveVideoPosition(context, positionKey, player.currentPosition, player.duration)
                    onOpenExternal()
                },
            )
        }

        if (errorMessage != null) {
            VideoFailurePanel(
                message = errorMessage.orEmpty(),
                onOpenExternal = onOpenExternal,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun VideoControls(
    modifier: Modifier,
    compactLandscape: Boolean,
    file: File,
    typeLabel: String,
    player: ExoPlayer,
    isPlaying: Boolean,
    playbackState: Int,
    currentPosition: Long,
    duration: Long,
    draggingProgress: Boolean,
    draggedPosition: Float,
    onDraggingProgressChange: (Boolean) -> Unit,
    onDraggedPositionChange: (Float) -> Unit,
    playbackSpeed: Float,
    onRequestedPlayChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    speedMenuVisible: Boolean,
    onSpeedMenuVisibleChange: (Boolean) -> Unit,
    fillMode: Boolean,
    onFillModeChange: (Boolean) -> Unit,
    fullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    infoVisible: Boolean,
    onInfoVisibleChange: (Boolean) -> Unit,
    videoWidth: Int,
    videoHeight: Int,
    onOpenExternal: () -> Unit,
) {
    val overlay = Color(0xC9000000)

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(overlay)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = if (compactLandscape) 4.dp else 7.dp),
        ) {
            VideoXpButton(
                label = "Info",
                icon = R.drawable.info,
                selected = infoVisible,
                onClick = { onInfoVisibleChange(!infoVisible) },
            )
            Spacer(Modifier.width(6.dp))
            VideoXpButton(
                label = if (fillMode) "Preencher" else "Ajustar",
                onClick = { onFillModeChange(!fillMode) },
            )
            Spacer(Modifier.width(6.dp))
            Box {
                VideoXpButton(
                    label = formatSpeed(playbackSpeed),
                    selected = speedMenuVisible,
                    onClick = { onSpeedMenuVisibleChange(!speedMenuVisible) },
                )
                DropdownMenu(
                    expanded = speedMenuVisible,
                    onDismissRequest = { onSpeedMenuVisibleChange(false) },
                    modifier = Modifier
                        .background(Color(0xFFF8F8F2))
                        .border(1.dp, XpControlBorder)
                        .widthIn(min = 126.dp),
                ) {
                    videoSpeeds.forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = formatSpeed(speed),
                                    fontSize = 13.sp,
                                    fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal,
                                    color = Color(0xFF202020),
                                )
                            },
                            onClick = { onPlaybackSpeedChange(speed) },
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            VideoXpButton(
                label = if (fullScreen) "Sair da tela cheia" else "Tela cheia",
                onClick = { onFullScreenChange(!fullScreen) },
            )
            Spacer(Modifier.width(6.dp))
            VideoXpButton(
                label = "Abrir fora",
                icon = R.drawable.drive_external,
                onClick = onOpenExternal,
            )
        }

        if (infoVisible) {
            VideoInfoPanel(
                file = file,
                typeLabel = typeLabel,
                duration = duration,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = if (compactLandscape) 48.dp else 58.dp, start = 10.dp, end = 10.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compactLandscape) 7.dp else 11.dp),
            modifier = Modifier.align(Alignment.Center),
        ) {
            VideoRoundButton(
                label = "−10",
                icon = R.drawable.back,
                onClick = {
                    val target = (player.currentPosition - VIDEO_SEEK_STEP_MS).coerceAtLeast(0L)
                    player.seekTo(target)
                },
            )
            VideoRoundButton(
                label = if (isPlaying) "Pausar" else "Reproduzir",
                symbol = if (isPlaying) "Ⅱ" else "▶",
                emphasized = true,
                onClick = {
                    if (isPlaying) {
                        onRequestedPlayChange(false)
                        player.pause()
                    } else {
                        if (playbackState == Player.STATE_ENDED) player.seekTo(0L)
                        onRequestedPlayChange(true)
                        player.play()
                    }
                },
            )
            VideoRoundButton(
                label = "+10",
                icon = R.drawable.forward,
                onClick = {
                    val max = if (duration > 0L) duration else Long.MAX_VALUE
                    player.seekTo((player.currentPosition + VIDEO_SEEK_STEP_MS).coerceAtMost(max))
                },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(overlay)
                .padding(horizontal = 10.dp, vertical = if (compactLandscape) 5.dp else 8.dp),
        ) {
            val sliderMax = if (duration > 0L) {
                duration.toFloat().coerceAtLeast(1f)
            } else {
                maxOf(currentPosition.toFloat(), draggedPosition, 1f)
            }
            Slider(
                value = (if (draggingProgress) draggedPosition else currentPosition.toFloat()).coerceIn(0f, sliderMax),
                onValueChange = { value: Float ->
                    onDraggingProgressChange(true)
                    onDraggedPositionChange(value)
                },
                onValueChangeFinished = {
                    player.seekTo(draggedPosition.toLong())
                    onDraggingProgressChange(false)
                },
                valueRange = 0f..sliderMax,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF3C8CE7),
                    inactiveTrackColor = Color(0xFF606060),
                ),
                modifier = Modifier.fillMaxWidth().height(if (compactLandscape) 22.dp else 30.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${formatVideoTime(if (draggingProgress) draggedPosition.toLong() else currentPosition)} / ${formatVideoTime(duration)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                VideoXpButton(
                    label = "Reiniciar",
                    icon = R.drawable.refresh,
                    compact = true,
                    onClick = {
                        player.seekTo(0L)
                        onRequestedPlayChange(true)
                        player.play()
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoInfoPanel(
    file: File,
    typeLabel: String,
    duration: Long,
    videoWidth: Int,
    videoHeight: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 430.dp)
            .background(Color(0xEEF8F8F2))
            .border(1.dp, XpControlBorder)
            .padding(10.dp),
    ) {
        Text(
            text = file.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF202020),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Text("$typeLabel • ${formatVideoBytes(file.length())}", fontSize = 12.sp, color = Color(0xFF333333))
        if (videoWidth > 0 && videoHeight > 0) {
            Text("Resolução: ${videoWidth} × ${videoHeight}", fontSize = 12.sp, color = Color(0xFF333333))
        }
        if (duration > 0L) {
            Text("Duração: ${formatVideoTime(duration)}", fontSize = 12.sp, color = Color(0xFF333333))
        }
        Text(
            text = file.absolutePath,
            fontSize = 11.sp,
            color = Color(0xFF5B5B5B),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VideoFailurePanel(
    message: String,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color.Black),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .widthIn(max = 460.dp)
                .padding(20.dp)
                .background(Color(0xEE1B1B1B))
                .border(1.dp, Color(0xFF777777))
                .padding(18.dp),
        ) {
            Text(
                text = "Não foi possível reproduzir este vídeo",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = Color(0xFFE7E7E7),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            VideoXpButton(
                label = "Abrir com outro aplicativo",
                icon = R.drawable.drive_external,
                onClick = onOpenExternal,
            )
        }
    }
}

@Composable
private fun VideoXpButton(
    label: String,
    icon: Int? = null,
    selected: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(if (compact) 27.dp else 30.dp)
            .background(if (selected) Color(0xFFD6E8FA) else Color(0xFFF8F8F2))
            .border(1.dp, if (selected) Color(0xFF3C74B5) else Color(0xFF8A8A8A))
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 7.dp else 9.dp),
    ) {
        if (icon != null) {
            CachedResourceIcon(
                resId = icon,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            color = Color(0xFF202020),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun VideoRoundButton(
    label: String,
    icon: Int? = null,
    symbol: String? = null,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (emphasized) 56.dp else 46.dp)
                .background(if (emphasized) Color(0xEAF8F8F2) else Color(0xDDF0F0EA))
                .border(1.dp, Color(0xFF808080))
                .clickable(onClick = onClick),
        ) {
            when {
                symbol != null -> Text(symbol, color = Color(0xFF101010), fontSize = if (emphasized) 25.sp else 18.sp, fontWeight = FontWeight.Bold)
                icon != null -> CachedResourceIcon(
                    resId = icon,
                    contentDescription = label,
                    modifier = Modifier.size(25.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SystemBarsForVideoFullscreen(fullScreen: Boolean) {
    val view = LocalView.current
    DisposableEffect(fullScreen, view) {
        val activity = view.context.findActivity()
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (fullScreen) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (fullScreen && window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private fun friendlyVideoError(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
        "O formato do arquivo foi reconhecido, mas o codec de vídeo ou áudio não é compatível com este aparelho. Tente abrir com outro aplicativo."

    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ->
        "Este contêiner de vídeo não é suportado pelo player interno. Você pode abri-lo com outro aplicativo."

    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_DECODING_FAILED ->
        "O arquivo parece estar corrompido ou usa uma codificação que o aparelho não conseguiu decodificar."

    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "O arquivo foi movido, renomeado ou apagado antes de o player conseguir abri-lo."

    PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
        "O Android não permitiu que o player lesse este arquivo. Verifique o acesso ao armazenamento."

    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "O player não conseguiu acessar a origem do vídeo."

    else -> "O player interno encontrou um problema ao abrir este vídeo. Tente novamente ou use outro aplicativo."
}

private fun loadVideoPosition(context: Context, key: String): Long =
    context.getSharedPreferences(VIDEO_POSITION_PREFS, Context.MODE_PRIVATE).getLong(key, 0L)

private fun saveVideoPosition(context: Context, key: String, position: Long, duration: Long) {
    val prefs = context.getSharedPreferences(VIDEO_POSITION_PREFS, Context.MODE_PRIVATE)
    val knownDuration = duration.takeIf { it != C.TIME_UNSET && it > 0L }
    val shouldForget = position < 1_000L || (knownDuration != null && position >= knownDuration - 2_000L)
    if (shouldForget) prefs.edit().remove(key).apply()
    else prefs.edit().putLong(key, position.coerceAtLeast(0L)).apply()
}

private fun clearVideoPosition(context: Context, key: String) {
    context.getSharedPreferences(VIDEO_POSITION_PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
}

private fun formatVideoTime(ms: Long): String {
    if (ms <= 0L || ms == C.TIME_UNSET) return "00:00"
    val totalSeconds = ms / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatVideoBytes(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val kb = bytes / 1_024.0
    if (kb < 1_024.0) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1_024.0
    if (mb < 1_024.0) return String.format(Locale.getDefault(), "%.1f MB", mb)
    return String.format(Locale.getDefault(), "%.2f GB", mb / 1_024.0)
}

private fun formatSpeed(speed: Float): String = when (speed) {
    0.5f -> "0.5x"
    0.75f -> "0.75x"
    1f -> "1x"
    1.25f -> "1.25x"
    1.5f -> "1.5x"
    2f -> "2x"
    else -> "${speed}x"
}
