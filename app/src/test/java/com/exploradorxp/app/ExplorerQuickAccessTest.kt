package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExplorerQuickAccessTest {
    @Test fun bottomBarKeepsSevenShortcutsWithHomeCentered() {
        assertEquals(listOf(
            ExplorerQuickDestination.PHOTOS,
            ExplorerQuickDestination.VIDEOS,
            ExplorerQuickDestination.DOWNLOADS,
            ExplorerQuickDestination.HOME,
            ExplorerQuickDestination.NETWORK,
            ExplorerQuickDestination.TRASH,
            ExplorerQuickDestination.TOOLS,
        ), ExplorerQuickDestination.values().toList())
    }

    @Test fun mediaIndexIgnoresPrivateTrashEntries() {
        assertTrue(MediaShortcutIndex.isPrivateExploradorCache(File("/storage/emulated/0/.ExploradorXP_Lixeira/example.mp4")))
        assertTrue(MediaShortcutIndex.isPrivateExploradorCache(File("/sdcard/DCIM/.exploradorxp_lixeira/picture.jpg")))
        assertFalse(MediaShortcutIndex.isPrivateExploradorCache(File("/storage/emulated/0/Movies/clip.mp4")))
        assertFalse(MediaShortcutIndex.isPrivateExploradorCache(File("/storage/emulated/0/DCIM/picture.jpg")))
    }
}
