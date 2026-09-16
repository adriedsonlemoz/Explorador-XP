package com.exploradorxp.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache pequeno para os PNGs usados nas listas/grades do Explorer.
 *
 * painterResource() é ótimo para recursos simples, mas a primeira decodificação
 * de um bitmap acontece na thread que está compondo a UI. Como o Explorer pode
 * mostrar muitos tipos de arquivo diferentes durante uma rolagem rápida, os
 * ícones de itens são decodificados em Dispatchers.IO e reaproveitados aqui.
 */
private object ResourceBitmapCache {
    private const val MAX_CACHE_BYTES = 6 * 1024 * 1024

    private val cache = object : LruCache<Long, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.allocationByteCount
    }
    private val locks = ConcurrentHashMap<Long, Mutex>()
    private val decodeSlots = Semaphore(permits = 2)

    private fun key(@DrawableRes resId: Int, densityDpi: Int): Long =
        (resId.toLong() shl 32) xor densityDpi.toLong()

    fun peek(@DrawableRes resId: Int, densityDpi: Int): Bitmap? =
        synchronized(cache) { cache.get(key(resId, densityDpi)) }

    suspend fun load(context: Context, @DrawableRes resId: Int, densityDpi: Int): Bitmap? {
        peek(resId, densityDpi)?.let { return it }
        val cacheKey = key(resId, densityDpi)
        val lock = locks.getOrPut(cacheKey) { Mutex() }
        return lock.withLock {
            peek(resId, densityDpi)?.let { return@withLock it }
            val decoded = withContext(Dispatchers.IO) {
                decodeSlots.withPermit {
                    BitmapFactory.decodeResource(context.resources, resId)
                }
            }
            if (decoded != null) {
                synchronized(cache) { cache.put(cacheKey, decoded) }
            }
            decoded
        }
    }

    suspend fun preload(context: Context, resIds: List<Int>, densityDpi: Int) {
        for (resId in resIds) load(context, resId, densityDpi)
    }
}

@Composable
fun CachedResourceIcon(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current.applicationContext
    val densityDpi = context.resources.displayMetrics.densityDpi
    var bitmap by remember(resId, densityDpi) {
        mutableStateOf(ResourceBitmapCache.peek(resId, densityDpi))
    }

    LaunchedEffect(resId, densityDpi) {
        if (bitmap == null) {
            bitmap = ResourceBitmapCache.load(context, resId, densityDpi)
        }
    }

    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        // Mantém o layout estável sem bloquear a UI enquanto o PNG é decodificado.
        Spacer(modifier = modifier)
    }
}

@Composable
fun PreloadResourceIcons(resIds: List<Int>) {
    val context = LocalContext.current.applicationContext
    val densityDpi = context.resources.displayMetrics.densityDpi
    val stableIds = remember(resIds) { resIds.distinct() }
    LaunchedEffect(stableIds, densityDpi) {
        ResourceBitmapCache.preload(context, stableIds, densityDpi)
    }
}
