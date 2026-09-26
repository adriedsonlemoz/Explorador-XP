package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCatalogServiceTest {
    @Test
    fun csvParser_handlesQuotedValues() {
        val row = DeviceCatalogService().parseCsvLine("\"Xiaomi\",\"Redmi A3\",\"blue\",\"23129RN51X\"")
        assertEquals(listOf("Xiaomi", "Redmi A3", "blue", "23129RN51X"), row)
    }

    @Test
    fun invalidCatalogRow_isNotMistakenForADeviceRecord() {
        val row = DeviceCatalogService().parseCsvLine("unexpected invalid response")
        assertTrue(row.size < 4)
    }
}
