package com.exploradorxp.app

import java.util.Locale
import kotlin.math.roundToInt

/** Resumo curto para caber AO LADO do ícone na barra estreita. Os valores completos ficam na Lixeira. */
internal fun compactTrashSpace(bytes: Long?): String {
    if (bytes == null) return "…"
    val safeBytes = bytes.coerceAtLeast(0L)
    if (safeBytes < 1024L) return "${safeBytes}B"
    val units = arrayOf("K", "M", "G", "T", "P")
    var size = safeBytes.toDouble()
    var unitIndex = -1
    while (size >= 1024.0 && unitIndex < units.lastIndex) {
        size /= 1024.0
        unitIndex++
    }
    if (size >= 10.0) return "${size.roundToInt()}${units[unitIndex]}"
    return String.format(Locale.forLanguageTag("pt-BR"), "%.1f%s", size, units[unitIndex])
}

/** Contagem abreviada só no desenho: a acessibilidade informa o valor exato. */
internal fun compactTrashCount(count: Int): String = if (count > 99) "99+" else count.coerceAtLeast(0).toString()

