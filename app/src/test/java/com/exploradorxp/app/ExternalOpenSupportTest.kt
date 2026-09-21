package com.exploradorxp.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalOpenSupportTest {
    @Test
    fun compatibleFilesAreAccepted() {
        assertTrue(ExternalOpenSupport.isCompatible("backup.zip", "application/zip"))
        assertTrue(ExternalOpenSupport.isCompatible("README.md", "text/plain"))
        assertTrue(ExternalOpenSupport.isCompatible("dados.json", "application/json"))
        assertTrue(ExternalOpenSupport.isCompatible("video.mkv", "video/x-matroska"))
        assertTrue(ExternalOpenSupport.isCompatible("sem-extensao", "text/plain"))
    }

    @Test
    fun unsupportedBinaryFilesAreRejected() {
        assertFalse(ExternalOpenSupport.isCompatible("arquivo.rar", "application/vnd.rar"))
        assertFalse(ExternalOpenSupport.isCompatible("imagem.svg", "image/svg+xml"))
        assertFalse(ExternalOpenSupport.isCompatible("desconhecido.bin", "application/octet-stream"))
    }
}
