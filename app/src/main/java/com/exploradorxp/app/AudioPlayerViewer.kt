package com.exploradorxp.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val audioAccent = Color(0xFF075FCF)
private val audioDark = Color(0xFF18283C)
private val audioMuted = Color(0xFF5C6776)

private data class AudioTags(
    val title: String,
    val artist: String?,
    val album: String?,
    val artwork: Bitmap?,
)

private fun extractAudioTags(file: File): AudioTags {
    val fallback = AudioQueueRules.readableTitle(file.name)
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            ?.trim()?.takeIf { it.isNotEmpty() }
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            ?.trim()?.takeIf { it.isNotEmpty() }
        val bytes = runCatching { retriever.embeddedPicture }.getOrNull()
        // Nunca decodificar arte embutida arbitrariamente grande.
        val art = bytes?.takeIf { it.size in 1..3_000_000 }?.let { payload ->
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(payload, 0, payload.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
                else {
                    val decode = BitmapFactory.Options().apply {
                        inSampleSize = generateSequence(1) { it * 2 }
                            .first { sample -> maxOf(bounds.outWidth, bounds.outHeight) / sample <= 600 }
                    }
                    BitmapFactory.decodeByteArray(payload, 0, payload.size, decode)
                }
            }.getOrNull()
        }
        AudioTags(title, artist, album, art)
    } catch (_: Exception) {
        AudioTags(fallback, null, null, null)
    } finally {
        runCatching { retriever.release() }
    }
}

private fun audioClock(ms: Long): String {
    val secs = (ms.coerceAtLeast(0L) / 1000L)
    val h = secs / 3600
    val m = (secs / 60) % 60
    val s = secs % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.ROOT, "%d:%02d", m, s)
}

