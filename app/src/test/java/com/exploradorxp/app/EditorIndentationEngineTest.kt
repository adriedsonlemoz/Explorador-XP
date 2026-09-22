package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorIndentationEngineTest {
    @Test
    fun `indenta todas as linhas selecionadas`() {
        assertEquals(
            "    primeira\n    segunda",
            EditorIndentationEngine.transformBlock("primeira\nsegunda", "    ", 4, outdent = false),
        )
    }

    @Test
    fun `recua espaços respeitando um nivel`() {
        assertEquals(
            "primeira\n  segunda\nterceira",
            EditorIndentationEngine.transformBlock("    primeira\n      segunda\nterceira", "    ", 4, outdent = true),
        )
    }

    @Test
    fun `recua tab mesmo quando preferencias usam espacos`() {
        assertEquals("valor", EditorIndentationEngine.removeOneLevel("\tvalor", 4))
    }
}
