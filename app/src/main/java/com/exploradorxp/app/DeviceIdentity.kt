package com.exploradorxp.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

internal data class DeviceIdentityInput(
    val manufacturer: String,
    val brand: String,
    val device: String,
    val model: String,
    val product: String,
) {
    companion object {
        fun current(): DeviceIdentityInput = DeviceIdentityInput(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            model = Build.MODEL.orEmpty(),
            product = Build.PRODUCT.orEmpty(),
        )
    }
}

internal data class DeviceCatalogEntry(
    val manufacturer: String,
    val brand: String,
    val marketingName: String,
    val device: String,
    val model: String,
)

internal enum class DeviceIdentitySource(val label: String) {
    LOCAL_CATALOG("Catálogo local"),
    CACHED_CATALOG("Catálogo atualizado em cache"),
    GOOGLE_PLAY_CATALOG("Lista pública de dispositivos do Google Play"),
    ANDROID_ONLY("Dados brutos do Android"),
}

internal enum class DeviceIdentityConfidence(val label: String) {
    HIGH("Alta"),
    NONE("Sem correspondência confirmada"),
}

internal data class DeviceIdentityResult(
    val marketingName: String?,
    val manufacturerNormalized: String,
    val variants: List<String>,
    val source: DeviceIdentitySource,
    val confidence: DeviceIdentityConfidence,
    val catalogVersion: String?,
    val model: String,
    val device: String,
    val brand: String,
) {
    val displayName: String get() = marketingName ?: "Não identificado"
    val confirmed: Boolean get() = marketingName != null && confidence == DeviceIdentityConfidence.HIGH
}

internal object DeviceIdentityMatcher {
    fun match(
        input: DeviceIdentityInput,
        entries: List<DeviceCatalogEntry>,
        source: DeviceIdentitySource,
        catalogVersion: String?,
    ): DeviceIdentityResult? {
        val inputModel = normalizedCode(input.model)
        if (inputModel.isBlank()) return null
        val acceptedMakers = setOf(normalizedMaker(input.manufacturer), normalizedMaker(input.brand))
            .filterTo(mutableSetOf()) { it.isNotBlank() }

        var candidates = entries.filter { entry ->
            normalizedCode(entry.model) == inputModel &&
                setOf(normalizedMaker(entry.manufacturer), normalizedMaker(entry.brand))
                    .any { it.isNotBlank() && it in acceptedMakers }
        }
        if (candidates.isEmpty()) return null

        val inputDevice = normalizedCode(input.device)
        if (candidates.size > 1 && inputDevice.isNotBlank()) {
            val deviceExact = candidates.filter { normalizedCode(it.device) == inputDevice }
            if (deviceExact.isNotEmpty()) candidates = deviceExact
        }

        val names = candidates.map { it.marketingName.trim() }.filter { it.isNotBlank() }.distinctBy { normalizedName(it) }
        if (names.size != 1) return null
        val chosenName = names.single()
        val chosen = candidates.firstOrNull { normalizedName(it.marketingName) == normalizedName(chosenName) } ?: return null

        return DeviceIdentityResult(
            marketingName = chosenName,
            // O fabricante real continua vindo do Android. O catálogo só normaliza o nome comercial.
            manufacturerNormalized = input.manufacturer.trim().ifBlank {
                chosen.manufacturer.trim().ifBlank { input.brand.trim() }
            },
            variants = candidates.mapNotNull { it.device.trim().takeIf(String::isNotBlank) }.distinct(),
            source = source,
            confidence = DeviceIdentityConfidence.HIGH,
            catalogVersion = catalogVersion,
            model = input.model,
            device = input.device,
            brand = input.brand,
        )
    }

    fun unknown(input: DeviceIdentityInput): DeviceIdentityResult = DeviceIdentityResult(
        marketingName = null,
        manufacturerNormalized = input.manufacturer.trim().ifBlank { input.brand.trim().ifBlank { "Não disponível" } },
        variants = emptyList(),
        source = DeviceIdentitySource.ANDROID_ONLY,
        confidence = DeviceIdentityConfidence.NONE,
        catalogVersion = null,
        model = input.model,
        device = input.device,
        brand = input.brand,
    )

    internal fun normalizedCode(value: String): String = value.trim().lowercase(Locale.ROOT)
    internal fun normalizedMaker(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "")

