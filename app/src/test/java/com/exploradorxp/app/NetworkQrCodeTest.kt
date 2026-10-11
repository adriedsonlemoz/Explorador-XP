package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Não exige câmera, rede nem certificado real. */
class NetworkQrCodeTest {
    private val sample = NetworkQrConnection(
        host = "192.168.1.23", username = "xp",
        password = "ABCDEFGHIJKLMNOPQRSTUVWX12345678",
        fingerprint = "A".repeat(64),
    )

    @Test fun qrRoundTrip() {
        assertEquals(sample, NetworkQrCode.parse(NetworkQrCode.create(sample)))
    }

    @Test fun rejectsPublicIp() {
        assertThrows(IllegalArgumentException::class.java) {
            NetworkQrCode.parse(NetworkQrCode.create(sample).replace("192.168.1.23", "8.8.8.8"))
        }
    }

    @Test fun rejectsModifiedPortAndRepeatedFields() {
        val raw = NetworkQrCode.create(sample)
        assertThrows(IllegalArgumentException::class.java) {
            NetworkQrCode.parse(raw.replace("port=18843", "port=443"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NetworkQrCode.parse(raw + "&ip=192.168.1.24")
        }
    }

    @Test fun rejectsInvalidSecretsOrOrigins() {
        val raw = NetworkQrCode.create(sample)
        assertThrows(IllegalArgumentException::class.java) { NetworkQrCode.parse("https://192.168.1.23") }
        assertThrows(IllegalArgumentException::class.java) {
            NetworkQrCode.parse(raw.replace("&user=xp", "&user=admin"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NetworkQrCode.parse(raw.replace("&secret=", "&secret=wrong"))
        }
    }
}
