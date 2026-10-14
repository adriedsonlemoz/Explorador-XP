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
        assertTrue(info.changes.any { it.contains("chips") && it.contains("ícones") })
        assertTrue(info.changes.any { it.contains("Usuário") && it.contains("Sistema") })
        assertTrue(info.changes.any { it.contains("Início") && it.contains("acessíveis") })
        assertFalse(info.changes.any { it.contains("ProduceState") || it.contains("fillMaxHeight") })
    }
}
