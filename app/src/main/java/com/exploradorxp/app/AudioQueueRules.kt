package com.exploradorxp.app

import java.io.File
import java.util.Locale

/** Fila local: somente arquivos de áudio do mesmo diretório, sem varredura recursiva. */
internal object AudioQueueRules {
    const val MAX_TRACKS = 1_000
    val extensions: Set<String> = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus")

    fun isAudio(name: String): Boolean = name.substringAfterLast('.', "").lowercase(Locale.ROOT) in extensions

    fun buildQueue(selected: File, maxTracks: Int = MAX_TRACKS): List<File> {
        val cap = maxTracks.coerceAtLeast(1)
        val siblings = runCatching { selected.parentFile?.listFiles()?.toList().orEmpty() }
            .getOrDefault(emptyList())
        val candidates = siblings.asSequence()
            .filter { it.isFile && it.canRead() && it.length() > 0L && isAudio(it.name) }
            .distinctBy { it.absolutePath }
            .sortedWith(compareBy<File>({ it.name.lowercase(Locale.ROOT) }, { it.name }))
            .toList()
        // A música escolhida não pode desaparecer se a pasta exceder o limite.
        val included = if (candidates.any { it.absolutePath == selected.absolutePath }) {
            val prefix = candidates.take(cap)
            if (prefix.any { it.absolutePath == selected.absolutePath }) prefix
            else (prefix.take(cap - 1) + selected).sortedWith(
                compareBy<File>({ it.name.lowercase(Locale.ROOT) }, { it.name })
            )
        } else {
            (candidates.take(cap - 1) + selected).distinctBy { it.absolutePath }.sortedWith(
                compareBy<File>({ it.name.lowercase(Locale.ROOT) }, { it.name })
            )
        }
        return included
    }

    fun readableTitle(name: String): String = name.substringBeforeLast('.', name).ifBlank { name }
}