    internal fun normalizedName(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}

internal data class DeviceCatalogSnapshot(
    val version: String,
    val generatedAtEpochMs: Long,
    val entries: List<DeviceCatalogEntry>,
)

internal object DeviceLookupPolicy {
    fun canUseExternalSources(enabled: Boolean, internetAvailable: Boolean): Boolean = enabled && internetAvailable
}

internal object DeviceCachePolicy {
    const val CATALOG_MAX_AGE_MS: Long = 30L * 24L * 60L * 60L * 1000L
    const val IMAGE_MAX_AGE_MS: Long = 30L * 24L * 60L * 60L * 1000L

    fun isFresh(savedAtEpochMs: Long, nowEpochMs: Long, maxAgeMs: Long): Boolean =
        savedAtEpochMs > 0L && nowEpochMs >= savedAtEpochMs && nowEpochMs - savedAtEpochMs <= maxAgeMs
}

internal class DeviceCatalogRepository(
    private val context: Context,
    private val service: DeviceCatalogService = DeviceCatalogService(),
) {
    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "device_catalog").also { it.mkdirs() }
    private val localCatalog: DeviceCatalogSnapshot by lazy { readLocalCatalog() }

    fun findLocal(input: DeviceIdentityInput): DeviceIdentityResult? = DeviceIdentityMatcher.match(
        input = input,
        entries = localCatalog.entries,
        source = DeviceIdentitySource.LOCAL_CATALOG,
        catalogVersion = localCatalog.version,
    )

    fun findFreshCache(input: DeviceIdentityInput, nowEpochMs: Long = System.currentTimeMillis()): DeviceIdentityResult? {
        val cached = readCache(input) ?: return null
        if (!DeviceCachePolicy.isFresh(cached.savedAtEpochMs, nowEpochMs, DeviceCachePolicy.CATALOG_MAX_AGE_MS)) return null
        if (cached.noMatch) return null
        return DeviceIdentityMatcher.match(
            input = input,
            entries = cached.entries,
            source = DeviceIdentitySource.CACHED_CATALOG,
            catalogVersion = cached.remoteVersion,
        )
    }

    fun refresh(input: DeviceIdentityInput): DeviceIdentityResult? {
        if (!internetAvailable(appContext)) return findFreshCache(input) ?: findLocal(input)

        val cached = readCache(input)
        if (cached != null && DeviceCachePolicy.isFresh(
                cached.savedAtEpochMs,
                System.currentTimeMillis(),
                DeviceCachePolicy.CATALOG_MAX_AGE_MS,
            )
        ) {
            return if (cached.noMatch) findLocal(input) else DeviceIdentityMatcher.match(
                input,
                cached.entries,
                DeviceIdentitySource.CACHED_CATALOG,
                cached.remoteVersion,
            ) ?: findLocal(input)
        }

        val remoteInfo = runCatching { service.remoteInfo() }.getOrNull()
        val localResult = findLocal(input)
        if (localResult != null && remoteInfo != null && remoteInfo.lastModifiedEpochMs > 0L &&
            remoteCatalogDay(remoteInfo.lastModifiedEpochMs) <= remoteCatalogDay(localCatalog.generatedAtEpochMs)
        ) {
            writeCache(
                input,
                CachedCatalog(
                    savedAtEpochMs = System.currentTimeMillis(),
                    remoteVersion = remoteInfo.version,
                    remoteLastModifiedEpochMs = remoteInfo.lastModifiedEpochMs,
                    entries = localCatalog.entries.filter { DeviceIdentityMatcher.normalizedCode(it.model) == DeviceIdentityMatcher.normalizedCode(input.model) },
                    noMatch = false,
                )
            )
            return localResult
        }

        val fetched = runCatching { service.fetchExactMatches(input, remoteInfo) }
            .onFailure { Log.w(TAG, "catalog_refresh_failed model=${safeLog(input.model)} type=${it.javaClass.simpleName}") }
            .getOrNull()
            ?: return findFreshCache(input) ?: localResult

        val remoteResult = DeviceIdentityMatcher.match(
            input = input,
            entries = fetched.entries,
            source = DeviceIdentitySource.GOOGLE_PLAY_CATALOG,
            catalogVersion = fetched.version,
        )
        writeCache(
            input,
            CachedCatalog(
                savedAtEpochMs = System.currentTimeMillis(),
                remoteVersion = fetched.version,
                remoteLastModifiedEpochMs = fetched.lastModifiedEpochMs,
                entries = fetched.entries,
                noMatch = remoteResult == null,
            )
        )
        return remoteResult ?: localResult
    }

    private fun readLocalCatalog(): DeviceCatalogSnapshot {
        return runCatching {
            val raw = appContext.assets.open(LOCAL_ASSET).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val root = JSONObject(raw)
            val entries = root.optJSONArray("entries").toCatalogEntries()
            DeviceCatalogSnapshot(
                version = root.optString("catalogVersion", "local"),
                generatedAtEpochMs = root.optLong("generatedAtEpochMs", 0L),
                entries = entries,
            )
        }.onFailure {
            Log.w(TAG, "local_catalog_invalid type=${it.javaClass.simpleName}")
        }.getOrElse { DeviceCatalogSnapshot("invalid", 0L, emptyList()) }
    }

