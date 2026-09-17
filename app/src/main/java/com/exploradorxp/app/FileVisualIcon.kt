package com.exploradorxp.app

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

private object ApkArtworkCache {
    private const val MAX_BYTES = 4 * 1024 * 1024
    private val cache = object : LruCache<String, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val decodeSlots = Semaphore(1)

    fun peek(key: String): Bitmap? = synchronized(cache) { cache.get(key) }

    suspend fun load(context: Context, file: File, targetPx: Int): Bitmap? {
        val key = "${file.absolutePath}:${file.lastModified()}:$targetPx"
        peek(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            decodeSlots.withPermit {
                runCatching {
                    val pm = context.packageManager
                    val info = if (Build.VERSION.SDK_INT >= 33) {
                        pm.getPackageArchiveInfo(file.absolutePath, android.content.pm.PackageManager.PackageInfoFlags.of(0L))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageArchiveInfo(file.absolutePath, 0)
                    } ?: return@runCatching null
                    val appInfo = info.applicationInfo ?: return@runCatching null
                    appInfo.sourceDir = file.absolutePath
                    appInfo.publicSourceDir = file.absolutePath
                    appInfo.loadIcon(pm).toBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
                }.getOrNull()?.also { bitmap -> synchronized(cache) { cache.put(key, bitmap) } }
            }
        }
    }
}

private data class VectorStyle(val icon: ImageVector, val tint: Color, val background: Color)

@Composable
fun FileVisualIcon(
    item: FileItem,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    if (item.icon.kind == FileIconKind.APK) {
        ApkVisualIcon(item = item, modifier = modifier, size = size)
    } else {
        VectorFileVisual(visual = item.icon, modifier = modifier, size = size)
    }
}

@Composable
fun FileVisualIcon(
    visual: FileVisual,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    VectorFileVisual(visual = visual, modifier = modifier, size = size)
}

@Composable
private fun ApkVisualIcon(item: FileItem, modifier: Modifier, size: Dp) {
    val context = LocalContext.current.applicationContext
    val density = context.resources.displayMetrics.density
    val targetPx = (size.value * density).toInt().coerceAtLeast(48)
    val key = remember(item.path, item.modifiedAt, targetPx) { "${item.path}:${item.modifiedAt}:$targetPx" }
    var bitmap by remember(key) { mutableStateOf(ApkArtworkCache.peek(key)) }

    LaunchedEffect(key) {
        if (bitmap == null) bitmap = ApkArtworkCache.load(context, item.file, targetPx)
    }

    if (bitmap != null) {
        ComposeImage(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.20f)),
        )
    } else {
        VectorFileVisual(visual = item.icon, modifier = modifier, size = size)
    }
}

@Composable
private fun VectorFileVisual(visual: FileVisual, modifier: Modifier, size: Dp) {
    val style = styleFor(visual.kind)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(style.background),
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.tint,
            modifier = Modifier.size(size * 0.68f),
        )
        if (!visual.badge.isNullOrBlank() && visual.kind != FileIconKind.FOLDER && visual.kind != FileIconKind.DOWNLOADS_FOLDER) {
            Text(
                text = visual.badge,
                color = Color.White,
                fontSize = (size.value * 0.17f).coerceIn(6f, 10f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(4.dp))
                    .background(style.tint)
                    .padding(horizontal = 2.dp, vertical = 1.dp),
            )
        }
    }
}

private fun styleFor(kind: FileIconKind): VectorStyle = when (kind) {
    FileIconKind.FOLDER -> VectorStyle(Icons.Filled.Folder, Color(0xFFF2A900), Color(0xFFFFF5D8))
    FileIconKind.DOWNLOADS_FOLDER -> VectorStyle(Icons.Filled.Download, Color(0xFF0A67D8), Color(0xFFE6F1FF))
    FileIconKind.PDF -> VectorStyle(Icons.Filled.PictureAsPdf, Color(0xFFC62828), Color(0xFFFFE8E8))
    FileIconKind.SPREADSHEET -> VectorStyle(Icons.Filled.TableChart, Color(0xFF16834B), Color(0xFFE5F6EC))
    FileIconKind.PRESENTATION -> VectorStyle(Icons.Filled.Slideshow, Color(0xFFE0661B), Color(0xFFFFEEE4))
    FileIconKind.IMAGE -> VectorStyle(Icons.Filled.Image, Color(0xFF7B3FC6), Color(0xFFF1E8FF))
    FileIconKind.AUDIO -> VectorStyle(Icons.Filled.Audiotrack, Color(0xFF7C42C3), Color(0xFFF1E8FF))
    FileIconKind.VIDEO -> VectorStyle(Icons.Filled.Movie, Color(0xFF3556B8), Color(0xFFE8EEFF))
    FileIconKind.ARCHIVE -> VectorStyle(Icons.Filled.Archive, Color(0xFF996515), Color(0xFFFFF1D9))
    FileIconKind.CODE -> VectorStyle(Icons.Filled.Code, Color(0xFF166D7A), Color(0xFFE1F5F7))
    FileIconKind.DATABASE -> VectorStyle(Icons.Filled.Storage, Color(0xFF325A8C), Color(0xFFE6EFF9))
    FileIconKind.APK -> VectorStyle(Icons.Filled.Android, Color(0xFF20A84A), Color(0xFFE4F7E8))
    FileIconKind.FONT -> VectorStyle(Icons.Filled.FontDownload, Color(0xFF5D4777), Color(0xFFF0EAF7))
    FileIconKind.DISK_IMAGE -> VectorStyle(Icons.Filled.Storage, Color(0xFF5E6875), Color(0xFFEEF1F4))
    FileIconKind.TORRENT -> VectorStyle(Icons.Filled.Download, Color(0xFF176DC1), Color(0xFFE5F1FC))
    FileIconKind.DOCUMENT -> VectorStyle(Icons.Filled.Description, Color(0xFF2569B0), Color(0xFFE8F2FC))
    FileIconKind.UNKNOWN -> VectorStyle(Icons.Filled.InsertDriveFile, Color(0xFF657180), Color(0xFFEEF1F4))
}
