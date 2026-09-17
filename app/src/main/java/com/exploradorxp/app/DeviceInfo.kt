package com.exploradorxp.app

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.telephony.euicc.EuiccManager
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Snapshot somente-leitura das informações que o próprio Android expõe ao aplicativo.
 * Não coleta identificadores únicos, contas, localização nem conteúdo dos arquivos.
 */
data class DeviceInfoSnapshot(
    val collectedAt: String,
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val deviceCode: String,
    val product: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildDisplay: String,
    val buildFingerprint: String,
    val kernelVersion: String,
    val socManufacturer: String?,
    val socModel: String?,
    val hardware: String,
    val cpuCores: Int,
    val cpuMaxFrequenciesMhz: List<Int>,
    val supportedAbis: List<String>,
    val is64Bit: Boolean,
    val ramTotalBytes: Long,
    val ramAvailableBytes: Long,
    val ramLow: Boolean,
    val storageTotalBytes: Long,
    val storageAvailableBytes: Long,
    val displayWidthPx: Int,
    val displayHeightPx: Int,
    val densityDpi: Int,
    val refreshRateHz: Float,
    val batteryPercent: Int?,
    val batteryStatus: String,
    val batterySource: String,
    val batteryTemperatureC: Float?,
    val batteryVoltageMv: Int?,
    val hasNfc: Boolean,
    val hasBluetooth: Boolean,
    val hasBluetoothLe: Boolean,
    val hasGps: Boolean,
    val hasCamera: Boolean,
    val hasFlash: Boolean,
    val hasFingerprint: Boolean,
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val hasMagnetometer: Boolean,
    val hasLightSensor: Boolean,
    val hasProximitySensor: Boolean,
    val hasBarometer: Boolean,
    val hasStepCounter: Boolean,
    val hasStepDetector: Boolean,
    val hasGravitySensor: Boolean,
    val hasLinearAcceleration: Boolean,
    val hasRotationVector: Boolean,
    val hasAmbientTemperature: Boolean,
    val hasRelativeHumidity: Boolean,
    val sensorCount: Int,
    val sensorInventory: List<String>,
    val hasRemovableStorage: Boolean,
    val networkTransport: String,
    val networkValidated: Boolean,
    val networkMetered: Boolean,
    val vpnActive: Boolean,
    val ethernetActive: Boolean,
    val wifiActive: Boolean,
    val wifiBand: String,
    val wifiStandard: String,
    val wifiFrequencyMhz: Int?,
    val wifiLinkSpeedMbps: Int?,
    val cellularActive: Boolean,
    val mobileNetworkType: String,
    val carrierName: String,
    val simSlotCount: Int,
    val simReadyCount: Int,
    val esimSupported: Boolean,
    val esimEnabled: Boolean,
    val esimMepSupported: Boolean,
    val appVersionName: String,
    val appVersionCode: Int,
) {
    val ramUsedBytes: Long get() = (ramTotalBytes - ramAvailableBytes).coerceAtLeast(0L)
    val storageUsedBytes: Long get() = (storageTotalBytes - storageAvailableBytes).coerceAtLeast(0L)
}

object DeviceInfoCollector {
    @SuppressLint("MissingPermission")
    fun collect(context: Context): DeviceInfoSnapshot {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager

        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)

        val storageRoot = Environment.getExternalStorageDirectory()
        val storage = StatFs(storageRoot.absolutePath)
        val storageTotal = storage.blockCountLong * storage.blockSizeLong
        val storageAvailable = storage.availableBlocksLong * storage.blockSizeLong

        val metrics = appContext.resources.displayMetrics
        val refreshRate = runCatching {
            @Suppress("DEPRECATION")
            val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }.getOrDefault(0f)

        val batteryIntent = runCatching {
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (batteryLevel >= 0 && batteryScale > 0) {
            ((batteryLevel * 100f) / batteryScale).toInt().coerceIn(0, 100)
        } else null

        val batteryStatus = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Carregada"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Em uso"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Conectada, sem carregar"
            else -> "Não disponível"
        }

