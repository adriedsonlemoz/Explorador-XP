package com.exploradorxp.app

import java.time.Instant
import java.time.ZoneId

/**
 * Regras puras usadas pela busca avançada. Manter os filtros fora da UI evita que
 * o Explorer acumule condicionais e permite validar a pesquisa com testes JVM.
 */
object AdvancedSearchMatcher {
    fun matches(
        name: String,
        extension: String,
        isDirectory: Boolean,
        size: Long,
        modifiedAt: Long,
        query: String,
        filters: AdvancedSearchFilters,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isNotBlank() && !name.contains(normalizedQuery, ignoreCase = true)) return false

        when (filters.itemKind) {
            SearchItemKind.ALL -> Unit
            SearchItemKind.FILES -> if (isDirectory) return false
            SearchItemKind.FOLDERS -> if (!isDirectory) return false
        }

        if (!matchesFileType(extension, isDirectory, filters.fileType)) return false

        val expectedExtension = normalizeExtension(filters.extension)
        if (expectedExtension.isNotBlank()) {
            val normalizedName = name.lowercase()
            if (isDirectory || !normalizedName.endsWith(".$expectedExtension")) return false
        }

        if (filters.minSizeBytes != null || filters.maxSizeBytes != null) {
            if (isDirectory) return false
            filters.minSizeBytes?.let { if (size < it) return false }
            filters.maxSizeBytes?.let { if (size > it) return false }
        }

        val cutoff = dateCutoffMillis(filters.dateRange, nowMillis, zoneId)
        if (cutoff != null && modifiedAt < cutoff) return false

        return true
    }

    fun normalizeExtension(value: String): String = value.trim().removePrefix(".").lowercase()

    fun dateCutoffMillis(
        range: SearchDateRange,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? = when (range) {
        SearchDateRange.ANY -> null
        SearchDateRange.TODAY -> Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        SearchDateRange.LAST_7_DAYS -> nowMillis - 7L * DAY_MILLIS
        SearchDateRange.LAST_30_DAYS -> nowMillis - 30L * DAY_MILLIS
    }

    private fun matchesFileType(extension: String, isDirectory: Boolean, type: SearchFileType): Boolean {
        if (type == SearchFileType.ALL) return true
        if (isDirectory) return false
        val category = FileTypeClassifier.storageCategory(extension).first
        return when (type) {
            SearchFileType.ALL -> true
            SearchFileType.IMAGES -> category == "images"
            SearchFileType.VIDEOS -> category == "videos"
            SearchFileType.AUDIO -> category == "audio"
            SearchFileType.DOCUMENTS -> category == "documents"
            SearchFileType.APKS -> category == "apps"
            SearchFileType.ARCHIVES -> category == "archives"
            SearchFileType.OTHER -> category == "other"
        }
    }

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
}
