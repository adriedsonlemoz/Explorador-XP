package com.exploradorxp.app

import java.util.Locale

internal enum class AppManagerFilter { ALL, USER, SYSTEM }
internal enum class AppManagerSort { NAME, SIZE }

internal data class ManagedApp(
    val label: String,
    val packageName: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val isUpdatedSystem: Boolean,
    val enabled: Boolean,
    val apkBytes: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val installerPackage: String?,
    val canLaunch: Boolean,
    val isOwnPackage: Boolean,
)

internal data class AppStorageInfo(
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
) {
    // getDataBytes() já inclui cache. Somar cache novamente inflaria o total.
    val totalBytes: Long get() = appBytes + dataBytes
}

internal data class ManagedPermission(
    val name: String,
    val granted: Boolean,
)

internal object AppManagerLogic {
    fun filterAndSort(
        apps: List<ManagedApp>,
        query: String,
        filter: AppManagerFilter,
        sort: AppManagerSort,
        storage: Map<String, AppStorageInfo>,
    ): List<ManagedApp> {
        val needle = query.trim().lowercase(Locale.ROOT)
        val filtered = apps.filter { app ->
            val typeMatches = when (filter) {
                AppManagerFilter.ALL -> true
                AppManagerFilter.USER -> !app.isSystem
                AppManagerFilter.SYSTEM -> app.isSystem
            }
            val searchMatches = needle.isBlank() ||
                app.label.lowercase(Locale.ROOT).contains(needle) ||
                app.packageName.lowercase(Locale.ROOT).contains(needle)
            typeMatches && searchMatches
        }
        return when (sort) {
            AppManagerSort.NAME -> filtered.sortedWith(
                compareBy<ManagedApp, String>(String.CASE_INSENSITIVE_ORDER) { it.label }
                    .thenBy { it.packageName }
            )
            AppManagerSort.SIZE -> filtered.sortedWith(
                compareByDescending<ManagedApp> { storage[it.packageName]?.totalBytes ?: it.apkBytes }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
            )
        }
    }

    fun displayBytes(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "Não disponível"
        if (bytes < 1024L) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = -1
        while (value >= 1024.0 && index < units.lastIndex) {
            value /= 1024.0
            index++
        }
        return if (value >= 100.0 || value % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%.0f %s", value, units[index])
        } else {
            String.format(Locale.getDefault(), "%.1f %s", value, units[index])
        }
    }
}

