package com.exploradorxp.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

internal data class DeviceImageMetadata(
    val imageFileName: String,
    val thumbnailUrl: String,
    val sourceUrl: String,
    val license: String?,
    val author: String?,
    val wikidataEntityId: String,
    val queriedAtEpochMs: Long,
)

internal sealed interface DeviceImageResult {
    data class Available(
        val bitmap: Bitmap,
        val metadata: DeviceImageMetadata,
        val fromCache: Boolean,
    ) : DeviceImageResult

    data class Unavailable(
        val reason: String,
        val checkedAtEpochMs: Long,
        val fromCache: Boolean,
    ) : DeviceImageResult
}

internal data class DeviceImageEntityCandidate(
    val entityId: String,
    val labelsAndAliases: List<String>,
    val manufacturerNames: List<String>,
    val imageFileName: String?,
)

internal object DeviceImageMatcher {
    fun selectReliableCandidate(
        identity: DeviceIdentityResult,
        candidates: List<DeviceImageEntityCandidate>,
    ): DeviceImageEntityCandidate? {
        val marketing = identity.marketingName?.let(DeviceIdentityMatcher::normalizedName).orEmpty()
        if (marketing.isBlank()) return null
        val expectedNames = setOf(
            marketing,
            DeviceIdentityMatcher.normalizedName("${identity.manufacturerNormalized} ${identity.marketingName}"),
            DeviceIdentityMatcher.normalizedName("${identity.brand} ${identity.marketingName}"),
        ).filterTo(mutableSetOf()) { it.isNotBlank() }
        val expectedMakers = setOf(
            DeviceIdentityMatcher.normalizedMaker(identity.manufacturerNormalized),
            DeviceIdentityMatcher.normalizedMaker(identity.brand),
        ).filterTo(mutableSetOf()) { it.isNotBlank() }

        val reliable = candidates.filter { candidate ->
            val nameMatch = candidate.labelsAndAliases
                .map { DeviceIdentityMatcher.normalizedName(it) }
                .any { it in expectedNames }
            val manufacturerMatch = candidate.manufacturerNames
                .map { DeviceIdentityMatcher.normalizedMaker(it) }
                .any { it.isNotBlank() && it in expectedMakers }
            nameMatch && manufacturerMatch && !candidate.imageFileName.isNullOrBlank()
        }
        return reliable.singleOrNull()
    }
}

internal class DeviceImageCache(context: Context) {
    private val cacheDir = File(context.applicationContext.cacheDir, "device_images").also { it.mkdirs() }