        val batterySource = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Carregador"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Sem fio"
            else -> "Bateria"
        }

        val batteryTemperatureRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val batteryTemperature = batteryTemperatureRaw
            .takeIf { it != Int.MIN_VALUE && it != 0 }
            ?.div(10f)
        val batteryVoltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }

        val deviceName = runCatching {
            Settings.Global.getString(appContext.contentResolver, "device_name")
        }.getOrNull().orEmpty().trim().ifBlank { Build.MODEL.orEmpty().ifBlank { "Dispositivo Android" } }

        val removable = runCatching {
            appContext.getExternalFilesDirs(null)
                .filterNotNull()
                .any { Environment.isExternalStorageRemovable(it) }
        }.getOrDefault(false)

        val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        fun hasSensor(type: Int): Boolean = sensorManager.getDefaultSensor(type) != null
        val sensors = runCatching { sensorManager.getSensorList(Sensor.TYPE_ALL) }.getOrDefault(emptyList())
        val sensorCount = sensors.size
        val sensorInventory = sensors.mapIndexed { index, sensor ->
            "${index + 1}|type=${sensor.type}|name=${reportValue(sensor.name)}|vendor=${reportValue(sensor.vendor)}|version=${sensor.version}"
        }

        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null
        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val cpuFrequencies = readCpuMaxFrequenciesMhz(cpuCores)

        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = runCatching { connectivityManager.activeNetwork }.getOrNull()
        val capabilities = activeNetwork?.let { network ->
            runCatching { connectivityManager.getNetworkCapabilities(network) }.getOrNull()
        }
        val vpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val otherCapabilities = if (vpnActive) {
            runCatching { connectivityManager.allNetworks.toList() }.getOrDefault(emptyList())
                .filter { it != activeNetwork }
                .mapNotNull { network -> runCatching { connectivityManager.getNetworkCapabilities(network) }.getOrNull() }
        } else emptyList()
        val wifiActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            (vpnActive && otherCapabilities.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) })
        val cellularActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ||
            (vpnActive && otherCapabilities.any { it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) })
        val ethernetActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true ||
            (vpnActive && otherCapabilities.any { it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) })
        val networkValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val networkMetered = runCatching { connectivityManager.isActiveNetworkMetered }.getOrDefault(false)
        val networkTransport = when {
            vpnActive && wifiActive -> "VPN sobre Wi-Fi"
            vpnActive && cellularActive -> "VPN sobre rede móvel"
            vpnActive && ethernetActive -> "VPN sobre Ethernet"
            wifiActive -> "Wi-Fi"
            cellularActive -> "Rede móvel"
            ethernetActive -> "Ethernet"
            vpnActive -> "VPN"
            activeNetwork != null -> "Outra conexão"
            else -> "Sem conexão"
        }

        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val wifiInfo = if (wifiActive) runCatching { wifiManager?.connectionInfo }.getOrNull() else null
        val wifiFrequency = wifiInfo?.frequency?.takeIf { it > 0 }
        val wifiBand = wifiFrequency?.let(::wifiBandLabel) ?: "Não disponível"
        val wifiStandard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiInfo != null) {
            wifiStandardLabel(wifiInfo.wifiStandard)
        } else {
            "Não disponível"
        }
        val wifiLinkSpeed = wifiInfo?.linkSpeed?.takeIf { it > 0 }

        val telephonyManager = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val hasTelephony = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        val simSlotCount = if (hasTelephony && telephonyManager != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) telephonyManager.activeModemCount
                else {
                    @Suppress("DEPRECATION")
                    telephonyManager.phoneCount
                }
            }.getOrDefault(0).coerceAtLeast(0)
        } else 0
        val simReadyCount = if (telephonyManager != null && simSlotCount > 0) {
            (0 until simSlotCount).count { slot ->
                runCatching { telephonyManager.getSimState(slot) == TelephonyManager.SIM_STATE_READY }.getOrDefault(false)
            }
        } else 0
        val carrierName = runCatching {
            telephonyManager?.networkOperatorName.orEmpty().trim()
                .ifBlank { telephonyManager?.simOperatorName.orEmpty().trim() }
        }.getOrDefault("").ifBlank { "Não disponível" }
        val mobileNetworkType = if (hasTelephony && telephonyManager != null) {
            runCatching { networkTypeLabel(telephonyManager.dataNetworkType) }.getOrDefault("Não disponível")
        } else {
            "Não disponível"
        }

        val esimManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            appContext.getSystemService(Context.EUICC_SERVICE) as? EuiccManager
        } else null
        val esimFeature = packageManager.hasSystemFeature("android.hardware.telephony.euicc")
        val esimMepSupported = packageManager.hasSystemFeature("android.hardware.telephony.euicc.mep")
        val esimEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { esimManager?.isEnabled == true }.getOrDefault(false)
        } else false
        val esimSupported = esimFeature || esimEnabled

        val timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())

        return DeviceInfoSnapshot(
            collectedAt = timestamp,
            deviceName = deviceName,
            manufacturer = Build.MANUFACTURER.orEmpty().ifBlank { "Não disponível" },
            model = Build.MODEL.orEmpty().ifBlank { "Não disponível" },
            deviceCode = Build.DEVICE.orEmpty().ifBlank { "Não disponível" },
            product = Build.PRODUCT.orEmpty().ifBlank { "Não disponível" },
            androidVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { "Não disponível" },
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty().ifBlank { "Não disponível" },
            buildDisplay = Build.DISPLAY.orEmpty().ifBlank { "Não disponível" },
            buildFingerprint = Build.FINGERPRINT.orEmpty().ifBlank { "Não disponível" },
            kernelVersion = System.getProperty("os.version").orEmpty().ifBlank { "Não disponível" },
            socManufacturer = socManufacturer?.takeUnless(String::isBlank),
            socModel = socModel?.takeUnless(String::isBlank),
            hardware = Build.HARDWARE.orEmpty().ifBlank { "Não disponível" },
            cpuCores = cpuCores,
            cpuMaxFrequenciesMhz = cpuFrequencies,
            supportedAbis = abis,
            is64Bit = abis.any { it.contains("64") },
            ramTotalBytes = memoryInfo.totalMem,
            ramAvailableBytes = memoryInfo.availMem,
            ramLow = memoryInfo.lowMemory,
            storageTotalBytes = storageTotal,
            storageAvailableBytes = storageAvailable,
            displayWidthPx = metrics.widthPixels,
            displayHeightPx = metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            refreshRateHz = refreshRate,
            batteryPercent = batteryPercent,
            batteryStatus = batteryStatus,
            batterySource = batterySource,
            batteryTemperatureC = batteryTemperature,
            batteryVoltageMv = batteryVoltage,
            hasNfc = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            hasBluetooth = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            hasBluetoothLe = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            hasGps = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            hasFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH),
            hasFingerprint = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT),
            hasAccelerometer = hasSensor(Sensor.TYPE_ACCELEROMETER),
            hasGyroscope = hasSensor(Sensor.TYPE_GYROSCOPE),
            hasMagnetometer = hasSensor(Sensor.TYPE_MAGNETIC_FIELD),
            hasLightSensor = hasSensor(Sensor.TYPE_LIGHT),
            hasProximitySensor = hasSensor(Sensor.TYPE_PROXIMITY),
            hasBarometer = hasSensor(Sensor.TYPE_PRESSURE),
            hasStepCounter = hasSensor(Sensor.TYPE_STEP_COUNTER),
            hasStepDetector = hasSensor(Sensor.TYPE_STEP_DETECTOR),
            hasGravitySensor = hasSensor(Sensor.TYPE_GRAVITY),
            hasLinearAcceleration = hasSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            hasRotationVector = hasSensor(Sensor.TYPE_ROTATION_VECTOR),
            hasAmbientTemperature = hasSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
            hasRelativeHumidity = hasSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
            sensorCount = sensorCount,
            sensorInventory = sensorInventory,
            hasRemovableStorage = removable,
            networkTransport = networkTransport,
            networkValidated = networkValidated,
            networkMetered = networkMetered,
            vpnActive = vpnActive,
            ethernetActive = ethernetActive,
            wifiActive = wifiActive,
            wifiBand = wifiBand,
            wifiStandard = wifiStandard,
            wifiFrequencyMhz = wifiFrequency,
            wifiLinkSpeedMbps = wifiLinkSpeed,
            cellularActive = cellularActive,
            mobileNetworkType = mobileNetworkType,
            carrierName = carrierName,
            simSlotCount = simSlotCount,
            simReadyCount = simReadyCount,
            esimSupported = esimSupported,
            esimEnabled = esimEnabled,
            esimMepSupported = esimMepSupported,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
        )
    }

    private fun readCpuMaxFrequenciesMhz(coreCount: Int): List<Int> = (0 until coreCount).mapNotNull { core ->
        val candidates = listOf(
            "/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq",
            "/sys/devices/system/cpu/cpu$core/cpufreq/scaling_max_freq",
        )
        candidates.firstNotNullOfOrNull { path ->
            runCatching { File(path).readText().trim().toLongOrNull() }.getOrNull()?.let(::normalizeFrequencyMhz)
        }
    }.filter { it in 100..10_000 }

    private fun normalizeFrequencyMhz(raw: Long): Int = when {
        raw >= 100_000_000L -> (raw / 1_000_000L).toInt() // Hz
        raw >= 100_000L -> (raw / 1_000L).toInt() // kHz (padrão sysfs)
        else -> raw.toInt() // já em MHz
    }

    private fun wifiBandLabel(frequencyMhz: Int): String = when (frequencyMhz) {
        in 2400..2500 -> "2,4 GHz"
        in 4900..5900 -> "5 GHz"
        in 5925..7125 -> "6 GHz"
        in 57_000..71_000 -> "60 GHz"
        else -> "${frequencyMhz} MHz"
    }

    private fun wifiStandardLabel(standard: Int): String = when (standard) {
        1 -> "Wi-Fi legado"
        4 -> "Wi-Fi 4 (802.11n)"
        5 -> "Wi-Fi 5 (802.11ac)"
        6 -> "Wi-Fi 6 (802.11ax)"
        7 -> "WiGig (802.11ad)"
        8 -> "Wi-Fi 7 (802.11be)"
        else -> "Não disponível"
    }

    private fun networkTypeLabel(type: Int): String = when (type) {
        TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_GSM -> "2G"

        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0,
        TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA,
        TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA,
        TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD,
        TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"

        TelephonyManager.NETWORK_TYPE_LTE -> "4G / LTE"
        TelephonyManager.NETWORK_TYPE_NR -> "5G"
        TelephonyManager.NETWORK_TYPE_IWLAN -> "Wi-Fi Calling"
        else -> "Não disponível"
    }
}

