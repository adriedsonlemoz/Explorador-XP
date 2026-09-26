package com.exploradorxp.app

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

internal class InstalledAppsRepository(private val context: Context) {
    private val pm = context.packageManager

    suspend fun loadApps(): List<ManagedApp> = withContext(Dispatchers.IO) {
        installedApplications().mapNotNull { appInfo ->
            runCatching { buildManagedApp(appInfo) }.getOrNull()
        }.sortedWith(compareBy<ManagedApp, String>(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    suspend fun loadStorageInfo(apps: List<ManagedApp>): Map<String, AppStorageInfo> {
        if (!hasUsageAccess()) return emptyMap()
        val semaphore = Semaphore(4)
        return coroutineScope {
            apps.map { app ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        app.packageName to queryStorage(app.packageName)
                    }
                }
            }.awaitAll().mapNotNull { (packageName, info) ->
                info?.let { packageName to it }
            }.toMap()
        }
    }

    suspend fun loadPermissions(packageName: String): List<ManagedPermission> = withContext(Dispatchers.IO) {
        val info = getPackageInfo(packageName, PackageManager.GET_PERMISSIONS) ?: return@withContext emptyList()
        info.requestedPermissions.orEmpty().map { permission ->
            ManagedPermission(
                name = permission,
                granted = pm.checkPermission(permission, packageName) == PackageManager.PERMISSION_GRANTED,
            )
        }.sortedWith(compareBy<ManagedPermission, String>(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun installedApplications(): List<ApplicationInfo> = if (Build.VERSION.SDK_INT >= 33) {
        pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.getInstalledApplications(0)
    }

    private fun buildManagedApp(appInfo: ApplicationInfo): ManagedApp {
        val packageInfo = getPackageInfo(appInfo.packageName, 0) ?: throw PackageManager.NameNotFoundException(appInfo.packageName)
        val label = runCatching { appInfo.loadLabel(pm).toString().trim() }
            .getOrDefault(appInfo.packageName)
            .ifBlank { appInfo.packageName }
        val system = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val updatedSystem = appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        val apkBytes = sequenceOf(appInfo.sourceDir)
            .plus(appInfo.splitSourceDirs.orEmpty().asSequence())
            .filterNotNull()
            .distinct()
            .sumOf { path -> runCatching { File(path).length() }.getOrDefault(0L).coerceAtLeast(0L) }

        val versionCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= 30) {
                pm.getInstallSourceInfo(appInfo.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(appInfo.packageName)
            }
        }.getOrNull()

        return ManagedApp(
            label = label,
            packageName = appInfo.packageName,
            versionName = packageInfo.versionName,
            versionCode = versionCode,
            isSystem = system,
            isUpdatedSystem = updatedSystem,
            enabled = appInfo.enabled,
            apkBytes = apkBytes,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            targetSdk = appInfo.targetSdkVersion,
            minSdk = appInfo.minSdkVersion,
            installerPackage = installer,
            canLaunch = pm.getLaunchIntentForPackage(appInfo.packageName) != null,
            isOwnPackage = appInfo.packageName == context.packageName,
        )
    }

    private fun queryStorage(packageName: String): AppStorageInfo? = runCatching {
        val appInfo = if (Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        val manager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
        val uuid = appInfo.storageUuid ?: StorageManager.UUID_DEFAULT
        val stats = manager.queryStatsForPackage(uuid, packageName, Process.myUserHandle())
        AppStorageInfo(
            appBytes = stats.appBytes.coerceAtLeast(0L),
            dataBytes = stats.dataBytes.coerceAtLeast(0L),
            cacheBytes = stats.cacheBytes.coerceAtLeast(0L),
        )
    }.getOrNull()

    private fun getPackageInfo(packageName: String, flags: Int): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }
    }.getOrNull()
}

@Composable
fun InstalledAppsManagerScreen(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { InstalledAppsRepository(context.applicationContext) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var apps by remember { mutableStateOf<List<ManagedApp>>(emptyList()) }
    var storage by remember { mutableStateOf<Map<String, AppStorageInfo>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var storageLoading by remember { mutableStateOf(false) }
    var usageAccess by remember { mutableStateOf(repository.hasUsageAccess()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppManagerFilter.ALL) }
    var sort by remember { mutableStateOf(AppManagerSort.NAME) }
    var selected by remember { mutableStateOf<ManagedApp?>(null) }

    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshToken++
    }
    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        selected = null
        refreshToken++
    }

    LaunchedEffect(refreshToken) {
        loading = true
        loadError = null
        storage = emptyMap()
        val loaded = runCatching { repository.loadApps() }
        apps = loaded.getOrElse {
            loadError = it.message ?: "Não foi possível carregar os aplicativos instalados."
            emptyList()
        }
        selected?.let { current ->
            selected = apps.firstOrNull { it.packageName == current.packageName }
        }
        usageAccess = repository.hasUsageAccess()
        loading = false
        if (usageAccess && apps.isNotEmpty()) {
            storageLoading = true
            storage = runCatching { repository.loadStorageInfo(apps) }.getOrDefault(emptyMap())
            storageLoading = false
        }
    }

    val visibleApps = remember(apps, storage, query, filter, sort) {
        AppManagerLogic.filterAndSort(apps, query, filter, sort, storage)
    }

    BackHandler(enabled = selected == null, onBack = onDismiss)

    if (selected != null) {
        InstalledAppDetailsScreen(
            app = selected!!,
            storageInfo = storage[selected!!.packageName],
            usageAccess = usageAccess,
            repository = repository,
            onBack = { selected = null },
            onRefresh = { refreshToken++ },
            onOpenUsageAccess = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                runCatching { settingsLauncher.launch(intent) }
                    .onFailure { settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            },
            onOpenSystemDetails = { packageName ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                runCatching { settingsLauncher.launch(intent) }
                    .onFailure { Toast.makeText(context, "Não foi possível abrir os detalhes do aplicativo.", Toast.LENGTH_SHORT).show() }
            },
            onUninstall = { packageName ->
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$packageName")).apply {
                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                }
                runCatching { uninstallLauncher.launch(intent) }
                    .onFailure { Toast.makeText(context, "O Android não disponibilizou a desinstalação deste aplicativo.", Toast.LENGTH_LONG).show() }
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB)),
    ) {
        AppManagerTitle(onDismiss = onDismiss, onRefresh = { refreshToken++ })
        HorizontalDivider(color = XpChromeBorder)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(XpControlBackground, RoundedCornerShape(6.dp))
                        .border(1.dp, XpControlBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isBlank()) {
                        Text("Buscar por nome ou pacote", fontSize = 12.sp, color = Color(0xFF78889B))
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF202020)),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppManagerChip("Todos", filter == AppManagerFilter.ALL) { filter = AppManagerFilter.ALL }
                AppManagerChip("Usuário", filter == AppManagerFilter.USER) { filter = AppManagerFilter.USER }
                AppManagerChip("Sistema", filter == AppManagerFilter.SYSTEM) { filter = AppManagerFilter.SYSTEM }
                Spacer(Modifier.weight(1f))
                AppManagerChip(if (sort == AppManagerSort.NAME) "Nome" else "Tamanho", true) {
                    sort = if (sort == AppManagerSort.NAME) AppManagerSort.SIZE else AppManagerSort.NAME
                }
            }
            Spacer(Modifier.height(7.dp))
            val userCount = apps.count { !it.isSystem }
            val systemCount = apps.count { it.isSystem }
            Text(
                "${apps.size} aplicativos • $userCount de usuário • $systemCount do sistema",
                fontSize = 11.sp,
                color = XpTextSecondary,
            )
            if (!usageAccess) {
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF8E6), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFE5C468), RoundedCornerShape(6.dp))
                        .clickable {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { settingsLauncher.launch(intent) }
                                .onFailure { settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        }
                        .padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CachedResourceIcon(R.drawable.info, "Informação", Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "Tamanho completo indisponível. Toque para conceder Acesso ao uso; sem ele, o Explorador XP mostra apenas o tamanho dos APKs instalados.",
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF5D4B16),
                        modifier = Modifier.weight(1f),
                    )
                }
            } else if (storageLoading) {
                Spacer(Modifier.height(6.dp))
                Text("Calculando código, dados e cache…", fontSize = 10.5.sp, color = XpTextSecondary)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("Carregando aplicativos…", fontSize = 12.sp, color = XpTextSecondary)
                }
            }
            loadError != null -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(loadError ?: "Erro ao carregar aplicativos.", color = Color(0xFF9B1C1C), fontSize = 13.sp)
            }
            visibleApps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum aplicativo encontrado.", fontSize = 13.sp, color = XpTextSecondary)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
            ) {
                items(visibleApps, key = { it.packageName }) { app ->
                    InstalledAppRow(
                        app = app,
                        storageInfo = storage[app.packageName],
                        onClick = { selected = app },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppManagerTitle(onDismiss: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(XpBlueDark)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color.White, RoundedCornerShape(6.dp))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            CachedResourceIcon(R.drawable.back, "Voltar", Modifier.size(20.dp))
        }
        Text(
            "Aplicativos instalados",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color.White, RoundedCornerShape(6.dp))
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center,
        ) {
            CachedResourceIcon(R.drawable.refresh, "Atualizar", Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AppManagerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color(0xFFDDEEFF) else Color.White)
            .border(1.dp, if (selected) XpBlue else XpControlBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 10.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF27435F))
    }
}

