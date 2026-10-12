package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class NetworkConnectionDiagnosticsTest {
    @Test fun connectionClosedIsHumanReadable() {
        val failure = NetworkFailure.from(IOException("connection closed"))
        assertEquals("CLOSED", failure.code)
        assertTrue(failure.explanation.contains("motivo exato"))
    }
    @Test fun timeoutIncludesPracticalTip() {
        val failure = NetworkFailure.from(SocketTimeoutException("timeout"))
        assertEquals("TIMEOUT", failure.code)
        assertTrue(failure.guidance.contains("Wi-Fi"))
    }
    @Test fun authenticationIsNotMisclassified() {
        assertEquals("AUTH", NetworkFailure.from(IOException("Listagem recusada (HTTP 401).")).code)
    }
    @Test fun connectionRefusedHasSeparateCause() {
        assertEquals("REACHABILITY", NetworkFailure.from(ConnectException("Connection refused")).code)
    }
    @Test fun tlsFailureMustNotRecommendBypass() {
        val failure = NetworkFailure.from(SSLHandshakeException("certificate verification failed"))
        assertEquals("TLS", failure.code)
        assertTrue(failure.guidance.contains("Não ignore"))
    }
    @Test fun sensitiveErrorDetailsAreNotCopied() {
        val text = NetworkFailure.from(IOException("connection closed password=TOPSECRET https://private")).copyable()
        assertFalse(text.contains("TOPSECRET"))
        assertFalse(text.contains("https://private"))
    }
}