    fun load(identity: DeviceIdentityResult, nowEpochMs: Long = System.currentTimeMillis()): DeviceImageResult? {
        if (!identity.confirmed) return null
        val metadataFile = metadataFile(identity)
        if (!metadataFile.isFile) return null
        val cache = readCache(identity) ?: return null
        if (!DeviceCachePolicy.isFresh(cache.savedAtEpochMs, nowEpochMs, DeviceCachePolicy.IMAGE_MAX_AGE_MS)) return null
        if (cache.status == STATUS_UNAVAILABLE) {
            return DeviceImageResult.Unavailable(
                reason = cache.reason.ifBlank { "Imagem deste modelo não disponível" },
                checkedAtEpochMs = cache.savedAtEpochMs,
                fromCache = true,
            )
        }
        val metadata = cache.metadata ?: return null
        val imageFile = imageFile(identity, metadata.imageFileName)
        if (!imageFile.isFile || imageFile.length() <= 0L) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(imageFile.absolutePath) }.getOrNull()
        if (bitmap == null) {
            runCatching { imageFile.delete() }
            runCatching { metadataFile.delete() }
            return null
        }
        Log.i(TAG, "model=${safeImageLog(identity.model)} status=found source=wikimedia cache=true")
        return DeviceImageResult.Available(bitmap, metadata, fromCache = true)
    }

    fun storeAvailable(identity: DeviceIdentityResult, metadata: DeviceImageMetadata, imageBytes: ByteArray, savedAt: Long) {
        val target = imageFile(identity, metadata.imageFileName)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeBytes(imageBytes)
        if (!tmp.renameTo(target)) {
            target.delete()
            check(tmp.renameTo(target)) { "image cache rename failed" }
        }
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("savedAtEpochMs", savedAt)
            .put("status", STATUS_AVAILABLE)
            .put("metadata", JSONObject()
                .put("imageFileName", metadata.imageFileName)
                .put("thumbnailUrl", metadata.thumbnailUrl)
                .put("sourceUrl", metadata.sourceUrl)
                .put("license", metadata.license.orEmpty())
                .put("author", metadata.author.orEmpty())
                .put("wikidataEntityId", metadata.wikidataEntityId)
                .put("queriedAtEpochMs", metadata.queriedAtEpochMs)
            )
        writeMetadataAtomically(identity, root)
    }

    fun storeUnavailable(identity: DeviceIdentityResult, reason: String, savedAt: Long) {
        val root = JSONObject()
            .put("schemaVersion", 1)
            .put("savedAtEpochMs", savedAt)
            .put("status", STATUS_UNAVAILABLE)
            .put("reason", reason)
        writeMetadataAtomically(identity, root)
    }

    private fun readCache(identity: DeviceIdentityResult): ImageCacheRecord? = runCatching {
        val root = JSONObject(metadataFile(identity).readText(StandardCharsets.UTF_8))
        val status = root.optString("status")
        val metadataObject = root.optJSONObject("metadata")
        val metadata = metadataObject?.let { item ->
            DeviceImageMetadata(
                imageFileName = item.optString("imageFileName"),
                thumbnailUrl = item.optString("thumbnailUrl"),
                sourceUrl = item.optString("sourceUrl"),
                license = item.optString("license").takeIf { it.isNotBlank() },
                author = item.optString("author").takeIf { it.isNotBlank() },
                wikidataEntityId = item.optString("wikidataEntityId"),
                queriedAtEpochMs = item.optLong("queriedAtEpochMs"),
            )
        }
        ImageCacheRecord(
            savedAtEpochMs = root.getLong("savedAtEpochMs"),
            status = status,
            reason = root.optString("reason"),
            metadata = metadata,
        )
    }.onFailure {
        Log.w(TAG, "model=${safeImageLog(identity.model)} status=cache_corrupt type=${it.javaClass.simpleName}")
        runCatching { metadataFile(identity).delete() }
    }.getOrNull()

    private fun writeMetadataAtomically(identity: DeviceIdentityResult, root: JSONObject) {
        val target = metadataFile(identity)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(root.toString(), StandardCharsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.delete()
            check(tmp.renameTo(target)) { "metadata cache rename failed" }
        }
    }

    private fun metadataFile(identity: DeviceIdentityResult): File = File(cacheDir, "${cacheKey(identity)}.json")

    private fun imageFile(identity: DeviceIdentityResult, imageFileName: String): File {
        val extension = imageFileName.substringAfterLast('.', "img").lowercase(Locale.ROOT)
            .takeIf { it.matches(Regex("[a-z0-9]{2,5}")) } ?: "img"
        return File(cacheDir, "${cacheKey(identity)}.$extension")
    }

    private fun cacheKey(identity: DeviceIdentityResult): String {
        val raw = "${identity.manufacturerNormalized}|${identity.model}|${identity.marketingName}".lowercase(Locale.ROOT)
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(StandardCharsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
    }

    private data class ImageCacheRecord(
        val savedAtEpochMs: Long,
        val status: String,
        val reason: String,
        val metadata: DeviceImageMetadata?,
    )

    companion object {
        private const val TAG = "DeviceImage"
        private const val STATUS_AVAILABLE = "available"
        private const val STATUS_UNAVAILABLE = "unavailable"
    }
}

internal class DeviceImageRepository(
    context: Context,
    private val service: DeviceImageService = DeviceImageService(),
    private val cache: DeviceImageCache = DeviceImageCache(context),
) {
    private val appContext = context.applicationContext

    fun loadCached(identity: DeviceIdentityResult, nowEpochMs: Long = System.currentTimeMillis()): DeviceImageResult? =
        cache.load(identity, nowEpochMs)

    fun resolve(identity: DeviceIdentityResult): DeviceImageResult {
        loadCached(identity)?.let { return it }
        if (!identity.confirmed) {
            return DeviceImageResult.Unavailable("Modelo sem identificação confiável", System.currentTimeMillis(), false)
        }
        if (!internetAvailable(appContext)) {
            return DeviceImageResult.Unavailable("Sem internet; usando imagem genérica", System.currentTimeMillis(), false)
        }

        val now = System.currentTimeMillis()
        val lookup = runCatching { service.findReliableImage(identity, now) }
            .onFailure {
                Log.w(TAG, "model=${safeImageLog(identity.model)} status=request_failed type=${it.javaClass.simpleName}")
            }
        if (lookup.isFailure) {
            return DeviceImageResult.Unavailable("Não foi possível consultar a fonte agora", now, false)
        }
        val metadata = lookup.getOrNull()
        if (metadata == null) {
            runCatching { cache.storeUnavailable(identity, "Imagem deste modelo não disponível", now) }
            Log.i(TAG, "model=${safeImageLog(identity.model)} status=no_reliable_image")
            return DeviceImageResult.Unavailable("Imagem deste modelo não disponível", now, false)
        }

        val imageDownload = runCatching { service.downloadThumbnail(metadata.thumbnailUrl) }
            .onFailure {
                Log.w(TAG, "model=${safeImageLog(identity.model)} status=download_failed type=${it.javaClass.simpleName}")
            }
        val imageBytes = imageDownload.getOrNull()
        if (imageDownload.isFailure || imageBytes == null || imageBytes.isEmpty()) {
            return DeviceImageResult.Unavailable("Imagem indisponível no momento", now, false)
        }
        val bitmap = runCatching { BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) }.getOrNull()
        if (bitmap == null) {
            runCatching { cache.storeUnavailable(identity, "Imagem recebida em formato inválido", now) }
            return DeviceImageResult.Unavailable("Imagem recebida em formato inválido", now, false)
        }
        val cachedMetadata = metadata.copy(queriedAtEpochMs = now)
        runCatching { cache.storeAvailable(identity, cachedMetadata, imageBytes, now) }
            .onFailure {
                Log.w(TAG, "model=${safeImageLog(identity.model)} status=cache_write_failed type=${it.javaClass.simpleName}")
            }
        Log.i(TAG, "model=${safeImageLog(identity.model)} status=found source=wikimedia cache=false")
        return DeviceImageResult.Available(bitmap, cachedMetadata, fromCache = false)
    }

    companion object {
        private const val TAG = "DeviceImage"
    }
}

