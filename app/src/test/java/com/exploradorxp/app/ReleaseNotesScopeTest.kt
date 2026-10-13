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
        assertEquals(6, info.changes.size)
        assertTrue(info.changes.any { it.contains("Fotos e Vídeos") && it.contains("pastas") })
        assertTrue(info.changes.any { it.contains("Voltar") && it.contains("Avançar") })
        assertTrue(info.changes.any { it.contains("Seleção total") })
        assertTrue(info.changes.any { it.contains("Aplicativos instalados") && it.contains("compacta") })
        assertTrue(info.changes.any { it.contains("Painel de Ferramentas") })
        assertTrue(info.changes.any { it.contains("ReleaseNotesScopeTest") })
        assertFalse(info.changes.any { it.contains("SMB") || it.contains("WebDAV") || it.contains("alpha.109") })
    }
}
