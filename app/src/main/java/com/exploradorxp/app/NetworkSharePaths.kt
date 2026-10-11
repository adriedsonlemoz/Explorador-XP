package com.exploradorxp.app

import java.net.URLDecoder
import java.net.URLEncoder

/** Independent of Android and the transport: reject traversal, separators and ambiguous names. */
internal object NetworkSharePaths {
    const val PREFIX = "dav"
    private val aliasRegex = Regex("[a-z0-9-]{1,40}")

    fun validAlias(alias: String): Boolean = aliasRegex.matches(alias)

    fun validName(name: String): Boolean = name.isNotBlank() && name != "." && name != ".." &&
        name.length <= 200 && name.none { it == '/' || it == '\\' || it == '\u0000' || it.code < 32 }

    fun parseRequestPath(requestTarget: String): List<String>? {
        if (!requestTarget.startsWith('/') || requestTarget.length > 2048 || requestTarget.contains('#')) return null
        val path = requestTarget.substringBefore('?')
        val parts = path.split('/').filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.first() != PREFIX || parts.size > 24) return null
        return parts.drop(1).map { encoded ->
            if (encoded.length > 600) return null
            val name = try {
                // URLDecoder is designed for form values; in a URI path '+' is literal.
                URLDecoder.decode(encoded.replace("+", "%2B"), "UTF-8")
            } catch (_: IllegalArgumentException) { return null }
            if (!validName(name)) return null
            name
        }
    }

    fun urlPath(segments: List<String>, directory: Boolean): String = buildString {
        append("/dav/")
        append(segments.joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") })
        if (directory && segments.isNotEmpty()) append('/')
    }
}
