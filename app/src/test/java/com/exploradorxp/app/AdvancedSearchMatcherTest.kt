package com.exploradorxp.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AdvancedSearchMatcherTest {
    @Test
    fun matchesNameExtensionAndSizeWithoutGuessing() {
        val filters = AdvancedSearchFilters(
            itemKind = SearchItemKind.FILES,
            extension = ".PDF",
            minSizeBytes = 1_000L,
            maxSizeBytes = 10_000L,
        )

        assertTrue(
            AdvancedSearchMatcher.matches(
                name = "Relatorio Final.pdf",
                extension = "pdf",
                isDirectory = false,
                size = 5_000L,
                modifiedAt = 1_700_000_000_000L,
                query = "relatorio",
                filters = filters,
            )
        )
        assertFalse(
            AdvancedSearchMatcher.matches(
                name = "Relatorio Final.docx",
                extension = "docx",
                isDirectory = false,
                size = 5_000L,
                modifiedAt = 1_700_000_000_000L,
                query = "relatorio",
                filters = filters,
            )
        )
    }

    @Test
    fun categoryFilterSeparatesImagesFromOtherFiles() {
        val filters = AdvancedSearchFilters(fileType = SearchFileType.IMAGES)
        assertTrue(AdvancedSearchMatcher.matches("foto.jpg", "jpg", false, 10, 1, "", filters))
        assertFalse(AdvancedSearchMatcher.matches("video.mp4", "mp4", false, 10, 1, "", filters))
        assertFalse(AdvancedSearchMatcher.matches("Fotos", "", true, 0, 1, "", filters))
    }

    @Test
    fun folderFilterDoesNotApplyFileSizeToDirectories() {
        val folders = AdvancedSearchFilters(itemKind = SearchItemKind.FOLDERS)
        assertTrue(AdvancedSearchMatcher.matches("Documentos", "", true, 0, 1, "doc", folders))

        val foldersWithSize = folders.copy(minSizeBytes = 1)
        assertFalse(AdvancedSearchMatcher.matches("Documentos", "", true, 0, 1, "doc", foldersWithSize))
    }

    @Test
    fun todayCutoffUsesStartOfLocalDay() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = Instant.parse("2026-09-22T15:00:00Z").toEpochMilli()
        val cutoff = AdvancedSearchMatcher.dateCutoffMillis(SearchDateRange.TODAY, now, zone)!!
        val before = cutoff - 1
        val after = cutoff + 1
        val filters = AdvancedSearchFilters(dateRange = SearchDateRange.TODAY)

        assertFalse(AdvancedSearchMatcher.matches("old.txt", "txt", false, 1, before, "", filters, now, zone))
        assertTrue(AdvancedSearchMatcher.matches("new.txt", "txt", false, 1, after, "", filters, now, zone))
    }


    @Test
    fun extensionFilterSupportsCompoundSuffixes() {
        val filters = AdvancedSearchFilters(extension = ".tar.gz")
        assertTrue(AdvancedSearchMatcher.matches("backup.tar.gz", "gz", false, 10, 1, "", filters))
        assertFalse(AdvancedSearchMatcher.matches("backup.gz", "gz", false, 10, 1, "", filters))
    }

    @Test
    fun extensionNormalizationAcceptsLeadingDot() {
        assertTrue(AdvancedSearchMatcher.normalizeExtension(" .JPG ") == "jpg")
    }
}
