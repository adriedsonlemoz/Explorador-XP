package com.exploradorxp.app

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveZipAutoSaveTest {
    @Test fun createsXpFolderAndCopiesIdenticalBytes() = withStorage { root ->
        val source = File(root, "projeto.zip").apply { writeBytes(byteArrayOf(3, 1, 4, 1)) }
        val result = ArchiveZipAutoSave.save(source, downloads(root))
        assertEquals("projeto.zip", result.fileName)
        assertFalse(result.alreadyInFolder)
        assertArrayEquals(source.readBytes(), File(downloads(root), "XP/projeto.zip").readBytes())
    }

    @Test fun doesNotOverwriteExistingZip() = withStorage { root ->
        val source = File(root, "projeto.zip").apply { writeText("novo") }
        File(downloads(root), "XP").mkdirs()
        File(downloads(root), "XP/projeto.zip").writeText("antigo")
        val result = ArchiveZipAutoSave.save(source, downloads(root))
        assertEquals("projeto (1).zip", result.fileName)
        assertEquals("antigo", File(downloads(root), "XP/projeto.zip").readText())
        assertEquals("novo", File(downloads(root), "XP/projeto (1).zip").readText())
    }

    @Test fun doesntDuplicateFileAlreadyInXp() = withStorage { root ->
        val folder = File(downloads(root), "XP").apply { mkdirs() }
        val source = File(folder, "projeto.zip").apply { writeText("ok") }
        val result = ArchiveZipAutoSave.save(source, downloads(root))
        assertTrue(result.alreadyInFolder)
        assertEquals(listOf("projeto.zip"), folder.list()?.toList())
    }

    @Test fun removesPartialZipOnWriteFailure() = withStorage { root ->
        val source = File(root, "projeto.zip").apply { writeBytes(ByteArray(200_000) { 5 }) }
        try {
            ArchiveZipAutoSave.save(source, downloads(root)) { throw IOException("Falha de teste") }
            fail("A falha deveria ser propagada")
        } catch (error: IOException) {
            assertEquals("Falha de teste", error.message)
        }
        assertTrue(File(downloads(root), "XP").listFiles().orEmpty().isEmpty())
    }

    @Test fun addsZipExtensionWhenOriginalHasNone() = withStorage { root ->
        val source = File(root, "artifact123").apply { writeText("conteúdo") }
        val result = ArchiveZipAutoSave.save(source, downloads(root))
        assertEquals("artifact123.zip", result.fileName)
        assertEquals("conteúdo", File(downloads(root), "XP/artifact123.zip").readText())
    }

    @Test fun rejectsUnreadableSource() = withStorage { root ->
        try {
            ArchiveZipAutoSave.save(File(root, "ausente.zip"), downloads(root))
            fail("Deveria recusar arquivo inexistente")
        } catch (error: IOException) {
            assertTrue(error.message!!.contains("origem"))
        }
    }

    @Test fun createsDownloadsFolderWhenNotPresent() = withStorage { root ->
        val downloads = File(root, "Download")
        val source = File(root, "novo.zip").apply { writeText("conteudo") }
        val result = ArchiveZipAutoSave.save(source, downloads)
        assertEquals("novo.zip", result.fileName)
        assertEquals("conteudo", File(downloads, "XP/novo.zip").readText())
    }

    private fun downloads(root: File): File = File(root, "Download")

    private fun withStorage(block: (File) -> Unit) {
        val root = Files.createTempDirectory("xp-zip-save-").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
}