fun DeviceInfoSnapshot.toAiReport(): String = buildString {
    appendLine("EXPLORADOR XP - RELATORIO DO DISPOSITIVO")
    appendLine("schema_version=3")
    appendLine("generated_at=$collectedAt")
    appendLine("purpose=diagnostico_tecnico_e_analise_por_ia")
    appendLine()
    appendLine("[privacy]")
    appendLine("contains_unique_device_ids=false")
    appendLine("contains_imei=false")
    appendLine("contains_serial=false")
    appendLine("contains_android_id=false")
    appendLine("contains_mac_address=false")
    appendLine("contains_location=false")
    appendLine("contains_ssid=false")
    appendLine("contains_bssid=false")
    appendLine("contains_phone_number=false")
    appendLine("contains_user_files=false")
    appendLine("note=O relatorio contem apenas informacoes de hardware, sistema, conectividade e estado geral expostas pelo Android.")
    appendLine()
    appendLine("[device]")
    appendLine("name=${reportValue(deviceName)}")
    appendLine("manufacturer=${reportValue(manufacturer)}")
    appendLine("model=${reportValue(model)}")
    appendLine("device_code=${reportValue(deviceCode)}")
    appendLine("product=${reportValue(product)}")
    appendLine()
    appendLine("[android]")
    appendLine("version=${reportValue(androidVersion)}")
    appendLine("api_level=$apiLevel")
    appendLine("security_patch=${reportValue(securityPatch)}")
    appendLine("build_display=${reportValue(buildDisplay)}")
    appendLine("build_fingerprint=${reportValue(buildFingerprint)}")
    appendLine("kernel=${reportValue(kernelVersion)}")
    appendLine()
    appendLine("[processor]")
    appendLine("soc_manufacturer=${reportValue(socManufacturer ?: "Nao disponivel")}")
    appendLine("soc_model=${reportValue(socModel ?: "Nao disponivel")}")
    appendLine("hardware=${reportValue(hardware)}")
    appendLine("cpu_cores=$cpuCores")
    appendLine("is_64_bit=$is64Bit")
    appendLine("primary_abi=${reportValue(supportedAbis.firstOrNull() ?: "Nao disponivel")}")
    appendLine("supported_abis=${supportedAbis.joinToString(",")}")
    appendLine("cpu_max_frequencies_mhz=${cpuMaxFrequenciesMhz.joinToString(",")}")
    appendLine("cpu_frequency_summary=${reportValue(cpuFrequencySummary(this@toAiReport))}")
    appendLine()
    appendLine("[memory]")
    appendLine("ram_total_bytes=$ramTotalBytes")
    appendLine("ram_total_human=${humanBytes(ramTotalBytes)}")
    appendLine("ram_available_bytes=$ramAvailableBytes")
    appendLine("ram_available_human=${humanBytes(ramAvailableBytes)}")
    appendLine("ram_used_bytes=$ramUsedBytes")
    appendLine("ram_used_human=${humanBytes(ramUsedBytes)}")
    appendLine("android_low_memory=$ramLow")
    appendLine()
    appendLine("[storage_internal]")
    appendLine("total_bytes=$storageTotalBytes")
    appendLine("total_human=${humanBytes(storageTotalBytes)}")
    appendLine("available_bytes=$storageAvailableBytes")
    appendLine("available_human=${humanBytes(storageAvailableBytes)}")
    appendLine("used_bytes=$storageUsedBytes")
    appendLine("used_human=${humanBytes(storageUsedBytes)}")
    appendLine("removable_storage_detected=$hasRemovableStorage")
    appendLine()
    appendLine("[display]")
    appendLine("resolution_px=${displayWidthPx}x${displayHeightPx}")
    appendLine("density_dpi=$densityDpi")
    appendLine("refresh_rate_hz=${formatOneDecimal(refreshRateHz)}")
    appendLine()
    appendLine("[battery]")
    appendLine("percent=${batteryPercent ?: "Nao disponivel"}")
    appendLine("status=${reportValue(batteryStatus)}")
    appendLine("power_source=${reportValue(batterySource)}")
    appendLine("temperature_c=${batteryTemperatureC?.let(::formatOneDecimal) ?: "Nao disponivel"}")
    appendLine("voltage_mv=${batteryVoltageMv ?: "Nao disponivel"}")
    appendLine()
    appendLine("[connectivity]")
    appendLine("active_transport=${reportValue(networkTransport)}")
    appendLine("internet_validated=$networkValidated")
    appendLine("metered=$networkMetered")
    appendLine("vpn_active=$vpnActive")
    appendLine("ethernet_active=$ethernetActive")
    appendLine("wifi_active=$wifiActive")
    appendLine("wifi_band=${reportValue(wifiBand)}")
    appendLine("wifi_standard=${reportValue(wifiStandard)}")
    appendLine("wifi_frequency_mhz=${wifiFrequencyMhz ?: "Nao disponivel"}")
    appendLine("wifi_link_speed_mbps=${wifiLinkSpeedMbps ?: "Nao disponivel"}")
    appendLine("cellular_active=$cellularActive")
    appendLine("mobile_network_type=${reportValue(mobileNetworkType)}")
    appendLine("carrier=${reportValue(carrierName)}")
    appendLine("sim_slot_count=$simSlotCount")
    appendLine("sim_ready_count=$simReadyCount")
    appendLine("esim_supported=$esimSupported")
    appendLine("euicc_manager_enabled=$esimEnabled")
    appendLine("esim_multiple_enabled_profiles_supported=$esimMepSupported")
    appendLine()
    appendLine("[capabilities]")
    appendLine("nfc=$hasNfc")
    appendLine("bluetooth=$hasBluetooth")
    appendLine("bluetooth_le=$hasBluetoothLe")
    appendLine("gps=$hasGps")
    appendLine("camera=$hasCamera")
    appendLine("camera_flash=$hasFlash")
    appendLine("fingerprint=$hasFingerprint")
    appendLine()
    appendLine("[sensors]")
    appendLine("sensor_count=$sensorCount")
    appendLine("accelerometer=$hasAccelerometer")
    appendLine("gyroscope=$hasGyroscope")
    appendLine("magnetometer=$hasMagnetometer")
    appendLine("light_sensor=$hasLightSensor")
    appendLine("proximity_sensor=$hasProximitySensor")
    appendLine("barometer=$hasBarometer")
    appendLine("step_counter=$hasStepCounter")
    appendLine("step_detector=$hasStepDetector")
    appendLine("gravity_sensor=$hasGravitySensor")
    appendLine("linear_acceleration=$hasLinearAcceleration")
    appendLine("rotation_vector=$hasRotationVector")
    appendLine("ambient_temperature=$hasAmbientTemperature")
    appendLine("relative_humidity=$hasRelativeHumidity")
    appendLine()
    appendLine("[sensor_inventory]")
    if (sensorInventory.isEmpty()) appendLine("none=true") else sensorInventory.forEach { appendLine(it) }
    appendLine()
    appendLine("[explorador_xp]")
    appendLine("version_name=${reportValue(appVersionName)}")
    appendLine("version_code=$appVersionCode")
    appendLine()
    appendLine("[ai_guidance]")
    appendLine("Preferir os campos numericos *_bytes para calculos e os campos *_human para explicacoes ao usuario.")
    appendLine("Nao inferir capacidade inexistente quando um campo estiver como Nao disponivel.")
    appendLine("SSID, BSSID, numero de telefone, IMEI, IMSI, ICCID e localizacao nao sao coletados.")
    appendLine("Os valores representam o estado informado pelo Android no momento generated_at.")
}

