package com.exploradorxp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipDescription
import android.os.PersistableBundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var showAdvanced by remember { mutableStateOf(false) }
    var showShareDetails by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<NetworkFailure?>(null) }
    var showFailureDetails by remember { mutableStateOf(false) }

    fun reportFailure(error: Throwable) {
        if (error is CancellationException) {
            message = "Operação cancelada."
            failure = null
        } else {
            failure = NetworkFailure.from(error)
            message = ""
        }
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

    // A leitura abre uma janela compacta; nunca conecta nem grava credenciais automaticamente.
    fun acceptQr(value: String) {
        showScanner = false
        runCatching { NetworkQrCode.parse(value) }
            .onSuccess { connection ->
                vibrateQrFeedback(context)
                client?.cancel(); client = null
                host = connection.endpoint; username = connection.username
                secret = connection.password; fingerprint = connection.fingerprint
                showAdvanced = false
                failure = null
                page = NetworkPage.MANUAL
                message = "QR reconhecido. Confira o aparelho e confirme em Conectar."
            }
            .onFailure { error ->
                failure = NetworkFailure.from(error)
                message = "QR inválido. Peça um novo QR ao outro aparelho."
            }
    }
    fun openScanner() { showScanner = true }

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
            page != NetworkPage.HOME -> {
                if (page == NetworkPage.DISCOVERY) discovery.stop()
                failure = null; message = ""; page = NetworkPage.HOME
            }
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
                        Text("Rede local", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NetworkTile("↔", "Acessar", Modifier.weight(1f)) {
                                failure = null; discovery.start(); page = NetworkPage.DISCOVERY
                            }
                            NetworkTile("📁", "Compartilhar", Modifier.weight(1f)) {
                                failure = null; page = NetworkPage.SHARE
                            }
                            NetworkTile("▦", "Ler QR", Modifier.weight(1f)) { openScanner() }
                        }
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Neste celular", fontWeight = FontWeight.SemiBold)
                                Text(if (server.active) "● Compartilhando ${folders.size} pasta(s)" else "○ Compartilhamento desligado")
                                if (server.active) Text("Toque em Compartilhar para ver o QR ou desligar.",
                                    style = MaterialTheme.typography.bodySmall)
                                if (client != null) {
                                    TextButton(onClick = { page = NetworkPage.REMOTE }) { Text("Abrir arquivos conectados ›") }
                                }
                            }
                        }
                        TextButton(onClick = { showHelp = true }) { Text("?  Como funciona a Rede?") }
                        OutlinedButton(onClick = { page = NetworkPage.WINDOWS }) { Text("Windows / SMB") }
                    }
                    NetworkPage.SHARE -> {
                        Text("Compartilhar pastas", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("Escolha uma pasta e ligue o compartilhamento.", style = MaterialTheme.typography.bodySmall)
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Pastas selecionadas", fontWeight = FontWeight.SemiBold)
                                if (folders.isEmpty()) Text("Nenhuma pasta escolhida.", style = MaterialTheme.typography.bodySmall)
                                folders.forEach { folder ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("📁 ${folder.name}", modifier = Modifier.weight(1f), maxLines = 1)
                                        TextButton(enabled = !server.active && !server.starting, onClick = {
                                            runCatching { store.remove(folder.alias); folders = store.list() }
                                                .onFailure { reportFailure(it) }
                                        }) { Text("Remover") }
                                    }
                                }
                                OutlinedButton(enabled = !server.active && !server.starting,
                                    onClick = { pickShare.launch(null) }) { Text("+ Escolher pasta") }
                            }
                        }
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (server.active) "● Compartilhamento ligado" else "○ Compartilhamento desligado",
                                    fontWeight = FontWeight.SemiBold)
                                if (!server.active) {
                                    Button(enabled = folders.isNotEmpty() && !server.starting, onClick = {
                                        message = ""; failure = null; NetworkShareService.start(context, allowUploads)
                                    }) { Text(if (server.starting) "Iniciando..." else "Ativar compartilhamento") }
                                } else {
                                    Button(onClick = { NetworkShareService.stop(context); showQrDialog = false }) {
                                        Text("Desligar compartilhamento")
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = allowUploads, enabled = !server.active && !server.starting,
                                        onCheckedChange = { allowUploads = it })
                                    Text("Permitir receber arquivos", modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                if (server.active) Text("Para mudar essa permissão, desligue o compartilhamento.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (server.active) {
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Conectar pelo outro aparelho", fontWeight = FontWeight.SemiBold)
                                    val shareIp = server.addresses.firstOrNull { NetworkSmbAddress.privateIpv4(it) }
                                    val shareAddress = shareIp?.let { "https://$it:${NetworkDavServer.PORT}" }
                                    Text(shareAddress ?: "Endereço Wi-Fi indisponível", style = MaterialTheme.typography.bodySmall)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(enabled = shareIp != null, onClick = { showQrDialog = true },
                                            modifier = Modifier.weight(1f)) { Text("▦  QR") }
                                        OutlinedButton(enabled = shareAddress != null, onClick = {
                                            copyNetworkText(context, "Endereço para conectar", shareAddress.orEmpty())
                                            message = "Endereço copiado."
                                        }, modifier = Modifier.weight(1f)) { Text("Copiar IP") }
                                        OutlinedButton(onClick = { showShareDetails = !showShareDetails },
                                            modifier = Modifier.weight(1f)) { Text("Detalhes") }
                                    }
                                    if (showShareDetails) {
                                        Text("Usuário: xp", style = MaterialTheme.typography.bodySmall)
                                        Text("Senha temporária: ${server.password}", style = MaterialTheme.typography.bodySmall)
                                        Text("SHA-256: ${server.fingerprint}", style = MaterialTheme.typography.bodySmall)
                                        OutlinedButton(onClick = {
                                            val text = NetworkConnectionData.format(
                                                shareAddress.orEmpty(), "xp", server.password, server.fingerprint)
                                            copyNetworkText(context, "Conexão Explorador XP", text, sensitive = true)
                                            message = "Dados copiados com senha. Compartilhe apenas com pessoas autorizadas."
                                        }) { Text("Copiar todos os dados") }
                                    }
                                }
                            }
                        }
                        server.lastTlsFailure?.let { tlsIssue ->
                            Surface(shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer) {
                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Falha TLS recebida neste celular", fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Código: ${tlsIssue.code}. O outro aparelho chegou ao servidor, " +
                                        "mas a negociação HTTPS não foi concluída.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Detalhe: ${tlsIssue.technical}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                    OutlinedButton(onClick = {
                                        copyNetworkText(context, "TLS servidor Explorador XP", tlsIssue.copyable())
                                        message = "Diagnóstico TLS do servidor copiado, sem credenciais."
                                    }) { Text("Copiar diagnóstico do servidor") }
                                }
                            }
                        }
                        server.error?.let { Text("Falha ao compartilhar: $it", color = MaterialTheme.colorScheme.error) }
                    }
                    NetworkPage.DISCOVERY -> {
                        Text("Acessar arquivos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Encontre um aparelho com compartilhamento ativo.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (discoveryState.searching) discovery.stop() else discovery.start()
                            }) { Text(if (discoveryState.searching) "Parar busca" else "Procurar") }
                            OutlinedButton(onClick = { discovery.stop(); page = NetworkPage.MANUAL }) {
                                Text("Por IP")
                            }
                            OutlinedButton(onClick = { openScanner() }) { Text("▦ QR") }
                        }
                        if (discoveryState.servers.isEmpty()) {
                            Text(if (discoveryState.searching) "Procurando compartilhamentos…" else "Nenhum compartilhamento encontrado.")
                            Text("No outro aparelho, escolha uma pasta e ative Compartilhar. Os dois devem estar na mesma rede Wi-Fi.",
                                style = MaterialTheme.typography.bodySmall)
                            if (!discoveryState.searching) TextButton(onClick = { showHelp = true }) {
                                Text("?  Por que não encontro meu celular?")
                            }
                        } else {
                            Text("Aparelhos disponíveis", fontWeight = FontWeight.SemiBold)
                            discoveryState.servers.forEach { device ->
                                NetworkHubAction(device.serviceName, device.url) {
                                    client?.cancel(); client = null; currentPath = emptyList(); remoteItems = emptyList()
                                    host = device.url; fingerprint = ""; secret = ""
                                    discovery.stop(); failure = null; page = NetworkPage.MANUAL
                                    showAdvanced = true
                                    message = "Informe os dados de segurança ou leia o QR no outro celular."
                                }
                            }
                        }
                        discoveryState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Text("A busca encontra servidores WebDAV anunciados, não todos os aparelhos Wi-Fi.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    NetworkPage.MANUAL -> {
                        Text("Conectar por endereço", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Text("Digite o endereço do outro aparelho ou leia o QR.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = host, onValueChange = { newHost ->
                            if (newHost != host) { fingerprint = ""; secret = ""; failure = null }
                            host = newHost
                        }, singleLine = true,
                            label = { Text("https://IP:18843") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        if (secret.isNotBlank() && fingerprint.isNotBlank()) {
                            Text("✓ Dados de segurança preenchidos${if (message.startsWith("QR reconhecido")) " pelo QR" else ""}.",
                                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        } else if (host.isNotBlank()) {
                            Text("Leia o QR ou informe os dados de segurança para continuar.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        // Ordem única: QR, Segurança, Conectar. Conectar fica à direita e só ativa com dados completos.
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { openScanner() }, modifier = Modifier.weight(1f), enabled = !busy,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                                Text("▦  QR", maxLines = 1)
                            }
                            OutlinedButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.weight(1f),
                                enabled = !busy, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                                Text("Segurança", maxLines = 1)
                            }
                            Button(enabled = !busy && host.isNotBlank() && fingerprint.isNotBlank() && secret.isNotBlank(),
                                modifier = Modifier.weight(1.15f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF218348), contentColor = Color.White,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                onClick = {
                                    scope.launch {
                                        busy = true; message = ""; failure = null
                                        var attempt: NetworkDavClient? = null
                                        try {
                                            attempt = withContext(Dispatchers.IO) {
                                                NetworkDavClient(host, fingerprint, secret, username)
                                            }
                                            val root = withContext(Dispatchers.IO) { attempt.list(emptyList()) }
                                            client?.cancel(); client = attempt
                                            currentPath = emptyList(); remoteItems = root
                                            page = NetworkPage.REMOTE
                                            vibrateQrFeedback(context, connected = true)
                                        } catch (error: Exception) { attempt?.cancel(); reportFailure(error) }
                                        finally { busy = false }
                                    }
                                }) { Text(if (busy) "Abrindo" else "Conectar", maxLines = 1) }
                        }
                        if (showAdvanced) {
                            Text("Verifique o SHA-256 e as credenciais do aparelho que compartilha.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (showAdvanced) {
                            OutlinedTextField(value = fingerprint, onValueChange = { fingerprint = it }, singleLine = true,
                                label = { Text("Código SHA-256") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                            OutlinedTextField(value = username, onValueChange = { username = it }, singleLine = true,
                                label = { Text("Usuário") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                            OutlinedTextField(value = secret, onValueChange = { secret = it }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                label = { Text("Senha temporária") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
                        }
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
                failure?.let { item ->
                    NetworkFailureSummary(item) { showFailureDetails = true }
                }
                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            // A navegação principal agora está nos três atalhos; não duplicar o botão + nas subtelas.

        }
    }

    if (showScanner) {
        NetworkInlineQrScanner(onDismiss = { showScanner = false }, onDecoded = ::acceptQr)
    }
    if (showHelp) {
        AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("Como funciona a Rede?") },
            text = { Text("Para explorar arquivos, o outro aparelho precisa compartilhar uma pasta e permitir o acesso. " +
                "No outro Explorador XP, ative Compartilhar; depois use QR ou procure na mesma rede Wi-Fi. " +
                "Computadores usam SMB na opção Windows / SMB. A busca WebDAV não encontra todos os celulares.") },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendi") } })
    }
    if (showQrDialog && server.active) {
        val qrIp = server.addresses.firstOrNull { NetworkSmbAddress.privateIpv4(it) }
        AlertDialog(onDismissRequest = { showQrDialog = false },
            title = { Text("QR para conectar") },
            text = {
                if (qrIp != null && server.password.matches(Regex("[A-Za-z0-9_-]{32}")) &&
                    server.fingerprint.matches(Regex("[A-Fa-f0-9]{64}"))) {
                    NetworkQrPreview(NetworkQrConnection(qrIp, "xp", server.password, server.fingerprint))
                } else Text("QR indisponível: confira o Wi-Fi e reinicie o compartilhamento.")
            },
            confirmButton = { TextButton(onClick = { showQrDialog = false }) { Text("Fechar") } })
    }
    if (showFailureDetails) {
        failure?.let { item ->
            AlertDialog(onDismissRequest = { showFailureDetails = false },
                title = { Text("Detalhes da conexão") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(item.explanation)
                        Text(item.guidance, style = MaterialTheme.typography.bodySmall)
                        Text("Código: ${item.code}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    copyNetworkText(context, "Diagnóstico Explorador XP", item.copyable())
                    showFailureDetails = false
                    message = "Diagnóstico copiado (sem senha)."
                }) { Text("Copiar diagnóstico") } },
                dismissButton = { TextButton(onClick = { showFailureDetails = false }) { Text("Fechar") } })
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

/** Três ações compactas, sem descrições longas na página inicial. */
@Composable
private fun NetworkTile(symbol: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.defaultMinSize(minHeight = 108.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(symbol, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun NetworkFailureSummary(failure: NetworkFailure, onDetails: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("!", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                        RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp))
                Text(failure.title, color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            Text(failure.explanation, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
            HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.20f))
            Text("O que fazer", fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Text(failure.guidance, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
            OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth()) {
                Text("Detalhes e copiar diagnóstico (${failure.code})")
            }
        }
    }
}

/** Vibração curta apenas depois de validar o QR; uma segunda confirmação ocorre ao conectar. */
private fun vibrateQrFeedback(context: Context, connected: Boolean = false) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return
    runCatching {
        @Suppress("DEPRECATION")
        vibrator.vibrate(VibrationEffect.createOneShot(if (connected) 105L else 65L,
            VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

private fun copyNetworkText(context: Context, label: String, value: String, sensitive: Boolean = false) {
    val clip = ClipData.newPlainText(label, value)
    if (sensitive) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
    }
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
}

/** Mini leitor em diálogo. Câmera liberada assim que o diálogo fecha. */
@Composable
private fun NetworkInlineQrScanner(onDismiss: () -> Unit, onDecoded: (String) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val cameraAvailable = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestDecoded by rememberUpdatedState(onDecoded)
    val scanner = remember(context) {
        DecoratedBarcodeView(context).apply {
            setDecoderFactory(DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE)))
            setStatusText("Centralize o código QR")
        }
    }
    DisposableEffect(scanner, hasPermission, cameraAvailable, lifecycleOwner) {
        if (hasPermission && cameraAvailable) {
            scanner.decodeSingle(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult?) {
                    result?.text?.let { latestDecoded(it) }
                }
                override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) = Unit
            })
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (hasPermission && cameraAvailable) scanner.resume()
                Lifecycle.Event.ON_PAUSE -> scanner.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (hasPermission && cameraAvailable &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) scanner.resume()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); scanner.pause() }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ler QR de conexão") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!cameraAvailable) {
                    Text("Este aparelho não tem uma câmera disponível. Use o endereço IP para conectar.")
                } else if (hasPermission) {
                    AndroidView(factory = { scanner }, modifier = Modifier.fillMaxWidth().height(260.dp))
                    Text("Aponte para o QR do outro celular.", style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("Autorize a câmera para ler o QR. Também é possível conectar pelo endereço IP.")
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Permitir câmera")
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
