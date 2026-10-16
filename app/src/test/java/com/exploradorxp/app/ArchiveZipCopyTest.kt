package com.exploradorxp.app

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ArchiveZipCopyTest {
    @Test fun preservesBytesWithoutRecompressing() {
        val original = ByteArray(200_000) { (it * 17).toByte() }
        val saved = ByteArrayOutputStream()
        val progress = mutableListOf<Long>()
        assertEquals(original.size.toLong(), ArchiveZipCopy.copy(ByteArrayInputStream(original), saved, original.size.toLong()) { progress += it })
        assertArrayEquals(original, saved.toByteArray())
        assertEquals(original.size.toLong(), progress.last())
    }

    @Test fun rejectsTruncatedInput() {
        try {
            ArchiveZipCopy.copy(ByteArrayInputStream(byteArrayOf(1, 2)), ByteArrayOutputStream(), 10)
            fail("Cópia incompleta deveria falhar")
        } catch (_: IOException) { /* esperado */ }
    }

    @Test fun rejectsSourceGrowingDuringCopy() {
        try {
            ArchiveZipCopy.copy(ByteArrayInputStream(byteArrayOf(1, 2, 3)), ByteArrayOutputStream(), 2)
            fail("Cópia de origem modificada deveria falhar")
        } catch (_: IOException) { /* esperado */ }
    }

    @Test fun propagatesDestinationFailure() {
        val brokenOutput = object : OutputStream() {
            override fun write(b: Int) { throw IOException("Sem espaço") }
            override fun write(b: ByteArray, off: Int, len: Int) { throw IOException("Sem espaço") }
        }
        try {
            ArchiveZipCopy.copy(ByteArrayInputStream(byteArrayOf(1)), brokenOutput, 1)
            fail("Falha de escrita deveria propagar")
        } catch (error: IOException) {
            assertEquals("Sem espaço", error.message)
        }
    }
}