fun DeviceInfoSnapshot.toShareSummary(): String = buildString {
    appendLine("Explorador XP — Informações do dispositivo")
    appendLine("${deviceName} • ${manufacturer.smartReportTitle()} ${model}")
    appendLine("Android ${androidVersion} • API ${apiLevel} • Patch ${securityPatch}")
    appendLine("Processador: ${listOfNotNull(socManufacturer, socModel).joinToString(" ").ifBlank { hardware }}")
    appendLine("CPU: ${cpuCores} núcleos • ${if (is64Bit) "64 bits" else "32 bits"} • ${supportedAbis.firstOrNull() ?: "ABI N/D"}")
    if (cpuMaxFrequenciesMhz.isNotEmpty()) appendLine("Clock: ${cpuFrequencySummary(this@toShareSummary)}")
    appendLine("RAM: ${humanBytes(ramTotalBytes)} • ${humanBytes(ramAvailableBytes)} livre")
    appendLine("Armazenamento: ${humanBytes(storageTotalBytes)} • ${humanBytes(storageAvailableBytes)} livre")
    appendLine("Tela: ${displayWidthPx} × ${displayHeightPx}px${if (refreshRateHz > 0f) " • ${refreshRateHz.toInt()} Hz" else ""}")
    appendLine("Bateria: ${batteryPercent?.let { "$it%" } ?: "N/D"} • $batteryStatus")
    appendLine("Conexão: ${connectivitySummary(this@toShareSummary)}")
    appendLine("SIM: $simReadyCount pronto(s) de $simSlotCount • eSIM ${if (esimSupported) "suportado" else "não detectado"}")
    appendLine("Sensores detectados: $sensorCount")
    appendLine("Gerado pelo Explorador XP ${appVersionName}")
}