internal class DeviceImageService {
    fun findReliableImage(identity: DeviceIdentityResult, queriedAtEpochMs: Long): DeviceImageMetadata? {
        val marketing = identity.marketingName ?: return null
        val searchTerms = listOf(
            "${identity.manufacturerNormalized} $marketing",
            marketing,
        ).distinct()
        val candidateIds = linkedSetOf<String>()
        searchTerms.forEach { term ->
            val root = requestJson(
                "https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encode(term)}" +
                    "&language=en&uselang=en&type=item&limit=8&format=json&origin=*"
            )
            val search = root.optJSONArray("search") ?: return@forEach
            for (i in 0 until search.length()) {
                val id = search.optJSONObject(i)?.optString("id").orEmpty()
                if (id.matches(Regex("Q\\d+"))) candidateIds += id
            }
        }
        if (candidateIds.isEmpty()) return null

        val candidates = candidateIds.mapNotNull { entityId -> loadEntityCandidate(entityId) }
        val reliable = DeviceImageMatcher.selectReliableCandidate(identity, candidates) ?: return null
        val imageName = reliable.imageFileName ?: return null
        val commons = loadCommonsMetadata(imageName) ?: return null
        return DeviceImageMetadata(
            imageFileName = imageName,
            thumbnailUrl = commons.thumbnailUrl,
            sourceUrl = commons.sourceUrl,
            license = commons.license,
            author = commons.author,
            wikidataEntityId = reliable.entityId,
            queriedAtEpochMs = queriedAtEpochMs,
        )
    }

