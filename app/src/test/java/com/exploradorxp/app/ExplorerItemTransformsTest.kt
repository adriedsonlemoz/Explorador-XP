package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExplorerItemTransformsTest {
    @Test
    fun queryFiltersSnapshotWithoutChangingOriginalList() {
        val snapshot = listOf(
            item("Foto.jpg", extension = "jpg"),
            item("Documento.pdf", extension = "pdf"),
            item("fotos", isDirectory = true),
        )

        val result = ExplorerItemTransforms.apply(
            snapshot = snapshot,
            query = "FOTO",
            sortMode = SortMode.NAME,
            showHidden = false,
            foldersFirst = true,
        )

        assertEquals(listOf("fotos", "Foto.jpg"), result.map { it.name })
        assertEquals(3, snapshot.size)
    }

    @Test
    fun hiddenItemsAreFilteredFromCachedSnapshot() {
        val snapshot = listOf(
            item("normal.txt"),
            item(".oculto", isHidden = true),
        )

        val hiddenOff = ExplorerItemTransforms.apply(snapshot, "", SortMode.NAME, showHidden = false, foldersFirst = true)
        val hiddenOn = ExplorerItemTransforms.apply(snapshot, "", SortMode.NAME, showHidden = true, foldersFirst = true)

        assertFalse(hiddenOff.any { it.isHidden })
        assertTrue(hiddenOn.any { it.isHidden })
    }

    @Test
    fun foldersFirstKeepsFoldersAheadOfFiles() {
        val snapshot = listOf(
            item("A.txt"),
            item("Zeta", isDirectory = true),
            item("B.txt"),
            item("Alpha", isDirectory = true),
        )

        val result = ExplorerItemTransforms.apply(snapshot, "", SortMode.NAME, showHidden = true, foldersFirst = true)

        assertEquals(listOf("Alpha", "Zeta", "A.txt", "B.txt"), result.map { it.name })
    }

    @Test
    fun sizeSortUsesCachedMetadata() {
        val snapshot = listOf(
            item("pequeno.bin", size = 10),
            item("grande.bin", size = 5000),
        )

        val result = ExplorerItemTransforms.apply(snapshot, "", SortMode.SIZE, showHidden = true, foldersFirst = false)

        assertEquals(listOf("grande.bin", "pequeno.bin"), result.map { it.name })
    }

    private fun item(
        name: String,
        isDirectory: Boolean = false,
        isHidden: Boolean = false,
        size: Long = 0L,
        extension: String = "txt",
    ): FileItem = FileItem(
        file = File("/tmp/$name"),
        iconRes = 0,
        name = name,
        isDirectory = isDirectory,
        isHidden = isHidden,
        size = size,
        modifiedAt = 1_700_000_000_000L,
        extension = if (isDirectory) "" else extension,
        listDetailText = "detail",
        gridDetailText = "grid",
        isFavorite = false,
    )
}