fun deviceInfoImageFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.now())
    return "ExploradorXP-dispositivo-$stamp.png"
}

private fun String.smartReportTitle(): String = lowercase().replaceFirstChar { char ->
    if (char.isLowerCase()) char.titlecase() else char.toString()
}

fun deviceInfoExportFileName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.now())
    return "ExploradorXP-relatorio-dispositivo-$stamp.txt"
}

fun humanBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return when {
        unit == 0 -> "${bytes} B"
        value >= 100 -> String.format(Locale.forLanguageTag("pt-BR"), "%.0f %s", value, units[unit])
        else -> String.format(Locale.forLanguageTag("pt-BR"), "%.1f %s", value, units[unit])
    }
}

fun cpuFrequencySummary(info: DeviceInfoSnapshot): String {
    if (info.cpuMaxFrequenciesMhz.isEmpty()) return "Não disponível"
    val grouped = info.cpuMaxFrequenciesMhz.groupingBy { ((it + 5) / 10) * 10 }.eachCount().toSortedMap()
    return grouped.entries.joinToString(" + ") { (mhz, count) ->
        val ghz = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", mhz / 1000.0)
            .trimEnd('0').trimEnd(',')
        "${count}×${ghz} GHz"
    }
}

fun connectivitySummary(info: DeviceInfoSnapshot): String = when {
    info.wifiActive -> buildString {
        append("Wi-Fi")
        if (info.wifiBand != "Não disponível") append(" • ${info.wifiBand}")
        if (info.wifiStandard != "Não disponível") append(" • ${info.wifiStandard}")
        info.wifiLinkSpeedMbps?.let { append(" • ${it} Mbps") }
        if (info.vpnActive) append(" • VPN")
    }
    info.cellularActive -> buildString {
        append(info.mobileNetworkType.takeUnless { it == "Não disponível" } ?: "Rede móvel")
        if (info.carrierName != "Não disponível") append(" • ${info.carrierName}")
        if (info.vpnActive) append(" • VPN")
    }
    info.ethernetActive -> "Ethernet"
    info.vpnActive -> "VPN"
    else -> info.networkTransport
}

private fun reportValue(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\n", " ")
    .replace("\r", " ")

private fun formatOneDecimal(value: Float): String = String.format(Locale.ROOT, "%.1f", value)
