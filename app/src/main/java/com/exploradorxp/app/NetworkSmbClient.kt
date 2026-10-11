package com.exploradorxp.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File as SmbFile
import java.io.IOException
import java.util.EnumSet

/** Cliente SMB2/3 somente leitura. Senha permanece em memória até desconectar; nunca é persistida. */
internal class NetworkSmbClient(
    host: String,
    shareName: String,
    username: String,
    password: String,
    domain: String,
) : NetworkRemoteClient {
    companion object { private const val MAX_TRANSFER = 4L * 1024 * 1024 * 1024 }
    private val smb: SMBClient
    private val connection: Connection
    private val session: Session
    private val share: DiskShare
    @Volatile private var stopped = false
    @Volatile private var currentFile: SmbFile? = null

    init {
        require(NetworkSmbAddress.privateIpv4(host)) { "Informe um IP privado do Wi-Fi (ex.: 192.168.1.10)." }
        require(NetworkSmbAddress.validShare(shareName)) { "Nome do compartilhamento SMB inválido." }
        require(username.isNotBlank() && username.length <= 128 && password.length <= 256 && domain.length <= 128) {
            "Usuário, senha ou domínio inválido."
        }
        // Não permite desativar assinatura SMB; protege contra alteração de dados em trânsito.
        smb = SMBClient(SmbConfig.builder().withSigningRequired(true).build())
        try {
            connection = smb.connect(host)
            try {
                session = connection.authenticate(AuthenticationContext(username, password.toCharArray(), domain.ifBlank { null }))
                try {
                    val connected = session.connectShare(shareName)
                    if (connected !is DiskShare) {
                        connected.close()
                        throw IOException("Este compartilhamento não oferece acesso a arquivos.")
                    }
                    share = connected
                } catch (e: Exception) { session.close(); throw e }
            } catch (e: Exception) { connection.close(); throw e }
        } catch (e: Exception) { smb.close(); throw e }
    }

    override fun list(path: List<String>): List<NetworkRemoteEntry> {
        check(!stopped) { "Conexão encerrada" }
        return share.list(NetworkSmbAddress.remotePath(path))
            .asSequence()
            .filter { it.fileName != "." && it.fileName != ".." && NetworkSmbAddress.validComponent(it.fileName) }
            .take(2001)
            .map { file ->
                val dir = (file.fileAttributes and 0x10L) != 0L // FILE_ATTRIBUTE_DIRECTORY
                NetworkRemoteEntry(file.fileName, path + file.fileName, dir, file.endOfFile.coerceAtLeast(0L))
            }.toList()
            .also { if (it.size > 2000) throw IOException("Pasta contém mais de 2.000 itens; limite de segurança.") }
            .sortedWith(compareByDescending<NetworkRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    override fun download(context: Context, item: NetworkRemoteEntry, destination: Uri, progress: (Long, Long) -> Unit) {
        require(!item.isDirectory && NetworkSmbAddress.validComponent(item.name)) { "Selecione um arquivo." }
        require(item.size <= MAX_TRANSFER) { "Arquivo excede 4 GiB." }
        check(!stopped) { "Conexão encerrada" }
        var completed = false
        try {
            val file = share.openFile(NetworkSmbAddress.remotePath(item.path),
                EnumSet.of(AccessMask.GENERIC_READ),
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null)
            currentFile = file
            file.use { remote ->
                remote.inputStream.use { input ->
                    val output = context.contentResolver.openOutputStream(destination, "wt")
                        ?: throw IOException("Não foi possível gravar o arquivo escolhido.")
                    output.use { target ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            if (stopped || Thread.currentThread().isInterrupted) throw IOException("Transferência interrompida")
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count == 0) continue
                            done += count
                            if (done > MAX_TRANSFER || (item.size > 0L && done > item.size))
                                throw IOException("Arquivo remoto excedeu o tamanho informado.")
                            target.write(buffer, 0, count)
                            progress(done, item.size)
                        }
                        target.flush()
                        if (item.size > 0L && done != item.size) throw IOException("Download incompleto")
                    }
                }
            }
            completed = true
        } finally {
            currentFile = null
            if (!completed) runCatching { DocumentsContract.deleteDocument(context.contentResolver, destination) }
        }
    }

    /** Fechar a conexão interrompe cópias em andamento e impede reuso de sessões canceladas. */
    override fun cancel() {
        stopped = true
        runCatching { currentFile?.close() }
        runCatching { share.close() }
        runCatching { session.close() }
        runCatching { connection.close() }
        runCatching { smb.close() }
    }
}
