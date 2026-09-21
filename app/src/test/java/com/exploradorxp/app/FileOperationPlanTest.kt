package com.exploradorxp.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileOperationPlanTest {
    @Test
    fun planCopiesAndDeletesWithoutASecondTreeWalk() = runBlocking {
        val temp = Files.createTempDirectory("exploradorxp-plan").toFile()
        try {
            val source = File(temp, "origem").apply { mkdirs() }
            val nested = File(source, "sub/pasta").apply { mkdirs() }
            File(source, "a.txt").writeText("abc")
            File(nested, "b.bin").writeBytes(byteArrayOf(1, 2, 3, 4, 5))

            val plan = buildFileOperationPlan(source)
            assertEquals(5, plan.entryCount) // raiz + sub + pasta + 2 arquivos
            assertEquals(8L, plan.totalBytes)
            assertTrue(plan.entries.first().relativePath.isEmpty())
            assertTrue(plan.entries.any { it.relativePath.endsWith("a.txt") })
            assertTrue(plan.entries.any { it.relativePath.endsWith("b.bin") })

            val target = File(temp, "destino")
            var copied = 0
            copyFileOperationPlan(plan, target) { copied++ }
            assertEquals(plan.entryCount, copied)
            assertEquals("abc", File(target, "a.txt").readText())
            assertEquals(5L, File(target, "sub/pasta/b.bin").length())

            var deleted = 0
            assertTrue(deleteFileOperationPlan(plan) { deleted++ })
            assertEquals(plan.entryCount, deleted)
            assertFalse(source.exists())
            assertTrue(target.exists())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun emptyDirectoryStillCountsAsOneProgressEntry() = runBlocking {
        val temp = Files.createTempDirectory("exploradorxp-empty-plan").toFile()
        try {
            val source = File(temp, "vazia").apply { mkdirs() }
            val plan = buildFileOperationPlan(source)
            assertEquals(1, plan.entryCount)
            assertEquals(0L, plan.totalBytes)
        } finally {
            temp.deleteRecursively()
        }
    }
}
