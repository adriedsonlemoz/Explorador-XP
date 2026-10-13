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
        assertEquals(2, info.changes.size)
        assertTrue(info.changes.any { it.contains("fillMaxHeight") })
        assertTrue(info.changes.any { it.contains("importações Compose") })
        assertFalse(info.changes.any { it.contains("Mini-player") || it.contains("MediaSessionService") })
    }
}
