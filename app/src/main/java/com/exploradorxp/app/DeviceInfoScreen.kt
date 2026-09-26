package com.exploradorxp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val DeviceNavy = Color(0xFF102C57)
private val DeviceBlue = Color(0xFF0B6FE8)
private val DeviceBlueDark = Color(0xFF0754BB)
private val DeviceText = Color(0xFF142744)
private val DeviceMuted = Color(0xFF61718A)
private val DeviceBorder = Color(0xFFD3DFEE)
private val DeviceSurface = Color(0xFFF7FAFF)
private val DeviceGreen = Color(0xFF1BA64A)
private val DeviceGreenSoft = Color(0xFFE9F7EE)
private val DeviceGraySoft = Color(0xFFF1F4F8)
private val DevicePurple = Color(0xFF7754E8)
private val DeviceOrange = Color(0xFFF3A11B)

@Composable
fun DeviceInfoScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<DeviceInfoSnapshot?>(null) }
    var identity by remember { mutableStateOf<DeviceIdentityResult?>(null) }
    var deviceImage by remember { mutableStateOf<DeviceImageResult?>(null) }
    var imageLoading by remember { mutableStateOf(false) }
    var showImageDetails by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val externalLookupEnabled = remember(refreshKey) { PreferencesStore(context).deviceImagesEnabled() }

    LaunchedEffect(refreshKey, externalLookupEnabled) {
        loading = true
        loadError = null
        imageLoading = false
        deviceImage = null
        val input = DeviceIdentityInput.current()
        runCatching {
            val local = withContext(Dispatchers.IO) {
                val info = DeviceInfoCollector.collect(context)
                val repository = DeviceIdentityRepository(context)
                val identified = if (externalLookupEnabled) repository.resolveWithCache(input) else repository.resolveLocal(input)
                info to identified
            }
            snapshot = local.first
            identity = local.second
            loading = false

            if (externalLookupEnabled) {
                imageLoading = local.second.confirmed
                val refreshed = withContext(Dispatchers.IO) { DeviceIdentityRepository(context).refresh(input) }
                identity = refreshed
                if (refreshed.confirmed) {
                    imageLoading = true
                    deviceImage = withContext(Dispatchers.IO) { DeviceImageRepository(context).resolve(refreshed) }
                }
                imageLoading = false
            }
        }.onFailure { throwable ->
            Log.e("ExploradorXP", "Falha ao abrir Informações do dispositivo", throwable)
            loadError = throwable.message?.takeIf { it.isNotBlank() }
                ?: "O Android ou o fabricante bloquearam alguma informação desta tela."
            loading = false
            imageLoading = false
            if (snapshot == null) {
                identity = null
                deviceImage = DeviceImageResult.Unavailable("Não foi possível carregar agora")
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val current = snapshot ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                        writer.write(current.toAiReport())
                    } ?: error("Não foi possível abrir o arquivo de destino.")
                }
            }
            Toast.makeText(
                context,
                if (result.isSuccess) "Relatório exportado." else "Não foi possível exportar o relatório.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val current = snapshot ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { DeviceInfoShare.savePng(context, uri, current) }
            }
            Toast.makeText(
                context,
                if (result.isSuccess) "Imagem salva." else "Não foi possível salvar a imagem.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun copySummary(current: DeviceInfoSnapshot) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Informações do dispositivo", current.toShareSummary()))
        Toast.makeText(context, "Resumo copiado.", Toast.LENGTH_SHORT).show()
    }

    fun shareImage(current: DeviceInfoSnapshot) {
        scope.launch {
            val shareIntent = withContext(Dispatchers.IO) {
                runCatching { DeviceInfoShare.createShareIntent(context, current) }.getOrNull()
            }
            if (shareIntent != null) {
                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar informações do dispositivo"))
            } else {
                Toast.makeText(context, "Não foi possível preparar a imagem.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareReport(current: DeviceInfoSnapshot) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Explorador XP — diagnóstico do dispositivo")
            putExtra(Intent.EXTRA_TEXT, current.toAiReport())
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Compartilhar relatório do dispositivo"))
        }.onFailure {
            Toast.makeText(context, "Não foi possível compartilhar o relatório.", Toast.LENGTH_SHORT).show()
        }
    }

    val availableImage = deviceImage as? DeviceImageResult.Available
    if (showImageDetails && availableImage != null && identity != null) {
        DeviceImageDetailsDialog(
            identity = identity!!,
            image = availableImage,
            onDismiss = { showImageDetails = false },
            onOpenSource = { source ->
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source))) }
                    .onFailure { Toast.makeText(context, "Não foi possível abrir a fonte.", Toast.LENGTH_SHORT).show() }
            },
        )
    }

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeviceSurface),
    ) {
        DeviceInfoHeader(
            loading = loading,
            onRefresh = { refreshKey++ },
        )
        HorizontalDivider(color = XpChromeBorder)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DeviceSurface),
        ) {
                when {
                    loading && snapshot == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(44.dp),
                        ) {
                            CircularProgressIndicator(color = DeviceBlue)
                            Spacer(Modifier.height(12.dp))
                            Text("Lendo informações do aparelho…", color = DeviceMuted, fontSize = 13.sp)
                        }
                    }
                    snapshot == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(26.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(62.dp)
                                    .background(DeviceGraySoft, RoundedCornerShape(18.dp))
                                    .border(1.dp, DeviceBorder, RoundedCornerShape(18.dp)),
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = DeviceBlue, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Não foi possível abrir Informações do dispositivo agora",
                                color = DeviceText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                loadError ?: "Ocorreu uma falha temporária ao consultar dados do Android.",
                                color = DeviceMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = onDismiss) { Text("Fechar") }
                                Button(onClick = { refreshKey++ }) { Text("Tentar novamente") }
                            }
                        }
                    }
                    else -> {
                    snapshot?.let { info ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 13.dp),
                        ) {
                            loadError?.let { message ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF7E8), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFF0D59C), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                ) {
                                    Text(
                                        text = "Algumas informações não puderam ser carregadas agora: $message",
                                        color = Color(0xFF785200),
                                        fontSize = 10.5.sp,
                                        lineHeight = 14.sp,
                                    )
                                }
                            }
                            DeviceHero(
                                info = info,
                                identity = identity,
                                image = deviceImage,
                                imageLoading = imageLoading,
                                externalLookupEnabled = externalLookupEnabled,
                                onImageDetails = { if (availableImage != null) showImageDetails = true },
                            )
                            DeviceModelInformation(
                                info = info,
                                identity = identity,
                                image = deviceImage,
                                externalLookupEnabled = externalLookupEnabled,
                            )
                            DeviceOriginLabel("DETECTADO NESTE APARELHO")
                            DeviceUsageCards(info)

                            DeviceSection(
                                title = "Sistema e processador",
                                icon = Icons.Rounded.Settings,
                                iconTint = DeviceBlue,
                            ) {
                                val socIdentity = DeviceSoCResolver.resolve(info.socManufacturer, info.socModel, info.hardware)
                                InfoRow("Android", "${info.androidVersion} • API ${info.apiLevel}")
                                SectionDivider()
                                InfoRow("Patch de segurança", info.securityPatch)
                                SectionDivider()
                                ProcessorInfoRow(socIdentity)
                                SectionDivider()
                                InfoRow("Fabricante do SoC", socIdentity.manufacturer ?: info.socManufacturer ?: "Não disponível")
                                SectionDivider()
                                InfoRow("Modelo / ID do SoC", socIdentity.technicalId)
                                SectionDivider()
                                InfoRow("Núcleos detectados", info.cpuCores.toString())
                                SectionDivider()
                                InfoRow("Arquitetura física", "Não disponível pela API pública")
                                SectionDivider()
                                InfoRow("ABI principal do sistema", info.supportedAbis.firstOrNull() ?: "Não disponível")
                                SectionDivider()
                                InfoRow("ABIs do sistema", abiListLabel(info.supportedAbis))
                                SectionDivider()
                                InfoRow("Suporte do sistema", systemBitnessSummary(info))
                                SectionDivider()
                                InfoRow(
                                    "Processo do app",
                                    "${info.appRuntimeArchitecture} • ${if (info.appProcessIs64Bit) "64 bits" else "32 bits"}",
                                )
                                SectionDivider()
                                InfoRow("ABI do aplicativo", "Não disponível pela API pública")
                                SectionDivider()
                                InfoRow("Arquitetura do kernel", info.kernelArchitecture)
                                SectionDivider()
                                InfoRow("Frequências", cpuFrequencySummary(info))
                                SectionDivider()
                                InfoRow("GPU", gpuSummary(info))
                                if (gpuSourceSummary(info) != "Não disponível") {
                                    SectionDivider()
                                    InfoRow("Origem da GPU", gpuSourceSummary(info))
                                }
                                SectionDivider()
                                InfoRow("Fabricação", socIdentity.processLabel ?: "Não disponível")
                                socIdentity.catalogSourceLabel?.let { source ->
                                    SectionDivider()
                                    InfoRow("Origem da fabricação", source)
                                }
                                SectionDivider()
                                InfoRow("Hardware Android", info.hardware)
                                SectionDivider()
                                InfoRow("Tela", displayLabel(info))
                            }

                            DeviceSection(
                                title = "Conectividade e SIM",
                                icon = Icons.Rounded.Wifi,
                                iconTint = DevicePurple,
                            ) {
                                InfoRow("Conexão atual", connectivitySummary(info))
                                SectionDivider()
                                InfoRow("Internet", if (info.networkValidated) "Conectada" else if (info.networkTransport == "Sem conexão") "Sem conexão" else "Sem validação")
                                SectionDivider()
                                InfoRow("Wi-Fi", wifiDetailsLabel(info))
                                SectionDivider()
                                InfoRow("Operadora", info.carrierName)
                                SectionDivider()
                                InfoRow("Tecnologia móvel", info.mobileNetworkType)
                                SectionDivider()
                                InfoRow("Sinal móvel", mobileSignalSummary(info))
                                SectionDivider()
                                InfoRow("SIM", simDetailsLabel(info))
                                SectionDivider()
                                InfoRow("eSIM", esimDetailsLabel(info))
                                SectionDivider()
                                InfoRow("Bluetooth", when {
                                    info.hasBluetoothLe -> "Clássico + BLE"
                                    info.hasBluetooth -> "Clássico"
                                    else -> "Não disponível"
                                })
                                SectionDivider()
                                InfoRow("VPN", if (info.vpnActive) "Ativa" else "Não ativa")
                            }

                            DeviceSection(
                                title = "Bateria",
                                icon = Icons.Rounded.BatteryFull,
                                iconTint = DeviceGreen,
                            ) {
                                InfoRow("Nível", info.batteryPercent?.let { "$it%" } ?: "Não disponível")
                                SectionDivider()
                                InfoRow("Estado", info.batteryStatus)
                                SectionDivider()
                                InfoRow("Fonte de alimentação", info.batterySource)
                                info.batteryTemperatureC?.let { value ->
                                    SectionDivider()
                                    InfoRow("Temperatura", String.format(Locale.forLanguageTag("pt-BR"), "%.1f °C", value))
                                }
                                info.batteryVoltageMv?.let { value ->
                                    SectionDivider()
                                    InfoRow("Tensão", "$value mV")
                                }
                                info.batteryCurrentMicroamps?.let { value ->
                                    SectionDivider()
                                    InfoRow("Corrente instantânea", formatBatteryCurrent(value))
                                }
                                if (info.batterySource != "Não conectado") {
                                    Spacer(Modifier.height(6.dp))
                                    BatteryPowerCallout(info)
                                }
                            }

                            DeviceSection(
                                title = "Diagnóstico rápido",
                                icon = Icons.Rounded.CheckCircle,
                                iconTint = DeviceGreen,
                            ) {
                                QuickDiagnosticGrid(info)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "“Não detectado” indica apenas que o recurso não foi exposto pelo Android; não significa defeito.",
                                    color = DeviceMuted,
                                    fontSize = 9.5.sp,
                                    lineHeight = 12.sp,
                                )
                            }

                            DeviceSection(
                                title = "Recursos",
                                icon = Icons.Rounded.Explore,
                                iconTint = DeviceBlue,
                                initiallyExpanded = false,
                            ) {
                                CapabilityGrid(info)
                            }

                            DeviceSection(
                                title = "Sensores • ${info.sensorCount} detectados",
                                icon = Icons.Rounded.Sensors,
                                iconTint = DevicePurple,
                            ) {
                                SensorGrid(info)
                            }

                            ExplorerVersionCard(info)

                            ShareDeviceCard(
                                enabled = !loading,
                                onCopy = { copySummary(info) },
                                onShareReport = { shareReport(info) },
                                onSaveImage = { imageLauncher.launch(deviceInfoImageFileName()) },
                                onShareImage = { shareImage(info) },
                            )

                            AiReportCard(
                                enabled = !loading,
                                onExport = { exportLauncher.launch(deviceInfoExportFileName()) },
                            )
                        }
                    }
                }
        }
        }
        HorizontalDivider(color = XpChromeBorder)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(XpChrome)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                if (loading) "Atualizando informações…" else "Role para ver todos os detalhes",
                color = DeviceMuted,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            XpDialogButton(
                "Fechar",
                iconRes = R.drawable.close,
                onClick = onDismiss,
            )
        }
    }

}

