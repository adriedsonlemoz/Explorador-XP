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
        assertTrue(info.changes.any { it.contains("sete atalhos") && it.contains("Início") })
        assertTrue(info.changes.any { it.contains("filtros compactos") })
        assertTrue(info.changes.any { it.contains("Verificação preventiva") })
        assertFalse(info.changes.any { it.contains("ProduceState") || it.contains("fillMaxHeight") })
    }
}
