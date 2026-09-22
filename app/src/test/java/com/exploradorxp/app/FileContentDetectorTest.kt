package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileContentDetectorTest {
    @Test
    fun extensionlessZipIsDetectedByContent() {
        val dir = Files.createTempDirectory("xp-detector").toFile()
        try {
            val file = dir.resolve("EditaAi-0.1.0-alpha.3")
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("EditaAi-0.1.0-alpha.3.apk"))
                zip.write(byteArrayOf(1, 2, 3, 4))
                zip.closeEntry()
            }
            assertEquals("zip", FileContentDetector.detectExtension(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun extensionlessApkIsDistinguishedFromGenericZip() {
        val dir = Files.createTempDirectory("xp-detector-apk").toFile()
        try {
            val file = dir.resolve("app-sem-extensao")
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write(byteArrayOf(3, 0, 8, 0))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("classes.dex"))
                zip.write("dex\n035\u0000".toByteArray())
                zip.closeEntry()
            }
            assertEquals("apk", FileContentDetector.detectExtension(file))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun commonExtensionlessFormatsAreDetected() {
        val dir = Files.createTempDirectory("xp-detector-common").toFile()
        try {
            val pdf = dir.resolve("documento").apply { writeBytes("%PDF-1.7\n".toByteArray()) }
            val png = dir.resolve("imagem").apply {
                writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3))
            }
            val text = dir.resolve("configuracao").apply {
                writeText("linha 1\nlinha 2\nchave=valor\n")
            }
            val binary = dir.resolve("binario").apply {
                writeBytes(byteArrayOf(1, 0, 2, 0, 3, 0, 4, 0))
            }

            assertEquals("pdf", FileContentDetector.detectExtension(pdf))
            assertEquals("png", FileContentDetector.detectExtension(png))
            assertEquals("txt", FileContentDetector.detectExtension(text))
            assertNull(FileContentDetector.detectExtension(binary))
        } finally {
            dir.deleteRecursively()
        }
    }
}
