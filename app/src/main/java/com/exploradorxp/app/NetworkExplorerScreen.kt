package com.exploradorxp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class NetworkPage { HOME, DISCOVERY, MANUAL, SHARE, REMOTE, WINDOWS }

/** Isolated screen: neither local file operations nor the main file ViewModel are mutated. */
@Composable
fun NetworkExplorerScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { NetworkSharedFolders(context) }
    val discovery = remember(context) { NetworkServiceDiscovery(context) }
    val discoveryState by discovery.state.collectAsState()
    DisposableEffect(discovery) { onDispose { discovery.stop() } }
    var folders by remember { mutableStateOf(store.list()) }
    val server by NetworkShareStatus.state.collectAsState()
    var allowUploads by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("xp") }
    var client by remember { mutableStateOf<NetworkDavClient?>(null) }
    // Cancelar requisições remotas ao sair desta tela ou trocar de conexão.
    DisposableEffect(client) { onDispose { client?.cancel() } }
    var currentPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var remoteItems by remember { mutableStateOf<List<NetworkRemoteEntry>>(emptyList()) }
    var selectedDownload by remember { mutableStateOf<NetworkRemoteEntry?>(null) }
    var transferJob by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(NetworkPage.HOME) }
    var showAddMenu by remember { mutableStateOf(false) }

    fun reportFailure(error: Throwable) {
        message = if (error is CancellationException) "Transferência cancelada." else
            (error.localizedMessage ?: "Não foi possível realizar a operação.")
    }

    fun readRemote(remote: NetworkDavClient, path: List<String>) {
        scope.launch {
            busy = true
            message = ""
            try {
                val entries = withContext(Dispatchers.IO) { remote.list(path) }
                client = remote
                currentPath = path
                remoteItems = entries
            } catch (error: Exception) { reportFailure(error) }
            finally { busy = false }
        }
    }

    fun transfer(action: suspend (NetworkDavClient) -> Unit) {
        val remote = client ?: return
        if (busy) return
        transferJob = scope.launch {
            busy = true
            message = ""
            progress = "Iniciando transferência..."
            try {
                action(remote)
                message = "Transferência concluída."
                progress = ""
                remoteItems = withContext(Dispatchers.IO) { remote.list(currentPath) }
            } catch (error: Exception) {
                reportFailure(error)
            } finally {
                busy = false
                transferJob = null
                progress = ""
            }
        }
    }

    val pickShare = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && !server.active && !server.starting) {
            runCatching { store.add(uri); folders = store.list() }
                .onFailure { message = it.localizedMessage ?: "Não foi possível autorizar a pasta." }
        }
    }
    val saveDownload = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val selected = selectedDownload
        selectedDownload = null
        if (uri != null && selected != null) {
            transfer { remote ->
                var lastUpdate = 0L
                withContext(Dispatchers.IO) {
                    remote.download(context, selected, uri) { done, total ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250 || done == total) {
                            lastUpdate = now
                            scope.launch { progress = "Baixando: ${sizeLabel(done)} / ${if (total < 0) "?" else sizeLabel(total)}" }
                        }
                    }
                }
            }
        }
    }
    val selectUpload = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val dest = currentPath.toList()
            transfer { remote ->
                var lastUpdate = 0L
                withContext(Dispatchers.IO) {
                    remote.upload(context, uri, dest) { done, total ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 250 || done == total) {
                            lastUpdate = now
                            scope.launch { progress = "Enviando: ${sizeLabel(done)} / ${sizeLabel(total)}" }
                        }
                    }
                }
            }
        }
    }
    fun goBack() {
        when {
            busy -> { client?.cancel(); transferJob?.cancel(); message = "Cancelando..." }
            page == NetworkPage.REMOTE && currentPath.isNotEmpty() -> client?.let { readRemote(it, currentPath.dropLast(1)) }
            page != NetworkPage.HOME -> { if (page == NetworkPage.DISCOVERY) discovery.stop(); page = NetworkPage.HOME }
            else -> onDismiss()
        }
    }
    BackHandler { goBack() }

    Column(Modifier.fillMaxSize().background(XpBackground)) {
        Row(Modifier.fillMaxWidth().background(XpBlueDark).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("‹  Voltar", modifier = Modifier.clickable { goBack() }, color = Color.White, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(when (page) {
                NetworkPage.HOME -> "Rede local"
                NetworkPage.DISCOVERY -> "Procurar aparelhos"
                NetworkPage.MANUAL -> "Conectar por IP"
                NetworkPage.SHARE -> "Compartilhar pastas"
                NetworkPage.REMOTE -> "Arquivos remotos"
                NetworkPage.WINDOWS -> "Windows / SMB"
            }, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    NetworkPage.HOME -> {
                        Text("Conexões e compartilhamentos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Conecte aparelhos pela rede Wi-Fi. Para adicionar uma conexão ou pasta, toque em +.",
                            style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        Text("Meus compartilhamentos", style = MaterialTheme.typography.titleMedium)
                        Text(if (server.active) "Servidor HTTPS ativo • ${folders.size} pasta(s) autorizada(s)" else
                            "Servidor desligado • ${folders.size} pasta(s) selecionada(s)",
                            style = MaterialTheme.typography.bodyMedium)
                        if (folders.isEmpty()) Text("Nenhuma pasta autorizada.", style = MaterialTheme.typography.bodySmall)
                        folders.forEach { folder ->
                            Text("📁 ${folder.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                        OutlinedButton(onClick = { page = NetworkPage.SHARE }) { Text("Gerenciar compartilhamento") }
                        HorizontalDivider()
                        Text("Dispositivos", style = MaterialTheme.typography.titleMedium)
                        Text(if (client != null) "Conectado: $host" else "Nenhum dispositivo conectado.",
                            style = MaterialTheme.typography.bodyMedium)
                        if (client != null) OutlinedButton(onClick = { page = NetworkPage.REMOTE }) { Text("Abrir dispositivo conectado") }
                        if (discoveryState.servers.isNotEmpty()) {
                            discoveryState.servers.forEach { device ->
                                NetworkHubAction("📱 ${device.serviceName}", device.url) {
                                    host = device.url
                                    fingerprint = ""
                                    secret = ""
                                    page = NetworkPage.MANUAL
                                }
                            }
                        }
                        Text("A busca é manual e a conexão exige autenticação e verificação do certificado.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    NetworkPage.SHARE -> {
                        Text("Pastas deste celular", style = MaterialTheme.typography.titleMedium)
                        Text("Somente pastas autorizadas são compartilhadas. O servidor inicia com escrita desativada.",
                            style = MaterialTheme.typography.bodySmall)
                        if (folders.isEmpty()) Text("Nenhuma pasta autorizada.")
                        folders.forEach { folder ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(folder.name, fontWeight = FontWeight.Medium)
                                    Text("/dav/${folder.alias}/", style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(enabled = !server.active && !server.starting, onClick = {
                                    runCatching { store.remove(folder.alias); folders = store.list() }
                                        .onFailure { message = it.localizedMessage ?: "Falha ao remover pasta" }
                                }) { Text("Remover") }
                            }
                        }
                        OutlinedButton(enabled = !server.active && !server.starting,
                            onClick = { pickShare.launch(null) }) { Text("Selecionar pasta") }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = allowUploads, enabled = !server.active && !server.starting,
                                onCheckedChange = { allowUploads = it })
                            Text("Permitir que outros dispositivos enviem arquivos (desativado por padrão)",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (server.active) {
                            Text("Servidor ativo • ${if (server.uploadEnabled) "envio permitido" else "somente leitura"}")
                            Text("Endereço: ${server.addresses.firstOrNull()?.let { "https://$it:${NetworkDavServer.PORT}" } ?: "Verifique o Wi-Fi"}")
                            Text("Usuário: xp")
                            Text("Senha temporária: ${server.password}")
                            Text("SHA-256 do certificado: ${server.fingerprint}", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = {
                                val text = "Endereço: https://${server.addresses.firstOrNull() ?: "IP-DO-CELULAR"}:${NetworkDavServer.PORT}\n" +
                                    "Usuário: xp\nSenha: ${server.password}\nSHA-256: ${server.fingerprint}"
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                    .setPrimaryClip(ClipData.newPlainText("Explorador XP conexão", text))
                                message = "Dados copiados. Compartilhe apenas com aparelhos autorizados."
                            }) { Text("Copiar dados da conexão") }
                            Button(onClick = { NetworkShareService.stop(context) }) { Text("Desligar compartilhamento") }
                        } else {
                            Button(enabled = folders.isNotEmpty() && !server.starting, onClick = {
                                message = ""; NetworkShareService.start(context, allowUploads)
                            }) { Text(if (server.starting) "Iniciando..." else "Ativar compartilhamento") }
                        }
                        if (server.error != null) Text("Servidor: ${server.error}", color = MaterialTheme.colorScheme.error)
                        Text("Ao parar o servidor, as credenciais da sessão deixam de ser válidas.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    NetworkPage.DISCOVERY -> {
                        Text("Busca automática", style = MaterialTheme.typography.titleMedium)
                        Text("Localiza anúncios WebDAV HTTPS no Wi-Fi. A descoberta não valida a identidade do aparelho.",
                            style = MaterialTheme.typography.bodySmall)
                        if (discoveryState.searching) {
                            OutlinedButton(onClick = { discovery.stop() }) { Text("Parar busca") }
                            if (discoveryState.servers.isEmpty()) Text("Procurando aparelhos compatíveis...")
                        } else {
                            Button(onClick = { discovery.start() }) { Text("Iniciar busca") }
                        }
                        discoveryState.servers.forEach { device ->
                            NetworkHubAction(device.serviceName, device.url) {
                                client?.cancel(); client = null; currentPath = emptyList(); remoteItems = emptyList()
                                host = device.url; fingerprint = ""; secret = ""
                                discovery.stop(); page = NetworkPage.MANUAL
                                message = "Confira a impressão SHA-256 no dispositivo remoto antes de conectar."
                            }
                        }
                        if (discoveryState.error != null) Text(discoveryState.error ?: "", color = MaterialTheme.colorScheme.error)
                    }
                    NetworkPage.MANUAL -> {
                        Text("Conectar por endereço HTTPS", style = MaterialTheme.typography.titleMedium)
                        Text("Para outro Explorador XP use https://IP:18843. Confira o SHA-256 exibido no outro aparelho.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = host, onValueChange = { host = it }, singleLine = true,
                            label = { Text("Endereço HTTPS e porta") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        OutlinedTextField(value = fingerprint, onValueChange = { fingerprint = it }, singleLine = true,
                            label = { Text("SHA-256 do certificado remoto") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        OutlinedTextField(value = username, onValueChange = { username = it }, singleLine = true,
                            label = { Text("Usuário WebDAV") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        OutlinedTextField(value = secret, onValueChange = { secret = it }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            label = { Text("Senha do dispositivo") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        Button(enabled = !busy && host.isNotBlank() && fingerprint.isNotBlank() && secret.isNotBlank(), onClick = {
                            scope.launch {
                                busy = true; message = ""
                                try {
                                    val remote = withContext(Dispatchers.IO) { NetworkDavClient(host, fingerprint, secret, username) }
                                    val root = withContext(Dispatchers.IO) { remote.list(emptyList()) }
                                    client?.cancel(); client = remote
                                    currentPath = emptyList(); remoteItems = root
                                    page = NetworkPage.REMOTE
                                } catch (error: Exception) { reportFailure(error) }
                                finally { busy = false }
                            }
                        }) { Text("Conectar") }
                    }
                    NetworkPage.REMOTE -> {
                        Text("Conectado a: $host", style = MaterialTheme.typography.bodySmall)
                        Text("/${currentPath.joinToString("/")}", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !busy && client != null, onClick = {
                                client?.let { readRemote(it, currentPath) }
                            }) { Text("Atualizar") }
                            OutlinedButton(enabled = !busy, onClick = {
                                client?.cancel(); client = null; remoteItems = emptyList(); currentPath = emptyList()
                                page = NetworkPage.HOME
                            }) { Text("Desconectar") }
                        }
                        if (currentPath.isNotEmpty()) OutlinedButton(enabled = !busy, onClick = {
                            client?.let { readRemote(it, currentPath.dropLast(1)) }
                        }) { Text("↑ Pasta anterior") }
                        if (remoteItems.isEmpty()) Text("Nenhum arquivo nesta pasta.")
                        remoteItems.forEach { file ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text((if (file.isDirectory) "📁 " else "📄 ") + file.name,
                                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(enabled = !busy, onClick = {
                                    if (file.isDirectory) client?.let { readRemote(it, file.path) }
                                    else { selectedDownload = file; saveDownload.launch(file.name) }
                                }) { Text(if (file.isDirectory) "Abrir" else "Baixar") }
                            }
                        }
                        if (currentPath.isNotEmpty()) {
                            OutlinedButton(enabled = !busy, onClick = { selectUpload.launch(arrayOf("*/*")) }) {
                                Text("Enviar arquivo para esta pasta")
                            }
                            Text("O servidor precisa permitir escrita; arquivos existentes não são substituídos.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    NetworkPage.WINDOWS -> {
                        Text("Compartilhamentos do Windows (SMB)", style = MaterialTheme.typography.titleMedium)
                        Text("SMB2/SMB3 ainda não está implementado nesta versão. " +
                            "A busca automática atual encontra servidores WebDAV HTTPS, não pastas Windows SMB.",
                            style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { page = NetworkPage.MANUAL }) { Text("Conectar a WebDAV por IP") }
                    }
                }
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (progress.isNotBlank()) Text(progress, style = MaterialTheme.typography.bodySmall)
                    if (transferJob != null) OutlinedButton(onClick = {
                        client?.cancel(); transferJob?.cancel(); message = "Cancelando transferência..."
                    }) { Text("Cancelar transferência") }
                }
                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(14.dp)) {
                Button(onClick = { showAddMenu = true }, modifier = Modifier.size(60.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("+", fontSize = 30.sp, color = Color.White)
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(text = { Text("Busca automática") }, onClick = {
                        showAddMenu = false; page = NetworkPage.DISCOVERY; discovery.start()
                    })
                    DropdownMenuItem(text = { Text("Conectar por IP / WebDAV") }, onClick = {
                        showAddMenu = false; discovery.stop(); page = NetworkPage.MANUAL
                    })
                    DropdownMenuItem(text = { Text("Buscar Windows / SMB (futuro)") }, onClick = {
                        showAddMenu = false; discovery.stop(); page = NetworkPage.WINDOWS
                    })
                    DropdownMenuItem(text = { Text("Compartilhar pasta") }, onClick = {
                        showAddMenu = false; discovery.stop(); page = NetworkPage.SHARE
                    })
                }
            }
        }
    }

}

private fun sizeLabel(bytes: Long): String = when {
    bytes < 0L -> "Tamanho desconhecido"
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f KiB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MiB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.getDefault(), "%.2f GiB", bytes / (1024.0 * 1024 * 1024))
}

@Composable
private fun NetworkHubAction(title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Text("Abrir ›", color = XpBlueDark, fontSize = 12.sp)
    }
    HorizontalDivider()
}
