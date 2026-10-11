package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkDiscoveryEndpointsTest {
    @Test fun acceptsOnlyPrivateOrLinkLocalIpv4() {
        assertEquals("https://192.168.1.20:18843", NetworkDiscoveryEndpoints.httpsAddress("192.168.1.20", 18843))
        assertEquals("https://10.0.0.5:443", NetworkDiscoveryEndpoints.httpsAddress("10.0.0.5", 443))
        assertEquals("https://172.31.9.1:5555", NetworkDiscoveryEndpoints.httpsAddress("172.31.9.1", 5555))
        assertEquals("https://169.254.1.2:80", NetworkDiscoveryEndpoints.httpsAddress("169.254.1.2", 80))
    }

    @Test fun rejectsPublicLoopbackAndMulticast() {
        listOf("8.8.8.8", "172.32.0.1", "192.0.2.1", "127.0.0.1", "0.0.0.0", "224.0.0.251")
            .forEach { assertNull(it, NetworkDiscoveryEndpoints.httpsAddress(it, 18843)) }
    }

    @Test fun rejectsInvalidAndAmbiguousHosts() {
        listOf("192.168.1", "192.168.1.256", "192.168.1.-1", "192.168.01.1:443", "example.com", "", "192.168.1.2/a")
            .forEach { assertNull(it, NetworkDiscoveryEndpoints.httpsAddress(it, 18843)) }
    }

    @Test fun choosesUsableIpv4EvenWhenFirstAddressIsNotLocal() {
        assertEquals("https://192.168.0.12:18843", NetworkDiscoveryEndpoints.firstHttpsAddress(
            listOf("8.8.8.8", "192.168.0.12", "10.0.0.3"), 18843))
        assertNull(NetworkDiscoveryEndpoints.firstHttpsAddress(listOf("::1", "8.8.8.8"), 18843))
    }

    @Test fun rejectsPortsOutOfRange() {
        assertNull(NetworkDiscoveryEndpoints.httpsAddress("192.168.0.2", 0))
        assertNull(NetworkDiscoveryEndpoints.httpsAddress("192.168.0.2", 65536))
    }
}
