package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypeClassifierTest {
    @Test
    fun extensionlessAndUnknownFilesHaveClearLabels() {
        assertEquals("Arquivo sem extensão", FileTypeClassifier.labelForExtension(""))
        assertEquals("Documento PDF", FileTypeClassifier.labelForExtension("pdf"))
        assertEquals("Arquivo XYZ", FileTypeClassifier.labelForExtension("xyz"))
    }
}
