package com.exploradorxp.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .safeDrawingPadding()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DeviceSurface)
                    .border(1.dp, Color(0xFF8AB8EE), RoundedCornerShape(20.dp)),
            ) {
                DeviceInfoHeader(
                    loading = loading,
                    onRefresh = { refreshKey++ },
                    onDismiss = onDismiss,
                )

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
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 12.dp),
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
                                InfoRow("Tela", displayLabel(info))
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

                            ExplorerVersionCard(info)

                            AiReportCard(
                                enabled = !loading,
                                onExport = { exportLauncher.launch(deviceInfoExportFileName()) },
                            )
                        }
                    }
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
                    listOf(Color(0xFF2388F0), DeviceBlue, DeviceBlueDark),
                ),
            )
            .padding(start = 14.dp, end = 8.dp, top = 11.dp, bottom = 11.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = .18f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = .26f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(27.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Informações do dispositivo",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
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
        IconButton(onClick = onRefresh, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar", tint = Color.White)
            }
        }
        TextButton(onClick = onDismiss) {
            Text("Fechar", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(3.dp))
            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                    maxLines = 1,
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
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(15.dp))
            .border(1.dp, DeviceBorder, RoundedCornerShape(15.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color(0xFF8394AA), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(7.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = DeviceMuted, fontSize = 11.5.sp, modifier = Modifier.weight(.46f))
        Text(
            value,
            color = DeviceText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(.54f),
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
        CapabilityUi("Acelerômetro", info.hasAccelerometer, Icons.Rounded.Sensors),
        CapabilityUi("Giroscópio", info.hasGyroscope, Icons.Rounded.ScreenRotation),
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
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
