package com.exploradorxp.app

/**
 * Lógica pura usada pelo editor para indentar/recuar blocos sem depender da UI Android.
 * Mantê-la isolada permite testar as regras sem carregar Compose ou EditText.
 */
internal object EditorIndentationEngine {
    fun transformBlock(
        source: String,
        indentUnit: String,
        indentSize: Int,
        outdent: Boolean,
    ): String {
        if (source.isEmpty()) return if (outdent) source else indentUnit
        return source.split('\n').joinToString("\n") { line ->
            if (outdent) removeOneLevel(line, indentSize) else indentUnit + line
        }
    }

    fun removeOneLevel(line: String, indentSize: Int): String {
        if (line.isEmpty()) return line
        if (line.startsWith("\t")) return line.drop(1)
        val maxSpaces = indentSize.coerceIn(2, 8)
        var count = 0
        while (count < line.length && count < maxSpaces && line[count] == ' ') count++
        return line.drop(count)
    }
}
