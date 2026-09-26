package com.exploradorxp.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

internal data class ApkPermission(
    val name: String,
    val label: String,
    val dangerous: Boolean,
)

internal data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int?,
    val targetSdk: Int?,
    val compileSdk: Int?,
    val installedVersion: String?,
    val installedVersionCode: Long?,
    val installedTargetSdk: Int?,
    val installedCompileSdk: Int?,
    val installedPackageBytes: Long?,
    val canLaunchInstalled: Boolean,
    val iconBitmap: Bitmap?,
    val apkAbis: List<String>,
    val deviceAbis: List<String>,
    val requestedPermissions: List<ApkPermission>,
    val installedPermissions: List<ApkPermission>,
    val signerDigests: List<String>,
    val installedSignerDigests: List<String>?,
    val signerName: String?,
    val installedSignerName: String?,
    val apkFileSha256: String,
) {
    val versionRelation: ApkVersionRelation
        get() = apkVersionRelation(versionCode, installedVersionCode)

    val signatureRelation: ApkSignatureRelation
        get() = apkSignatureRelation(signerDigests, installedSignerDigests)

    val androidCompatible: Boolean
        get() = minSdk == null || minSdk <= Build.VERSION.SDK_INT

    val abiCompatible: Boolean
        get() = isApkAbiCompatible(apkAbis, deviceAbis)

    val hasNativeLibraries: Boolean
        get() = apkAbis.isNotEmpty()

    val dangerousPermissionCount: Int
        get() = requestedPermissions.count { it.dangerous }

    val newPermissions: List<ApkPermission>
        get() {
            if (installedVersion == null) return emptyList()
            val installedNames = installedPermissions.mapTo(hashSetOf()) { it.name }
            return requestedPermissions.filterNot { it.name in installedNames }
        }

    val removedPermissions: List<ApkPermission>
        get() {
            if (installedVersion == null) return emptyList()
            val apkNames = requestedPermissions.mapTo(hashSetOf()) { it.name }
            return installedPermissions.filterNot { it.name in apkNames }
        }

    val newDangerousPermissionCount: Int
        get() = newPermissions.count { it.dangerous }

    val canAttemptInstall: Boolean
        get() = androidCompatible && abiCompatible &&
            signatureRelation != ApkSignatureRelation.MISMATCH &&
            versionRelation != ApkVersionRelation.DOWNGRADE
}

internal fun inspectApk(context: Context, file: File): ApkInfo {
    require(file.exists() && file.isFile) { "O APK não existe mais." }
    val pm = context.packageManager
    val archiveInfo = getArchivePackageInfo(pm, file)
        ?: error("O Android não conseguiu ler os metadados deste APK.")
    val appInfo = archiveInfo.applicationInfo?.also {
        it.sourceDir = file.absolutePath
        it.publicSourceDir = file.absolutePath
    }
    val packageName = archiveInfo.packageName
    val installedInfo = getInstalledPackageInfo(pm, packageName)
    val apkVersionCode = packageVersionCode(archiveInfo)
    val installedVersionCode = installedInfo?.let(::packageVersionCode)
    val iconBitmap = runCatching {
        appInfo?.loadIcon(pm)?.toBitmap(width = 144, height = 144, config = Bitmap.Config.ARGB_8888)
    }.getOrNull()

    val requestedPermissions = archiveInfo.requestedPermissions.orEmpty()
        .distinct()
        .map { describePermission(pm, it) }
        .sortedWith(compareByDescending<ApkPermission> { it.dangerous }.thenBy { it.label.lowercase() })
    val installedPermissions = installedInfo?.requestedPermissions.orEmpty()
        .distinct()
        .map { describePermission(pm, it) }
        .sortedWith(compareByDescending<ApkPermission> { it.dangerous }.thenBy { it.label.lowercase() })

    return ApkInfo(
        appName = runCatching { appInfo?.loadLabel(pm)?.toString() }.getOrNull().orEmpty()
            .ifBlank { file.nameWithoutExtension },
        packageName = packageName,
        versionName = archiveInfo.versionName ?: "Desconhecida",
        versionCode = apkVersionCode,
        minSdk = appInfo?.minSdkVersion,
        targetSdk = appInfo?.targetSdkVersion,
        compileSdk = applicationCompileSdk(appInfo),
        installedVersion = installedInfo?.versionName,
        installedVersionCode = installedVersionCode,
        installedTargetSdk = installedInfo?.applicationInfo?.targetSdkVersion,
        installedCompileSdk = applicationCompileSdk(installedInfo?.applicationInfo),
        installedPackageBytes = installedInfo?.let(::installedPackageBytes),
        canLaunchInstalled = runCatching { pm.getLaunchIntentForPackage(packageName) != null }.getOrDefault(false),
        iconBitmap = iconBitmap,
        apkAbis = collectApkAbis(file),
        deviceAbis = Build.SUPPORTED_ABIS.toList(),
        requestedPermissions = requestedPermissions,
        installedPermissions = installedPermissions,
        signerDigests = signingDigests(archiveInfo),
        installedSignerDigests = installedInfo?.let(::signingDigests),
        signerName = signerCommonName(archiveInfo),
        installedSignerName = installedInfo?.let(::signerCommonName),
        apkFileSha256 = sha256ApkFile(file),
    )
}

