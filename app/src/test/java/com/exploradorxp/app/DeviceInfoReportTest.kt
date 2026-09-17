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
            cpuMaxFrequenciesMhz = listOf(1800, 1800, 1800, 1800, 2200, 2200, 2200, 2200),
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
            hasMagnetometer = true,
            hasLightSensor = true,
            hasProximitySensor = true,
            hasBarometer = false,
            hasStepCounter = true,
            hasStepDetector = true,
            hasGravitySensor = true,
            hasLinearAcceleration = true,
            hasRotationVector = true,
            hasAmbientTemperature = false,
            hasRelativeHumidity = false,
            sensorCount = 12,
            sensorInventory = listOf("1|type=1|name=Accelerometer|vendor=Example|version=1"),
            hasRemovableStorage = false,
            networkTransport = "Wi-Fi",
            networkValidated = true,
            networkMetered = false,
            vpnActive = false,
            ethernetActive = false,
            wifiActive = true,
            wifiBand = "5 GHz",
            wifiStandard = "Wi-Fi 5 (802.11ac)",
            wifiFrequencyMhz = 5180,
            wifiLinkSpeedMbps = 433,
            cellularActive = false,
            mobileNetworkType = "Não ativa",
            carrierName = "Example Carrier",
            simSlotCount = 2,
            simReadyCount = 1,
            esimSupported = true,
            esimEnabled = true,
            esimMepSupported = true,
            appVersionName = "0.1.0-alpha.26",
            appVersionCode = 26,
        )

        val report = snapshot.toAiReport()

        assertTrue(report.contains("ram_total_bytes=8000000000"))
        assertTrue(report.contains("resolution_px=1080x2400"))
        assertTrue(report.contains("nfc=true"))
        assertTrue(report.contains("[sensors]"))
        assertTrue(report.contains("magnetometer=true"))
        assertTrue(report.contains("sensor_count=12"))
        assertTrue(report.contains("[connectivity]"))
        assertTrue(report.contains("wifi_standard=Wi-Fi 5 (802.11ac)"))
        assertTrue(report.contains("esim_supported=true"))
        assertTrue(report.contains("euicc_manager_enabled=true"))
        assertTrue(report.contains("esim_multiple_enabled_profiles_supported=true"))
        assertTrue(report.contains("cpu_frequency_summary="))
        assertTrue(report.contains("[sensor_inventory]"))
        assertTrue(report.contains("name=Accelerometer"))
        assertTrue(report.contains("contains_imei=false"))
        assertTrue(report.contains("contains_location=false"))
        assertTrue(report.contains("contains_ssid=false"))
        assertTrue(report.contains("contains_bssid=false"))
        assertTrue(report.contains("contains_phone_number=false"))
        assertFalse(report.contains("imei_value="))
        assertFalse(report.contains("android_id_value="))

        val summary = snapshot.toShareSummary()
        assertTrue(summary.contains("Android 16"))
        assertTrue(summary.contains("Sensores detectados: 12"))
        assertTrue(summary.contains("Conexão: Wi-Fi"))
        assertFalse(summary.contains("fingerprint"))
    }
}
