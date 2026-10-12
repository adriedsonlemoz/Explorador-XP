package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class TrashBarSummaryTest {
    @Test fun unavailableSpaceIsNotShownAsZero() {
        assertEquals("…", compactTrashSpace(null))
        assertEquals("0B", compactTrashSpace(0L))
    }

    @Test fun compactSizeFitsNarrowToolbar() {
        assertEquals("1B", compactTrashSpace(1L))
        assertEquals("1023B", compactTrashSpace(1023L))
        assertEquals("1,0K", compactTrashSpace(1024L))
        assertEquals("76K", compactTrashSpace(77_414L))
        assertEquals("1,0M", compactTrashSpace(1024L * 1024L))
        assertEquals("1,0G", compactTrashSpace(1024L * 1024L * 1024L))
    }

    @Test fun counterFitsInsideTheTrashIcon() {
        assertEquals("0", compactTrashCount(0))
        assertEquals("1", compactTrashCount(1))
        assertEquals("99", compactTrashCount(99))
        assertEquals("99+", compactTrashCount(100))
        assertEquals("99+", compactTrashCount(98765))
    }
}