@Composable
private fun InstalledAppRow(app: ManagedApp, storageInfo: AppStorageInfo?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstalledAppIcon(app.packageName, Modifier.size(46.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202020),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                AppTypeBadge(
                    when {
                        app.isUpdatedSystem -> "Sistema atualizado"
                        app.isSystem -> "Sistema"
                        else -> "Usuário"
                    },
                    app.isSystem,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                app.packageName,
                fontSize = 10.5.sp,
                color = XpTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            val sizeText = storageInfo?.let { "Total ${AppManagerLogic.displayBytes(it.totalBytes)}" }
                ?: "APK ${AppManagerLogic.displayBytes(app.apkBytes)}"
            val version = app.versionName?.takeIf { it.isNotBlank() } ?: "sem nome de versão"
            Text(
                "$sizeText • $version (${app.versionCode})${if (!app.enabled) " • Desativado" else ""}",
                fontSize = 10.5.sp,
                color = if (app.enabled) Color(0xFF4D5D6F) else Color(0xFF9B5B1C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        CachedResourceIcon(R.drawable.xp_chevron_right, "Abrir detalhes", Modifier.size(18.dp))
    }
    HorizontalDivider(color = Color(0xFFE5EAF0))
}

@Composable
private fun InstalledAppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
        },
        update = { view ->
            val drawable = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
            if (drawable != null) view.setImageDrawable(drawable) else view.setImageResource(R.drawable.file_apk)
        },
    )
}

