package com.exploradorxp.app

/** Palavra de código ao redor do cursor. Não usa uma cópia de todo o conteúdo. */
internal object EditorSelectionRules {
    fun wordRange(text: CharSequence, cursor: Int): IntRange? {
        if (text.isEmpty()) return null
        val at = cursor.coerceIn(0, text.length)
        fun identifier(c: Char) = c.isLetterOrDigit() || c == '_'
        val pivot = when {
            at < text.length && identifier(text[at]) -> at
            at < text.length && !text[at].isWhitespace() -> at
            at > 0 && identifier(text[at - 1]) -> at - 1
            else -> return null
        }
        if (!identifier(text[pivot])) return pivot..pivot
        var first = pivot
        var last = pivot
        while (first > 0 && identifier(text[first - 1])) first--
        while (last + 1 < text.length && identifier(text[last + 1])) last++
        return first..last
    }
}