@Suppress("DEPRECATION")
private fun getArchivePackageInfo(pm: PackageManager, file: File): PackageInfo? {
    val flags = PackageManager.GET_META_DATA.toLong() or
        PackageManager.GET_PERMISSIONS.toLong() or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        } else {
            PackageManager.GET_SIGNATURES.toLong()
        }
    return if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(flags))
    } else {
        pm.getPackageArchiveInfo(file.absolutePath, flags.toInt())
    }
}

@Suppress("DEPRECATION")
private fun getInstalledPackageInfo(pm: PackageManager, packageName: String): PackageInfo? {
    val flags = PackageManager.GET_PERMISSIONS.toLong() or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        } else {
            PackageManager.GET_SIGNATURES.toLong()
        }
    return runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
        } else {
            pm.getPackageInfo(packageName, flags.toInt())
        }
    }.getOrNull()
}

@Suppress("DEPRECATION")
private fun packageVersionCode(info: PackageInfo): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()

@Suppress("DEPRECATION")
private fun signingDigests(info: PackageInfo): List<String> = signingCertificateBytes(info)
    .map(::sha256Hex)
    .distinct()

@Suppress("DEPRECATION")
private fun signingCertificateBytes(info: PackageInfo): List<ByteArray> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = info.signingInfo ?: return emptyList()
        if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
    } else {
        info.signatures
    } ?: return emptyList()
    return signatures.map { it.toByteArray() }
}

private fun signerCommonName(info: PackageInfo): String? {
    val bytes = signingCertificateBytes(info).firstOrNull() ?: return null
    return runCatching {
        val certificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(bytes.inputStream()) as X509Certificate
        val subject = certificate.subjectX500Principal.name
        subject.split(',')
            .map { it.trim() }
            .firstOrNull { it.startsWith("CN=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: subject.takeIf { it.isNotBlank() }
    }.getOrNull()
}

private fun applicationCompileSdk(info: ApplicationInfo?): Int? {
    if (info == null) return null
    return runCatching {
        ApplicationInfo::class.java.getField("compileSdkVersion")
            .getInt(info)
            .takeIf { it > 0 }
    }.getOrNull()
}

private fun installedPackageBytes(info: PackageInfo): Long? {
    val app = info.applicationInfo ?: return null
    val paths = buildList {
        app.sourceDir?.let { add(it) }
        app.splitSourceDirs.orEmpty().forEach { add(it) }
    }.distinct()
    if (paths.isEmpty()) return null
    return paths.sumOf { path -> File(path).takeIf { it.isFile }?.length() ?: 0L }
        .takeIf { it > 0L }
}

@Suppress("DEPRECATION")
private fun describePermission(pm: PackageManager, name: String): ApkPermission {
    val info = runCatching { pm.getPermissionInfo(name, 0) }.getOrNull()
    val label = runCatching { info?.loadLabel(pm)?.toString() }.getOrNull()
        .orEmpty()
        .ifBlank { name.removePrefix("android.permission.").replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } }
    val dangerous = info?.let {
        (it.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
    } ?: false
    return ApkPermission(name = name, label = label, dangerous = dangerous)
}