@Composable
private fun AppTypeBadge(text: String, system: Boolean) {
    Box(
        modifier = Modifier
            .background(if (system) Color(0xFFEAF0F7) else Color(0xFFE9F7ED), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, fontSize = 8.5.sp, color = if (system) Color(0xFF48617C) else Color(0xFF2D6A3F), maxLines = 1)
    }
}

@Composable
private fun InstalledAppDetailsScreen(
    app: ManagedApp,
    storageInfo: AppStorageInfo?,
    usageAccess: Boolean,
    repository: InstalledAppsRepository,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenSystemDetails: (String) -> Unit,
    onUninstall: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissions by remember(app.packageName) { mutableStateOf<List<ManagedPermission>>(emptyList()) }
    var permissionsLoading by remember(app.packageName) { mutableStateOf(true) }
    var showClearDataInfo by remember { mutableStateOf(false) }
    var showUninstallConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(app.packageName) {
        permissionsLoading = true
        permissions = runCatching { repository.loadPermissions(app.packageName) }.getOrDefault(emptyList())
        permissionsLoading = false
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(XpBlueDark)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(Color.White, RoundedCornerShape(6.dp)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                CachedResourceIcon(R.drawable.back, "Voltar", Modifier.size(20.dp))
            }
            Text("Detalhes do aplicativo", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(34.dp).background(Color.White, RoundedCornerShape(6.dp)).clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                CachedResourceIcon(R.drawable.refresh, "Atualizar", Modifier.size(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, XpCardBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InstalledAppIcon(app.packageName, Modifier.size(66.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2D3A))
                    Spacer(Modifier.height(3.dp))
                    Text(app.packageName, fontSize = 11.sp, color = XpTextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppTypeBadge(if (app.isUpdatedSystem) "Sistema atualizado" else if (app.isSystem) "Sistema" else "Usuário", app.isSystem)
                        AppTypeBadge(if (app.enabled) "Ativo" else "Desativado", !app.enabled)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            AppDetailsCard("Armazenamento") {
                if (storageInfo != null) {
                    AppDetailLine("Total", AppManagerLogic.displayBytes(storageInfo.totalBytes), boldValue = true)
                    AppDetailLine("Aplicativo/código", AppManagerLogic.displayBytes(storageInfo.appBytes))
                    AppDetailLine("Dados", AppManagerLogic.displayBytes(storageInfo.dataBytes))
                    AppDetailLine("Cache", AppManagerLogic.displayBytes(storageInfo.cacheBytes))
                    Text(
                        "O valor de Dados informado pelo Android já inclui o cache; por isso o total não soma Cache duas vezes.",
                        fontSize = 9.5.sp,
                        color = XpTextSecondary,
                        lineHeight = 13.sp,
                    )
                } else {
                    AppDetailLine("APKs instalados", AppManagerLogic.displayBytes(app.apkBytes), boldValue = true)
                    Text(
                        if (usageAccess) "O Android não forneceu estatísticas completas para este pacote."
                        else "Conceda Acesso ao uso para consultar código, dados e cache separadamente.",
                        fontSize = 10.5.sp,
                        color = XpTextSecondary,
                    )
                    if (!usageAccess) {
                        Spacer(Modifier.height(7.dp))
                        AppManagerActionButton("Permitir acesso ao uso", R.drawable.info, onClick = onOpenUsageAccess)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            AppDetailsCard("Informações") {
                AppDetailLine("Versão", app.versionName?.takeIf { it.isNotBlank() } ?: "Não informada")
                AppDetailLine("Version code", app.versionCode.toString())
                AppDetailLine("Tipo", if (app.isUpdatedSystem) "Aplicativo do sistema atualizado" else if (app.isSystem) "Aplicativo do sistema" else "Aplicativo do usuário")
                AppDetailLine("Estado", if (app.enabled) "Ativo" else "Desativado")
                AppDetailLine("Android alvo", "SDK ${app.targetSdk}")
                AppDetailLine("Android mínimo", "SDK ${app.minSdk}")
                AppDetailLine("Instalado", formatAppDate(app.firstInstallTime))
                AppDetailLine("Atualizado", formatAppDate(app.lastUpdateTime))
                AppDetailLine("Instalador", app.installerPackage ?: "Não informado pelo Android")
            }

            Spacer(Modifier.height(10.dp))
            AppDetailsCard("Ações") {
                if (app.canLaunch) {
                    AppManagerActionButton("Abrir aplicativo", R.drawable.file_apk) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(launchIntent) }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }
                AppManagerActionButton("Limpar dados", R.drawable.delete) { showClearDataInfo = true }
                Spacer(Modifier.height(7.dp))
                if (!app.isOwnPackage) {
                    AppManagerActionButton(
                        if (app.isSystem) "Desinstalar / remover atualizações" else "Desinstalar",
                        R.drawable.delete,
                        destructive = true,
                    ) { showUninstallConfirm = true }
                    Spacer(Modifier.height(7.dp))
                }
                AppManagerActionButton("Detalhes no Android", R.drawable.settings) {
                    onOpenSystemDetails(app.packageName)
                }
                Spacer(Modifier.height(7.dp))
                AppManagerActionButton("Copiar nome do pacote", R.drawable.copy) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Pacote", app.packageName))
                    Toast.makeText(context, "Nome do pacote copiado.", Toast.LENGTH_SHORT).show()
                }
                if (app.isOwnPackage) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "A desinstalação do próprio Explorador XP não é oferecida aqui porque encerraria o gerenciador durante a operação.",
                        fontSize = 10.sp,
                        color = XpTextSecondary,
                        lineHeight = 14.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            AppDetailsCard("Permissões declaradas") {
                when {
                    permissionsLoading -> Text("Carregando permissões…", fontSize = 10.5.sp, color = XpTextSecondary)
                    permissions.isEmpty() -> Text("Nenhuma permissão declarada pelo pacote.", fontSize = 10.5.sp, color = XpTextSecondary)
                    else -> permissions.forEachIndexed { index, permission ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (permission.granted) Color(0xFF3A8B4D) else Color(0xFFB08A2E), RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text(permission.name, fontSize = 10.5.sp, color = Color(0xFF303B46))
                                Text(if (permission.granted) "Concedida" else "Não concedida", fontSize = 9.5.sp, color = XpTextSecondary)
                            }
                        }
                        if (index != permissions.lastIndex) HorizontalDivider(color = Color(0xFFEDF0F4))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }

    if (showClearDataInfo) {
        AlertDialog(
            onDismissRequest = { showClearDataInfo = false },
            title = { Text("Limpar dados do aplicativo") },
            text = {
                Text(
                    "O Android não permite que um aplicativo comum apague diretamente os dados privados de outro aplicativo. O Explorador XP abrirá os detalhes deste app no Android para você concluir a limpeza. Essa ação pode remover contas, configurações e arquivos privados do aplicativo."
                )
            },
            confirmButton = {
                Text(
                    "Continuar",
                    color = XpBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        showClearDataInfo = false
                        onOpenSystemDetails(app.packageName)
                    }.padding(10.dp),
                )
            },
            dismissButton = {
                Text("Cancelar", modifier = Modifier.clickable { showClearDataInfo = false }.padding(10.dp))
            },
        )
    }

    if (showUninstallConfirm) {
        AlertDialog(
            onDismissRequest = { showUninstallConfirm = false },
            title = { Text(if (app.isSystem) "Remover aplicativo do sistema?" else "Desinstalar aplicativo?") },
            text = {
                Text(
                    if (app.isSystem) {
                        "O Android decidirá se este pacote pode ser removido. Em aplicativos pré-instalados, a ação pode apenas remover atualizações ou ser bloqueada pelo sistema. Nenhuma remoção silenciosa será tentada."
                    } else {
                        "O Android exibirá a confirmação final de desinstalação. Dados privados do aplicativo podem ser apagados."
                    }
                )
            },
            confirmButton = {
                Text(
                    "Continuar",
                    color = Color(0xFFB32323),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        showUninstallConfirm = false
                        onUninstall(app.packageName)
                    }.padding(10.dp),
                )
            },
            dismissButton = {
                Text("Cancelar", modifier = Modifier.clickable { showUninstallConfirm = false }.padding(10.dp))
            },
        )
    }
}

@Composable
private fun AppDetailsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, XpCardBorder, RoundedCornerShape(8.dp))
            .padding(11.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = XpBlueDark)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AppDetailLine(label: String, value: String, boldValue: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, fontSize = 10.5.sp, color = XpTextSecondary, modifier = Modifier.width(112.dp))
        Text(
            value,
            fontSize = 10.5.sp,
            color = Color(0xFF26333F),
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AppManagerActionButton(
    label: String,
    icon: Int,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (destructive) Color(0xFFFFF3F3) else XpControlBackground, RoundedCornerShape(6.dp))
            .border(1.dp, if (destructive) Color(0xFFE6B6B6) else XpControlBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CachedResourceIcon(icon, label, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (destructive) Color(0xFFA32121) else Color(0xFF2B4055))
    }
}

private fun formatAppDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Não informado pelo Android"
    return runCatching {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
    }.getOrDefault("Não informado pelo Android")
}
