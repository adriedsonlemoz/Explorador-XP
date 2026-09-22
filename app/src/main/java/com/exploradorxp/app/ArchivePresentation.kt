package com.exploradorxp.app

import java.io.File
import java.util.Locale

internal data class ArchiveTypeSummary(
    val typeLabel: String,
    val detectedByContent: Boolean,
    val originLabel: String,
)

internal fun archiveTypeSummary(file: File): ArchiveTypeSummary {
    val extension = file.extension.lowercase(Locale.ROOT)
    val detectedExtension = if (FileContentDetector.needsContentDetection(file)) {
        FileContentDetector.detectExtension(file)
    } else null
    val detectedByContent = detectedExtension == "zip" && extension != "zip"
    val origin = when {
        detectedByContent && extension.isBlank() -> "Tipo ZIP detectado pelo conteúdo • arquivo sem extensão"
        detectedByContent -> "Tipo ZIP detectado pelo conteúdo • extensão .${extension.uppercase(Locale.ROOT)}"
        extension == "zip" -> "Extensão .ZIP"
        else -> "Aberto como ZIP"
    }
    return ArchiveTypeSummary(
        typeLabel = "Arquivo compactado ZIP",
        detectedByContent = detectedByContent,
        originLabel = origin,
    )
}

internal fun archiveCompressionMethod(info: ZipArchiveInfo): String {
    val methods = info.entries
        .asSequence()
        .filterNot { it.isDirectory }
        .map { humanCompressionMethod(it.compressionMethod) }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
    return when (methods.size) {
        0 -> "Não disponível"
        1 -> methods.first()
        else -> methods.joinToString(" + ")
    }
}

private fun humanCompressionMethod(value: String): String = when (value.trim().uppercase(Locale.ROOT)) {
    "DEFLATE", "DEFLATED" -> "Deflate"
    "STORE", "STORED" -> "Sem compressão"
    "AES" -> "AES"
    "ZIP_STANDARD" -> "ZIP padrão"
    else -> value.trim().lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
}