/** Interface do player: a reprodução pertence ao MusicPlaybackService. */
@Composable
internal fun AudioPlayerViewer(
    file: File,
    controller: androidx.media3.session.MediaController,
    onTrackChanged: (File) -> Unit,
    onOpenExternal: (File) -> Unit,
) {
    val issue = remember(file.absolutePath, file.length()) {
        when {
            !file.exists() -> "O arquivo de música não existe mais."
            !file.canRead() -> "Sem permissão para ler este arquivo."
            file.length() <= 0L -> "Este arquivo de música está vazio."
            else -> null
        }
    }
    if (issue != null) {
        AudioProblem(issue) { onOpenExternal(file) }
        return
    }

    val queue by produceState<List<File>?>(initialValue = null, file.absolutePath) {
        value = withContext(Dispatchers.IO) { AudioQueueRules.buildQueue(file) }
    }
    val tracks = queue
    if (tracks == null) {
        Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = audioAccent)
        }
        return
    }

    val startIndex = tracks.indexOfFirst { it.absolutePath == file.absolutePath }.coerceAtLeast(0)
    val player = controller
    LaunchedEffect(player, file.absolutePath, tracks) {
        val newUris = tracks.map { Uri.fromFile(it) }
        val currentUris = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).localConfiguration?.uri }
        // Ao reabrir a mesma fila, não interromper a música nem reiniciar o tempo.
        if (currentUris != newUris) {
            val items = tracks.map {
                MediaItem.Builder().setUri(Uri.fromFile(it)).setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(AudioQueueRules.readableTitle(it.name)).build()
                ).build()
            }
            player.setMediaItems(items, startIndex, 0L)
            player.prepare()
            player.play()
        } else if (player.currentMediaItemIndex != startIndex &&
            file.absolutePath != musicItemFile(player.currentMediaItem)?.absolutePath) {
            player.seekTo(startIndex, 0L)
            player.play()
        }
    }
    var index by remember(player) { mutableIntStateOf(player.currentMediaItemIndex.takeIf { it >= 0 } ?: startIndex) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var status by remember(player) { mutableIntStateOf(player.playbackState) }
    var position by remember(player) { mutableLongStateOf(0L) }
    var duration by remember(player) { mutableLongStateOf(0L) }
    var dragging by remember(player) { mutableStateOf(false) }
    var seekPreview by remember(player) { mutableFloatStateOf(0f) }
    var message by remember(player) { mutableStateOf<String?>(null) }
    var showQueue by remember(player) { mutableStateOf(false) }
    var repeatMode by remember(player) { mutableIntStateOf(player.repeatMode) }
    var shuffle by remember(player) { mutableStateOf(player.shuffleModeEnabled) }

    DisposableEffect(player, tracks) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { isPlaying = value }
            override fun onPlaybackStateChanged(state: Int) {
                status = state
                if (player.duration != C.TIME_UNSET && player.duration > 0L) duration = player.duration
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                index = player.currentMediaItemIndex.coerceIn(0, tracks.lastIndex)
                position = 0L
                duration = 0L
                message = null
                onTrackChanged(tracks[index])
            }
            override fun onPlayerError(error: PlaybackException) {
                message = "Não foi possível reproduzir esta faixa. Verifique o formato ou tente 'Abrir com'."
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            val value = player.duration
            if (value != C.TIME_UNSET && value > 0L) duration = value
            if (!dragging) seekPreview = position.toFloat()
            delay(300L)
        }
    }

    val currentFile = tracks[index.coerceIn(0, tracks.lastIndex)]
    val tags by produceState<AudioTags?>(initialValue = null, currentFile.absolutePath) {
        value = withContext(Dispatchers.IO) { extractAudioTags(currentFile) }
    }
    val selectedTags = tags ?: AudioTags(AudioQueueRules.readableTitle(currentFile.name), null, null, null)

    Column(
        Modifier.fillMaxSize().background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.size(180.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF0F5FC)).border(1.dp, Color(0xFFB8C7DA), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val cover = selectedTags.artwork
            if (cover != null) Image(
                bitmap = cover.asImageBitmap(), contentDescription = "Capa do álbum",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            ) else Icon(
                Icons.Filled.Album, contentDescription = null, tint = audioAccent,
                modifier = Modifier.size(88.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            selectedTags.title, fontWeight = FontWeight.Bold, fontSize = 19.sp,
            color = audioDark, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
        Text(
            listOfNotNull(selectedTags.artist, selectedTags.album).joinToString(" • ").ifBlank { "Artista/álbum não informados" },
            fontSize = 12.sp, color = audioMuted, maxLines = 2, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Slider(
            value = if (dragging) seekPreview.coerceIn(0f, duration.coerceAtLeast(1L).toFloat())
                else position.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat(),
            onValueChange = { dragging = true; seekPreview = it },
            onValueChangeFinished = {
                if (duration > 0L) player.seekTo(seekPreview.toLong().coerceIn(0L, duration))
                dragging = false
            },
            valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
            enabled = duration > 0L,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(audioClock(if (dragging) seekPreview.toLong() else position), fontSize = 11.sp, color = audioMuted)
            Text(audioClock(duration), fontSize = 11.sp, color = audioMuted)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            IconButton(onClick = { shuffle = !shuffle; player.shuffleModeEnabled = shuffle }) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Aleatório", tint = if (shuffle) audioAccent else audioMuted)
            }
            IconButton(onClick = {
                if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0L)
            }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Faixa anterior", tint = audioAccent, modifier = Modifier.size(33.dp))
            }
            IconButton(onClick = {
                if (isPlaying) player.pause() else { if (status == Player.STATE_ENDED) player.seekTo(0L); player.play() }
            }) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                    tint = audioAccent, modifier = Modifier.size(42.dp),
                )
            }
            IconButton(onClick = {
                if (player.hasNextMediaItem()) player.seekToNextMediaItem()
            }, enabled = player.hasNextMediaItem() || shuffle) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Próxima faixa", tint = audioAccent, modifier = Modifier.size(33.dp))
            }
            IconButton(onClick = {
                repeatMode = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                player.repeatMode = repeatMode
            }) {
                Icon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = when (repeatMode) {
                        Player.REPEAT_MODE_ALL -> "Repetir fila"
                        Player.REPEAT_MODE_ONE -> "Repetir faixa"
                        else -> "Repetição desligada"
                    }, tint = if (repeatMode == Player.REPEAT_MODE_OFF) audioMuted else audioAccent,
                )
            }
        }
        if (status == Player.STATE_BUFFERING) CircularProgressIndicator(Modifier.size(18.dp), color = audioAccent)
        if (message != null) {
            Text(message.orEmpty(), color = Color(0xFFB42318), fontSize = 12.sp, textAlign = TextAlign.Center)
            Text("Tentar novamente", color = audioAccent, fontSize = 12.sp,
                modifier = Modifier.clickable { message = null; player.prepare(); player.play() }.padding(8.dp))
            Text("Abrir com outro aplicativo", color = audioAccent, fontSize = 12.sp,
                modifier = Modifier.clickable { onOpenExternal(currentFile) }.padding(8.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFB8C7DA), RoundedCornerShape(8.dp))
                .clickable { showQueue = true }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = audioAccent)
            Text("Fila da pasta • ${tracks.size} músicas", color = audioDark, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${index + 1}/${tracks.size}", color = audioMuted, fontSize = 12.sp)
        }
    }
    if (showQueue) {
        Dialog(onDismissRequest = { showQueue = false }) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 500.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Músicas desta pasta", fontWeight = FontWeight.Bold, color = audioDark, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showQueue = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = audioAccent)
                    }
                }
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(tracks, key = { _, item -> item.absolutePath }) { i, item ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (i != index) player.seekTo(i, 0L)
                                player.play()
                                showQueue = false
                            }.padding(vertical = 10.dp, horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null,
                                tint = if (i == index) audioAccent else audioMuted)
                            Spacer(Modifier.width(10.dp))
                            Text(AudioQueueRules.readableTitle(item.name), fontSize = 13.sp,
                                fontWeight = if (i == index) FontWeight.Bold else FontWeight.Normal,
                                color = audioDark, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioProblem(message: String, onOpenExternal: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = Color(0xFFB42318), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Abrir com outro aplicativo", color = audioAccent,
            modifier = Modifier.clickable(onClick = onOpenExternal).padding(8.dp))
    }
}
