package com.exploradorxp.app

import org.junit.Assert.*
import org.junit.Test

class FileRenameRulesTest {
    @Test fun regularAndCompoundExtensionsAreKept() {
        assertEquals("novo.jpg", FileRenameRules.parts("imagem.jpg").compose("novo"))
        assertEquals("backup.tar.gz", FileRenameRules.parts("antigo.tar.gz").compose("backup"))
        assertEquals("script.user.js", FileRenameRules.parts("original.user.js").compose("script"))
        assertEquals("novo.MP4", FileRenameRules.parts("video.MP4").compose("novo"))
    }
    @Test fun dotfilesDirectoriesAndBareNamesStayWhole() {
        assertEquals(".nomedia", FileRenameRules.parts(".nomedia").stem)
        assertEquals("", FileRenameRules.parts(".nomedia").extension)
        assertEquals("fotos.2026", FileRenameRules.parts("fotos.2025", isDirectory = true).compose("fotos.2026"))
        assertEquals("semext", FileRenameRules.parts("arquivo").compose("semext"))
    }
    @Test fun extensionEditMustBeExplicit() {
        assertEquals("clipe.mkv", FileRenameRules.parts("clipe.mp4").compose("clipe", "mkv"))
        assertEquals("clipe", FileRenameRules.parts("clipe.mp4").compose("clipe", ""))
    }
    @Test fun invalidNamesAreRejected() {
        listOf("", "  ", "..", ".", "x/y", "a\\b", " x", "x ", "a\u0000b", "a\nb").forEach {
            assertNotNull("Deveria recusar $it", FileRenameRules.validationError(it))
        }
        assertNull(FileRenameRules.validationError("Foto nova.png"))
    }
}
