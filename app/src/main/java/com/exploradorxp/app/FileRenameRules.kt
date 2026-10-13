package com.exploradorxp.app

/** Separa apenas a extensão real, mantendo dotfiles, pastas e extensões compostas. */
internal data class EditableFileName(val stem: String, val extension: String) {
    fun compose(newStem: String, newExtension: String = extension): String =
        newStem.trim() + newExtension.trim().let { if (it.isBlank()) "" else if (it.startsWith('.')) it else ".$it" }
}

internal object FileRenameRules {
    private val compound = listOf(".tar.gz", ".tar.bz2", ".tar.xz", ".tar.zst", ".tar.lz4", ".user.js")
    fun parts(original: String, isDirectory: Boolean = false): EditableFileName {
        if (isDirectory || (original.startsWith('.') && original.indexOf('.', 1) == -1))
            return EditableFileName(original, "")
        val suffix = compound.firstOrNull { original.endsWith(it, ignoreCase = true) && original.length > it.length }
        if (suffix != null) return EditableFileName(original.dropLast(suffix.length), original.takeLast(suffix.length))
        val dot = original.lastIndexOf('.')
        return if (dot <= 0 || dot == original.lastIndex) EditableFileName(original, "")
            else EditableFileName(original.substring(0, dot), original.substring(dot))
    }

    fun validationError(name: String): String? = when {
        name.isBlank() || name != name.trim() -> "Informe um nome sem espaços nas extremidades."
        name == "." || name == ".." -> "Nome reservado pelo sistema."
        name.any { it == '/' || it == '\\' || it == '\u0000' || it.code < 32 } -> "O nome contém caracteres inválidos."
        else -> null
    }
}
