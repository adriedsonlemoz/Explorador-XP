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
}