    fun downloadThumbnail(url: String): ByteArray {
        require(url.startsWith("https://")) { "insecure image URL" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "image/webp,image/*;q=0.9")
        }
        return connection.useConnection {
            require(responseCode in 200..299) { "HTTP $responseCode" }
            val length = contentLengthLong
            require(length <= 0L || length <= MAX_IMAGE_BYTES) { "thumbnail too large" }
            inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= MAX_IMAGE_BYTES) { "thumbnail too large" }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
        }
    }

    private fun loadEntityCandidate(entityId: String): DeviceImageEntityCandidate? {
        val root = requestJson(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${encode(entityId)}" +
                "&props=labels%7Caliases%7Cclaims&languages=en%7Cpt-br%7Cpt&format=json&origin=*"
        )
        val entity = root.optJSONObject("entities")?.optJSONObject(entityId) ?: return null
        val labelsAndAliases = buildList {
            val labels = entity.optJSONObject("labels")
            listOf("en", "pt-br", "pt").forEach { language ->
                labels?.optJSONObject(language)?.optString("value")?.takeIf(String::isNotBlank)?.let(::add)
            }
            val aliases = entity.optJSONObject("aliases")
            listOf("en", "pt-br", "pt").forEach { language ->
                val values = aliases?.optJSONArray(language)
                if (values != null) {
                    for (i in 0 until values.length()) {
                        values.optJSONObject(i)?.optString("value")?.takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }
        }.distinct()
        val claims = entity.optJSONObject("claims") ?: return null
        val imageFileName = claimStringValue(claims.optJSONArray("P18"))
        val manufacturerIds = claimEntityIds(claims.optJSONArray("P176"))
        if (manufacturerIds.isEmpty()) return null
        val manufacturerNames = loadEntityLabels(manufacturerIds)
        return DeviceImageEntityCandidate(
            entityId = entityId,
            labelsAndAliases = labelsAndAliases,
            manufacturerNames = manufacturerNames,
            imageFileName = imageFileName,
        )
    }

    private fun loadEntityLabels(ids: List<String>): List<String> {
        if (ids.isEmpty()) return emptyList()
        val root = requestJson(
            "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${encode(ids.joinToString("|"))}" +
                "&props=labels&languages=en%7Cpt-br%7Cpt&format=json&origin=*"
        )
        val entities = root.optJSONObject("entities") ?: return emptyList()
        return buildList {
            ids.forEach { id ->
                val labels = entities.optJSONObject(id)?.optJSONObject("labels")
                listOf("en", "pt-br", "pt").forEach { language ->
                    labels?.optJSONObject(language)?.optString("value")?.takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }.distinct()
    }

    private fun loadCommonsMetadata(imageFileName: String): CommonsMetadata? {
        val title = if (imageFileName.startsWith("File:", ignoreCase = true)) imageFileName else "File:$imageFileName"
        val root = requestJson(
            "https://commons.wikimedia.org/w/api.php?action=query&prop=imageinfo&titles=${encode(title)}" +
                "&iiprop=url%7Cextmetadata&iiurlwidth=320&format=json&origin=*"
        )
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
        val keys = pages.keys()
        while (keys.hasNext()) {
            val page = pages.optJSONObject(keys.next()) ?: continue
            val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val thumbUrl = info.optString("thumburl").takeIf { it.startsWith("https://") } ?: continue
            val sourceUrl = info.optString("descriptionurl").takeIf { it.startsWith("https://") }
                ?: info.optString("url").takeIf { it.startsWith("https://") }
                ?: continue
            val metadata = info.optJSONObject("extmetadata")
            val license = metadata.metadataValue("LicenseShortName") ?: metadata.metadataValue("UsageTerms")
            val author = cleanMetadata(metadata.metadataValue("Artist") ?: metadata.metadataValue("Credit"))
            return CommonsMetadata(
                thumbnailUrl = thumbUrl,
                sourceUrl = sourceUrl,
                license = cleanMetadata(license),
                author = author,
            )
        }
        return null
    }

    private fun requestJson(url: String): JSONObject {
        require(url.startsWith("https://")) { "insecure URL" }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return connection.useConnection {
            require(responseCode in 200..299) { "HTTP $responseCode" }
            val length = contentLengthLong
            require(length <= 0L || length <= MAX_JSON_BYTES) { "response too large" }
            val text = inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                val result = reader.readText()
                require(result.toByteArray(StandardCharsets.UTF_8).size <= MAX_JSON_BYTES) { "response too large" }
                result
            }
            JSONObject(text)
        }
    }

    private fun claimStringValue(claims: org.json.JSONArray?): String? {
        if (claims == null) return null
        for (i in 0 until claims.length()) {
            val value = claims.optJSONObject(i)
                ?.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.opt("value")
            if (value is String && value.isNotBlank()) return value
        }
        return null
    }

    private fun claimEntityIds(claims: org.json.JSONArray?): List<String> {
        if (claims == null) return emptyList()
        return buildList {
            for (i in 0 until claims.length()) {
                val value = claims.optJSONObject(i)
                    ?.optJSONObject("mainsnak")
                    ?.optJSONObject("datavalue")
                    ?.optJSONObject("value")
                val numericId = value?.optLong("numeric-id", -1L) ?: -1L
                if (numericId > 0L) add("Q$numericId")
            }
        }
    }

    private fun JSONObject?.metadataValue(key: String): String? = this
        ?.optJSONObject(key)
        ?.optString("value")
        ?.takeIf(String::isNotBlank)

    private fun cleanMetadata(value: String?): String? {
        if (value.isNullOrBlank()) return null
        @Suppress("DEPRECATION")
        return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf(String::isNotBlank)
    }

    private data class CommonsMetadata(
        val thumbnailUrl: String,
        val sourceUrl: String,
        val license: String?,
        val author: String?,
    )

    companion object {
        private const val USER_AGENT = "ExploradorXP-DeviceImage/1.0 (public Wikimedia lookup)"
        private const val CONNECT_TIMEOUT_MS = 7_000
        private const val READ_TIMEOUT_MS = 12_000
        private const val MAX_JSON_BYTES = 1_500_000L
        private const val MAX_IMAGE_BYTES = 1_500_000L
    }
}

private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

private fun safeImageLog(value: String): String = value
    .replace(Regex("[\\r\\n\\t]"), " ")
    .take(96)
