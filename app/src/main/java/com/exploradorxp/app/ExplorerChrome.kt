package com.exploradorxp.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Componentes comuns de chrome: mesmo desenho e dimensões em Arquivos, Fotos e Vídeos. */
@Composable
internal fun XpExplorerMenuBar(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(29.dp).background(XpChrome)
            .border(1.dp, XpChromeBorder).horizontalScroll(rememberScrollState())
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

internal data class XpNavigationCommands(
    val canBack: Boolean,
    val canForward: Boolean,
    val canUp: Boolean,
    val onBack: () -> Unit,
    val onForward: () -> Unit,
    val onHome: () -> Unit,
    val onUp: () -> Unit,
    val onSearch: () -> Unit,
    val onNewFolder: () -> Unit,
    val onNewFile: () -> Unit,
    val canNew: Boolean,
    val onRefresh: () -> Unit,
    val onLast: () -> Unit,
    val lastLabel: String,
    val lastIcon: Int,
    val canLast: Boolean = true,
)

@Composable
internal fun XpExplorerNavigationBar(commands: XpNavigationCommands) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(XpChrome)
            .border(1.dp, XpChromeBorder).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XpNavigationButtons(commands)
    }
}

@Composable
internal fun RowScope.XpNavigationButtons(commands: XpNavigationCommands) {
        XpClassicToolButton(R.drawable.back, "Voltar", commands.canBack, commands.onBack, Modifier.weight(1f), compact = true)
        XpClassicToolButton(R.drawable.forward, "Avançar", commands.canForward, commands.onForward, Modifier.weight(1f), compact = true)
        XpClassicToolButton(R.drawable.home, "Início", true, commands.onHome, Modifier.weight(1f), compact = true)
        XpClassicToolButton(R.drawable.up, "Subir", commands.canUp, commands.onUp, Modifier.weight(1f), compact = true)
        XpClassicToolButton(R.drawable.search, "Pesquisar", true, commands.onSearch, Modifier.weight(1f), compact = true)
        var newMenu by remember { mutableStateOf(false) }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            XpClassicToolButton(R.drawable.file_new, "Novo", commands.canNew,
                { newMenu = true }, Modifier.fillMaxWidth(), compact = true)
            XpPopupMenu(newMenu, { newMenu = false }) {
                XpMenuItem("Nova pasta", R.drawable.folder_new) { newMenu = false; commands.onNewFolder() }
                XpMenuItem("Novo arquivo", R.drawable.file_new) { newMenu = false; commands.onNewFile() }
            }
        }
        XpClassicToolButton(R.drawable.refresh, "Atualizar", true, commands.onRefresh, Modifier.weight(1f), compact = true)
        XpClassicToolButton(commands.lastIcon, commands.lastLabel, commands.canLast, commands.onLast, Modifier.weight(1f), compact = true)
}

/** Menus reutilizados, inclusive suas ações, pelo explorador e pelas duas galerias. */
@Composable
internal fun XpExplorerToolsMenu(
    onApps: () -> Unit,
    onNetwork: () -> Unit,
    onSettings: () -> Unit,
    onDevice: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        XpMenuLabel("Ferramentas", expanded) { expanded = true }
        XpPopupMenu(expanded, { expanded = false }) {
            XpMenuItem("Aplicativos instalados", R.drawable.file_apk) { expanded = false; onApps() }
            XpMenuItem("Rede local (WebDAV)", R.drawable.device_mobile) { expanded = false; onNetwork() }
            XpMenuDivider()
            XpMenuItem("Configurações", R.drawable.settings) { expanded = false; onSettings() }
            XpMenuItem("Informações do dispositivo", R.drawable.device_mobile) { expanded = false; onDevice() }
        }
    }
}

@Composable
internal fun XpExplorerHelpMenu(onHelp: () -> Unit, onAbout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        XpMenuLabel("Ajuda", expanded) { expanded = true }
        XpPopupMenu(expanded, { expanded = false }) {
            XpMenuItem("Ajuda", R.drawable.help) { expanded = false; onHelp() }
            XpMenuDivider()
            XpMenuItem("Sobre o Explorador XP", R.drawable.info) { expanded = false; onAbout() }
        }
    }
}
