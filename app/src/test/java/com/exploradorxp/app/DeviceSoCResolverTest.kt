package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSoCResolverTest {
    @Test
    fun ambiguousMt6765KeepsTechnicalIdentityInsteadOfGuessingCommercialName() {
        val mediatek = DeviceSoCResolver.resolve("MediaTek", "MT6765", "mt6765")

        assertNull(mediatek.commercialName)
        assertEquals("MediaTek", mediatek.manufacturer)
        assertEquals("MT6765", mediatek.technicalId)
        assertEquals("MediaTek MT6765", mediatek.technicalLabel)
        assertEquals("PowerVR GE8320", mediatek.gpu)
        assertEquals("12 nm", mediatek.processLabel)
        assertTrue(mediatek.matchedCatalog)
        assertEquals("MT6765", mediatek.matchedAlias)
    }

    @Test
    fun resolvesOnlyKnownExactCommercialAliases() {
        val snapdragon = DeviceSoCResolver.resolve("Qualcomm Technologies, Inc", "SM6225", "qcom")
        assertEquals("Snapdragon 680", snapdragon.commercialName)
        assertEquals("Qualcomm", snapdragon.manufacturer)
        assertEquals("SM6225", snapdragon.technicalId)

        val legacy = DeviceSoCResolver.resolve("Qualcomm", "SDM660", "sdm660")
        assertEquals("Snapdragon 660", legacy.commercialName)
    }

    @Test
    fun distinguishesKnownSuffixInsteadOfGuessingFamilyVariant() {
        val identity = DeviceSoCResolver.resolve("Qualcomm", "SM6225-AD", "qcom")
        assertEquals("Snapdragon 685", identity.commercialName)
        assertEquals("SM6225-AD", identity.technicalId)
    }

    @Test
    fun unknownIdentifierFallsBackToAndroidValues() {
        val identity = DeviceSoCResolver.resolve("Example Silicon", "ABC-123", "example")
        assertNull(identity.commercialName)
        assertEquals("Example Silicon", identity.manufacturer)
        assertEquals("ABC-123", identity.technicalId)
        assertEquals("ABC-123", identity.primaryLabel)
        assertNull(identity.gpu)
        assertNull(identity.processLabel)
        assertFalse(identity.matchedCatalog)
    }
}
