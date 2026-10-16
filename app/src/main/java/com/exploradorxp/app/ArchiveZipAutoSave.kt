package com.exploradorxp.app

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** Copia o ZIP original para a pasta pública /XP do armazenamento interno, sem diálogo. */
internal object ArchiveZipAutoSave {
    data class Saved(val fileName: String, val alreadyInFolder: Boolean)

    fun save(source: File, storageRoot: File, onChunk: (Long) -> Unit = {}): Saved {
        if (!source.isFile || !source.canRead()) throw IOException("ZIP de origem indisponível para leitura.")
        if (!storageRoot.isDirectory) throw IOException("Armazenamento interno indisponível.")

        val folder = File(storageRoot, "XP")
        if (!folder.isDirectory && !folder.mkdirs() && !folder.isDirectory) {
            throw IOException("Não foi possível criar a pasta XP.")
        }
        if (!folder.isDirectory) throw IOException("XP existe, mas não é uma pasta.")

        // Caso o ZIP já esteja nesta pasta, não criar uma segunda cópia desnecessária.
        if (source.canonicalFile.parentFile == folder.canonicalFile) {
            return Saved(source.name, alreadyInFolder = true)
        }

        // Um ZIP sem extensão (por exemplo, um artifact aberto via "Abrir com")
        // deve continuar reconhecível após ser salvo.
        val original = if (source.name.contains('.') && !source.name.endsWith('.')) {
            source.name
        } else {
            "${source.name.trimEnd('.')}.zip"
        }
        val stem = original.substringBeforeLast('.', original)
        val suffix = if (original.contains('.')) ".${original.substringAfterLast('.')}" else ""
        // createNewFile é atômico: não sobrescreve outro ZIP entre a checagem e a escrita.
        for (index in 0..999) {
            val name = if (index == 0) original else "$stem ($index)$suffix"
            val target = File(folder, name)
            if (!target.createNewFile()) continue
            var complete = false
            try {
                FileInputStream(source).use { input ->
                    FileOutputStream(target).use { output ->
                        ArchiveZipCopy.copy(input, output, source.length(), onChunk)
                    }
                }
                complete = true
                return Saved(name, alreadyInFolder = false)
            } finally {
                if (!complete) target.delete() // Não deixar ZIP parcial após erro/cancelamento.
            }
        }
        throw IOException("Há muitos ZIPs com o mesmo nome na pasta XP.")
    }
}
