package com.exploradorxp.app

import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.ArrayDeque
import kotlin.coroutines.coroutineContext

/**
 * Snapshot leve de uma árvore usada por copiar/mover/excluir.
 *
 * A árvore é enumerada uma única vez. As operações seguintes reutilizam a lista planejada,
 * evitando repetir listFiles() apenas para calcular o total e depois percorrer tudo de novo.
 */
internal data class PlannedFileEntry(
    val source: File,
    val relativePath: String,
    val isDirectory: Boolean,
    val bytes: Long,
    val modifiedAt: Long,
)

internal data class FileOperationPlan(
    val root: File,
    val entries: List<PlannedFileEntry>,
    val totalBytes: Long,
) {
    val entryCount: Int get() = entries.size
}

private data class PendingFileEntry(
    val file: File,
    val relativePath: String,
)

/**
 * Faz uma única enumeração iterativa da árvore. Além de reduzir I/O duplicado, evita uma
 * recursão profunda no stack quando existem estruturas de pastas muito aninhadas.
 */
internal suspend fun buildFileOperationPlan(
    root: File,
    awaitIfPaused: suspend () -> Unit = {},
): FileOperationPlan {
    val pending = ArrayDeque<PendingFileEntry>()
    val entries = ArrayList<PlannedFileEntry>()
    pending.addLast(PendingFileEntry(root, ""))
    var totalBytes = 0L
    var visited = 0

    while (pending.isNotEmpty()) {
        if ((visited++ and 63) == 0) {
            coroutineContext.ensureActive()
            awaitIfPaused()
        }
        val current = pending.removeFirst()
        val source = current.file
        val isDirectory = source.isDirectory
        val isRegularFile = !isDirectory && source.isFile
        val bytes = if (isRegularFile) {
            runCatching { source.length() }.getOrDefault(0L).coerceAtLeast(0L)
        } else {
            0L
        }
        totalBytes += bytes
        entries += PlannedFileEntry(
            source = source,
            relativePath = current.relativePath,
            isDirectory = isDirectory,
            bytes = bytes,
            modifiedAt = runCatching { source.lastModified() }.getOrDefault(0L),
        )

        if (isDirectory) {
            source.listFiles().orEmpty().forEach { child ->
                val relativePath = if (current.relativePath.isEmpty()) {
                    child.name
                } else {
                    current.relativePath + File.separator + child.name
                }
                pending.addLast(PendingFileEntry(child, relativePath))
            }
        }
    }

    return FileOperationPlan(root = root, entries = entries, totalBytes = totalBytes)
}

/** Copia a árvore já enumerada sem chamar listFiles() novamente.
 *
 * O callback recebe incrementos de bytes enquanto o arquivo é copiado e sinaliza quando
 * a entrada terminou. Isso permite progresso real por bytes sem uma segunda leitura.
 */
internal suspend fun copyFileOperationPlan(
    plan: FileOperationPlan,
    targetRoot: File,
    awaitIfPaused: suspend () -> Unit = {},
    onProgress: (name: String, bytesDelta: Long, entryCompleted: Boolean) -> Unit,
) {
    val buffer = ByteArray(COPY_BUFFER_SIZE)
    plan.entries.forEachIndexed { index, entry ->
        if ((index and 31) == 0) {
            coroutineContext.ensureActive()
            awaitIfPaused()
        }
        val target = if (entry.relativePath.isEmpty()) {
            targetRoot
        } else {
            File(targetRoot, entry.relativePath)
        }

        if (entry.isDirectory) {
            check(target.mkdirs() || target.isDirectory) { "Não foi possível criar ${target.name}." }
            onProgress(target.name, 0L, true)
        } else {
            target.parentFile?.let { parent ->
                check(parent.mkdirs() || parent.isDirectory) { "Não foi possível criar ${parent.name}." }
            }
            FileInputStream(entry.source).use { input ->
                FileOutputStream(target).use { output ->
                    while (true) {
                        coroutineContext.ensureActive()
                        awaitIfPaused()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        onProgress(target.name, read.toLong(), false)
                    }
                }
            }
            if (entry.modifiedAt > 0L) target.setLastModified(entry.modifiedAt)
            onProgress(target.name, 0L, true)
        }
    }
}

/**
 * Exclui em ordem inversa (filhos antes dos pais) reutilizando o plano. Assim a exclusão
 * não precisa enumerar novamente cada diretório depois da fase de preparação.
 */
internal suspend fun deleteFileOperationPlan(
    plan: FileOperationPlan,
    awaitIfPaused: suspend () -> Unit = {},
    onEntry: (PlannedFileEntry) -> Unit = {},
): Boolean {
    for (index in plan.entries.lastIndex downTo 0) {
        coroutineContext.ensureActive()
        awaitIfPaused()
        val entry = plan.entries[index]
        val source = entry.source
        val removed = source.delete() || !source.exists()
        if (!removed) return false
        onEntry(entry)
    }
    return true
}

private const val COPY_BUFFER_SIZE = 256 * 1024
