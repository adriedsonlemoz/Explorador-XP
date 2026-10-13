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
        assertTrue(info.changes.any { it.contains("APK") && it.contains("ícones reais") })
        assertTrue(info.changes.any { it.contains("Lista compacta") })
        assertTrue(info.changes.any { it.contains("Exibir") && it.contains("classificação") })
        assertTrue(info.changes.any { it.contains("Aplicativos instalados") })
        assertFalse(info.changes.any { it.contains("SMB") || it.contains("WebDAV") || it.contains("alpha.109") })
    }
}
