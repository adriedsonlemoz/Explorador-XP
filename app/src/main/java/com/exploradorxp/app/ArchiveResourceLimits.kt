package com.exploradorxp.app

import java.io.File

/** Limites únicos usados na extração, previews e abertura de arquivos externos. */
internal const val MAX_ZIP_ENTRIES = 20_000
internal const val MAX_EXTRACTED_BYTES = 8L * 1024L * 1024L * 1024L
internal const val MAX_PREVIEW_BYTES = 512L * 1024L * 1024L
internal const val MAX_EXTERNAL_OPEN_CACHE_BYTES = 4L * 1024L * 1024L * 1024L

private const val MAX_ARCHIVE_PREVIEW_CACHE_BYTES = 1024L * 1024L * 1024L
private const val ARCHIVE_PREVIEW_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L
private const val PREVIEW_IN_PROGRESS_GRACE_MS = 60L * 60L * 1000L

/**
 * Limpa apenas o cache privado archive-preview do app, sem tocar nos ZIPs originais.
 * O arquivo em uso é preservado mesmo quando o cache ultrapassa o limite.
 * Arquivos de staging recentes são ignorados para não interromper outro preview em andamento.
 */
internal fun trimArchivePreviewCache(
    previewRoot: File,
    activeArchiveKey: String,
    activePreviewName: String? = null,
) {
    if (!previewRoot.isDirectory) return
    val now = System.currentTimeMillis()
    val files = previewRoot.listFiles().orEmpty()
        .filter { it.isDirectory }
        .flatMap { dir ->
            dir.walkTopDown().filter { it.isFile }.toList()
        }
    var totalBytes = 0L
    files.forEach { file ->
        val size = file.length().coerceAtLeast(0L)
        totalBytes = if (size > Long.MAX_VALUE - totalBytes) Long.MAX_VALUE else totalBytes + size
    }

    val protectedFile = activePreviewName?.let { File(File(previewRoot, activeArchiveKey), it).absolutePath }
    files.sortedBy { it.lastModified() }.forEach { file ->
        if (file.absolutePath == protectedFile) return@forEach
        val age = (now - file.lastModified()).coerceAtLeast(0L)
        val isRecentStage = ".abrindo-" in file.name && age < PREVIEW_IN_PROGRESS_GRACE_MS
        if (isRecentStage) return@forEach
        if (age > ARCHIVE_PREVIEW_MAX_AGE_MS || totalBytes > MAX_ARCHIVE_PREVIEW_CACHE_BYTES) {
            val size = file.length().coerceAtLeast(0L)
            if (runCatching { file.delete() }.getOrDefault(false)) {
                totalBytes = (totalBytes - size).coerceAtLeast(0L)
            }
        }
    }
    // Não remover a pasta ativa: outra pré-visualização pode estar sendo criada nela.
    previewRoot.listFiles().orEmpty()
        .filter { it.isDirectory && it.name != activeArchiveKey && it.listFiles().isNullOrEmpty() }
        .forEach { runCatching { it.delete() } }
}