    private fun readCache(input: DeviceIdentityInput): CachedCatalog? {
        val file = cacheFile(input)
        if (!file.isFile) return null
        return runCatching {
            val root = JSONObject(file.readText(StandardCharsets.UTF_8))
            CachedCatalog(
                savedAtEpochMs = root.getLong("savedAtEpochMs"),
                remoteVersion = root.optString("remoteVersion", "unknown"),
                remoteLastModifiedEpochMs = root.optLong("remoteLastModifiedEpochMs", 0L),
                entries = root.optJSONArray("entries").toCatalogEntries(),
                noMatch = root.optBoolean("noMatch", false),
            )
        }.onFailure {
            Log.w(TAG, "catalog_cache_corrupt model=${safeLog(input.model)} type=${it.javaClass.simpleName}")
            runCatching { file.delete() }
        }.getOrNull()
    }

    private fun writeCache(input: DeviceIdentityInput, cached: CachedCatalog) {
        runCatching {
            val root = JSONObject()
                .put("schemaVersion", 1)
                .put("savedAtEpochMs", cached.savedAtEpochMs)
                .put("remoteVersion", cached.remoteVersion)
                .put("remoteLastModifiedEpochMs", cached.remoteLastModifiedEpochMs)
                .put("noMatch", cached.noMatch)
                .put("entries", JSONArray().apply {
                    cached.entries.forEach { entry ->
                        put(
                            JSONObject()
                                .put("manufacturer", entry.manufacturer)
                                .put("brand", entry.brand)
                                .put("marketingName", entry.marketingName)
                                .put("device", entry.device)
                                .put("model", entry.model)
                        )
                    }
                })
            val file = cacheFile(input)
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(root.toString(), StandardCharsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.delete()
                check(tmp.renameTo(file)) { "cache rename failed" }
            }
        }.onFailure {
            Log.w(TAG, "catalog_cache_write_failed model=${safeLog(input.model)} type=${it.javaClass.simpleName}")
        }
    }

    private fun cacheFile(input: DeviceIdentityInput): File {
        val key = "${input.manufacturer}|${input.brand}|${input.model}".lowercase(Locale.ROOT)
        val hash = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(StandardCharsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
        return File(cacheDir, "device_$hash.json")
    }

    private fun JSONArray?.toCatalogEntries(): List<DeviceCatalogEntry> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val name = item.optString("marketingName").trim()
                val model = item.optString("model").trim()
                if (name.isBlank() || model.isBlank()) continue
                add(
                    DeviceCatalogEntry(
                        manufacturer = item.optString("manufacturer").trim(),
                        brand = item.optString("brand").trim(),
                        marketingName = name,
                        device = item.optString("device").trim(),
                        model = model,
                    )
                )
            }
        }
    }

    private fun remoteCatalogDay(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).toLocalDate()

    private data class CachedCatalog(
        val savedAtEpochMs: Long,
        val remoteVersion: String,
        val remoteLastModifiedEpochMs: Long,
        val entries: List<DeviceCatalogEntry>,
        val noMatch: Boolean,
    )

    companion object {
        private const val LOCAL_ASSET = "device_catalog.json"
        private const val TAG = "DeviceIdentity"
    }
}

internal class DeviceIdentityRepository(context: Context) {
    private val catalog = DeviceCatalogRepository(context)

    fun resolveLocal(input: DeviceIdentityInput = DeviceIdentityInput.current()): DeviceIdentityResult {
        val result = catalog.findLocal(input) ?: DeviceIdentityMatcher.unknown(input)
        logResult(input, result)
        return result
    }

    fun resolveWithCache(input: DeviceIdentityInput = DeviceIdentityInput.current()): DeviceIdentityResult {
        val result = catalog.findFreshCache(input) ?: catalog.findLocal(input) ?: DeviceIdentityMatcher.unknown(input)
        logResult(input, result)
        return result
    }

    fun refresh(input: DeviceIdentityInput = DeviceIdentityInput.current()): DeviceIdentityResult {
        val result = catalog.refresh(input) ?: DeviceIdentityMatcher.unknown(input)
        logResult(input, result)
        return result
    }

    fun warmupCurrentDevice() {
        val input = DeviceIdentityInput.current()
        // O cache/local já cobre a abertura imediata. Rede é usada somente quando a política pede atualização.
        refresh(input)
    }

