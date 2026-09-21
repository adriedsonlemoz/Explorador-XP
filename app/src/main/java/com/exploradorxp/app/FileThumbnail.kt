package com.exploradorxp.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

private val thumbnailImageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp", "gif", "heic")
private val thumbnailVideoExtensions = setOf("mp4", "m4v", "3gp", "webm", "mkv", "avi", "mov")

private object FileThumbnailCache {
    private const val MAX_CACHE_BYTES = 18 * 1024 * 1024
    private val imageSlots = Semaphore(permits = 2)
    private val videoSlots = Semaphore(permits = 1)
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun peek(key: String): Bitmap? = synchronized(cache) { cache.get(key) }

    suspend fun load(file: File, targetPx: Int, isVideo: Boolean): Bitmap? {
        val key = "${file.absolutePath}:${file.lastModified()}:$targetPx:${if (isVideo) 1 else 0}"
        peek(key)?.let { return it }
        val decoded = withContext(Dispatchers.IO) {
            val gate = if (isVideo) videoSlots else imageSlots
            gate.withPermit {
                if (isVideo) decodeVideoThumbnail(file, targetPx) else decodeImageThumbnail(file, targetPx)
            }
        }
        if (decoded != null) synchronized(cache) { cache.put(key, decoded) }
        return decoded
    }
}

@Composable
fun FileVisual(
    item: FileItem,
    size: Dp,
    modifier: Modifier = Modifier,
    loadThumbnail: Boolean = true,
) {
    val extension = item.extension.lowercase()
    val isVideo = !item.isDirectory && extension in thumbnailVideoExtensions
    val supportsThumbnail = !item.isDirectory && (extension in thumbnailImageExtensions || isVideo)
    val targetPx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(48) }
    val cacheKey = remember(item.path, item.modifiedAt, targetPx, isVideo) {
        "${item.path}:${item.modifiedAt}:$targetPx:${if (isVideo) 1 else 0}"
    }
    var bitmap by remember(cacheKey) { mutableStateOf(FileThumbnailCache.peek(cacheKey)) }

    if (supportsThumbnail && loadThumbnail) {
        LaunchedEffect(cacheKey, loadThumbnail) {
            // Evita iniciar decodificação cara para itens que só passaram rapidamente pela
            // viewport. Se a rolagem começar, o efeito é cancelado e o trabalho não disputa
            // I/O/CPU com a navegação.
            delay(if (isVideo) 140L else 55L)
            bitmap = FileThumbnailCache.load(item.file, targetPx, isVideo)
        }
    }

    val preview = bitmap
    if (supportsThumbnail && preview != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFE6EEF7))
        ) {
            Image(
                bitmap = preview.asImageBitmap(),
                contentDescription = item.name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            if (isVideo) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size((size.value * 0.38f).dp.coerceAtLeast(20.dp))
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Text("▶", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Os PNGs XP têm proporções e margens internas diferentes (Movies/Music são os casos
        // mais perceptíveis). Mantém uma caixa externa estável e aplica uma área segura ao
        // desenho para impedir corte/encosto nas bordas sem deformar o recurso original.
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size),
        ) {
            CachedResourceIcon(
                resId = item.iconRes,
                contentDescription = item.name,
                modifier = Modifier.size((size.value * 0.86f).dp.coerceAtLeast(16.dp)),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun decodeImageThumbnail(file: File, targetPx: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

    var sample = 1
    while (bounds.outWidth / sample > targetPx * 2 || bounds.outHeight / sample > targetPx * 2) {
        sample *= 2
    }
    BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    )
}.getOrNull()

private fun decodeVideoThumbnail(file: File, targetPx: Int): Bitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(file.absolutePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                -1L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                targetPx,
                targetPx,
            )
        } else {
            val raw = retriever.getFrameAtTime(-1L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            raw?.let {
                if (it.width <= targetPx && it.height <= targetPx) it
                else Bitmap.createScaledBitmap(it, targetPx, targetPx, true).also { scaled ->
                    if (scaled !== it) it.recycle()
                }
            }
        }
    } finally {
        retriever.release()
    }
}.getOrNull()
