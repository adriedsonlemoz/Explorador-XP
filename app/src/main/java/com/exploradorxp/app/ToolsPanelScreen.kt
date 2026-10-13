package com.exploradorxp.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Apenas módulos existentes: o painel não cria serviços ou permissões adicionais. */
internal enum class ExplorerToolAction { APPS, STORAGE, DEVICE, SETTINGS, NETWORK, TRASH, HELP, ABOUT }

private data class ToolShortcut(val action: ExplorerToolAction, val label: String, val icon: Int, val description: String)

private val panelShortcuts = listOf(
    ToolShortcut(ExplorerToolAction.APPS, "Aplicativos", R.drawable.file_apk, "Instalados e uso"),
    ToolShortcut(ExplorerToolAction.STORAGE, "Armazenamento", R.drawable.drive_storage, "Espaço e arquivos"),
    ToolShortcut(ExplorerToolAction.DEVICE, "Dispositivo", R.drawable.device_mobile, "Dados do aparelho"),
    ToolShortcut(ExplorerToolAction.NETWORK, "Rede local", R.drawable.device_mobile, "Wi-Fi e conexões"),
    ToolShortcut(ExplorerToolAction.TRASH, "Lixeira", R.drawable.trash_empty, "Itens excluídos"),
    ToolShortcut(ExplorerToolAction.SETTINGS, "Configurações", R.drawable.settings, "Preferências do XP"),
    ToolShortcut(ExplorerToolAction.HELP, "Ajuda", R.drawable.help, "Dúvidas e uso"),
    ToolShortcut(ExplorerToolAction.ABOUT, "Sobre", R.drawable.info, "Versão e novidades"),
)

/** Atalhos agrupados em quatro colunas, com legenda para reduzir a ambiguidade. */
@Composable
internal fun ToolsPanelScreen(
    onOpenTool: (ExplorerToolAction) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Column(Modifier.fillMaxSize().background(XpBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(XpBlueDark).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.settings), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Painel de Ferramentas", color = Color.White, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Fechar", color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
            )
        }
        HorizontalDivider(color = XpChromeBorder)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 82.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(panelShortcuts, key = { it.action }) { shortcut ->
                ToolShortcutTile(shortcut, onClick = { onOpenTool(shortcut.action) })
            }
        }
    }
}

@Composable
private fun ToolShortcutTile(shortcut: ToolShortcut, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().height(112.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .border(1.dp, XpCardBorder, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(45.dp)
                .background(Color(0xFFEAF3FF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(shortcut.icon), contentDescription = null,
                modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            shortcut.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, color = Color(0xFF253B56),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            shortcut.description, fontSize = 8.sp, lineHeight = 9.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, color = XpTextSecondary,
        )
    }
}
