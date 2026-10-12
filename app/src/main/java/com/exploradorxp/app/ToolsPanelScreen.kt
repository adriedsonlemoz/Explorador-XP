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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
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

private data class ToolShortcut(val action: ExplorerToolAction, val label: String, val icon: Int)

private val panelShortcuts = listOf(
    ToolShortcut(ExplorerToolAction.APPS, "Aplicativos", R.drawable.file_apk),
    ToolShortcut(ExplorerToolAction.STORAGE, "Armazenamento", R.drawable.drive_storage),
    ToolShortcut(ExplorerToolAction.DEVICE, "Dispositivo", R.drawable.device_mobile),
    ToolShortcut(ExplorerToolAction.NETWORK, "Rede local", R.drawable.device_mobile),
    ToolShortcut(ExplorerToolAction.TRASH, "Lixeira", R.drawable.trash_empty),
    ToolShortcut(ExplorerToolAction.SETTINGS, "Configurações", R.drawable.settings),
    ToolShortcut(ExplorerToolAction.HELP, "Ajuda", R.drawable.help),
    ToolShortcut(ExplorerToolAction.ABOUT, "Sobre", R.drawable.info),
)

/** Grade de atalhos no estilo Painel de Controle: ícone e título, sem descrições. */
@Composable
internal fun ToolsPanelScreen(
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
            columns = GridCells.Adaptive(minSize = 76.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
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
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(40.dp)
                .background(Color(0xFFEAF3FF), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(shortcut.icon), contentDescription = null,
                modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            shortcut.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, color = Color(0xFF253B56),
        )
    }
}
