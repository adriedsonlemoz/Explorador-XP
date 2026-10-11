package com.exploradorxp.app

import android.content.Context
import android.net.Uri
import java.io.IOException

/** Operações remotas comuns a WebDAV e SMB. Nenhuma concede acesso sem autorização no aparelho remoto. */
internal interface NetworkRemoteClient {
    fun list(path: List<String>): List<NetworkRemoteEntry>
    fun download(context: Context, item: NetworkRemoteEntry, destination: Uri, progress: (Long, Long) -> Unit)
    fun upload(context: Context, source: Uri, destinationDir: List<String>, progress: (Long, Long) -> Unit) {
        throw IOException("O protocolo selecionado está disponível somente para leitura.")
    }
    fun cancel()
}
