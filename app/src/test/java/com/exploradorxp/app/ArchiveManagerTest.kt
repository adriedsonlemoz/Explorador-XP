package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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

    @Test
    fun zipReplacementPreservesExistingUntilSuccess() = runBlocking {
        val dir = Files.createTempDirectory("exploradorxp-zip-replace-").toFile()
        try {
            val target = File(dir, "documento.txt").apply { writeText("versao antiga") }
            val archive = File(dir, "entrada.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("documento.txt"))
                zip.write("versao nova".toByteArray())
                zip.closeEntry()
            }
            val result = ArchiveManager.extract(archive, dir, false, conflictMode = ArchiveConflictMode.REPLACE)
            assertTrue(result.exceptionOrNull()?.message.orEmpty(), result.isSuccess)
            assertEquals(0, result.getOrThrow().errors)
            assertEquals("versao nova", target.readText())
            assertTrue(dir.listFiles().orEmpty().none { it.name.startsWith(".xp-") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun extractionFailureWhileStreamingKeepsPreviousFile() = runBlocking {
        val dir = Files.createTempDirectory("exploradorxp-zip-failure-").toFile()
        try {
            val target = File(dir, "documento.txt").apply { writeText("conteudo original") }
            val archive = File(dir, "entrada.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("documento.txt"))
                zip.write(ByteArray(250_000) { (it % 251).toByte() })
                zip.closeEntry()
            }
            val result = ArchiveManager.extract(
                archive, dir, false, conflictMode = ArchiveConflictMode.REPLACE,
                onProgress = { progress ->
                    if (progress.extractedBytes > 0 && progress.completedEntries == 0) {
                        throw IllegalStateException("Falha de escrita simulada")
                    }
                },
            )
            // The callback may throw again when publishing the final progress; either way
            // the destination must remain untouched.
            assertEquals("conteudo original", target.readText())
            assertTrue(dir.listFiles().orEmpty().none { it.name.startsWith(".xp-stage-") })
            assertTrue(result.isFailure || result.getOrThrow().errors > 0)
        } finally {
            dir.deleteRecursively()
        }
    }

}
