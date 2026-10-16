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
        assertTrue(info.changes.any { it.contains("Salvar ZIP") })
        assertTrue(info.changes.any { it.contains("duas linhas") })
        assertTrue(info.changes.any { it.contains("streaming") })
        assertFalse(info.changes.any { it.contains("ProduceState") || it.contains("fillMaxHeight") })
    }
}
