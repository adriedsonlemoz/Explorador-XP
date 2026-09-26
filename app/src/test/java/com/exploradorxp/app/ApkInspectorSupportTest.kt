package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkInspectorSupportTest {
    @Test
    fun comparesVersions() {
        assertEquals(ApkVersionRelation.NOT_INSTALLED, apkVersionRelation(10, null))
        assertEquals(ApkVersionRelation.UPGRADE, apkVersionRelation(11, 10))
        assertEquals(ApkVersionRelation.SAME, apkVersionRelation(10, 10))
        assertEquals(ApkVersionRelation.DOWNGRADE, apkVersionRelation(9, 10))
    }

    @Test
    fun comparesSignerDigestsByCommonCertificate() {
        assertEquals(ApkSignatureRelation.NOT_APPLICABLE, apkSignatureRelation(listOf("A"), null))
        assertEquals(ApkSignatureRelation.UNKNOWN, apkSignatureRelation(emptyList(), listOf("A")))
        assertEquals(ApkSignatureRelation.MATCH, apkSignatureRelation(listOf("A", "B"), listOf("B")))
        assertEquals(ApkSignatureRelation.MISMATCH, apkSignatureRelation(listOf("A"), listOf("B")))
    }

    @Test
    fun readsArchitecturesFromApkZip() {
        val apk = File.createTempFile("apk-inspector", ".apk")
        try {
            ZipOutputStream(apk.outputStream()).use { out ->
                listOf(
                    "AndroidManifest.xml",
                    "lib/armeabi-v7a/libsample.so",
                    "lib/arm64-v8a/libsample.so",
                    "lib/arm64-v8a/libsecond.so",
                    "assets/readme.txt",
                ).forEach { name ->
                    out.putNextEntry(ZipEntry(name))
                    out.write(byteArrayOf(1, 2, 3))
                    out.closeEntry()
                }
            }
            assertEquals(listOf("arm64-v8a", "armeabi-v7a"), collectApkAbis(apk))
            assertTrue(isApkAbiCompatible(collectApkAbis(apk), listOf("arm64-v8a")))
            assertFalse(isApkAbiCompatible(collectApkAbis(apk), listOf("x86_64")))
        } finally {
            apk.delete()
        }
    }

    @Test
    fun noNativeLibrariesMeansUniversalForCpuCheck() {
        assertTrue(isApkAbiCompatible(emptyList(), listOf("arm64-v8a")))
        assertEquals("Sem bibliotecas nativas", formatAbiList(emptyList()))
    }

    @Test
    fun sha256FormattingIsStable() {
        val digest = sha256Hex("abc".toByteArray())
        assertEquals("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD", digest)
        assertTrue(compactSha256(digest).startsWith("BA:78:16:BF"))
    }

    @Test
    fun sha256FileHashesFileContents() {
        val file = File.createTempFile("apk-sha256", ".bin")
        try {
            file.writeText("abc")
            assertEquals(
                "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
                sha256ApkFile(file),
            )
        } finally {
            file.delete()
        }
    }
}
