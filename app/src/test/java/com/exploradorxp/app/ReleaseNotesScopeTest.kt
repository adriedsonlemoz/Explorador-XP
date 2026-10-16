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
        assertTrue(info.title.isNotBlank())
        assertTrue("A tela de novidades precisa conter alterações.", info.changes.isNotEmpty())
        assertTrue("A tela de novidades deve ser compacta.", info.changes.size <= 5)
        assertEquals("Novidades não podem estar duplicadas.", info.changes.size, info.changes.toSet().size)
        assertTrue("Itens vazios ou longos demais.", info.changes.all {
            it.isNotBlank() && it == it.trim() && it.length <= 280
        })
        // Não exigir frases de uma versão específica: isso bloqueava o próximo release.
        // Menções a números de versões antigas também não são novidades da versão atual.
        val versionReference = Regex("""(?i)\balpha\.\d+\b""")
        assertFalse("A tela de novidades não deve conter o histórico de versões.",
            info.changes.any { versionReference.containsMatchIn(it) })
    }
}
