package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.charset.Charset

class LargeTextFileEngineTest {
    @Test
    fun windowsKeepLargeFileOnDiskAndNavigateByLine() {
        withTempDir { dir ->
            val file = File(dir, "large.txt")
            file.bufferedWriter(Charsets.UTF_8).use { out ->
                for (i in 1..35_000) out.write("linha-$i TOKEN_$i abcdefghijklmnopqrstuvwxyz\n")
            }
            assertTrue(file.length() > 750_000L)

            val first = LargeTextFileEngine.loadInitial(file, Charsets.UTF_8, byteArrayOf())
            assertTrue(first.hasNext)
            assertTrue(first.endByte - first.startByte < 450_000L)
            assertEquals(1, first.firstLine)

            val next = LargeTextFileEngine.loadNext(file, first, Charsets.UTF_8, byteArrayOf())!!
            assertEquals(first.endByte, next.startByte)
            assertEquals(1 + first.text.count { it == '\n' }, next.firstLine)

            val previous = LargeTextFileEngine.loadPrevious(file, next, Charsets.UTF_8, byteArrayOf())!!
            assertEquals(next.startByte, previous.endByte)
        }
    }

    @Test
    fun globalSearchAndGoToLineDoNotNeedWholeDocumentString() {
        withTempDir { dir ->
            val file = File(dir, "search.log")
            file.bufferedWriter(Charsets.UTF_8).use { out ->
                for (i in 1..40_000) out.write("evento-$i payload-$i\n")
            }
            val result = LargeTextFileEngine.search(
                file = file,
                query = "payload-32123",
                currentLine = 1,
                currentColumn = 0,
                forward = true,
                charset = Charsets.UTF_8,
                bom = byteArrayOf(),
            )
            assertEquals(1, result.total)
            assertEquals(32123, result.match?.line)

            val window = LargeTextFileEngine.loadAtLine(file, 32123, Charsets.UTF_8, byteArrayOf())!!
            assertEquals(32123, window.firstLine)
            assertTrue(window.text.startsWith("evento-32123"))
        }
    }

    @Test
    fun patchInPlaceChangesOnlyLoadedWindowAndPreservesTail() {
        withTempDir { dir ->
            val file = File(dir, "patch.txt")
            file.bufferedWriter(Charsets.UTF_8).use { out ->
                for (i in 1..45_000) out.write("linha-$i TOKEN_$i conteúdo\n")
            }
            val first = LargeTextFileEngine.loadInitial(file, Charsets.UTF_8, byteArrayOf())
            val edited = first.text.replace("TOKEN_100 ", "TOKEN_EDITADO_100 ")
            val patched = LargeTextFileEngine.patchInPlace(
                file = file,
                window = first,
                editedText = edited,
                charset = Charsets.UTF_8,
                bom = byteArrayOf(),
                preferredLineEnding = "\n",
            )
            assertEquals(file.length(), patched.fileSizeBytes)
            assertEquals(1, LargeTextFileEngine.search(file, "TOKEN_EDITADO_100 ", 1, 0, true, Charsets.UTF_8, byteArrayOf()).total)
            assertEquals(1, LargeTextFileEngine.search(file, "TOKEN_44999", 1, 0, true, Charsets.UTF_8, byteArrayOf()).total)
        }
    }

    @Test
    fun giantSingleLineStillUsesBoundedWindows() {
        withTempDir { dir ->
            val file = File(dir, "single.json")
            file.outputStream().buffered().use { out ->
                out.write("{\"data\":\"".toByteArray())
                val block = "a".repeat(32 * 1024).toByteArray()
                repeat(40) { out.write(block) }
                out.write("MARKER_END\"}".toByteArray())
            }
            val first = LargeTextFileEngine.loadInitial(file, Charsets.UTF_8, byteArrayOf())
            assertTrue(first.endByte - first.startByte <= 450_000L)
            val next = LargeTextFileEngine.loadNext(file, first, Charsets.UTF_8, byteArrayOf())!!
            assertEquals(1, next.firstLine)
            assertEquals(first.firstColumn + first.text.length, next.firstColumn)
            assertFalse(next.text.isEmpty())
        }
    }

    @Test
    fun utf16BomSearchAndLineNavigationRemainSupported() {
        withTempDir { dir ->
            val file = File(dir, "utf16.txt")
            val bom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
            file.outputStream().use { raw ->
                raw.write(bom)
                raw.writer(Charset.forName("UTF-16LE")).use { out ->
                    for (i in 1..20_000) out.write("linha16-$i conteúdo-$i\r\n")
                }
            }
            val charset = Charset.forName("UTF-16LE")
            val result = LargeTextFileEngine.search(file, "conteúdo-15000", 1, 0, true, charset, bom)
            assertEquals(15000, result.match?.line)
            val window = LargeTextFileEngine.loadAtLine(file, 15000, charset, bom)!!
            assertEquals(15000, window.firstLine)
            assertTrue(window.text.startsWith("linha16-15000"))
        }
    }

    private inline fun withTempDir(block: (File) -> Unit) {
        val dir = kotlin.io.path.createTempDirectory("exploradorxp-large-test").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }
}
