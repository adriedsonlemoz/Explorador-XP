package com.exploradorxp.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoReportTest {
    @Test
    fun aiReport_containsUsefulRawValuesAndPrivacyContract() {
        val snapshot = DeviceInfoSnapshot(
            collectedAt = "2026-09-16T18:00:00-03:00",
            deviceName = "Meu aparelho",
            manufacturer = "Example",
            model = "Model X",
            deviceCode = "codename",
            product = "product",
            androidVersion = "16",
            apiLevel = 36,
            securityPatch = "2026-09-01",
            buildDisplay = "build",
            buildFingerprint = "fingerprint",
            kernelVersion = "6.1",
            socManufacturer = "Chip Co",
            socModel = "Chip X",
            hardware = "hardware",
            cpuCores = 8,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
            is64Bit = true,
            ramTotalBytes = 8_000_000_000L,
            ramAvailableBytes = 3_000_000_000L,
            ramLow = false,
            storageTotalBytes = 256_000_000_000L,
            storageAvailableBytes = 128_000_000_000L,
            displayWidthPx = 1080,
            displayHeightPx = 2400,
            densityDpi = 420,
            refreshRateHz = 120f,
            batteryPercent = 75,
            batteryStatus = "Em uso",
            batterySource = "Bateria",
            batteryTemperatureC = 31.4f,
            batteryVoltageMv = 4100,
            hasNfc = true,
            hasBluetooth = true,
            hasBluetoothLe = true,
            hasGps = true,
            hasCamera = true,
            hasFlash = true,
            hasFingerprint = true,
            hasAccelerometer = true,
            hasGyroscope = true,
            hasRemovableStorage = false,
            appVersionName = "0.1.0-alpha.24",
            appVersionCode = 23,
        )

        val report = snapshot.toAiReport()

        assertTrue(report.contains("ram_total_bytes=8000000000"))
        assertTrue(report.contains("resolution_px=1080x2400"))
        assertTrue(report.contains("nfc=true"))
        assertTrue(report.contains("contains_imei=false"))
        assertTrue(report.contains("contains_location=false"))
        assertFalse(report.contains("imei_value="))
        assertFalse(report.contains("android_id_value="))
    }
}
