package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferStateTest {
    @Test
    fun overallProgressPrefersBytesWhenAvailable() {
        val state = TransferState(
            kind = TransferKind.COPY,
            done = 1,
            total = 4,
            currentName = "arquivo.bin",
            bytesDone = 50L,
            bytesTotal = 100L,
        )
        assertEquals(0.5f, state.fraction, 0.0001f)
    }

    @Test
    fun currentItemProgressIsIndependentFromOverallProgress() {
        val state = TransferState(
            kind = TransferKind.MOVE,
            done = 8,
            total = 20,
            currentName = "foto.jpg",
            currentItemDone = 2,
            currentItemTotal = 4,
            currentItemBytesDone = 25L,
            currentItemBytesTotal = 100L,
            queueItems = listOf(
                TransferQueueItem("/a", "a", TransferQueueItemStatus.DONE),
                TransferQueueItem("/b", "b", TransferQueueItemStatus.RUNNING),
            ),
        )
        assertEquals(0.25f, state.currentItemFraction, 0.0001f)
        assertEquals(2, state.queueItems.size)
    }
}
