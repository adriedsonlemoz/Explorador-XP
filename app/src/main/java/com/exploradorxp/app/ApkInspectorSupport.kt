package com.exploradorxp.app

import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

internal enum class ApkVersionRelation {
    NOT_INSTALLED,
    UPGRADE,
    SAME,
    DOWNGRADE,
}

internal enum class ApkSignatureRelation {
    NOT_APPLICABLE,
    MATCH,
    MISMATCH,
    UNKNOWN,
}

internal fun apkVersionRelation(apkVersionCode: Long, installedVersionCode: Long?): ApkVersionRelation = when {
    installedVersionCode == null -> ApkVersionRelation.NOT_INSTALLED
    apkVersionCode > installedVersionCode -> ApkVersionRelation.UPGRADE
    apkVersionCode == installedVersionCode -> ApkVersionRelation.SAME
    else -> ApkVersionRelation.DOWNGRADE
}

internal fun apkSignatureRelation(
    apkDigests: Collection<String>,
    installedDigests: Collection<String>?,
): ApkSignatureRelation {
    if (installedDigests == null) return ApkSignatureRelation.NOT_APPLICABLE
    if (apkDigests.isEmpty() || installedDigests.isEmpty()) return ApkSignatureRelation.UNKNOWN
    val installed = installedDigests.toHashSet()
    return if (apkDigests.any(installed::contains)) ApkSignatureRelation.MATCH else ApkSignatureRelation.MISMATCH
}

internal fun collectApkAbis(file: File): List<String> {
    if (!file.exists() || !file.isFile) return emptyList()
    return runCatching {
        ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith("lib/") && it.endsWith(".so", ignoreCase = true) }
                .mapNotNull { entry ->
                    val rest = entry.removePrefix("lib/")
                    rest.substringBefore('/').takeIf { abi -> abi.isNotBlank() && '/' in rest }
                }
                .distinct()
                .sortedWith(compareBy<String> { abiRank(it) }.thenBy { it })
                .toList()
        }
    }.getOrDefault(emptyList())
}

internal fun isApkAbiCompatible(apkAbis: Collection<String>, deviceAbis: Collection<String>): Boolean {
    if (apkAbis.isEmpty()) return true
    if (deviceAbis.isEmpty()) return false
    val supported = deviceAbis.map { it.lowercase(Locale.ROOT) }.toHashSet()
    return apkAbis.any { it.lowercase(Locale.ROOT) in supported }
}

internal fun sha256Hex(bytes: ByteArray): String = MessageDigest
    .getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { "%02X".format(Locale.ROOT, it.toInt() and 0xFF) }

internal fun sha256File(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02X".format(Locale.ROOT, it.toInt() and 0xFF) }
}

internal fun compactSha256(digest: String): String {
    val normalized = digest.filter { it.isLetterOrDigit() }.uppercase(Locale.ROOT)
    if (normalized.length <= 24) return normalized
    return normalized.chunked(2).take(12).joinToString(":") + "…"
}

internal fun formatAbiList(abis: Collection<String>): String = when {
    abis.isEmpty() -> "Sem bibliotecas nativas"
    else -> abis.joinToString(", ")
}

private fun abiRank(abi: String): Int = when (abi.lowercase(Locale.ROOT)) {
    "arm64-v8a" -> 0
    "armeabi-v7a" -> 1
    "x86_64" -> 2
    "x86" -> 3
    else -> 10
}
