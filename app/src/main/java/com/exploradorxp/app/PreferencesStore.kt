package com.exploradorxp.app

import android.content.Context
import org.json.JSONArray
import java.io.File

class PreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("explorador_xp", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()

    fun toggleFavorite(file: File): Boolean {
        val current = favorites().toMutableSet()
        val added = if (current.contains(file.absolutePath)) {
            current.remove(file.absolutePath)
            false
        } else {
            current.add(file.absolutePath)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return added
    }

    fun showHidden(): Boolean = prefs.getBoolean(KEY_SHOW_HIDDEN, false)

    fun setShowHidden(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN, show).apply()
    }

    fun foldersFirst(): Boolean = prefs.getBoolean(KEY_FOLDERS_FIRST, true)

    fun setFoldersFirst(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOLDERS_FIRST, enabled).apply()
    }

    fun lastSeenUpdateVersionCode(): Int = prefs.getInt(KEY_LAST_SEEN_UPDATE_VERSION_CODE, -1)

    fun shouldShowUpdateHighlights(currentVersionCode: Int, installedThroughUpdate: Boolean): Boolean =
        installedThroughUpdate && currentVersionCode > lastSeenUpdateVersionCode()

    fun markUpdateVersionSeen(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_SEEN_UPDATE_VERSION_CODE, versionCode).apply()
    }

    fun lastSeenUsageAccessEducationVersionCode(): Int =
        prefs.getInt(KEY_LAST_SEEN_USAGE_ACCESS_EDUCATION_VERSION_CODE, -1)

    fun markUsageAccessEducationSeen(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_SEEN_USAGE_ACCESS_EDUCATION_VERSION_CODE, versionCode).apply()
    }

    fun deviceImagesEnabled(): Boolean = prefs.getBoolean(KEY_DEVICE_IMAGES_ENABLED, true)

    fun setDeviceImagesEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVICE_IMAGES_ENABLED, enabled).apply()
    }

    fun addRecent(file: File) {
        val items = recents().toMutableList()
        items.remove(file.absolutePath)
        items.add(0, file.absolutePath)
        while (items.size > 50) items.removeAt(items.lastIndex)
        val array = JSONArray()
        items.forEach(array::put)
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }

    fun recents(): List<String> {
        val raw = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(array.getString(i))
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENTS = "recents"
        private const val KEY_SHOW_HIDDEN = "show_hidden"
        private const val KEY_FOLDERS_FIRST = "folders_first"
        private const val KEY_LAST_SEEN_UPDATE_VERSION_CODE = "last_seen_update_version_code"
        private const val KEY_LAST_SEEN_USAGE_ACCESS_EDUCATION_VERSION_CODE = "last_seen_usage_access_education_version_code"
        private const val KEY_DEVICE_IMAGES_ENABLED = "device_images_enabled"
    }
}
