package com.exploradorxp.app

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import java.io.File

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
    val installedVersion: String?,
    val installedVersionCode: Long?,
    val installedTargetSdk: Int?,
    val canLaunchInstalled: Boolean,
    val iconBitmap: Bitmap?,
    val apkAbis: List<String>,
    val deviceAbis: List<String>,
    val requestedPermissions: List<ApkPermission>,
    val signerDigests: List<String>,
    val installedSignerDigests: List<String>?,
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

    return ApkInfo(
        appName = runCatching { appInfo?.loadLabel(pm)?.toString() }.getOrNull().orEmpty()
            .ifBlank { file.nameWithoutExtension },
        packageName = packageName,
        versionName = archiveInfo.versionName ?: "Desconhecida",
        versionCode = apkVersionCode,
        minSdk = appInfo?.minSdkVersion,
        targetSdk = appInfo?.targetSdkVersion,
        installedVersion = installedInfo?.versionName,
        installedVersionCode = installedVersionCode,
        installedTargetSdk = installedInfo?.applicationInfo?.targetSdkVersion,
        canLaunchInstalled = runCatching { pm.getLaunchIntentForPackage(packageName) != null }.getOrDefault(false),
        iconBitmap = iconBitmap,
        apkAbis = collectApkAbis(file),
        deviceAbis = Build.SUPPORTED_ABIS.toList(),
        requestedPermissions = archiveInfo.requestedPermissions.orEmpty()
            .distinct()
            .map { describePermission(pm, it) }
            .sortedWith(compareByDescending<ApkPermission> { it.dangerous }.thenBy { it.label.lowercase() }),
        signerDigests = signingDigests(archiveInfo),
        installedSignerDigests = installedInfo?.let(::signingDigests),
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
private fun signingDigests(info: PackageInfo): List<String> {
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
    return signatures.map { sha256Hex(it.toByteArray()) }.distinct()
}

@Suppress("DEPRECATION")
private fun describePermission(pm: PackageManager, name: String): ApkPermission {
    val info = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPermissionInfo(name, PackageManager.PermissionInfoFlags.of(0))
        } else {
            pm.getPermissionInfo(name, 0)
        }
    }.getOrNull()
    val label = runCatching { info?.loadLabel(pm)?.toString() }.getOrNull()
        .orEmpty()
        .ifBlank { name.removePrefix("android.permission.").replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() } }
    val dangerous = info?.let {
        (it.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
    } ?: false
    return ApkPermission(name = name, label = label, dangerous = dangerous)
}
