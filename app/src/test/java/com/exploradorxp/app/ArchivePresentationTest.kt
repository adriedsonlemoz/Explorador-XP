package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchivePresentationTest {
    @Test
    fun compressionMethodCollapsesEqualMethodsAndHumanizesNames() {
        val info = ZipArchiveInfo(
            entries = listOf(
                ArchiveEntryInfo("a.txt", false, 100, 50, 0, "DEFLATE", false),
                ArchiveEntryInfo("b.txt", false, 100, 50, 0, "DEFLATE", false),
                ArchiveEntryInfo("folder/", true, 0, 0, 0, "STORE", false),
            ),
            encrypted = false,
            splitArchive = false,
            validHeaders = true,
            compressedBytes = 100,
            uncompressedBytes = 200,
        )
        assertEquals("Deflate", archiveCompressionMethod(info))
    }

    @Test
    fun compressionMethodShowsMixedArchiveWithoutGuessing() {
        val info = ZipArchiveInfo(
            entries = listOf(
                ArchiveEntryInfo("a.txt", false, 100, 100, 0, "STORE", false),
                ArchiveEntryInfo("b.txt", false, 100, 50, 0, "DEFLATE", false),
            ),
            encrypted = false,
            splitArchive = false,
            validHeaders = true,
            compressedBytes = 150,
            uncompressedBytes = 200,
        )
        assertEquals("Sem compressão + Deflate", archiveCompressionMethod(info))
    }
}
