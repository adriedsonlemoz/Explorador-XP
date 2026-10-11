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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
    var host by remember { mutableStateOf("https://192.168.1.100:18843") }
    var fingerprint by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("xp") }
    var client by remember { mutableStateOf<NetworkDavClient?>(null) }
    var currentPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var remoteItems by remember { mutableStateOf<List<NetworkRemoteEntry>>(emptyList()) }
    var selectedDownload by remember { mutableStateOf<NetworkRemoteEntry?>(null) }
    var transferJob by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf("") }

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
    BackHandler {
        when {
            busy -> { client?.cancel(); transferJob?.cancel(); message = "Cancelando..." }
            client != null && currentPath.isNotEmpty() -> client?.let { readRemote(it, currentPath.dropLast(1)) }
            else -> onDismiss()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rede local", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onDismiss) { Text("Fechar") }
        }
        Text("Compartilhe apenas pastas escolhidas. Conexões HTTPS/WebDAV na mesma rede Wi-Fi; não é necessário internet.",
            style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Compartilhar este celular", style = MaterialTheme.typography.titleMedium)
        if (folders.isEmpty()) Text("Nenhuma pasta autorizada.")
        folders.forEach { folder ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
        Row(Modifier.fillMaxWidth()) {
            Checkbox(checked = allowUploads, enabled = !server.active && !server.starting,
                onCheckedChange = { allowUploads = it })
            Text("Permitir que outros aparelhos ENVIEM arquivos para as pastas selecionadas (desligado por padrão)",
                modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (server.active) {
            Text("Servidor ativo, porta ${NetworkDavServer.PORT}. ${if (server.uploadEnabled) "Envio permitido." else "Somente leitura."}")
            Text("Endereço: ${server.addresses.firstOrNull()?.let { "https://$it:${NetworkDavServer.PORT}" } ?: "Verifique o Wi-Fi"}")
            Text("Usuário: xp")
            Text("Senha desta sessão: ${server.password}")
            Text("Impressão SHA-256 do certificado: ${server.fingerprint}", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                val text = "Endereço: https://${server.addresses.firstOrNull() ?: "IP-DO-CELULAR"}:${NetworkDavServer.PORT}\n" +
                    "Usuário: xp\nSenha: ${server.password}\nSHA-256: ${server.fingerprint}"
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Explorador XP conexão", text))
                message = "Dados copiados. Compartilhe somente com quem você autorizar."
            }) { Text("Copiar dados da conexão") }
            Button(onClick = { NetworkShareService.stop(context) }) { Text("Desligar compartilhamento") }
        } else {
            Button(enabled = folders.isNotEmpty() && !server.starting, onClick = {
                message = ""
                NetworkShareService.start(context, allowUploads)
            }) { Text(if (server.starting) "Iniciando..." else "Ativar compartilhamento") }
        }
        if (server.error != null) Text("Servidor: ${server.error}", color = MaterialTheme.colorScheme.error)
        Text("O compartilhamento é encerrado ao desligar o servidor. A senha muda a cada ativação. " +
            "O Android pode encerrar serviços prolongados em segundo plano.", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Acessar outro aparelho", style = MaterialTheme.typography.titleMedium)
        Text("Localize servidores WebDAV HTTPS na rede. A descoberta não autentica o aparelho: confira a impressão SHA-256 no dispositivo remoto antes de conectar.",
            style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!discoveryState.searching) {
                OutlinedButton(onClick = { discovery.start() }) { Text("Procurar aparelhos") }
            } else {
                OutlinedButton(onClick = { discovery.stop() }) { Text("Parar busca") }
            }
        }
        if (discoveryState.searching && discoveryState.servers.isEmpty()) {
            Text("Pesquisando serviços HTTPS/WebDAV...", style = MaterialTheme.typography.bodySmall)
        }
        discoveryState.servers.forEach { device ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(device.serviceName, fontWeight = FontWeight.Medium)
                    Text(device.url, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(enabled = !busy, onClick = {
                    client?.cancel()
                    client = null
                    remoteItems = emptyList()
                    currentPath = emptyList()
                    host = device.url
                    fingerprint = ""
                    secret = ""
                    message = "Endereço preenchido. Confira a impressão SHA-256 e informe a senha no outro aparelho."
                    discovery.stop()
                }) { Text("Usar") }
            }
        }
        if (discoveryState.error != null) {
            Text(discoveryState.error ?: "", color = MaterialTheme.colorScheme.error)
        }
        Text("O outro dispositivo precisa oferecer WebDAV por HTTPS. Para o Explorador XP use https://IP:18843; para outros servidores, informe também a pasta-base na URL. Confira o SHA-256 no outro aparelho antes de conectar.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = host, onValueChange = { host = it }, singleLine = true,
            label = { Text("Endereço HTTPS e porta") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(value = fingerprint, onValueChange = { fingerprint = it }, singleLine = true,
            label = { Text("SHA-256 do certificado remoto") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(value = username, onValueChange = { username = it }, singleLine = true,
            label = { Text("Usuário WebDAV") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        OutlinedTextField(value = secret, onValueChange = { secret = it }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Senha do outro aparelho") }, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    message = ""
                    try {
                        val remote = withContext(Dispatchers.IO) { NetworkDavClient(host, fingerprint, secret, username) }
                        val root = withContext(Dispatchers.IO) { remote.list(emptyList()) }
                        client = remote
                        currentPath = emptyList()
                        remoteItems = root
                    } catch (error: Exception) { reportFailure(error) }
                    finally { busy = false }
                }
            }) { Text("Conectar") }
            OutlinedButton(enabled = !busy && client != null, onClick = { client?.let { readRemote(it, currentPath) } }) {
                Text("Atualizar")
            }
            OutlinedButton(enabled = !busy && client != null, onClick = {
                client?.cancel(); client = null; remoteItems = emptyList(); currentPath = emptyList()
            }) { Text("Sair") }
        }
        if (client != null) {
            HorizontalDivider()
            Text("Remoto: /${currentPath.joinToString("/")}", fontWeight = FontWeight.SemiBold)
            if (currentPath.isNotEmpty()) {
                OutlinedButton(enabled = !busy, onClick = { client?.let { readRemote(it, currentPath.dropLast(1)) } }) {
                    Text("↑ Pasta anterior")
                }
            }
            remoteItems.forEach { file ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text((if (file.isDirectory) "📁 " else "📄 ") + file.name)
                        if (!file.isDirectory) Text(sizeLabel(file.size), style = MaterialTheme.typography.bodySmall)
                    }
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
                Text("O aparelho remoto deve permitir escrita. Arquivos existentes não serão substituídos.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (progress.isNotEmpty()) Text(progress, style = MaterialTheme.typography.bodySmall)
            if (transferJob != null) OutlinedButton(onClick = {
                client?.cancel(); transferJob?.cancel(); message = "Cancelando transferência..."
            }) { Text("Cancelar transferência") }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
    }
}

private fun sizeLabel(bytes: Long): String = when {
    bytes < 0L -> "Tamanho desconhecido"
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> String.format(Locale.getDefault(), "%.1f KiB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MiB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.getDefault(), "%.2f GiB", bytes / (1024.0 * 1024 * 1024))
}