    private fun logResult(input: DeviceIdentityInput, result: DeviceIdentityResult) {
        Log.i(
            "DeviceIdentity",
            "model=${safeLog(input.model)} manufacturer=${safeLog(input.manufacturer)} " +
                "match=${safeLog(result.marketingName ?: "none")} source=${result.source.name.lowercase(Locale.ROOT)} " +
                "confidence=${result.confidence.name.lowercase(Locale.ROOT)}"
        )
    }
}

internal data class RemoteCatalogInfo(
    val version: String,
    val lastModifiedEpochMs: Long,
)

internal data class RemoteCatalogMatch(
    val version: String,
    val lastModifiedEpochMs: Long,
    val entries: List<DeviceCatalogEntry>,
)

internal class DeviceCatalogService {
    fun remoteInfo(): RemoteCatalogInfo {
        val connection = (URL(CATALOG_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return connection.useConnection {
            require(responseCode in 200..399) { "HTTP $responseCode" }
            val lastModified = lastModified.takeIf { it > 0L } ?: 0L
            val etag = getHeaderField("ETag").orEmpty().trim('"')
            RemoteCatalogInfo(
                version = etag.ifBlank { lastModified.takeIf { it > 0L }?.let { "lm-$it" } ?: "unknown" },
                lastModifiedEpochMs = lastModified,
            )
        }
    }

    fun fetchExactMatches(input: DeviceIdentityInput, knownInfo: RemoteCatalogInfo? = null): RemoteCatalogMatch {
        val connection = (URL(CATALOG_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "text/csv,*/*;q=0.8")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return connection.useConnection {
            require(responseCode in 200..299) { "HTTP $responseCode" }
            val contentLength = contentLengthLong
            require(contentLength <= 0L || contentLength <= MAX_CATALOG_BYTES) { "catalog too large" }
            val rows = mutableListOf<DeviceCatalogEntry>()
            var totalChars = 0L
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_16LE)).use { reader ->
                var first = true
                while (true) {
                    val line = reader.readLine() ?: break
                    totalChars += line.length
                    require(totalChars <= MAX_CATALOG_CHARS) { "catalog too large" }
                    if (first) {
                        first = false
                        continue
                    }
                    val columns = parseCsvLine(line)
                    if (columns.size < 4) continue
                    val brand = columns[0].trim().removePrefix("\uFEFF")
                    val marketing = columns[1].trim()
                    val device = columns[2].trim()
                    val model = columns[3].trim()
                    if (marketing.isBlank() || model.isBlank()) continue
                    if (DeviceIdentityMatcher.normalizedCode(model) != DeviceIdentityMatcher.normalizedCode(input.model)) continue
                    val brandNormalized = DeviceIdentityMatcher.normalizedMaker(brand)
                    val inputMakers = setOf(
                        DeviceIdentityMatcher.normalizedMaker(input.manufacturer),
                        DeviceIdentityMatcher.normalizedMaker(input.brand),
                    )
                    if (brandNormalized.isBlank() || brandNormalized !in inputMakers) continue
                    rows += DeviceCatalogEntry(
                        manufacturer = brand,
                        brand = brand,
                        marketingName = marketing,
                        device = device,
                        model = model,
                    )
                }
            }
            val lastModified = lastModified.takeIf { it > 0L } ?: knownInfo?.lastModifiedEpochMs ?: 0L
            val etag = getHeaderField("ETag").orEmpty().trim('"')
            RemoteCatalogMatch(
                version = etag.ifBlank { knownInfo?.version ?: lastModified.takeIf { it > 0L }?.let { "lm-$it" } ?: "unknown" },
                lastModifiedEpochMs = lastModified,
                entries = rows,
            )
        }
    }

    internal fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>(4)
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    out += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        out += current.toString()
        return out
    }

    companion object {
        const val CATALOG_URL = "https://storage.googleapis.com/play_public/supported_devices.csv"
        private const val USER_AGENT = "ExploradorXP-DeviceCatalog/1.0"
        private const val CONNECT_TIMEOUT_MS = 7_000
        private const val READ_TIMEOUT_MS = 20_000
        private const val MAX_CATALOG_BYTES = 24L * 1024L * 1024L
        private const val MAX_CATALOG_CHARS = 12L * 1024L * 1024L
    }
}

internal fun internetAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
}

internal inline fun <T> HttpURLConnection.useConnection(block: HttpURLConnection.() -> T): T = try {
    block()
} finally {
    disconnect()
}

private fun safeLog(value: String): String = value
    .replace(Regex("[\\r\\n\\t]"), " ")
    .take(96)
