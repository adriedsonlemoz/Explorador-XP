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
        assertTrue(info.changes.any { it.contains("ZIP") })
        assertTrue(info.changes.any { it.contains("Lixeira") })
        assertTrue(info.changes.any { it.contains("Dimensity 920") })
        assertFalse(info.changes.any { it.contains("workflow Gerar APK 108") || it.contains("alpha.112") })
    }
}
