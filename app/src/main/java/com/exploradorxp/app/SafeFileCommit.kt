package com.exploradorxp.app

import java.io.File
import java.util.UUID

/** Names stay short even when the user file name approaches the filesystem's limit. */
internal fun temporarySibling(target: File, purpose: String): File {
    val parent = target.parentFile ?: error("O destino precisa ter uma pasta pai.")
    require(purpose == "stage" || purpose == "backup")
    repeat(10) {
        val path = File(parent, ".xp-$purpose-${UUID.randomUUID()}")
        if (!path.exists()) return path
    }
    error("Não foi possível reservar um nome temporário seguro.")
}

/**
 * Only renames on the SAME volume. With a prior destination, move it aside before committing
 * the complete staged item. If the second rename fails, restore the old item. An unexpected
 * rollback failure preserves the backup on disk and reports its location, instead of deleting it.
 *
 * Warning: this provides rollback for normal exceptions; power loss between the two renames
 * is not a filesystem transaction. The hidden backup remains recoverable in that case.
 */
internal fun commitStagedItem(stage: File, target: File) {
    require(stage.exists()) { "A cópia temporária não existe mais." }
    require(stage.parentFile?.canonicalPath == target.parentFile?.canonicalPath) {
        "A cópia temporária precisa estar no mesmo diretório do destino."
    }
    val backup = if (target.exists()) temporarySibling(target, "backup") else null
    if (backup != null) {
        check(target.renameTo(backup)) { "Não foi possível preservar ${target.name} antes da substituição." }
    }
    if (!stage.renameTo(target)) {
        if (backup != null && !backup.renameTo(target)) {
            error("Não foi possível finalizar ${target.name} nem restaurar o original. " +
                "O original está preservado em ${backup.absolutePath}.")
        }
        error("Não foi possível finalizar ${target.name}. O original foi restaurado.")
    }
    // The new item is committed; failure to clear the backup must not jeopardize the new item.
    // A hidden .xp-backup-* is intentionally left recoverable if deletion fails.
    if (backup != null) runCatching { if (backup.isDirectory) backup.deleteRecursively() else backup.delete() }
}
