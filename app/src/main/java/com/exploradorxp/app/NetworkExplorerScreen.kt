package com.exploradorxp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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
import kotlinx.coroutines.ensureActive
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
    val smbDiscovery = remember(context) { NetworkServiceDiscovery(context, "_smb._tcp.") }
    val smbDiscoveryState by smbDiscovery.state.collectAsState()
    DisposableEffect(discovery, smbDiscovery) {
        onDispose { discovery.stop(); smbDiscovery.stop() }
    }
    var folders by remember { mutableStateOf(store.list()) }
    val server by NetworkShareStatus.state.collectAsState()
    var allowUploads by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("xp") }
    var client by remember { mutableStateOf<NetworkRemoteClient?>(null) }
    var smbHost by remember { mutableStateOf("") }
    var smbShare by remember { mutableStateOf("") }
    var smbUsername by remember { mutableStateOf("") }
    var smbPassword by remember { mutableStateOf("") }
    var smbDomain by remember { mutableStateOf("") }
    var selectedPaths by remember { mutableStateOf<Set<List<String>>>(emptySet()) }
    // Cancelar requisições remotas ao sair desta tela ou trocar de conexão.
    DisposableEffect(client) {
        val attached = client
        onDispose { attached?.cancel() }
    }
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

    fun readRemote(remote: NetworkRemoteClient, path: List<String>) {
        scope.launch {
            busy = true
            message = ""
            try {
                val entries = withContext(Dispatchers.IO) { remote.list(path) }
                client = remote
                currentPath = path
                remoteItems = entries
                selectedPaths = emptySet()
            } catch (error: Exception) { reportFailure(error) }
            finally { busy = false }
        }
    }

    fun transfer(action: suspend (NetworkRemoteClient) -> Unit) {
        val remote = client ?: return
        if (busy) return
        transferJob = scope.launch {
            busy = true
            message = ""
            progress = "Iniciando transferência..."
            try {
                action(remote)
                if (message.isBlank()) message = "Transferência concluída."
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
    val copyToFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val paths = selectedPaths.toSet()
        val files = remoteItems.filter { !it.isDirectory && it.path in paths }
        if (uri != null && files.isNotEmpty()) {
            transfer { remote ->
                withContext(Dispatchers.IO) {
                    val folder = DocumentFile.fromTreeUri(context, uri)
                        ?: throw java.io.IOException("Não foi possível abrir a pasta de destino.")
                    if (!folder.canWrite()) throw java.io.IOException("Pasta de destino sem permissão de escrita.")
                    var copied = 0
                    var skipped = 0
                    for ((index, file) in files.withIndex()) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        // Não substituir arquivos locais já existentes.
                        if (!NetworkSharePaths.validName(file.name) || folder.findFile(file.name) != null) {
                            skipped++
                            continue
                        }
                        val document = folder.createFile("application/octet-stream", file.name)
                            ?: throw java.io.IOException("Não foi possível criar ${file.name}.")
                        try {
                            remote.download(context, file, document.uri) { done, total ->
                                scope.launch {
                                    progress = "Arquivo ${index + 1}/${files.size}: ${file.name} • ${sizeLabel(done)} / ${sizeLabel(total)}"
                                }
                            }
                            copied++
                        } catch (error: Exception) {
                            runCatching { document.delete() }
                            throw error
                        }
                    }
                    withContext(Dispatchers.Main) { message = "$copied arquivo(s) copiado(s); $skipped ignorado(s) por nome repetido ou inválido." }
                }
            }
        }
    }
    val quickShare = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(context.contentResolver, "Arquivo escolhido", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Enviar arquivo para outro aparelho"))
            }.onFailure { message = it.localizedMessage ?: "Não há aplicativo de compartilhamento disponível." }
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
                        Text("Arquivos entre aparelhos", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("O que você quer fazer?", style = MaterialTheme.typography.bodyMedium)
                        NetworkHubAction("📱  Acessar outro celular",
                            "Encontrar um celular que já está compartilhando uma pasta") {
                            discovery.start(); page = NetworkPage.DISCOVERY
                        }
                        NetworkHubAction("📂  Compartilhar arquivos deste celular",
                            "Escolha a pasta que o outro celular poderá abrir") { page = NetworkPage.SHARE }
                        HorizontalDivider()
                        Text("Neste celular", style = MaterialTheme.typography.titleMedium)
                        Text(if (server.active) "🟢 Compartilhamento ligado" else "⚪ Compartilhamento desligado",
                            fontWeight = FontWeight.Medium)
                        Text(if (folders.isEmpty()) "Nenhuma pasta escolhida. Toque em Compartilhar para selecionar."
                            else if (server.active) "${folders.size} pasta(s) disponível(is) para outros aparelhos."
                            else "${folders.size} pasta(s) escolhida(s), mas ainda não disponível(is) na rede.",
                            style = MaterialTheme.typography.bodySmall)
                        if (server.active) {
                            Text("Endereço: https://${server.addresses.firstOrNull() ?: "IP-DO-CELULAR"}:${NetworkDavServer.PORT}",
                                style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { page = NetworkPage.SHARE }) { Text("Ver dados para conectar") }
                        }
                        HorizontalDivider()
                        Text("Outro aparelho", style = MaterialTheme.typography.titleMedium)
                        if (client != null) {
                            NetworkHubAction("📁  Abrir arquivos conectados", host) { page = NetworkPage.REMOTE }
                        } else Text("Nenhum aparelho conectado. Abra 'Acessar outro celular' para começar.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { page = NetworkPage.MANUAL }) { Text("Conectar usando endereço IP") }
                        Text("Importante: a busca encontra servidores WebDAV ativos, não todos os celulares da rede nem pastas do Windows.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    NetworkPage.SHARE -> {
                        Text("Compartilhar arquivos deste celular", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("1. Escolha uma pasta.  2. Ative o compartilhamento.  3. Abra 'Acessar outro celular' no outro aparelho.",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Suas outras pastas continuam privadas. Nada será compartilhado até você ativar.",
                            style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        Text("1  Pastas escolhidas", style = MaterialTheme.typography.titleMedium)
                        if (folders.isEmpty()) Text("Nenhuma pasta selecionada. Exemplo: escolha a pasta Download ou DCIM para compartilhar apenas seu conteúdo.")
                        folders.forEach { folder ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("📁 ${folder.name}", fontWeight = FontWeight.Medium)
                                    Text("Pasta disponível para leitura quando ativar", style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(enabled = !server.active && !server.starting, onClick = {
                                    runCatching { store.remove(folder.alias); folders = store.list() }
                                        .onFailure { message = it.localizedMessage ?: "Falha ao remover pasta" }
                                }) { Text("Remover") }
                            }
                        }
                        OutlinedButton(enabled = !server.active && !server.starting,
                            onClick = { pickShare.launch(null) }) { Text("+ Escolher pasta") }
                        HorizontalDivider()
                        Text("2  Ligar compartilhamento", style = MaterialTheme.typography.titleMedium)
                        Text(if (server.active) "🟢 Ligado: os arquivos das pastas acima podem ser acessados por quem tiver os dados da conexão."
                            else "⚪ Desligado: o outro aparelho não consegue encontrar suas pastas.",
                            style = MaterialTheme.typography.bodyMedium)
                        if (!server.active) {
                            Button(enabled = folders.isNotEmpty() && !server.starting, onClick = {
                                message = ""; NetworkShareService.start(context, allowUploads)
                            }) { Text(if (server.starting) "Iniciando..." else "Ativar compartilhamento") }
                            if (folders.isEmpty()) Text("Escolha uma pasta primeiro para liberar este botão.",
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            Button(onClick = { NetworkShareService.stop(context) }) { Text("Desligar compartilhamento") }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = allowUploads, enabled = !server.active && !server.starting,
                                onCheckedChange = { allowUploads = it })
                            Text("Permitir receber novos arquivos (opcional). Por segurança, fica desligado.",
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        }
                        if (server.active) {
                            HorizontalDivider()
                            Text("3  Conectar pelo outro aparelho", style = MaterialTheme.typography.titleMedium)
                            Text("No outro celular: Rede > Acessar outro celular > Procurar. Se não aparecer, escolha Conectar por IP.",
                                style = MaterialTheme.typography.bodySmall)
                            Text("Endereço: ${server.addresses.firstOrNull()?.let { "https://$it:${NetworkDavServer.PORT}" } ?: "IP indisponível: confira Wi-Fi/hotspot"}")
                            if (server.addresses.size > 1) {
                                Text("Outros endereços possíveis (use o IP da rede Wi-Fi):", style = MaterialTheme.typography.bodySmall)
                                server.addresses.drop(1).forEach { ip ->
                                    Text("https://$ip:${NetworkDavServer.PORT}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Text("Usuário: xp")
                            Text("Senha temporária: ${server.password}")
                            Text("Código de segurança SHA-256: ${server.fingerprint}", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = {
                                val text = "Endereço: https://${server.addresses.firstOrNull() ?: "IP-DO-CELULAR"}:${NetworkDavServer.PORT}\n" +
                                    "Usuário: xp\nSenha: ${server.password}\nSHA-256: ${server.fingerprint}"
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                    .setPrimaryClip(ClipData.newPlainText("Explorador XP conexão", text))
                                message = "Dados copiados. A área de transferência contém a senha; não compartilhe com desconhecidos."
                            }) { Text("Copiar dados para conectar") }
                        }
                        if (server.error != null) Text("Não foi possível compartilhar: ${server.error}",
                            color = MaterialTheme.colorScheme.error)
                    }
                    NetworkPage.DISCOVERY -> {
                        Text("Encontrar outro celular", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("No OUTRO celular, abra Explorador XP > Rede > Compartilhar arquivos, escolha uma pasta e toque em Ativar compartilhamento.",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Depois, mantenha os dois na mesma rede Wi-Fi ou hotspot e procure aqui.",
                            style = MaterialTheme.typography.bodySmall)
                        if (discoveryState.searching) {
                            OutlinedButton(onClick = { discovery.stop() }) { Text("Parar busca") }
                        } else {
                            Button(onClick = { discovery.start() }) { Text("Procurar novamente") }
                        }
                        if (discoveryState.servers.isEmpty()) {
                            Text(if (discoveryState.searching) "Buscando compartilhamentos WebDAV ativos..."
                                else "Nenhum compartilhamento encontrado.",
                                style = MaterialTheme.typography.titleSmall)
                            Text(when {
                                discoveryState.addressFailures > 0 -> "Encontrei um anúncio, mas não consegui obter um endereço IPv4 local. Tente usar o IP exibido no outro celular."
                                discoveryState.resolveFailures > 0 -> "Um aparelho respondeu, mas não foi possível obter o endereço dele. Confira o Wi-Fi dos dois celulares."
                                else -> "Se o outro celular não ativou o compartilhamento, ele NÃO aparece nesta lista."
                            }, style = MaterialTheme.typography.bodySmall)
                            Text("Se já ativou: verifique se os dois estão na mesma rede, se o roteador permite aparelhos se comunicarem e se o servidor continua ligado.",
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Compartilhamentos encontrados", style = MaterialTheme.typography.titleMedium)
                        }
                        discoveryState.servers.forEach { device ->
                            NetworkHubAction("📱 ${device.serviceName}", device.url) {
                                client?.cancel(); client = null; currentPath = emptyList(); remoteItems = emptyList()
                                host = device.url; fingerprint = ""; secret = ""
                                discovery.stop(); page = NetworkPage.MANUAL
                                message = "Confira o código SHA-256 mostrado no outro celular antes de conectar."
                            }
                        }
                        if (discoveryState.error != null) Text(discoveryState.error ?: "", color = MaterialTheme.colorScheme.error)
                        HorizontalDivider()
                        OutlinedButton(onClick = { discovery.stop(); page = NetworkPage.MANUAL }) {
                            Text("Não apareceu? Conectar pelo IP")
                        }
                        Text("A busca encontra apenas servidores WebDAV HTTPS, como outro Explorador XP com compartilhamento ligado. Não encontra pastas Windows/SMB.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    NetworkPage.MANUAL -> {
                        Text("Conectar pelo endereço do outro aparelho", style = MaterialTheme.typography.titleMedium)
                        Text("No outro celular, ative Compartilhar e copie os dados exibidos. Informe aqui endereço, código de segurança e senha. Exemplo: https://192.168.1.20:18843.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = host, onValueChange = { host = it }, singleLine = true,
                            label = { Text("Endereço HTTPS e porta") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        OutlinedTextField(value = fingerprint, onValueChange = { fingerprint = it }, singleLine = true,
                            label = { Text("Código SHA-256 exibido no outro celular") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
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
                        Text("Conectado a: ${if (client is NetworkSmbClient) "SMB $smbHost/$smbShare" else host}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("/${currentPath.joinToString("/")}", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !busy && client != null, onClick = {
                                client?.let { readRemote(it, currentPath) }
                            }) { Text("Atualizar") }
                            OutlinedButton(enabled = !busy, onClick = {
                                client?.cancel(); client = null; remoteItems = emptyList(); currentPath = emptyList()
                                selectedPaths = emptySet()
                                page = NetworkPage.HOME
                            }) { Text("Desconectar") }
                        }
                        if (currentPath.isNotEmpty()) OutlinedButton(enabled = !busy, onClick = {
                            client?.let { readRemote(it, currentPath.dropLast(1)) }
                        }) { Text("↑ Pasta anterior") }
                        if (remoteItems.isEmpty()) Text("Nenhum arquivo nesta pasta.")
                        val selectable = remoteItems.filterNot { it.isDirectory }
                        if (selectable.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(enabled = !busy, onClick = {
                                    selectedPaths = if (selectable.all { it.path in selectedPaths }) emptySet()
                                    else selectable.map { it.path }.toSet()
                                }) { Text(if (selectable.all { it.path in selectedPaths }) "Limpar seleção" else "Selecionar arquivos") }
                                Text("${selectedPaths.size} selecionado(s)", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(enabled = !busy && selectedPaths.isNotEmpty(), onClick = {
                                copyToFolder.launch(null)
                            }) { Text("Copiar selecionados para uma pasta deste celular") }
                        }
                        remoteItems.forEach { file ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (!file.isDirectory) {
                                    Checkbox(checked = file.path in selectedPaths, enabled = !busy,
                                        onCheckedChange = { checked ->
                                            selectedPaths = if (checked) selectedPaths + setOf(file.path)
                                            else selectedPaths - setOf(file.path)
                                        })
                                }
                                Column(Modifier.weight(1f)) {
                                    Text((if (file.isDirectory) "📁 " else "📄 ") + file.name,
                                        style = MaterialTheme.typography.bodyMedium)
                                    if (!file.isDirectory) Text(sizeLabel(file.size), style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(enabled = !busy, onClick = {
                                    if (file.isDirectory) client?.let { readRemote(it, file.path) }
                                    else { selectedDownload = file; saveDownload.launch(file.name) }
                                }) { Text(if (file.isDirectory) "Abrir" else "Salvar") }
                            }
                        }
                        if (currentPath.isNotEmpty() && client is NetworkDavClient) {
                            OutlinedButton(enabled = !busy, onClick = { selectUpload.launch(arrayOf("*/*")) }) {
                                Text("Enviar arquivo para esta pasta")
                            }
                            Text("O servidor precisa permitir escrita; arquivos existentes não são substituídos.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    NetworkPage.WINDOWS -> {
                        Text("Acessar pasta compartilhada (SMB2/SMB3)", style = MaterialTheme.typography.titleLarge)
                        Text("Funciona com Windows, NAS ou Android que tenha um servidor SMB ativo e autorizado. " +
                            "Um celular comum sem compartilhamento NÃO oferece suas pastas pela rede.",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Procurar servidores SMB anunciados no Wi-Fi", style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(onClick = { if (smbDiscoveryState.searching) smbDiscovery.stop() else smbDiscovery.start() }) {
                            Text(if (smbDiscoveryState.searching) "Parar busca SMB" else "Procurar SMB")
                        }
                        smbDiscoveryState.servers.forEach { device ->
                            NetworkHubAction("🖥️ ${device.serviceName}", "IP: ${device.url}") {
                                smbHost = device.url; smbDiscovery.stop()
                            }
                        }
                        if (smbDiscoveryState.servers.isEmpty()) Text(
                            if (smbDiscoveryState.searching) "Procurando anúncios SMB (mDNS)..."
                            else "Se não aparecer, informe o IP manualmente. Nem todo Windows anuncia SMB por mDNS.",
                            style = MaterialTheme.typography.bodySmall)
                        smbDiscoveryState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        OutlinedTextField(value = smbHost, onValueChange = { smbHost = it }, enabled = !busy,
                            singleLine = true, label = { Text("IP do aparelho (ex.: 192.168.1.10)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = smbShare, onValueChange = { smbShare = it }, enabled = !busy,
                            singleLine = true, label = { Text("Nome da pasta compartilhada (ex.: Publico)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = smbUsername, onValueChange = { smbUsername = it }, enabled = !busy,
                            singleLine = true, label = { Text("Usuário autorizado") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = smbPassword, onValueChange = { smbPassword = it }, enabled = !busy,
                            singleLine = true, visualTransformation = PasswordVisualTransformation(),
                            label = { Text("Senha") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = smbDomain, onValueChange = { smbDomain = it }, enabled = !busy,
                            singleLine = true, label = { Text("Domínio (opcional)") }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = !busy && NetworkSmbAddress.privateIpv4(smbHost) &&
                            NetworkSmbAddress.validShare(smbShare) && smbUsername.isNotBlank(), onClick = {
                            scope.launch {
                                busy = true; message = ""
                                try {
                                    val remote = withContext(Dispatchers.IO) {
                                        NetworkSmbClient(smbHost.trim(), smbShare.trim(), smbUsername, smbPassword, smbDomain)
                                    }
                                    try {
                                        val root = withContext(Dispatchers.IO) { remote.list(emptyList()) }
                                        client?.cancel(); client = remote
                                        currentPath = emptyList(); remoteItems = root; selectedPaths = emptySet()
                                        smbDiscovery.stop(); page = NetworkPage.REMOTE
                                    } catch (error: Exception) { remote.cancel(); throw error }
                                } catch (error: Exception) { reportFailure(error) }
                                finally { busy = false }
                            }
                        }) { Text("Conectar e abrir arquivos") }
                        Text("Conexão SMB somente leitura no Explorador XP. A assinatura de mensagens é exigida; " +
                            "a criptografia SMB3 depende também do servidor. Senhas não são salvas.",
                            style = MaterialTheme.typography.bodySmall)
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
                    DropdownMenuItem(text = { Text("Encontrar outro celular") }, onClick = {
                        showAddMenu = false; page = NetworkPage.DISCOVERY; discovery.start()
                    })
                    DropdownMenuItem(text = { Text("Conectar por IP / WebDAV") }, onClick = {
                        showAddMenu = false; discovery.stop(); page = NetworkPage.MANUAL
                    })
                    DropdownMenuItem(text = { Text("Windows / Android SMB2/3") }, onClick = {
                        showAddMenu = false; discovery.stop(); page = NetworkPage.WINDOWS
                    })
                    DropdownMenuItem(text = { Text("Enviar via Quick Share / Android") }, onClick = {
                        showAddMenu = false; quickShare.launch(arrayOf("*/*"))
                    })
                    DropdownMenuItem(text = { Text("Compartilhar arquivos deste celular") }, onClick = {
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
