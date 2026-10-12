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
        assertEquals(4, info.changes.size)
        assertTrue(info.changes.any { it.contains("Ferramentas") && it.contains("grade") })
        assertTrue(info.changes.any { it.contains("Aplicativos") })
        assertTrue(info.changes.any { it.contains("Lixeira") })
        assertFalse(info.changes.any { it.contains("SMB") || it.contains("WebDAV") || it.contains("alpha.109") })
    }
}
