package com.exploradorxp.app

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Copia o ZIP original para a saída SAF, verificando se todos os bytes foram gravados. */
internal object ArchiveZipCopy {
    fun copy(
        input: InputStream,
        output: OutputStream,
        expectedBytes: Long,
        onChunk: (Long) -> Unit = {},
    ): Long {
        require(expectedBytes >= 0L) { "Tamanho do ZIP inválido." }
        val buffer = ByteArray(64 * 1024)
        var copied = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            output.write(buffer, 0, count)
            copied += count
            if (copied > expectedBytes) throw IOException("O ZIP de origem mudou durante a cópia.")
            onChunk(copied)
        }
        output.flush()
        if (copied != expectedBytes) throw IOException("A cópia do ZIP ficou incompleta.")
        return copied
    }
}
