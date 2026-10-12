package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TrashQuickSummaryTest {
    @Test fun initialStateIsNotReportedAsEmptyBeforeLoading() {
        val state = TrashUiState()
        assertFalse(state.loaded)
        assertEquals(0L, state.totalBytes)
        assertFalse(state.hasItems)
    }

    @Test fun sizeAddsManagedEntriesIncludingDirectoriesWithoutDoubleCounting() {
        val state = TrashUiState(items = listOf(item("folder", 1024), item("file", 2048)), loaded = true)
        assertTrue(state.loaded)
        assertEquals(2, state.items.size)
        assertEquals(3072L, state.totalBytes)
    }

    @Test fun negativeCorruptMetadataDoesNotMakeNegativeSpace() {
        val state = TrashUiState(items = listOf(item("damaged", -5), item("valid", 100)), loaded = true)
        assertEquals(100L, state.totalBytes)
    }

    private fun item(id: String, size: Long) = TrashItem(
        id = id,
        trashedFile = File("/tmp/$id"),
        originalPath = "/storage/emulated/0/$id",
        originalName = id,
        deletedAt = 1_700_000_000_000L,
        size = size,
        isDirectory = false,
        typeLabel = "Arquivo",
        iconRes = 0,
    )
}
