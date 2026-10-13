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
        assertTrue(info.changes.any { it.contains("Mini-player") })
        assertTrue(info.changes.any { it.contains("MediaSessionService") })
        assertTrue(info.changes.any { it.contains("mesmo player", ignoreCase = true) })
        assertFalse(info.changes.any { it.contains("Dimensity 920") || it.contains("alpha.116") })
    }
}
