package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppsLogicTest {
    private val userApp = ManagedApp(
        label = "Beta",
        packageName = "com.example.beta",
        versionName = "2.0",
        versionCode = 2,
        isSystem = false,
        isUpdatedSystem = false,
        enabled = true,
        apkBytes = 50,
        firstInstallTime = 1,
        lastUpdateTime = 2,
        targetSdk = 35,
        minSdk = 26,
        installerPackage = null,
        canLaunch = true,
        isOwnPackage = false,
    )
    private val systemApp = userApp.copy(
        label = "Alpha System",
        packageName = "android.alpha",
        isSystem = true,
        apkBytes = 200,
    )

    @Test
    fun filtersUserAndSystemApps() {
        assertEquals(
            listOf("com.example.beta"),
            AppManagerLogic.filterAndSort(
                listOf(userApp, systemApp), "", AppManagerFilter.USER, AppManagerSort.NAME, emptyMap()
            ).map { it.packageName }
        )
        assertEquals(
            listOf("android.alpha"),
            AppManagerLogic.filterAndSort(
                listOf(userApp, systemApp), "", AppManagerFilter.SYSTEM, AppManagerSort.NAME, emptyMap()
            ).map { it.packageName }
        )
    }

    @Test
    fun searchesByLabelOrPackage() {
        assertEquals(
            listOf("android.alpha"),
            AppManagerLogic.filterAndSort(
                listOf(userApp, systemApp), "alpha", AppManagerFilter.ALL, AppManagerSort.NAME, emptyMap()
            ).map { it.packageName }
        )
        assertEquals(
            listOf("com.example.beta"),
            AppManagerLogic.filterAndSort(
                listOf(userApp, systemApp), "example", AppManagerFilter.ALL, AppManagerSort.NAME, emptyMap()
            ).map { it.packageName }
        )
    }

    @Test
    fun sortsByMeasuredTotalWhenAvailableAndApkOtherwise() {
        val storage = mapOf(
            userApp.packageName to AppStorageInfo(appBytes = 100, dataBytes = 500, cacheBytes = 50)
        )
        assertEquals(
            listOf("com.example.beta", "android.alpha"),
            AppManagerLogic.filterAndSort(
                listOf(systemApp, userApp), "", AppManagerFilter.ALL, AppManagerSort.SIZE, storage
            ).map { it.packageName }
        )
    }

    @Test
    fun totalDoesNotDoubleCountCache() {
        val info = AppStorageInfo(appBytes = 100, dataBytes = 500, cacheBytes = 50)
        assertEquals(600L, info.totalBytes)
    }

    @Test
    fun byteFormatterIsDeterministic() {
        assertEquals("0 B", AppManagerLogic.displayBytes(0))
        assertEquals("1 KB", AppManagerLogic.displayBytes(1024))
        assertEquals("2 KB", AppManagerLogic.displayBytes(2048))
        assertEquals("Não disponível", AppManagerLogic.displayBytes(null))
    }
    @Test
    fun filtersSizeWithExactBoundaryAndUsesMeasuredTotal() {
        val mb = 1024L * 1024L
        val small = userApp.copy(apkBytes = 49L * mb)
        val medium = systemApp.copy(apkBytes = 50L * mb)
        val large = userApp.copy(packageName = "large", label = "Large", apkBytes = 200L * mb)
        val apps = listOf(small, medium, large)
        val mediumMatches = AppManagerLogic.filterAndSort(
            apps, "", AppManagerFilter.ALL, AppManagerSort.NAME, emptyMap(),
            AppManagerSizeFilter.FROM_50_TO_200_MB,
        )
        assertEquals(listOf("android.alpha"), mediumMatches.map { it.packageName })
        val largeMatches = AppManagerLogic.filterAndSort(
            apps, "", AppManagerFilter.ALL, AppManagerSort.NAME, emptyMap(),
            AppManagerSizeFilter.OVER_200_MB,
        )
        assertEquals(listOf("large"), largeMatches.map { it.packageName })
        val overridden = mapOf(small.packageName to AppStorageInfo(200L * mb, 0, 0))
        assertEquals(listOf(small.packageName, "large"), AppManagerLogic.filterAndSort(
            apps, "", AppManagerFilter.ALL, AppManagerSort.NAME, overridden,
            AppManagerSizeFilter.OVER_200_MB,
        ).map { it.packageName })
    }

    @Test
    fun filtersStateAndDateOrder() {
        val disabled = systemApp.copy(enabled = false, lastUpdateTime = 100, firstInstallTime = 1)
        val enabled = userApp.copy(lastUpdateTime = 20, firstInstallTime = 200)
        val apps = listOf(enabled, disabled)
        assertEquals(listOf(disabled.packageName), AppManagerLogic.filterAndSort(
            apps, "", AppManagerFilter.ALL, AppManagerSort.NAME, emptyMap(),
            statusFilter = AppManagerStatusFilter.DISABLED,
        ).map { it.packageName })
        assertEquals(listOf(disabled.packageName, enabled.packageName), AppManagerLogic.filterAndSort(
            apps, "", AppManagerFilter.ALL, AppManagerSort.LAST_UPDATE, emptyMap(),
        ).map { it.packageName })
        assertEquals(listOf(enabled.packageName, disabled.packageName), AppManagerLogic.filterAndSort(
            apps, "", AppManagerFilter.ALL, AppManagerSort.INSTALL_DATE, emptyMap(),
        ).map { it.packageName })
    }

    @Test
    fun sortsSmallestFirstUsingMeasuredSize() {
        val storage = mapOf(userApp.packageName to AppStorageInfo(500, 0, 0))
        assertEquals(listOf(systemApp.packageName, userApp.packageName), AppManagerLogic.filterAndSort(
            listOf(userApp, systemApp), "", AppManagerFilter.ALL, AppManagerSort.SIZE_ASC, storage,
        ).map { it.packageName })
    }

    @Test
    fun measuredSummaryNeverReplacesMissingSizesWithApks() {
        val measured = mapOf(userApp.packageName to AppStorageInfo(100, 500, 20))
        val partial = AppUsageSummary.from(listOf(userApp, systemApp), measured)
        assertEquals(600L, partial.measuredBytes)
        assertEquals(1, partial.measuredApps)
        assertEquals(2, partial.totalApps)
        assertEquals(false, partial.complete)
        val full = AppUsageSummary.from(listOf(userApp, systemApp), measured +
            (systemApp.packageName to AppStorageInfo(200, 200, 10)))
        assertEquals(1000L, full.measuredBytes)
        assertEquals(true, full.complete)
    }

}
