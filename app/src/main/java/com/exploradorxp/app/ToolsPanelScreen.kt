package com.exploradorxp.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Entradas do painel. Somente módulos já existentes são oferecidos como ações. */
internal enum class ExplorerToolAction { APPS, STORAGE, DEVICE, SETTINGS, NETWORK, TRASH, HELP, ABOUT }

/** Central de ferramentas: apenas redireciona para telas existentes e controles já implementados. */
@Composable
internal fun ToolsPanelScreen(
    sortMode: SortMode,
    foldersFirst: Boolean,
    onSortMode: (SortMode) -> Unit,
    onFoldersFirstChange: (Boolean) -> Unit,
    onOpenTool: (ExplorerToolAction) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F8FB))) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(XpBlueDark).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.settings), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("Painel de Ferramentas", color = Color.White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Text("Fechar", color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
        }
        HorizontalDivider(color = XpChromeBorder)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("Gerencie o aparelho e o Explorador XP em um só lugar.",
                color = XpTextSecondary, fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 5.dp))
            PanelGroupTitle("Sistema e armazenamento")
            PanelToolEntry(R.drawable.file_apk, "Aplicativos instalados", "Aplicativos, dados, cache e permissões", onClick = { onOpenTool(ExplorerToolAction.APPS) })
            PanelToolEntry(R.drawable.drive_storage, "Gerenciar armazenamento", "Espaço, categorias, pastas e arquivos grandes", onClick = { onOpenTool(ExplorerToolAction.STORAGE) })
            PanelToolEntry(R.drawable.device_mobile, "Informações do dispositivo", "Hardware, sistema, sensores e bateria", onClick = { onOpenTool(ExplorerToolAction.DEVICE) })

            Spacer(Modifier.height(6.dp))
            PanelGroupTitle("Aplicativo e rede")
            PanelToolEntry(R.drawable.settings, "Configurações do Explorador XP", "Preferências do aplicativo", onClick = { onOpenTool(ExplorerToolAction.SETTINGS) })
            PanelToolEntry(R.drawable.device_mobile, "Rede local", "Compartilhamentos SMB e WebDAV", onClick = { onOpenTool(ExplorerToolAction.NETWORK) })
            PanelToolEntry(R.drawable.trash_empty, "Lixeira", "Restaurar ou apagar arquivos descartados", onClick = { onOpenTool(ExplorerToolAction.TRASH) })

            Spacer(Modifier.height(6.dp))
            PanelGroupTitle("Organização de arquivos")
            Text("Ordenar por", color = XpTextSecondary, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                listOf(
                    SortMode.NAME to "Nome", SortMode.DATE to "Data",
                    SortMode.SIZE to "Tamanho", SortMode.TYPE to "Tipo",
                ).forEach { (mode, label) ->
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                        color = if (sortMode == mode) Color.White else XpBlueDark,
                        modifier = Modifier.clip(RoundedCornerShape(7.dp))
                            .background(if (sortMode == mode) XpBlue else Color.White)
                            .border(1.dp, XpChromeBorder, RoundedCornerShape(7.dp))
                            .clickable { onSortMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onFoldersFirstChange(!foldersFirst) }) {
                Checkbox(checked = foldersFirst, onCheckedChange = onFoldersFirstChange)
                Text("Exibir pastas primeiro", fontSize = 12.sp, color = Color(0xFF253B56))
            }

            Spacer(Modifier.height(6.dp))
            PanelGroupTitle("Informações e ajuda")
            PanelToolEntry(R.drawable.help, "Ajuda", "Orientações de uso", onClick = { onOpenTool(ExplorerToolAction.HELP) })
            PanelToolEntry(R.drawable.info, "Sobre o Explorador XP", "Versão e novidades", onClick = { onOpenTool(ExplorerToolAction.ABOUT) })
            Text(
                "Diagnóstico de downloads, teste de velocidade, IP público e ferramentas de VPN: previstos para etapas futuras, ainda não disponíveis.",
                color = XpTextSecondary, fontSize = 11.sp, lineHeight = 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun PanelGroupTitle(title: String) {
    Text(title, color = XpBlueDark, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        modifier = Modifier.padding(top = 5.dp, bottom = 2.dp))
}

@Composable
private fun PanelToolEntry(icon: Int, title: String, detail: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, XpChromeBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF253B56))
            Text(detail, fontSize = 11.sp, color = XpTextSecondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(4.dp))
        Text("›", fontSize = 22.sp, color = XpBlueDark)
    }
}
