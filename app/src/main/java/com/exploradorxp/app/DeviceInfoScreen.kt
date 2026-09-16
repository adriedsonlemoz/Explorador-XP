package com.exploradorxp.app

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
                .padding(horizontal = 10.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(XpSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, XpBorder, RoundedCornerShape(14.dp)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF2F92F6), Color(0xFF0A67D8), Color(0xFF0752B8))),
                            RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Informações do dispositivo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Dados reais informados pelo Android", color = Color.White.copy(alpha = .86f), fontSize = 11.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Fechar", color = Color.White) }
                }

                if (loading && snapshot == null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(44.dp),
                    ) {
                        CircularProgressIndicator(color = XpBlue)
                        Spacer(Modifier.height(10.dp))
                        Text("Lendo informações do aparelho…", color = XpTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    snapshot?.let { info ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp),
                        ) {
                            DeviceHero(info)
                            DeviceUsageCards(info)
                            DeviceSection("Sistema") {
                                InfoRow("Android", "${info.androidVersion} • API ${info.apiLevel}")
                                InfoRow("Atualização de segurança", info.securityPatch)
                                InfoRow("Processador", processorLabel(info))
                                InfoRow("CPU", "${info.cpuCores} núcleos • ${if (info.is64Bit) "64 bits" else "32 bits"}")
                                InfoRow("Tela", displayLabel(info))
                            }
                            DeviceSection("Bateria") {
                                InfoRow("Carga", info.batteryPercent?.let { "$it%" } ?: "Não disponível")
                                InfoRow("Estado", info.batteryStatus)
                                InfoRow("Fonte", info.batterySource)
                            }
                            DeviceSection("Recursos") {
                                CapabilityGrid(info)
                            }
                            DeviceSection("Explorador XP") {
                                InfoRow("Versão", "${info.appVersionName} (${info.appVersionCode})")
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F7FF), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFC8DCF5), RoundedCornerShape(10.dp))
                                    .padding(11.dp),
                            ) {
                                Text("Relatório para IA", fontWeight = FontWeight.Bold, color = Color(0xFF173A67), fontSize = 14.sp)
                                Text(
                                    "A exportação inclui detalhes técnicos extras para diagnóstico, sem IMEI, serial, Android ID, MAC, localização ou lista dos seus arquivos.",
                                    color = XpTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                )
                                Button(
                                    onClick = { exportLauncher.launch(deviceInfoExportFileName()) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Exportar relatório para IA")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = XpChromeBorder)
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    TextButton(
                        onClick = { refreshKey++ },
                        enabled = !loading,
                    ) { Text(if (loading) "Atualizando…" else "Atualizar") }
                }
            }
        }
    }
}

@Composable
private fun DeviceHero(info: DeviceInfoSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFEAF4FF))), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFB8D2EE), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            text = info.deviceName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF173A67),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "${info.manufacturer.smartTitle()} • ${info.model}",
            color = XpTextSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "Android ${info.androidVersion}   •   ${humanBytes(info.ramTotalBytes)} RAM   •   ${humanBytes(info.storageTotalBytes)}",
            color = Color(0xFF244E7C),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DeviceUsageCards(info: DeviceInfoSnapshot) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        UsageCard(
            title = "Memória",
            value = "${humanBytes(info.ramAvailableBytes)} livre",
            fraction = fractionUsed(info.ramUsedBytes, info.ramTotalBytes),
            modifier = Modifier.weight(1f),
        )
        UsageCard(
            title = "Armazenamento",
            value = "${humanBytes(info.storageAvailableBytes)} livre",
            fraction = fractionUsed(info.storageUsedBytes, info.storageTotalBytes),
            modifier = Modifier.weight(1f),
        )
        UsageCard(
            title = "Bateria",
            value = info.batteryPercent?.let { "$it%" } ?: "N/D",
            fraction = ((info.batteryPercent ?: 0) / 100f).coerceIn(0f, 1f),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UsageCard(title: String, value: String, fraction: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, XpChromeBorder, RoundedCornerShape(10.dp))
            .padding(9.dp),
    ) {
        Text(title, color = XpTextSecondary, fontSize = 10.sp, maxLines = 1)
        Text(value, color = Color(0xFF173A67), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = XpBlue,
            trackColor = Color(0xFFDCE6F2),
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

@Composable
private fun DeviceSection(title: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, XpChromeBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF173A67), fontSize = 14.sp)
        Spacer(Modifier.height(3.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = XpTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(.44f))
        Text(value, color = Color(0xFF202D3D), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.56f))
    }
}

@Composable
private fun CapabilityGrid(info: DeviceInfoSnapshot) {
    val capabilities = listOf(
        "NFC" to info.hasNfc,
        "Bluetooth" to info.hasBluetooth,
        "GPS" to info.hasGps,
        "Câmera" to info.hasCamera,
        "Flash" to info.hasFlash,
        "Digital" to info.hasFingerprint,
        "Acelerômetro" to info.hasAccelerometer,
        "Giroscópio" to info.hasGyroscope,
        "Cartão removível" to info.hasRemovableStorage,
    )
    capabilities.chunked(3).forEach { rowItems ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            rowItems.forEach { (name, available) ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (available) Color(0xFFEAF6EC) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                        .border(1.dp, if (available) Color(0xFFB8D8BE) else Color(0xFFD6D9DE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 5.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "${if (available) "✓" else "—"} $name",
                        color = if (available) Color(0xFF245B30) else Color(0xFF6E737A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
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
