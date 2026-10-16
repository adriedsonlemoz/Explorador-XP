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
        val result = ArchiveZipAutoSave.save(source, root)
        assertEquals("projeto.zip", result.fileName)
        assertFalse(result.alreadyInFolder)
        assertArrayEquals(source.readBytes(), File(root, "XP/projeto.zip").readBytes())
    }

    @Test fun doesNotOverwriteExistingZip() = withStorage { root ->
        val source = File(root, "projeto.zip").apply { writeText("novo") }
        File(root, "XP").mkdirs()
        File(root, "XP/projeto.zip").writeText("antigo")
        val result = ArchiveZipAutoSave.save(source, root)
        assertEquals("projeto (1).zip", result.fileName)
        assertEquals("antigo", File(root, "XP/projeto.zip").readText())
        assertEquals("novo", File(root, "XP/projeto (1).zip").readText())
    }

    @Test fun doesntDuplicateFileAlreadyInXp() = withStorage { root ->
        val folder = File(root, "XP").apply { mkdirs() }
        val source = File(folder, "projeto.zip").apply { writeText("ok") }
        val result = ArchiveZipAutoSave.save(source, root)
        assertTrue(result.alreadyInFolder)
        assertEquals(listOf("projeto.zip"), folder.list()?.toList())
    }

    @Test fun removesPartialZipOnWriteFailure() = withStorage { root ->
        val source = File(root, "projeto.zip").apply { writeBytes(ByteArray(200_000) { 5 }) }
        try {
            ArchiveZipAutoSave.save(source, root) { throw IOException("Falha de teste") }
            fail("A falha deveria ser propagada")
        } catch (error: IOException) {
            assertEquals("Falha de teste", error.message)
        }
        assertTrue(File(root, "XP").listFiles().orEmpty().isEmpty())
    }

    @Test fun addsZipExtensionWhenOriginalHasNone() = withStorage { root ->
        val source = File(root, "artifact123").apply { writeText("conteúdo") }
        val result = ArchiveZipAutoSave.save(source, root)
        assertEquals("artifact123.zip", result.fileName)
        assertEquals("conteúdo", File(root, "XP/artifact123.zip").readText())
    }

    @Test fun rejectsUnreadableSource() = withStorage { root ->
        try {
            ArchiveZipAutoSave.save(File(root, "ausente.zip"), root)
            fail("Deveria recusar arquivo inexistente")
        } catch (error: IOException) {
            assertTrue(error.message!!.contains("origem"))
        }
    }

    private fun withStorage(block: (File) -> Unit) {
        val root = Files.createTempDirectory("xp-zip-save-").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
}
