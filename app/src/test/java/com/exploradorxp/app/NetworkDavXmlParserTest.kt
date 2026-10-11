package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDavXmlParserTest {
    @Test fun readsValidDavMultistatus() {
        val xml = """<?xml version="1.0"?><d:multistatus xmlns:d="DAV:"><d:response><d:href>/dav/fotos/</d:href></d:response></d:multistatus>"""
        val doc = NetworkDavXmlParser.parse(xml.toByteArray(Charsets.UTF_8))
        assertEquals("/dav/fotos/", doc.getElementsByTagNameNS("DAV:", "href").item(0).textContent)
    }

    @Test fun rejectsDoctypeAndInternalEntity() {
        val xml = """<?xml version="1.0"?><!DOCTYPE d [<!ENTITY exploit "value">]><d>&exploit;</d>"""
        var refused = false
        try { NetworkDavXmlParser.parse(xml.toByteArray(Charsets.UTF_8)) }
        catch (_: Exception) { refused = true }
        assertTrue("WebDAV XML DOCTYPE deve ser bloqueado", refused)
    }

    @Test fun rejectsExternalEntityReference() {
        val xml = """<?xml version="1.0"?><!DOCTYPE d [<!ENTITY exploit SYSTEM "file:///etc/passwd">]><d>&exploit;</d>"""
        var refused = false
        try { NetworkDavXmlParser.parse(xml.toByteArray(Charsets.UTF_8)) }
        catch (_: Exception) { refused = true }
        assertTrue("XXE deve ser bloqueado", refused)
    }
}
