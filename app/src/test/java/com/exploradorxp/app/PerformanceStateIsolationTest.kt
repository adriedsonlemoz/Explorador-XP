package com.exploradorxp.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceStateIsolationTest {
    @Test
    fun explorerUiStateDoesNotContainHighFrequencyProgressBuckets() {
        val names = ExplorerUiState::class.java.declaredFields.map { it.name }.toSet()

        assertFalse("transfer must stay outside ExplorerUiState", "transfer" in names)
        assertFalse("storageScan must stay outside ExplorerUiState", "storageScan" in names)
        assertFalse("trashItems must stay outside ExplorerUiState", "trashItems" in names)
        assertFalse("trashLoading must stay outside ExplorerUiState", "trashLoading" in names)
    }

    @Test
    fun trashStateComputesAvailabilityWithoutExplorerUiState() {
        val empty = TrashUiState()
        val populated = TrashUiState(
            items = listOf(
                TrashItem(
                    id = "1",
                    trashedFile = File("/tmp/item.txt"),
                    originalPath = "/Download/item.txt",
                    originalName = "item.txt",
                    deletedAt = 1L,
                    size = 10L,
                    isDirectory = false,
                    typeLabel = "Arquivo de texto",
                    iconRes = 1,
                )
            )
        )

        assertFalse(empty.hasItems)
        assertTrue(populated.hasItems)
    }
}
