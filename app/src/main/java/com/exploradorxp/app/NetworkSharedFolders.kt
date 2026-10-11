package com.exploradorxp.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

internal data class NetworkSharedFolder(val alias: String, val name: String, val uri: Uri)

/** Persist only explicit SAF tree grants. Never expose unselected filesystem roots. */
internal class NetworkSharedFolders(private val context: Context) {
    private val preferences = context.getSharedPreferences("network_shares_alpha95", Context.MODE_PRIVATE)

    fun list(): List<NetworkSharedFolder> {
        val json = preferences.getString("folders", "[]") ?: "[]"
        return runCatching {
            val items = JSONArray(json)
            (0 until items.length()).mapNotNull { index ->
                val item = items.getJSONObject(index)
                val alias = item.optString("alias")
                val uri = Uri.parse(item.optString("uri"))
                if (!NetworkSharePaths.validAlias(alias) || uri.scheme != "content") null
                else if (context.contentResolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission }) null
                else NetworkSharedFolder(alias, item.optString("name", alias), uri)
            }
        }.getOrDefault(emptyList())
    }

    fun add(uri: Uri): NetworkSharedFolder {
        require(uri.scheme == "content") { "Somente pastas selecionadas pelo Android." }
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Read-only document providers can still be shared for browsing/download.
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val tree = DocumentFile.fromTreeUri(context, uri)
        require(tree != null && tree.isDirectory) { "A pasta não está disponível." }
        val before = list()
        require(before.none { it.uri == uri }) { "Essa pasta já está compartilhada." }
        val random = ByteArray(6).also { SecureRandom().nextBytes(it) }
        val alias = "pasta-" + random.joinToString("") { "%02x".format(it) }
        val item = NetworkSharedFolder(alias, tree.name ?: "Pasta", uri)
        save(before + item)
        return item
    }

    fun remove(alias: String) {
        val current = list()
        val match = current.firstOrNull { it.alias == alias } ?: return
        save(current.filterNot { it.alias == alias })
        if (current.count { it.uri == match.uri } == 1) {
            // Revoke each persisted grant independently; read-only providers have no WRITE grant.
            runCatching {
                context.contentResolver.releasePersistableUriPermission(match.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.contentResolver.releasePersistableUriPermission(match.uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    }

    private fun save(items: List<NetworkSharedFolder>) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("alias", it.alias).put("name", it.name).put("uri", it.uri.toString())) }
        check(preferences.edit().putString("folders", array.toString()).commit()) { "Não foi possível salvar pastas." }
    }
}
