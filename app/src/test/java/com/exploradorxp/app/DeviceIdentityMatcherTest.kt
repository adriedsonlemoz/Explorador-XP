package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdentityMatcherTest {
    private val redmiA3 = DeviceCatalogEntry(
        manufacturer = "Xiaomi",
        brand = "Redmi",
        marketingName = "Redmi A3",
        device = "",
        model = "23129RN51X",
    )

    @Test
    fun knownXiaomiModel_matchesExactModelAndMaker() {
        val input = DeviceIdentityInput(
            manufacturer = "Xiaomi",
            brand = "Redmi",
            device = "blue",
            model = "23129RN51X",
            product = "blue_global",
        )
        val result = DeviceIdentityMatcher.match(
            input,
            listOf(redmiA3),
            DeviceIdentitySource.LOCAL_CATALOG,
            "seed-test",
        )
        assertEquals("Redmi A3", result?.marketingName)
        assertEquals(DeviceIdentityConfidence.HIGH, result?.confidence)
    }

    @Test
    fun unknownModel_returnsNoMatch() {
        val input = DeviceIdentityInput("Xiaomi", "Redmi", "unknown", "UNKNOWN-123", "unknown")
        assertNull(DeviceIdentityMatcher.match(input, listOf(redmiA3), DeviceIdentitySource.LOCAL_CATALOG, "seed-test"))
    }

    @Test
    fun sameModelFromDifferentManufacturer_isRejected() {
        val input = DeviceIdentityInput("ExampleCorp", "Example", "blue", "23129RN51X", "x")
        assertNull(DeviceIdentityMatcher.match(input, listOf(redmiA3), DeviceIdentitySource.LOCAL_CATALOG, "seed-test"))
    }

    @Test
    fun ambiguousMarketingNames_areRejected() {
        val input = DeviceIdentityInput("Xiaomi", "Redmi", "shared", "MODEL-1", "x")
        val entries = listOf(
            DeviceCatalogEntry("Xiaomi", "Redmi", "Phone One", "shared", "MODEL-1"),
            DeviceCatalogEntry("Xiaomi", "Redmi", "Phone Two", "shared", "MODEL-1"),
        )
        assertNull(DeviceIdentityMatcher.match(input, entries, DeviceIdentitySource.GOOGLE_PLAY_CATALOG, "remote"))
    }

    @Test
    fun exactDevice_breaksModelCollisionSafely() {
        val input = DeviceIdentityInput("Xiaomi", "Redmi", "device_b", "MODEL-2", "x")
        val entries = listOf(
            DeviceCatalogEntry("Xiaomi", "Redmi", "Phone A", "device_a", "MODEL-2"),
            DeviceCatalogEntry("Xiaomi", "Redmi", "Phone B", "device_b", "MODEL-2"),
        )
        val result = DeviceIdentityMatcher.match(input, entries, DeviceIdentitySource.GOOGLE_PLAY_CATALOG, "remote")
        assertEquals("Phone B", result?.marketingName)
    }

    @Test
    fun externalLookupPolicy_requiresSettingAndInternet() {
        assertTrue(DeviceLookupPolicy.canUseExternalSources(enabled = true, internetAvailable = true))
        assertFalse(DeviceLookupPolicy.canUseExternalSources(enabled = true, internetAvailable = false))
        assertFalse(DeviceLookupPolicy.canUseExternalSources(enabled = false, internetAvailable = true))
    }
}
