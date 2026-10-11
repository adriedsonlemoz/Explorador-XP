package com.exploradorxp.app

import org.junit.Assert.*
import org.junit.Test

class NetworkSharePathsTest {
    @Test fun allowsSharedDirectoryAndUnicodeNames() {
        assertEquals(listOf("pasta-1", "Fotos de férias"),
            NetworkSharePaths.parseRequestPath("/dav/pasta-1/Fotos%20de%20f%C3%A9rias/"))
    }

    @Test fun rejectsTraversalAndEncodedSeparators() {
        listOf(
            "/dav/a/../etc", "/dav/a/%2e%2e/secret", "/dav/a/%2Fetc", "/dav/a/%5cconfig",
            "/dav/a/f%00oo", "/other/a", "/dav/a/%ZZ", "/dav/a/%2e/secret"
        ).forEach { assertNull(it, NetworkSharePaths.parseRequestPath(it)) }
    }

    @Test fun rejectsNamesOutsideSafePathRules() {
        assertFalse(NetworkSharePaths.validName("."))
        assertFalse(NetworkSharePaths.validName(".."))
        assertFalse(NetworkSharePaths.validName("folder/name"))
        assertFalse(NetworkSharePaths.validName("folder\\name"))
        assertFalse(NetworkSharePaths.validAlias("../downloads"))
    }

    @Test fun encodesSegmentsWithoutBreakingDirectoryBoundary() {
        val path = NetworkSharePaths.urlPath(listOf("pasta-1", "a+b & c"), true)
        assertEquals(listOf("pasta-1", "a+b & c"), NetworkSharePaths.parseRequestPath(path))
    }

    @Test fun rejectsPathsAboveLimits() {
        assertNull(NetworkSharePaths.parseRequestPath("/dav/" + "x".repeat(601)))
        assertNull(NetworkSharePaths.parseRequestPath("/dav/" + "a/".repeat(30)))
    }
}
