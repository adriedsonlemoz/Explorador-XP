package com.exploradorxp.app

import android.hardware.Sensor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoReportTest {
    @Test
    fun aiReport_separatesSystemAbiFromAppProcessAndKeepsPrivacyContract() {
        val snapshot = DeviceInfoSnapshot(
            collectedAt = "2026-09-26T09:15:00-03:00",
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
            supported32BitAbis = listOf("armeabi-v7a"),
            supported64BitAbis = listOf("arm64-v8a"),
            appProcessIs64Bit = false,
            appRuntimeArchitecture = "armv7l",
            kernelArchitecture = "aarch64",
            gpuRenderer = "Example GPU",
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
            batteryStatus = "Descarregando",
            batterySource = "Não conectado",
            batteryTemperatureC = 31.4f,
            batteryVoltageMv = 4100,
            batteryCurrentMicroamps = -350_000L,
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
            sensorInventory = listOf("1|type=1|string_type=android.sensor.accelerometer|name=Accelerometer|vendor=Example|version=1"),
            sensorDetails = listOf(
                DeviceSensorInfo(
                    type = Sensor.TYPE_ACCELEROMETER,
                    stringType = "android.sensor.accelerometer",
                    name = "Accelerometer",
                    vendor = "Example",
                    version = 1,
                    resolution = 0.01f,
                    maximumRange = 39.2f,
                    powerMa = 0.2f,
                    minDelayUs = 5_000,
                    maxDelayUs = 1_000_000,
                    reportingMode = Sensor.REPORTING_MODE_CONTINUOUS,
                    wakeUpSensor = false,
                    fifoReservedEventCount = 0,
                    fifoMaxEventCount = 300,
                )
            ),
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
            mobileNetworkType = "4G / LTE",
            carrierName = "Example Carrier",
            mobileSignalLevel = 3,
            mobileSignalDbm = -95,
            simSlotCount = 2,
            simReadyCount = 1,
            esimSupported = true,
            esimEnabled = true,
            esimMepSupported = true,
            appVersionName = "0.1.0-alpha.84",
            appVersionCode = 82,
        )

        val report = snapshot.toAiReport()

        assertTrue(report.contains("schema_version=4"))
        assertTrue(report.contains("generated_at=2026-09-26T09:15:00-03:00"))
        assertTrue(report.contains("ram_total_bytes=8000000000"))
        assertTrue(report.contains("ram_available_percent="))
        assertTrue(report.contains("storage=StatFs"))
        assertTrue(report.contains("resolution_px=1080x2400"))
        assertTrue(report.contains("system_supported_32_bit_abis=armeabi-v7a"))
        assertTrue(report.contains("system_supported_64_bit_abis=arm64-v8a"))
        assertTrue(report.contains("system_bitness_support=32 e 64 bits"))
        assertTrue(report.contains("app_process_bitness=32"))
        assertTrue(report.contains("app_runtime_architecture=armv7l"))
        assertTrue(report.contains("physical_hardware_architecture=Nao disponivel"))
        assertTrue(report.contains("app_abi=Nao disponivel"))
        assertTrue(report.contains("gpu=Example GPU"))
        assertTrue(report.contains("gpu_source=OpenGL ES (GL_RENDERER)"))
        assertTrue(report.contains("current_now_microamps=-350000"))
        assertTrue(report.contains("mobile_signal_dbm=-95"))
        assertTrue(report.contains("nfc=true"))
        assertTrue(report.contains("[sensor_inventory]"))
        assertTrue(report.contains("name=Accelerometer"))
        assertTrue(report.contains("contains_imei=false"))
        assertTrue(report.contains("contains_serial=false"))
        assertTrue(report.contains("contains_android_id=false"))
        assertTrue(report.contains("contains_mac_address=false"))
        assertTrue(report.contains("contains_location=false"))
        assertTrue(report.contains("contains_user_files=false"))
        assertTrue(report.contains("contains_user_defined_device_name=false"))
        assertFalse(report.contains("imei_value="))
        assertFalse(report.contains("android_id_value="))
        assertFalse(report.contains("name=Meu aparelho"))
        assertFalse(report.contains("is_64_bit="))

        val summary = snapshot.toShareSummary()
        assertTrue(summary.contains("Android 16"))
        assertTrue(summary.contains("processo do app 32 bits"))
        assertTrue(summary.contains("Suporte do sistema: 32 e 64 bits"))
        assertTrue(summary.contains("Arquitetura física: Não disponível"))
        assertTrue(summary.contains("Sensores detectados: 12"))
        assertTrue(summary.contains("Conexão: Wi-Fi"))
        assertFalse(summary.contains("fingerprint"))
    }
}