@Composable
private fun DeviceInfoHeader(
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2F92F6), XpBlue, XpBlueDark),
                ),
            )
            .padding(start = 9.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .background(Color.White.copy(alpha = .16f), RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = .34f), RoundedCornerShape(4.dp)),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Informações do dispositivo",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Dados Android e informações do modelo separados",
                color = Color.White.copy(alpha = .84f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(32.dp)) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun DeviceHero(
    info: DeviceInfoSnapshot,
    identity: DeviceIdentityResult?,
    image: DeviceImageResult?,
    imageLoading: Boolean,
    externalLookupEnabled: Boolean,
    onImageDetails: () -> Unit,
) {
    val commercialName = identity?.displayName ?: "Identificando modelo…"
    val maker = identity?.manufacturerNormalized?.takeIf { it.isNotBlank() } ?: info.manufacturer.smartTitle()
    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color.White, Color(0xFFF6FAFF), Color(0xFFE7F3FF)),
                ),
                RoundedCornerShape(14.dp),
            )
            .border(1.dp, Color(0xFFC8DDF3), RoundedCornerShape(14.dp))
            .padding(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFEEF6FF), Color(0xFFD8EAFE))),
                    )
                    .border(1.dp, Color(0xFFBDD8F4), RoundedCornerShape(14.dp))
                    .clickable(enabled = image is DeviceImageResult.Available, onClick = onImageDetails),
            ) {
                when (image) {
                    is DeviceImageResult.Available -> ComposeImage(
                        bitmap = image.bitmap.asImageBitmap(),
                        contentDescription = "Imagem de ${identity?.displayName ?: "dispositivo"}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = DeviceBlue,
                        modifier = Modifier.size(40.dp),
                    )
                }
                if (imageLoading) {
                    CircularProgressIndicator(color = DeviceBlue, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = commercialName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeviceText,
                    maxLines = 2,
                    lineHeight = 21.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = maker,
                    color = DeviceMuted,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = info.model,
                    color = DeviceText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    image is DeviceImageResult.Available -> Text(
                        "Toque na imagem para ver fonte e licença",
                        color = DeviceBlueDark,
                        fontSize = 9.5.sp,
                    )
                    !externalLookupEnabled -> Text(
                        "Busca de imagem desativada nas Configurações",
                        color = DeviceMuted,
                        fontSize = 9.5.sp,
                    )
                    !imageLoading -> Text(
                        "Imagem deste modelo não disponível",
                        color = DeviceMuted,
                        fontSize = 9.5.sp,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            HeroSpecChip(
                icon = Icons.Rounded.PhoneAndroid,
                tint = DeviceGreen,
                text = "Android ${info.androidVersion}",
                modifier = Modifier.weight(1f),
            )
            HeroSpecChip(
                icon = Icons.Rounded.Memory,
                tint = DeviceBlue,
                text = "${humanBytes(info.ramTotalBytes)} RAM",
                modifier = Modifier.weight(1f),
            )
            HeroSpecChip(
                icon = Icons.Rounded.Storage,
                tint = DevicePurple,
                text = humanBytes(info.storageTotalBytes),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroSpecChip(
    icon: ImageVector,
    tint: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .background(tint.copy(alpha = .09f), RoundedCornerShape(10.dp))
            .border(1.dp, tint.copy(alpha = .18f), RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = text,
            color = DeviceText,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeviceOriginLabel(text: String) {
    Text(
        text = text,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        color = DeviceMuted,
        letterSpacing = .6.sp,
        modifier = Modifier.padding(start = 3.dp, top = 2.dp, bottom = (-3).dp),
    )
}

@Composable
private fun DeviceModelInformation(
    info: DeviceInfoSnapshot,
    identity: DeviceIdentityResult?,
    image: DeviceImageResult?,
    externalLookupEnabled: Boolean,
) {
    DeviceOriginLabel("INFORMAÇÕES DO MODELO")
    DeviceSection(
        title = "Identificação do modelo",
        icon = Icons.Rounded.Image,
        iconTint = DevicePurple,
    ) {
        Text(
            "Dados de catálogo ficam separados dos valores medidos/detectados neste aparelho.",
            color = DeviceMuted,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        InfoRow("Nome comercial", identity?.displayName ?: "Identificando…")
        SectionDivider()
        InfoRow("Fabricante (Android)", info.manufacturer)
        SectionDivider()
        InfoRow("Marca (Android)", DeviceIdentityInput.current().brand.ifBlank { "Não disponível" })
        SectionDivider()
        InfoRow("Modelo (Android)", info.model)
        SectionDivider()
        InfoRow("Device (Android)", info.deviceCode)
        SectionDivider()
        InfoRow("Product (Android)", info.product)
        SectionDivider()
        InfoRow("Origem da identificação", identity?.source?.label ?: "Não disponível")
        identity?.catalogVersion?.let { version ->
            SectionDivider()
            InfoRow("Versão do catálogo", version)
        }
        identity?.variants?.takeIf { it.isNotEmpty() }?.let { variants ->
            SectionDivider()
            InfoRow("Variantes (device)", variants.take(4).joinToString(" • "))
        }
        SectionDivider()
        val imageStatus = when {
            !externalLookupEnabled -> "Busca externa desativada"
            image is DeviceImageResult.Available -> if (image.fromCache) "Wikimedia Commons • cache" else "Wikimedia Commons"
            image is DeviceImageResult.Unavailable -> image.reason
            else -> "Aguardando consulta"
        }
        InfoRow("Imagem", imageStatus)
    }
}

@Composable
private fun DeviceImageDetailsDialog(
    identity: DeviceIdentityResult,
    image: DeviceImageResult.Available,
    onDismiss: () -> Unit,
    onOpenSource: (String) -> Unit,
) {
    val metadata = image.metadata
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalhes da imagem") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Modelo: ${identity.displayName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Fonte: Wikimedia Commons via Wikidata", fontSize = 12.sp)
                Text("Autor: ${metadata.author ?: "Não informado pela fonte"}", fontSize = 12.sp)
                Text("Licença: ${metadata.license ?: "Não informada pela fonte"}", fontSize = 12.sp)
                Text("Entidade: ${metadata.wikidataEntityId}", fontSize = 12.sp)
                Text("Consulta: ${formatImageQueryTime(metadata.queriedAtEpochMs)}", fontSize = 12.sp)
                Text(
                    if (image.fromCache) "Imagem carregada do cache local." else "Imagem consultada nesta sessão e armazenada em cache.",
                    fontSize = 11.sp,
                    color = DeviceMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onOpenSource(metadata.sourceUrl) }) { Text("Ver fonte") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

private fun formatImageQueryTime(epochMs: Long): String = runCatching {
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))
}.getOrDefault("Não disponível")

@Composable
private fun DeviceUsageCards(info: DeviceInfoSnapshot) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
        UsageCard(
            title = "RAM",
            value = "${humanBytes(info.ramAvailableBytes)} disponíveis",
            subtitle = buildRamPercentSubtitle(info.ramAvailablePercent, info.ramUsedPercent, info.ramTotalBytes),
            fraction = fractionUsed(info.ramUsedBytes, info.ramTotalBytes),
            icon = Icons.Rounded.Memory,
            accent = DevicePurple,
            modifier = Modifier.weight(1f),
        )
        UsageCard(
            title = "Armazenamento",
            value = "${humanBytes(info.storageAvailableBytes)} livres",
            subtitle = buildPercentSubtitle(info.storageAvailablePercent, info.storageUsedPercent, info.storageTotalBytes),
            fraction = fractionUsed(info.storageUsedBytes, info.storageTotalBytes),
            icon = Icons.Rounded.Storage,
            accent = DeviceGreen,
            modifier = Modifier.weight(1f),
        )
        UsageCard(
            title = "Bateria",
            value = info.batteryPercent?.let { "$it%" } ?: "N/D",
            subtitle = if (info.batterySource == "Não conectado") info.batteryStatus else "${info.batteryStatus} • ${info.batterySource}",
            fraction = ((info.batteryPercent ?: 0) / 100f).coerceIn(0f, 1f),
            icon = Icons.Rounded.BatteryFull,
            accent = DeviceOrange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UsageCard(
    title: String,
    value: String,
    subtitle: String,
    fraction: Float,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .background(accent.copy(alpha = .12f), RoundedCornerShape(7.dp)),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(title, color = DeviceMuted, fontSize = 8.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            value,
            color = DeviceText,
            fontSize = 10.7.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(
            progress = { fraction },
            color = accent,
            trackColor = Color(0xFFE6EDF6),
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
        )
        Text(
            subtitle,
            color = DeviceMuted,
            fontSize = 8.2.sp,
            lineHeight = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeviceSection(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(13.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .background(iconTint.copy(alpha = .12f), RoundedCornerShape(9.dp)),
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(7.dp))
            Text(title, fontWeight = FontWeight.Bold, color = DeviceText, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) "Recolher $title" else "Expandir $title",
                tint = Color(0xFF8394AA),
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            Spacer(Modifier.height(5.dp))
            content()
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            color = DeviceMuted,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            modifier = Modifier.weight(.42f).padding(end = 8.dp),
        )
        Text(
            value,
            color = DeviceText,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(.58f),
        )
    }
}

@Composable
private fun ProcessorInfoRow(identity: DeviceSoCResolver.Identity) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            "Processador",
            color = DeviceMuted,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            modifier = Modifier.weight(.42f).padding(end = 8.dp),
        )
        Column(modifier = Modifier.weight(.58f)) {
            Text(
                if (identity.commercialName != null) identity.primaryLabel else identity.technicalLabel,
                color = DeviceText,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            if (identity.commercialName != null) {
                Text(
                    identity.technicalLabel,
                    color = DeviceMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
            }
            Text(
                "Fonte: ${identity.identitySourceLabel}",
                color = DeviceMuted,
                fontSize = 8.8.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = Color(0xFFE8EEF6), thickness = 1.dp)
}

@Composable
private fun BatteryPowerCallout(info: DeviceInfoSnapshot) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(DeviceGreenSoft, RoundedCornerShape(11.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).background(DeviceGreen, CircleShape),
        ) {
            Icon(Icons.Rounded.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (info.batteryStatus == "Completa") "Bateria completa e conectada à energia" else "Fonte detectada: ${info.batterySource}",
            color = Color(0xFF26723B),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private data class CapabilityUi(
    val name: String,
    val available: Boolean,
    val icon: ImageVector,
)

@Composable
private fun CapabilityGrid(info: DeviceInfoSnapshot) {
    val capabilities = listOf(
        CapabilityUi("NFC", info.hasNfc, Icons.Rounded.Nfc),
        CapabilityUi("Bluetooth", info.hasBluetooth, Icons.Rounded.Bluetooth),
        CapabilityUi("GPS", info.hasGps, Icons.Rounded.LocationOn),
        CapabilityUi("Câmera", info.hasCamera, Icons.Rounded.CameraAlt),
        CapabilityUi("Flash", info.hasFlash, Icons.Rounded.FlashOn),
        CapabilityUi("Digital", info.hasFingerprint, Icons.Rounded.Fingerprint),
        CapabilityUi("Cartão removível", info.hasRemovableStorage, Icons.Rounded.SdCard),
    )

    capabilities.chunked(3).forEach { rowItems ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        ) {
            rowItems.forEach { capability ->
                CapabilityTile(capability, Modifier.weight(1f))
            }
            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun QuickDiagnosticGrid(info: DeviceInfoSnapshot) {
    val items = listOf(
        CapabilityUi("Câmera", info.hasCamera, Icons.Rounded.CameraAlt),
        CapabilityUi("Bluetooth", info.hasBluetooth, Icons.Rounded.Bluetooth),
        CapabilityUi("Giroscópio", info.hasGyroscope, Icons.Rounded.ScreenRotation),
        CapabilityUi("NFC", info.hasNfc, Icons.Rounded.Nfc),
    )
    items.chunked(2).forEach { rowItems ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) {
            rowItems.forEach { item ->
                DiagnosticTile(item, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DiagnosticTile(item: CapabilityUi, modifier: Modifier = Modifier) {
    val accent = if (item.available) DeviceGreen else Color(0xFF8A98AA)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(if (item.available) Color(0xFFF0FAF3) else DeviceGraySoft, RoundedCornerShape(10.dp))
            .border(1.dp, if (item.available) Color(0xFFC4E6CE) else Color(0xFFDDE3EA), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
    ) {
        Icon(
            if (item.available) Icons.Rounded.CheckCircle else Icons.Rounded.RemoveCircleOutline,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = DeviceText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (item.available) "Disponível" else "Não detectado",
                color = if (item.available) Color(0xFF277A3E) else DeviceMuted,
                fontSize = 8.8.sp,
                maxLines = 1,
            )
        }
    }
}

private data class SensorTileDefinition(
    val name: String,
    val type: Int,
    val icon: ImageVector,
)

@Composable
private fun SensorGrid(info: DeviceInfoSnapshot) {
    val definitions = listOf(
        SensorTileDefinition("Acelerômetro", Sensor.TYPE_ACCELEROMETER, Icons.Rounded.Sensors),
        SensorTileDefinition("Giroscópio", Sensor.TYPE_GYROSCOPE, Icons.Rounded.ScreenRotation),
        SensorTileDefinition("Bússola", Sensor.TYPE_MAGNETIC_FIELD, Icons.Rounded.Explore),
        SensorTileDefinition("Luz ambiente", Sensor.TYPE_LIGHT, Icons.Rounded.Sensors),
        SensorTileDefinition("Proximidade", Sensor.TYPE_PROXIMITY, Icons.Rounded.Sensors),
        SensorTileDefinition("Barômetro", Sensor.TYPE_PRESSURE, Icons.Rounded.Sensors),
        SensorTileDefinition("Contador de passos", Sensor.TYPE_STEP_COUNTER, Icons.Rounded.Sensors),
        SensorTileDefinition("Detector de passos", Sensor.TYPE_STEP_DETECTOR, Icons.Rounded.Sensors),
        SensorTileDefinition("Gravidade", Sensor.TYPE_GRAVITY, Icons.Rounded.Sensors),
        SensorTileDefinition("Movimento linear", Sensor.TYPE_LINEAR_ACCELERATION, Icons.Rounded.Sensors),
        SensorTileDefinition("Rotação 3D", Sensor.TYPE_ROTATION_VECTOR, Icons.Rounded.ScreenRotation),
        SensorTileDefinition("Temperatura ambiente", Sensor.TYPE_AMBIENT_TEMPERATURE, Icons.Rounded.Sensors),
        SensorTileDefinition("Umidade relativa", Sensor.TYPE_RELATIVE_HUMIDITY, Icons.Rounded.Sensors),
    )
    var selectedSensor by remember { mutableStateOf<DeviceSensorInfo?>(null) }

    definitions.chunked(3).forEach { rowItems ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) {
            rowItems.forEach { definition ->
                val details = info.sensorDetails.firstOrNull { it.type == definition.type }
                SensorTile(
                    definition = definition,
                    details = details,
                    selected = selectedSensor == details && details != null,
                    onClick = {
                        selectedSensor = if (selectedSensor == details) null else details
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
    }

    selectedSensor?.let { sensor ->
        Spacer(Modifier.height(7.dp))
        SensorDetailsPanel(
            sensorInfo = sensor,
            onClose = { selectedSensor = null },
        )
    }
}

@Composable
private fun SensorTile(
    definition: SensorTileDefinition,
    details: DeviceSensorInfo?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = details != null
    val accent = if (available) DeviceGreen else Color(0xFFAAB5C4)
    val shape = RoundedCornerShape(10.dp)
    var tileModifier = modifier
        .background(
            when {
                selected -> Color(0xFFE5F2FF)
                available -> Color(0xFFF0FAF3)
                else -> DeviceGraySoft
            },
            shape,
        )
        .border(
            1.dp,
            when {
                selected -> DeviceBlue
                available -> Color(0xFFC4E6CE)
                else -> Color(0xFFDDE3EA)
            },
            shape,
        )
    if (available) tileModifier = tileModifier.clickable(onClick = onClick)

    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = tileModifier.padding(horizontal = 6.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(25.dp).background(if (selected) DeviceBlue else accent, CircleShape),
            ) {
                Icon(definition.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                definition.name,
                color = DeviceText,
                fontSize = 8.7.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 10.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (available) Icons.Rounded.CheckCircle else Icons.Rounded.RemoveCircleOutline,
                contentDescription = null,
                tint = if (selected) DeviceBlue else accent,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                when {
                    !available -> "Não detectado"
                    selected -> "Detalhes abertos"
                    else -> "Disponível • toque"
                },
                color = if (available) Color(0xFF277A3E) else DeviceMuted,
                fontSize = 7.8.sp,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SensorDetailsPanel(
    sensorInfo: DeviceSensorInfo,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember(sensorInfo) {
        sensorManager.getSensorList(sensorInfo.type).firstOrNull { candidate ->
            candidate.name == sensorInfo.name &&
                candidate.vendor == sensorInfo.vendor &&
                candidate.version == sensorInfo.version
        } ?: sensorManager.getSensorList(sensorInfo.type).firstOrNull()
    }
    var liveValues by remember(sensorInfo) { mutableStateOf<List<Float>?>(null) }
    var liveAvailable by remember(sensorInfo) { mutableStateOf(sensor != null) }

    DisposableEffect(sensor) {
        if (sensor == null) {
            liveAvailable = false
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    liveValues = event.values.toList()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            liveAvailable = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { sensorManager.unregisterListener(listener, sensor) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7FAFF), RoundedCornerShape(11.dp))
            .border(1.dp, Color(0xFFC8DCF5), RoundedCornerShape(11.dp))
            .padding(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Detalhes reais do sensor",
                color = DeviceNavy,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Ocultar",
                color = DeviceBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onClose).padding(4.dp),
            )
        }
        SectionDivider()
        InfoRow("Nome", sensorInfo.name)
        SectionDivider()
        InfoRow("Fabricante", sensorInfo.vendor)
        SectionDivider()
        InfoRow("Versão", sensorInfo.version.toString())
        SectionDivider()
        InfoRow("Tipo Android", sensorInfo.stringType)
        SectionDivider()
        InfoRow("Resolução", formatSensorMetric(sensorInfo.resolution, sensorInfo.type))
        SectionDivider()
        InfoRow("Alcance máximo", formatSensorMetric(sensorInfo.maximumRange, sensorInfo.type))
        SectionDivider()
        InfoRow("Consumo", formatSensorPower(sensorInfo.powerMa))
        SectionDivider()
        InfoRow("Atraso mínimo", "${sensorInfo.minDelayUs} µs")
        SectionDivider()
        InfoRow("Modo de relatório", sensorReportingModeLabel(sensorInfo.reportingMode))
        SectionDivider()
        InfoRow("Wake-up", if (sensorInfo.wakeUpSensor) "Sim" else "Não")
        SectionDivider()
        InfoRow(
            "Leitura atual",
            when {
                !liveAvailable -> "Não disponível para leitura em tempo real"
                liveValues == null -> "Aguardando evento do SensorManager…"
                else -> formatLiveSensorValues(sensorInfo.type, liveValues.orEmpty())
            },
        )
    }
}

@Composable
private fun CapabilityTile(capability: CapabilityUi, modifier: Modifier = Modifier) {
    val background = if (capability.available) Color(0xFFF0FAF3) else DeviceGraySoft
    val border = if (capability.available) Color(0xFFC4E6CE) else Color(0xFFDDE3EA)
    val accent = if (capability.available) DeviceGreen else Color(0xFFAAB5C4)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .background(background, RoundedCornerShape(11.dp))
            .border(1.dp, border, RoundedCornerShape(11.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp).background(accent, CircleShape),
            ) {
                Icon(capability.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text(
                capability.name,
                color = DeviceText,
                fontSize = 9.2.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 10.5.sp,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (capability.available) Icons.Rounded.CheckCircle else Icons.Rounded.RemoveCircleOutline,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                if (capability.available) "Disponível" else "Não disponível",
                color = if (capability.available) Color(0xFF277A3E) else DeviceMuted,
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExplorerVersionCard(info: DeviceInfoSnapshot) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(13.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp).background(DeviceBlue, RoundedCornerShape(9.dp)),
        ) {
            Icon(Icons.Rounded.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Explorador XP ${info.appVersionName} (${info.appVersionCode})", color = DeviceText, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            Text("Coleta: ${collectionTimeLabel(info)}", color = DeviceMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ShareDeviceCard(
    enabled: Boolean,
    onCopy: () -> Unit,
    onShareReport: () -> Unit,
    onSaveImage: () -> Unit,
    onShareImage: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).background(DevicePurple.copy(alpha = .12f), RoundedCornerShape(9.dp)),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = DevicePurple, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Copiar e compartilhar", fontWeight = FontWeight.Bold, color = DeviceNavy, fontSize = 12.5.sp)
                Text(
                    "Resumo, relatório técnico e imagem usam somente os dados exibidos nesta tela.",
                    color = DeviceMuted,
                    fontSize = 9.sp,
                    lineHeight = 11.5.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onCopy,
                enabled = enabled,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.weight(1f).height(39.dp),
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copiar", fontSize = 9.5.sp)
            }
            OutlinedButton(
                onClick = onShareReport,
                enabled = enabled,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.weight(1f).height(39.dp),
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Relatório", fontSize = 9.5.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onSaveImage,
                enabled = enabled,
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.weight(1f).height(39.dp),
            ) {
                Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Salvar imagem", fontSize = 9.5.sp)
            }
            Button(
                onClick = onShareImage,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = DevicePurple),
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.weight(1f).height(39.dp),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Compart. imagem", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AiReportCard(enabled: Boolean, onExport: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFFF4F9FF), Color(0xFFE8F3FF))),
                RoundedCornerShape(16.dp),
            )
            .border(1.dp, Color(0xFFC8DCF5), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).background(Color(0xFFDCEBFF), RoundedCornerShape(11.dp)),
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, tint = DeviceBlueDark, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Diagnóstico completo", fontWeight = FontWeight.Bold, color = DeviceNavy, fontSize = 13.sp)
                Text(
                    "Relatório técnico com origem dos dados e campos não disponíveis explicitamente marcados.",
                    color = DeviceMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
            }
        }
        Button(
            onClick = onExport,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = DeviceBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Exportar diagnóstico completo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Spacer(Modifier.width(5.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

private fun wifiDetailsLabel(info: DeviceInfoSnapshot): String {
    if (!info.wifiActive) return "Não conectado"
    return buildString {
        if (info.wifiStandard != "Não disponível") append(info.wifiStandard) else append("Wi-Fi")
        if (info.wifiBand != "Não disponível") append(" • ${info.wifiBand}")
        info.wifiLinkSpeedMbps?.let { append(" • ${it} Mbps") }
    }
}

private fun simDetailsLabel(info: DeviceInfoSnapshot): String = when {
    info.simSlotCount <= 0 -> "Nenhum slot/modem reportado"
    info.simSlotCount == 1 -> "${info.simReadyCount} pronto de 1 slot/modem reportado"
    else -> "${info.simReadyCount} prontos de ${info.simSlotCount} slots/modems reportados"
}

private fun esimDetailsLabel(info: DeviceInfoSnapshot): String = when {
    !info.esimSupported -> "Não detectado"
    info.esimMepSupported -> "Suportado • múltiplos perfis compatíveis"
    info.esimEnabled -> "Suportado • gerenciador ativo"
    else -> "Suportado pelo aparelho"
}

private fun abiListLabel(values: List<String>): String = values.joinToString(", ").ifBlank { "Não disponível" }

private fun buildRamPercentSubtitle(availablePercent: Int?, usedPercent: Int?, totalBytes: Long): String = buildString {
    append("de ${humanBytes(totalBytes)}")
    if (availablePercent != null && usedPercent != null) append(" • $availablePercent% disponível • $usedPercent% usado")
}

private fun buildPercentSubtitle(freePercent: Int?, usedPercent: Int?, totalBytes: Long): String = buildString {
    append("de ${humanBytes(totalBytes)}")
    if (freePercent != null && usedPercent != null) append(" • $freePercent% livre • $usedPercent% usado")
}

private fun formatSensorPower(powerMa: Float): String = if (powerMa.isFinite() && powerMa >= 0f) {
    "${String.format(Locale.forLanguageTag("pt-BR"), "%.3f", powerMa).trimTrailingSensorZeros()} mA"
} else {
    "Não disponível"
}

private fun formatSensorMetric(value: Float, type: Int): String {
    if (!value.isFinite()) return "Não disponível"
    val number = String.format(Locale.forLanguageTag("pt-BR"), "%.4f", value).trimTrailingSensorZeros()
    val unit = sensorUnit(type)
    return if (unit == null) number else "$number $unit"
}

private fun formatLiveSensorValues(type: Int, values: List<Float>): String {
    if (values.isEmpty()) return "Aguardando evento do SensorManager…"
    val labels = when (values.size) {
        1 -> listOf("v")
        2 -> listOf("x", "y")
        3 -> listOf("x", "y", "z")
        4 -> listOf("x", "y", "z", "w")
        else -> values.indices.map { "v${it + 1}" }
    }
    val unit = sensorUnit(type)
    val body = values.mapIndexed { index, value ->
        val formatted = if (value.isFinite()) {
            String.format(Locale.forLanguageTag("pt-BR"), "%.4f", value).trimTrailingSensorZeros()
        } else {
            "N/D"
        }
        "${labels.getOrElse(index) { "v${index + 1}" }}=$formatted"
    }.joinToString(" • ")
    return if (unit == null) body else "$body $unit"
}

private fun sensorUnit(type: Int): String? = when (type) {
    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"
    Sensor.TYPE_GYROSCOPE -> "rad/s"
    Sensor.TYPE_MAGNETIC_FIELD -> "µT"
    Sensor.TYPE_LIGHT -> "lx"
    Sensor.TYPE_PROXIMITY -> "cm"
    Sensor.TYPE_PRESSURE -> "hPa"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
    Sensor.TYPE_STEP_COUNTER -> "passos"
    else -> null
}

private fun sensorReportingModeLabel(mode: Int): String = when (mode) {
    Sensor.REPORTING_MODE_CONTINUOUS -> "Contínuo"
    Sensor.REPORTING_MODE_ON_CHANGE -> "Quando muda"
    Sensor.REPORTING_MODE_ONE_SHOT -> "Evento único"
    Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Gatilho especial"
    else -> "Não disponível ($mode)"
}

private fun String.trimTrailingSensorZeros(): String {
    val comma = lastIndexOf(',')
    if (comma < 0) return this
    val trimmed = trimEnd('0').trimEnd(',')
    return trimmed.ifBlank { "0" }
}

private fun displayLabel(info: DeviceInfoSnapshot): String {
    val hz = if (info.refreshRateHz > 0f) " • ${info.refreshRateHz.roundToInt()} Hz" else ""
    return "${info.displayWidthPx} × ${info.displayHeightPx}px$hz"
}

private fun fractionUsed(used: Long, total: Long): Float = if (total > 0L) {
    (used.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
} else 0f

private fun String.smartTitle(): String = lowercase().replaceFirstChar { char ->
    if (char.isLowerCase()) char.titlecase() else char.toString()
}
