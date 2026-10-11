package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files

class ArchiveResourceLimitsTest {
    @Test
    fun securityLimitsAreDefinedInBytes() {
        assertEquals(20_000, MAX_ZIP_ENTRIES)
        assertEquals(8L * 1024L * 1024L * 1024L, MAX_EXTRACTED_BYTES)
        assertEquals(512L * 1024L * 1024L, MAX_PREVIEW_BYTES)
        assertEquals(4L * 1024L * 1024L * 1024L, MAX_EXTERNAL_OPEN_CACHE_BYTES)
    }

    @Test
    fun cleanupPreservesCurrentPreviewAndRemovesExpiredCache() {
        val root = Files.createTempDirectory("xp-preview-ttl-").toFile()
        try {
            val current = File(root, "atual").apply { mkdirs() }
            val preserved = File(current, "documento.txt").apply { writeText("em uso") }
            val old = File(File(root, "antigo").apply { mkdirs() }, "arquivo.txt").apply {
                writeText("expirado")
                setLastModified(System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000)
            }
            trimArchivePreviewCache(root, "atual", "documento.txt")
            assertTrue(preserved.exists())
            assertFalse(old.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun cleanupEnforcesTotalSizeWithoutDeletingTheCurrentFile() {
        val root = Files.createTempDirectory("xp-preview-cap-").toFile()
        try {
            val current = File(root, "atual").apply { mkdirs() }
            val protected = File(current, "aberto.bin")
            val expiredCandidate = File(File(root, "outro").apply { mkdirs() }, "antigo.bin")
            // Arquivos esparsos: testam o tamanho lógico sem consumir mais de 1 GiB em disco.
            RandomAccessFile(protected, "rw").use { it.setLength(600L * 1024 * 1024) }
            RandomAccessFile(expiredCandidate, "rw").use { it.setLength(600L * 1024 * 1024) }
            expiredCandidate.setLastModified(System.currentTimeMillis() - 60_000L)
            trimArchivePreviewCache(root, "atual", "aberto.bin")
            assertTrue(protected.exists())
            assertFalse(expiredCandidate.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun recentlyActivePreviewStagingIsNotDeleted() {
        val root = Files.createTempDirectory("xp-preview-staging-").toFile()
        try {
            val inProgress = File(File(root, "atual").apply { mkdirs() }, ".arquivo.abrindo-123")
                .apply { writeText("parcial") }
            trimArchivePreviewCache(root, "atual")
            assertTrue(inProgress.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
