package com.exploradorxp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun DeviceInfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var snapshot by remember { mutableStateOf<DeviceInfoSnapshot?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        loading = true
        snapshot = withContext(Dispatchers.IO) { DeviceInfoCollector.collect(context) }
        loading = false
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

    XpModalWindow(
        onDismiss = onDismiss,
        maxWidth = 680,
        heightFraction = 0.88f,
        background = DeviceSurface,
        header = {
            DeviceInfoHeader(
                loading = loading,
                onRefresh = { refreshKey++ },
                onDismiss = onDismiss,
            )
        },
        footer = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (loading) "Atualizando informações…" else "Role para ver todos os detalhes",
                    color = DeviceMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                XpDialogButton("Fechar", iconRes = R.drawable.close, onClick = onDismiss)
            }
        },
    ) {
        if (loading && snapshot == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(44.dp),
            ) {
                CircularProgressIndicator(color = DeviceBlue)
                Spacer(Modifier.height(12.dp))
                Text("Lendo informações do aparelho…", color = DeviceMuted, fontSize = 13.sp)
            }
        } else {
            snapshot?.let { info ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
                ) {
                    DeviceHero(info)
                    DeviceUsageCards(info)

                    DeviceSection(
                        title = "Sistema",
                        icon = Icons.Rounded.Settings,
                        iconTint = DeviceBlue,
                    ) {
                        InfoRow("Android", "${info.androidVersion} • API ${info.apiLevel}")
                        SectionDivider()
                        InfoRow("Atualização de segurança", info.securityPatch)
                        SectionDivider()
                        InfoRow("Processador", processorLabel(info))
                        SectionDivider()
                        InfoRow("CPU", "${info.cpuCores} núcleos • ${if (info.is64Bit) "64 bits" else "32 bits"}")
                        SectionDivider()
                        InfoRow("Arquitetura", info.supportedAbis.firstOrNull() ?: "Não disponível")
                        SectionDivider()
                        InfoRow("Frequência", cpuFrequencySummary(info))
                        SectionDivider()
                        InfoRow("Hardware", info.hardware)
                        SectionDivider()
                        InfoRow("Tela", displayLabel(info))
                    }

                    DeviceSection(
                        title = "Conectividade",
                        icon = Icons.Rounded.Wifi,
                        iconTint = DevicePurple,
                    ) {
                        InfoRow("Conexão atual", connectivitySummary(info))
                        SectionDivider()
                        InfoRow("Internet", if (info.networkValidated) "Conectada" else if (info.networkTransport == "Sem conexão") "Sem conexão" else "Sem validação")
                        SectionDivider()
                        InfoRow("Wi-Fi", wifiDetailsLabel(info))
                        SectionDivider()
                        InfoRow("Rede móvel", mobileDetailsLabel(info))
                        SectionDivider()
                        InfoRow("SIM", simDetailsLabel(info))
                        SectionDivider()
                        InfoRow("eSIM", if (info.esimSupported) buildString {
                            append("Suportado")
                            if (info.esimMepSupported) append(" • múltiplos perfis")
                            else if (info.esimEnabled) append(" • gerenciador ativo")
                        } else "Não detectado")
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
                        InfoRow("Carga", info.batteryPercent?.let { "$it%" } ?: "Não disponível")
                        SectionDivider()
                        InfoRow("Estado", info.batteryStatus)
                        SectionDivider()
                        InfoRow("Fonte", info.batterySource)
                        if (info.batterySource != "Bateria") {
                            Spacer(Modifier.height(8.dp))
                            BatteryPowerCallout(info)
                        }
                    }

                    DeviceSection(
                        title = "Recursos",
                        icon = Icons.Rounded.Explore,
                        iconTint = DeviceBlue,
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

@Composable
private fun DeviceInfoHeader(
    loading: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
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
            .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(Color.White.copy(alpha = .16f), RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = .34f), RoundedCornerShape(4.dp)),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Informações do dispositivo",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Dados reais informados pelo Android",
                color = Color.White.copy(alpha = .84f),
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.size(36.dp)) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar", tint = Color.White, modifier = Modifier.size(21.dp))
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(28.dp)
                .width(58.dp)
                .background(Color(0xFFE8F1FB), RoundedCornerShape(2.dp))
                .border(1.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(2.dp))
                .clickable(onClick = onDismiss),
        ) {
            Text("Fechar", color = XpBlueDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DeviceHero(info: DeviceInfoSnapshot) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color.White, Color(0xFFF6FAFF), Color(0xFFE7F3FF)),
                ),
                RoundedCornerShape(16.dp),
            )
            .border(1.dp, Color(0xFFC8DDF3), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFEEF6FF), Color(0xFFD8EAFE))),
                        RoundedCornerShape(16.dp),
                    )
                    .border(1.dp, Color(0xFFBDD8F4), RoundedCornerShape(16.dp)),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = DeviceBlue,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = info.deviceName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeviceText,
                    maxLines = 2,
                    lineHeight = 24.sp,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${info.manufacturer.smartTitle()} • ${info.model}",
                    color = DeviceMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(DeviceGreen, CircleShape),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Dispositivo ativo", color = Color(0xFF247A3C), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
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
private fun HeroSpecChip(icon: ImageVector, tint: Color, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .background(Color.White.copy(alpha = .78f), RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            color = DeviceText,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeviceUsageCards(info: DeviceInfoSnapshot) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        UsageCard(
            title = "Memória",
            value = "${humanBytes(info.ramAvailableBytes)} livre",
            subtitle = "de ${humanBytes(info.ramTotalBytes)}",
            fraction = fractionUsed(info.ramUsedBytes, info.ramTotalBytes),
            icon = Icons.Rounded.Memory,
            accent = DevicePurple,
            modifier = Modifier.weight(1f),
        )
        UsageCard(
            title = "Armazenamento",
            value = "${humanBytes(info.storageAvailableBytes)} livre",
            subtitle = "de ${humanBytes(info.storageTotalBytes)}",
            fraction = fractionUsed(info.storageUsedBytes, info.storageTotalBytes),
            icon = Icons.Rounded.Storage,
            accent = DeviceGreen,
            modifier = Modifier.weight(1f),
        )
        UsageCard(
            title = "Bateria",
            value = info.batteryPercent?.let { "$it%" } ?: "N/D",
            subtitle = info.batteryStatus,
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(Color.White, RoundedCornerShape(13.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(13.dp))
            .padding(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = .12f), RoundedCornerShape(9.dp)),
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(5.dp))
            Text(title, color = DeviceMuted, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            value,
            color = DeviceText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(
            progress = { fraction },
            color = accent,
            trackColor = Color(0xFFE6EDF6),
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        )
        Text(subtitle, color = DeviceMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            .background(Color.White, RoundedCornerShape(15.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 3.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .background(iconTint.copy(alpha = .12f), RoundedCornerShape(10.dp)),
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = DeviceText, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                contentDescription = if (expanded) "Recolher $title" else "Expandir $title",
                tint = Color(0xFF8394AA),
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            Spacer(Modifier.height(7.dp))
            content()
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            label,
            color = DeviceMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            modifier = Modifier.weight(.42f).padding(end = 8.dp),
        )
        Text(
            value,
            color = DeviceText,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(.58f),
        )
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
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).background(DeviceGreen, CircleShape),
        ) {
            Icon(Icons.Rounded.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (info.batteryStatus == "Carregada") "Bateria carregada e conectada à energia" else "Dispositivo conectado à energia elétrica",
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
private fun SensorGrid(info: DeviceInfoSnapshot) {
    val sensors = listOf(
        CapabilityUi("Acelerômetro", info.hasAccelerometer, Icons.Rounded.Sensors),
        CapabilityUi("Giroscópio", info.hasGyroscope, Icons.Rounded.ScreenRotation),
        CapabilityUi("Bússola", info.hasMagnetometer, Icons.Rounded.Explore),
        CapabilityUi("Luz ambiente", info.hasLightSensor, Icons.Rounded.Sensors),
        CapabilityUi("Proximidade", info.hasProximitySensor, Icons.Rounded.Sensors),
        CapabilityUi("Barômetro", info.hasBarometer, Icons.Rounded.Sensors),
        CapabilityUi("Contador de passos", info.hasStepCounter, Icons.Rounded.Sensors),
        CapabilityUi("Passos em tempo real", info.hasStepDetector, Icons.Rounded.Sensors),
        CapabilityUi("Gravidade", info.hasGravitySensor, Icons.Rounded.Sensors),
        CapabilityUi("Movimento linear", info.hasLinearAcceleration, Icons.Rounded.Sensors),
        CapabilityUi("Rotação 3D", info.hasRotationVector, Icons.Rounded.ScreenRotation),
        CapabilityUi("Temperatura", info.hasAmbientTemperature, Icons.Rounded.Sensors),
        CapabilityUi("Umidade", info.hasRelativeHumidity, Icons.Rounded.Sensors),
    )

    sensors.chunked(3).forEach { rowItems ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        ) {
            rowItems.forEach { sensor ->
                CapabilityTile(sensor, Modifier.weight(1f))
            }
            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
        }
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
            .background(Color.White, RoundedCornerShape(15.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(15.dp))
            .padding(12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).background(DeviceBlue, RoundedCornerShape(11.dp)),
        ) {
            Icon(Icons.Rounded.Description, contentDescription = null, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("Explorador XP", color = DeviceText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Versão", color = DeviceMuted, fontSize = 10.sp)
        }
        Text(
            "${info.appVersionName} (${info.appVersionCode})",
            color = DeviceText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ShareDeviceCard(
    enabled: Boolean,
    onCopy: () -> Unit,
    onSaveImage: () -> Unit,
    onShareImage: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).background(DevicePurple.copy(alpha = .12f), RoundedCornerShape(11.dp)),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = DevicePurple, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("Compartilhar informações", fontWeight = FontWeight.Bold, color = DeviceNavy, fontSize = 14.sp)
                Text(
                    "Copie um resumo, salve a ficha em PNG ou compartilhe diretamente.",
                    color = DeviceMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 420.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCopy, enabled = enabled, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copiar", fontSize = 10.5.sp)
                    }
                    OutlinedButton(
                        onClick = onSaveImage, enabled = enabled, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Salvar PNG", fontSize = 10.5.sp)
                    }
                    Button(
                        onClick = onShareImage, enabled = enabled,
                        colors = ButtonDefaults.buttonColors(containerColor = DevicePurple),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Compartilhar", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCopy,
                        enabled = enabled,
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Copiar", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onSaveImage,
                        enabled = enabled,
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Salvar PNG", fontSize = 11.sp)
                    }
                }
                Button(
                    onClick = onShareImage,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = DevicePurple),
                    shape = RoundedCornerShape(11.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartilhar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
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
                Text("Relatório para IA", fontWeight = FontWeight.Bold, color = DeviceNavy, fontSize = 14.sp)
                Text(
                    "Detalhes técnicos extras para diagnóstico, sem identificadores pessoais ou lista dos seus arquivos.",
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
            Text("Exportar relatório para IA", fontWeight = FontWeight.Bold)
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

private fun mobileDetailsLabel(info: DeviceInfoSnapshot): String = buildString {
    when {
        info.mobileNetworkType != "Não disponível" -> append(info.mobileNetworkType)
        info.cellularActive -> append("Rede móvel")
        else -> append("Não ativa")
    }
    if (info.carrierName != "Não disponível") append(" • ${info.carrierName}")
    if (!info.cellularActive && info.mobileNetworkType != "Não disponível") append(" • em espera")
}

private fun simDetailsLabel(info: DeviceInfoSnapshot): String = when {
    info.simSlotCount <= 0 -> "Não detectado"
    else -> "${info.simSlotCount} slot(s) • ${info.simReadyCount} pronto(s)"
}

private fun processorLabel(info: DeviceInfoSnapshot): String {
    val parts = listOfNotNull(info.socManufacturer, info.socModel).filter { it.isNotBlank() }
    return parts.joinToString(" ").ifBlank { info.hardware }
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
