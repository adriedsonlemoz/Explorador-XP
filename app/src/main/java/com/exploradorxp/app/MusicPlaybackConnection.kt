package com.exploradorxp.app

import android.content.ComponentName
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import java.io.File

/** Controller liberado com a composição; o Service mantém a sessão ao bloquear a tela. */
@Composable
internal fun rememberMusicController(enabled: Boolean): MediaController? {
    val appContext = LocalContext.current.applicationContext
    var controller by remember(appContext) { mutableStateOf<MediaController?>(null) }
    DisposableEffect(appContext, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val future = MediaController.Builder(
                appContext,
                SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java)),
            ).buildAsync()
            var disposed = false
            future.addListener({
                if (!disposed) controller = runCatching { future.get() }.getOrNull()
            }, ContextCompat.getMainExecutor(appContext))
            onDispose {
                disposed = true
                controller = null
                MediaController.releaseFuture(future)
            }
        }
    }
    return controller
}

/** Caminhos locais convertidos apenas para reabrir a interface do app. */
internal fun musicItemFile(item: MediaItem?): File? {
    val uri: Uri = item?.localConfiguration?.uri ?: return null
    if (uri.scheme != "file") return null
    return uri.path?.let(::File)?.takeIf { it.isFile && it.canRead() }
}

@Composable
internal fun MusicMiniPlayer(
    controller: MediaController,
    onOpen: (File) -> Unit,
    onClose: () -> Unit,
) {
    var item by remember(controller) { mutableStateOf(controller.currentMediaItem) }
    var playing by remember(controller) { mutableStateOf(controller.isPlaying) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { item = mediaItem }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                item = controller.currentMediaItem
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }
    if (controller.mediaItemCount == 0 || item == null) return
    val current = musicItemFile(item)
    val name = item?.mediaMetadata?.title?.toString()
        ?.takeIf(String::isNotBlank) ?: current?.let { AudioQueueRules.readableTitle(it.name) } ?: "Música"
    Row(
        Modifier.fillMaxWidth().background(Color(0xFFF6FAFF))
            .border(1.dp, Color(0xFFB8C7DA)).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = XpBlue, modifier = Modifier.size(23.dp))
        Column(
            Modifier.weight(1f).clickable(enabled = current != null) { current?.let(onOpen) }
                .padding(vertical = 7.dp, horizontal = 3.dp),
        ) {
            Text(name, color = Color(0xFF18283C), fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Toque para abrir o player", color = Color(0xFF36577F),
                fontSize = 10.sp, maxLines = 1)
        }
        IconButton(onClick = { controller.seekToPreviousMediaItem() },
            enabled = controller.hasPreviousMediaItem(), modifier = Modifier.size(38.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Faixa anterior", tint = XpBlue)
        }
        IconButton(onClick = { if (playing) controller.pause() else controller.play() },
            modifier = Modifier.size(38.dp)) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pausar" else "Reproduzir", tint = XpBlue)
        }
        IconButton(onClick = { controller.seekToNextMediaItem() },
            enabled = controller.hasNextMediaItem(), modifier = Modifier.size(38.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Próxima faixa", tint = XpBlue)
        }
        IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Encerrar reprodução", tint = Color(0xFFB42318))
        }
    }
}
