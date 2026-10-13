package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesScopeTest {
    @Test fun updateScreenShowsOnlyChangesFromTheCurrentBuild() {
        val info = ReleaseNotes.current
        assertEquals(BuildConfig.VERSION_NAME, info.versionName)
        assertEquals(BuildConfig.VERSION_CODE, info.versionCode)
        assertEquals(3, info.changes.size)
        assertTrue(info.changes.any { it.contains("abas") && it.contains("OBB") })
        assertTrue(info.changes.any { it.contains("aplicativos") && it.contains("parciais") })
        assertTrue(info.changes.any { it.contains("ZIP") && it.contains("GitHub Manager") })
        assertFalse(info.changes.any { it.contains("ProduceState") || it.contains("fillMaxHeight") })
    }
}
