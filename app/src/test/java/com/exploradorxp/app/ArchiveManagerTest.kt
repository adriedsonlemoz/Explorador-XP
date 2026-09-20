package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchiveManagerTest {
    @Test
    fun normalizesArchivePaths() {
        assertEquals("folder/file.txt", normalizeEntryPath("/folder\\file.txt/"))
    }

    @Test(expected = ArchiveUnsafePathException::class)
    fun blocksParentTraversal() {
        safeEntryPath("../../outside.txt")
    }

    @Test(expected = ArchiveUnsafePathException::class)
    fun blocksAbsoluteUnixPath() {
        safeEntryPath("/system/secret.txt")
    }

    @Test(expected = ArchiveUnsafePathException::class)
    fun blocksWindowsDrivePath() {
        safeEntryPath("C:\\Windows\\secret.txt")
    }

    @Test
    fun keepsSafeNestedPath() {
        val result = safeEntryPath("assets/ui/button.png")
        assertFalse(result.contains(".."))
        assertTrue(result.endsWith("assets${File.separator}ui${File.separator}button.png"))
    }

    @Test
    fun createsUniqueSiblingName() {
        val root = createTempDir(prefix = "archive-unique-")
        try {
            val first = File(root, "asset.png")
            first.writeText("x")
            val second = uniqueFile(first)
            assertEquals("asset (1).png", second.name)
        } finally {
            root.deleteRecursively()
        }
    }
    @Test
    fun normalizesZipOutputName() {
        assertEquals("Fotos.zip", normalizeArchiveFileName("Fotos"))
        assertEquals("backup.ZIP", normalizeArchiveFileName("backup.ZIP"))
        assertEquals("pasta_sub.zip", normalizeArchiveFileName("pasta/sub"))
    }

}
