package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorSelectionRulesTest {
    @Test fun selectsIdentifierAtCursor() {
        assertEquals(4..11, EditorSelectionRules.wordRange("val meu_nome = 1", 7))
    }
    @Test fun selectsIdentifierAtItsEnd() {
        assertEquals(4..11, EditorSelectionRules.wordRange("val meu_nome = 1", 11))
    }
    @Test fun selectsPunctuation() {
        assertEquals(3..3, EditorSelectionRules.wordRange("foo(bar)", 3))
    }
    @Test fun ignoresWhitespaceOrEmptyContent() {
        assertNull(EditorSelectionRules.wordRange("  ", 1))
        assertNull(EditorSelectionRules.wordRange("", 0))
    }
    @Test fun clampsCursorAndDoesNotCrossSpaces() {
        assertEquals(0..2, EditorSelectionRules.wordRange("abc def", -10))
        assertEquals(4..6, EditorSelectionRules.wordRange("abc def", 200))
    }
}
