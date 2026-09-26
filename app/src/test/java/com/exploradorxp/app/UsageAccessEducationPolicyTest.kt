package com.exploradorxp.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageAccessEducationPolicyTest {
    @Test
    fun `shows once after update when access is missing`() {
        assertTrue(
            UsageAccessEducationPolicy.shouldShow(
                installedThroughUpdate = true,
                currentVersionCode = 88,
                lastSeenEducationVersionCode = -1,
                hasUsageAccess = false,
            )
        )
    }

    @Test
    fun `does not show when access is already granted`() {
        assertFalse(
            UsageAccessEducationPolicy.shouldShow(
                installedThroughUpdate = true,
                currentVersionCode = 88,
                lastSeenEducationVersionCode = -1,
                hasUsageAccess = true,
            )
        )
    }

    @Test
    fun `does not repeat after education was seen`() {
        assertFalse(
            UsageAccessEducationPolicy.shouldShow(
                installedThroughUpdate = true,
                currentVersionCode = 89,
                lastSeenEducationVersionCode = 88,
                hasUsageAccess = false,
            )
        )
    }

    @Test
    fun `does not interrupt a fresh install`() {
        assertFalse(
            UsageAccessEducationPolicy.shouldShow(
                installedThroughUpdate = false,
                currentVersionCode = 88,
                lastSeenEducationVersionCode = -1,
                hasUsageAccess = false,
            )
        )
    }
}
