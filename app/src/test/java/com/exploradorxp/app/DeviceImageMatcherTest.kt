package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceImageMatcherTest {
    private val identity = DeviceIdentityResult(
        marketingName = "Redmi A3",
        manufacturerNormalized = "Xiaomi",
        variants = emptyList(),
        source = DeviceIdentitySource.LOCAL_CATALOG,
        confidence = DeviceIdentityConfidence.HIGH,
        catalogVersion = "test",
        model = "23129RN51X",
        device = "blue",
        brand = "Redmi",
    )

    @Test
    fun imageFound_requiresExactNameManufacturerAndImageProperty() {
        val candidate = DeviceImageEntityCandidate(
            entityId = "Q1",
            labelsAndAliases = listOf("Redmi A3"),
            manufacturerNames = listOf("Xiaomi"),
            imageFileName = "Redmi A3.jpg",
        )
        assertEquals(candidate, DeviceImageMatcher.selectReliableCandidate(identity, listOf(candidate)))
    }

    @Test
    fun imageMissing_isRejected() {
        val candidate = DeviceImageEntityCandidate(
            entityId = "Q1",
            labelsAndAliases = listOf("Redmi A3"),
            manufacturerNames = listOf("Xiaomi"),
            imageFileName = null,
        )
        assertNull(DeviceImageMatcher.selectReliableCandidate(identity, listOf(candidate)))
    }

    @Test
    fun wrongManufacturer_isRejected() {
        val candidate = DeviceImageEntityCandidate(
            entityId = "Q1",
            labelsAndAliases = listOf("Redmi A3"),
            manufacturerNames = listOf("Another Manufacturer"),
            imageFileName = "phone.jpg",
        )
        assertNull(DeviceImageMatcher.selectReliableCandidate(identity, listOf(candidate)))
    }

    @Test
    fun ambiguousEntities_areRejected() {
        val candidates = listOf(
            DeviceImageEntityCandidate("Q1", listOf("Redmi A3"), listOf("Xiaomi"), "one.jpg"),
            DeviceImageEntityCandidate("Q2", listOf("Redmi A3"), listOf("Xiaomi"), "two.jpg"),
        )
        assertNull(DeviceImageMatcher.selectReliableCandidate(identity, candidates))
    }

    @Test
    fun cachePolicy_distinguishesFreshAndExpired() {
        val now = 2_000_000_000L
        assertTrue(DeviceCachePolicy.isFresh(now - 1_000L, now, 2_000L))
        assertFalse(DeviceCachePolicy.isFresh(now - 3_000L, now, 2_000L))
        assertFalse(DeviceCachePolicy.isFresh(0L, now, 2_000L))
    }
}
