package com.exploradorxp.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
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
            var copiedBytes = 0L
            copyFileOperationPlan(plan, target) { _, bytesDelta, entryCompleted ->
                copiedBytes += bytesDelta
                if (entryCompleted) copied++
            }
            assertEquals(plan.entryCount, copied)
            assertEquals(plan.totalBytes, copiedBytes)
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
    fun copyCanPauseBetweenBlocksAndResumeWithoutRestarting() = runBlocking {
        val temp = Files.createTempDirectory("exploradorxp-pause-plan").toFile()
        try {
            val source = File(temp, "large.bin")
            source.writeBytes(ByteArray(900_000) { index -> (index % 251).toByte() })
            val plan = buildFileOperationPlan(source)
            val target = File(temp, "copy.bin")
            val resume = CompletableDeferred<Unit>()
            var shouldPause = false
            var pauseObserved = false

            val job = async {
                copyFileOperationPlan(
                    plan = plan,
                    targetRoot = target,
                    awaitIfPaused = {
                        if (shouldPause && !resume.isCompleted) {
                            pauseObserved = true
                            resume.await()
                        }
                    },
                    onProgress = { _, bytesDelta, _ ->
                        if (bytesDelta > 0L && !shouldPause) shouldPause = true
                    },
                )
            }

            withTimeout(2_000) {
                while (!pauseObserved) yield()
            }
            val pausedLength = target.length()
            delay(40)
            assertFalse(job.isCompleted)
            assertEquals(pausedLength, target.length())

            resume.complete(Unit)
            job.await()
            assertEquals(source.length(), target.length())
            assertTrue(source.readBytes().contentEquals(target.readBytes()))
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
