package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SafeFileCommitTest {
    @Test
    fun replacesFileOnlyAfterCompleteStageIsPresent() {
        val dir = Files.createTempDirectory("exploradorxp-safe-replace-").toFile()
        try {
            val target = File(dir, "foto.txt").apply { writeText("original") }
            val stage = temporarySibling(target, "stage").apply { writeText("novo conteudo") }
            assertEquals("original", target.readText())
            commitStagedItem(stage, target)
            assertEquals("novo conteudo", target.readText())
            assertFalse(stage.exists())
            assertFalse(dir.listFiles().orEmpty().any { it.name.startsWith(".xp-") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun nonexistentOrFailedStageDoesNotDamageOldContent() {
        val dir = Files.createTempDirectory("exploradorxp-failed-stage-").toFile()
        try {
            val target = File(dir, "importante.txt").apply { writeText("nao remover") }
            val stage = temporarySibling(target, "stage")
            val failure = runCatching { commitStagedItem(stage, target) }
            assertTrue(failure.isFailure)
            assertEquals("nao remover", target.readText())
            assertTrue(dir.listFiles().orEmpty().none { it.name.startsWith(".xp-backup-") })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun replacesDirectoryWithFileAndPreservesTheFileWhenStagingFails() {
        val dir = Files.createTempDirectory("exploradorxp-dir-conflict-").toFile()
        try {
            val target = File(dir, "item").apply { mkdirs() }
            File(target, "inside.txt").writeText("antigo")
            val stage = temporarySibling(target, "stage").apply { writeText("novo") }
            commitStagedItem(stage, target)
            assertTrue(target.isFile)
            assertEquals("novo", target.readText())
            assertFalse(File(target, "inside.txt").exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
