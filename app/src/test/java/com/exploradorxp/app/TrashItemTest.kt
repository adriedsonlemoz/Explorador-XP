package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class TrashItemTest {
    @Test
    fun name_prefersPersistedOriginalName() {
        val item = trashItem(
            originalPath = "/storage/emulated/0/Download/outro-nome.zip",
            originalName = "arquivo-real.zip",
            trashedFile = File("/tmp/container/arquivo-fisico.zip"),
        )

        assertEquals("arquivo-real.zip", item.name)
    }

    @Test
    fun name_fallsBackToOriginalPathForLegacyTrashEntries() {
        val item = trashItem(
            originalPath = "/storage/emulated/0/Download/relatorio.txt",
            originalName = "",
            trashedFile = File("/tmp/container/relatorio.txt"),
        )

        assertEquals("relatorio.txt", item.name)
    }

    @Test
    fun name_fallsBackToPhysicalFileWhenPathHasNoName() {
        val item = trashItem(
            originalPath = "/",
            originalName = "",
            trashedFile = File("/tmp/container/recuperado.bin"),
        )

        assertEquals("recuperado.bin", item.name)
    }

    private fun trashItem(
        originalPath: String,
        originalName: String,
        trashedFile: File,
    ) = TrashItem(
        id = "id",
        trashedFile = trashedFile,
        originalPath = originalPath,
        originalName = originalName,
        deletedAt = 1_700_000_000_000L,
        size = 10L,
        isDirectory = false,
        typeLabel = "Arquivo",
        iconRes = 0,
    )
}
