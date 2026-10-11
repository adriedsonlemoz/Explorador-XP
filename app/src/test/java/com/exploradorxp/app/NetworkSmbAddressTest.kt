package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkSmbAddressTest {
    @Test fun privateWifiAddresses() {
        listOf("192.168.1.10", "192.168.43.1", "10.2.3.4", "172.16.0.1", "172.31.255.254")
            .forEach { assertTrue("Local: $it", NetworkSmbAddress.privateIpv4(it)) }
    }

    @Test fun rejectsExternalAndAmbiguousAddresses() {
        listOf("8.8.8.8", "127.0.0.1", "169.254.0.1", "172.32.0.1", "192.167.1.1",
            "1.1.1.1", "192.168.1.256", "192.168.1.1/24", "bad.host", "::1", "192.168..1", "010.0.0.1", "192.168.001.1")
            .forEach { assertFalse("Não local: $it", NetworkSmbAddress.privateIpv4(it)) }
    }

    @Test fun rejectsShareTraversal() {
        listOf("", ".", "..", "foo/bar", "foo\\bar", "docs:", "a*b", "arquivo.", " pasta ")
            .forEach { assertFalse("Compartilhamento proibido: $it", NetworkSmbAddress.validShare(it)) }
        assertTrue(NetworkSmbAddress.validShare("Compartilhado"))
        assertTrue(NetworkSmbAddress.validShare("Fotos 2026"))
    }

    @Test fun safeRemotePaths() {
        assertEquals("DCIM\\Camera\\imagem.jpg", NetworkSmbAddress.remotePath(listOf("DCIM", "Camera", "imagem.jpg")))
        assertEquals("", NetworkSmbAddress.remotePath(emptyList()))
        assertTrue(runCatching { NetworkSmbAddress.remotePath(listOf("DCIM", "..", "camera.jpg")) }.isFailure)
        assertTrue(runCatching { NetworkSmbAddress.remotePath(listOf("DCIM", "sub\\pasta")) }.isFailure)
    }
}
