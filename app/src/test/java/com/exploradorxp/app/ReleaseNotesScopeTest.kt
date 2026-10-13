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
        assertEquals(1, info.changes.size)
        assertTrue(info.changes.single().contains("horizontalScroll"))
        assertTrue(info.changes.single().contains("108"))
        assertFalse(info.changes.any { it.contains("workflow Gerar APK 107") || it.contains("alpha.112") })
    }
}
