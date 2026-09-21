package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveBrowserIndexTest {
    @Test
    fun buildsImplicitFoldersAndRecursiveStatistics() {
        val entries = listOf(
            entry("docs/readme.md", size = 10, compressed = 6),
            entry("docs/sub/a.bin", size = 5, compressed = 3),
            entry("root.txt", size = 2, compressed = 2),
        )

        val index = buildArchiveBrowserIndex(entries)
        val docs = index.itemsByPath.getValue("docs")

        assertEquals(3, index.totalFiles)
        assertEquals(2, index.totalDirectories)
        assertEquals(2, docs.descendantFiles)
        assertEquals(1, docs.descendantDirectories)
        assertEquals(15L, docs.size)
        assertEquals(9L, docs.compressedSize)
        assertEquals(listOf("docs", "root.txt"), index.childrenByParent.getValue("").map { it.path })
    }

    @Test
    fun descendingSortStillKeepsFoldersAheadOfFiles() {
        val index = buildArchiveBrowserIndex(
            listOf(
                entry("pasta/inside.txt", size = 1),
                entry("z.txt", size = 999),
                entry("a.txt", size = 50),
            )
        )

        val result = buildArchiveBrowserItems(index, "", "", ArchiveSortMode.SIZE, ascending = false)

        assertTrue(result.first().directory)
        assertEquals(listOf("pasta", "z.txt", "a.txt"), result.map { it.name })
    }

    @Test
    fun searchAlsoFindsImplicitFolders() {
        val index = buildArchiveBrowserIndex(listOf(entry("docs/sub/a.bin", size = 5)))
        val result = buildArchiveBrowserItems(index, "", "sub", ArchiveSortMode.NAME, ascending = true)

        assertTrue(result.any { it.path == "docs/sub" && it.directory })
    }

    private fun entry(
        path: String,
        size: Long,
        compressed: Long = size,
    ) = ArchiveEntryInfo(
        path = path,
        isDirectory = false,
        size = size,
        compressedSize = compressed,
        modifiedAt = 1_700_000_000_000L,
        compressionMethod = "DEFLATE",
        encrypted = false,
    )
}
