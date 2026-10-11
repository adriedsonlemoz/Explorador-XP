package com.exploradorxp.app

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.util.Base64
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

/** Restricted WebDAV level-1 server; TLS + per-session credentials, no delete, no recursion. */
internal class NetworkDavServer(
    private val context: Context,
    ssl: SSLContext,
    private val folders: List<NetworkSharedFolder>,
    private val password: String,
    private val allowUploads: Boolean,
) {
    companion object {
        const val PORT = 18843
        private const val MAX_UPLOAD = 4L * 1024 * 1024 * 1024
        private const val MAX_ITEMS = 1500
    }
    private val running = AtomicBoolean(false)
    private val activeSockets = java.util.concurrent.ConcurrentHashMap.newKeySet<SSLSocket>()
    private val workers = ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS, ArrayBlockingQueue(8)) { job ->
        Thread(job, "xp-dav-worker").apply { isDaemon = true }
    }
    private val server = ssl.serverSocketFactory.createServerSocket() as SSLServerSocket
    private var acceptThread: Thread? = null

    fun start() {
        server.reuseAddress = true
        server.enabledProtocols = server.supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
        server.bind(InetSocketAddress(PORT), 4)
        running.set(true)
        acceptThread = Thread({
            while (running.get()) {
                try {
                    val socket = server.accept() as SSLSocket
                    if (workers.isShutdown) { socket.close(); continue }
                    activeSockets.add(socket)
                    try { workers.execute { socket.use { try { serve(it) } finally { activeSockets.remove(it) } } } }
                    catch (_: java.util.concurrent.RejectedExecutionException) { activeSockets.remove(socket); socket.close() }
                } catch (e: SocketException) {
                    if (running.get()) android.util.Log.w("XP-Network", "Servidor interrompido", e)
                    break
                } catch (e: IOException) {
                    if (running.get()) android.util.Log.w("XP-Network", "Falha em conexão de rede", e)
                }
            }
        }, "xp-dav-accept").apply { isDaemon = true; start() }
    }

    fun stop() {
        running.set(false)
        runCatching { server.close() }
        activeSockets.forEach { runCatching { it.close() } }
        activeSockets.clear()
        workers.shutdownNow()
        acceptThread?.interrupt()
    }

    private fun serve(socket: SSLSocket) {
        // No interface is reachable from an internet address, even if tethering routing changes.
        if (!socket.inetAddress.isSiteLocalAddress && !socket.inetAddress.isLinkLocalAddress) return
        socket.soTimeout = 60_000
        socket.enabledProtocols = socket.supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
        try {
            socket.startHandshake()
            val input = BufferedInputStream(socket.inputStream)
            val output = BufferedOutputStream(socket.outputStream)
            val requestLine = readLine(input, 4096) ?: return
            val parts = requestLine.split(' ')
            if (parts.size != 3 || !parts[2].startsWith("HTTP/1.")) return
            val method = parts[0]
            val target = parts[1]
            val headers = mutableMapOf<String, String>()
            var remaining = 16_384
            while (remaining > 0) {
                val line = readLine(input, minOf(remaining, 4096)) ?: return
                remaining -= line.length + 2
                if (line.isEmpty()) break
                val at = line.indexOf(':')
                if (at <= 0) { error(output, 400, "Cabeçalho inválido"); return }
                headers[line.substring(0, at).trim().lowercase()] = line.substring(at + 1).trim()
            }
            if (remaining <= 0 || headers.size > 64) { error(output, 431, "Cabeçalhos grandes"); return }
            val expected = "Basic " + Base64.getEncoder().encodeToString("xp:$password".toByteArray(StandardCharsets.UTF_8))
            val supplied = headers["authorization"] ?: ""
            if (!MessageDigest.isEqual(supplied.toByteArray(), expected.toByteArray())) {
                writeHeaders(output, 401, "Unauthorized", 0, mapOf("WWW-Authenticate" to "Basic realm=\"Explorador XP\""))
                return
            }
            val path = NetworkSharePaths.parseRequestPath(target)
            if (path == null) { error(output, 400, "Caminho inválido"); return }
            if (method == "OPTIONS") {
                writeHeaders(output, 200, "OK", 0, mapOf("DAV" to "1", "Allow" to "OPTIONS, PROPFIND, HEAD, GET, PUT"))
                return
            }
            val selected = locate(path)
            when (method) {
                "PROPFIND" -> {
                    if (headers["depth"] !in listOf(null, "0", "1")) { error(output, 403, "Profundidade indisponível"); return }
                    if (selected == null && path.isNotEmpty()) { error(output, 404, "Pasta não encontrada"); return }
                    val nodes = mutableListOf<Pair<List<String>, DocumentFile?>>()
                    nodes += path to selected
                    if (headers["depth"] != "0" && (path.isEmpty() || selected?.isDirectory == true)) {
                        val children = if (path.isEmpty()) folders.map { listOf(it.alias) to rootDocument(it) }
                            else selected?.listFiles()?.take(MAX_ITEMS + 1)?.map { (path + (it.name ?: "")) to it } ?: emptyList()
                        if (children.size > MAX_ITEMS) { error(output, 507, "Pasta tem muitos itens"); return }
                        nodes += children.filter { (segments, doc) -> doc != null && segments.all(NetworkSharePaths::validName) }
                    }
                    val xml = buildString {
                        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                        append("<d:multistatus xmlns:d=\"DAV:\">")
                        nodes.forEach { (segments, doc) ->
                            val directory = doc?.isDirectory != false
                            val displayName = if (segments.isEmpty()) "Compartilhamentos" else if (segments.size == 1) {
                                folders.firstOrNull { it.alias == segments[0] }?.name ?: segments.last()
                            } else segments.last()
                            append("<d:response><d:href>").append(xmlEscape(NetworkSharePaths.urlPath(segments, directory)))
                            append("</d:href><d:propstat><d:prop><d:displayname>").append(xmlEscape(displayName))
                            append("</d:displayname><d:resourcetype>")
                            if (directory) append("<d:collection/>")
                            append("</d:resourcetype><d:getcontentlength>")
                            append(if (directory) 0 else doc?.length()?.coerceAtLeast(0) ?: 0)
                            append("</d:getcontentlength></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>")
                        }
                        append("</d:multistatus>")
                    }.toByteArray(StandardCharsets.UTF_8)
                    writeHeaders(output, 207, "Multi-Status", xml.size.toLong(), mapOf("Content-Type" to "application/xml; charset=utf-8"))
                    output.write(xml)
                    output.flush()
                }
                "GET", "HEAD" -> {
                    if (path.isEmpty() || selected == null || !selected.isFile) {
                        error(output, 404, "Arquivo não encontrado"); return
                    }
                    val extras = mapOf("Content-Type" to "application/octet-stream", "Content-Disposition" to "attachment")
                    if (method == "HEAD") {
                        writeHeaders(output, 200, "OK", selected.length().coerceAtLeast(0), extras)
                    } else {
                        // Use chunked responses: SAF providers sometimes report unreliable lengths.
                        val source = context.contentResolver.openInputStream(selected.uri)
                        if (source == null) { error(output, 404, "Arquivo não disponível"); return }
                        source.use {
                            writeHeaders(output, 200, "OK", -1L, extras)
                            writeChunked(it, output)
                            output.flush()
                        }
                    }
                }
                "PUT" -> {
                    if (!allowUploads) { error(output, 403, "Envio desativado"); return }
                    if (path.size < 2 || locate(path) != null) { error(output, 409, "Arquivo existente ou pasta inválida"); return }
                    val parent = locate(path.dropLast(1))
                    if (parent == null || !parent.isDirectory || !parent.canWrite()) { error(output, 403, "Pasta sem escrita"); return }
                    if (headers["transfer-encoding"] != null) { error(output, 411, "Envie Content-Length"); return }
                    val count = headers["content-length"]?.toLongOrNull()
                    if (count == null || count < 0 || count > MAX_UPLOAD) { error(output, 413, "Arquivo acima do limite ou tamanho desconhecido"); return }
                    val name = path.last()
                    val tempName = ".xp-envio-" + java.util.UUID.randomUUID().toString()
                    val temp = parent.createFile("application/octet-stream", tempName)
                    if (temp == null) { error(output, 507, "Impossível criar arquivo temporário"); return }
                    var completed = false
                    try {
                        val stream = context.contentResolver.openOutputStream(temp.uri, "wt") ?: throw IOException("Sem saída")
                        stream.use { destination ->
                            val buffer = ByteArray(64 * 1024)
                            var left = count
                            while (left > 0) {
                                val read = input.read(buffer, 0, minOf(left, buffer.size.toLong()).toInt())
                                if (read < 0) throw IOException("Upload incompleto")
                                destination.write(buffer, 0, read)
                                left -= read
                            }
                            destination.flush()
                        }
                        synchronized(this@NetworkDavServer) {
                            if (parent.findFile(name) != null || !temp.renameTo(name)) throw IOException("Não foi possível concluir o envio")
                            if (parent.findFile(name) == null) throw IOException("O provedor não confirmou o nome do arquivo")
                            completed = true
                        }
                        writeHeaders(output, 201, "Created", 0)
                    } finally {
                        if (!completed) runCatching { temp.delete() }
                    }
                }
                else -> error(output, 405, "Operação desativada")
            }
        } catch (e: Exception) {
            android.util.Log.w("XP-Network", "Falha em requisição WebDAV", e)
            // Never include exceptions, tokens or local paths in network responses.
        }
    }

    /** /dav/ is a virtual root. Each following segment is resolved only through DocumentFile children. */
    private fun locate(path: List<String>): DocumentFile? {
        if (path.isEmpty()) return null // virtual root handled explicitly below
        val shared = folders.firstOrNull { it.alias == path.first() } ?: return null
        var doc = rootDocument(shared) ?: return null
        for (name in path.drop(1)) {
            if (!doc.isDirectory) return null
            doc = doc.findFile(name) ?: return null
        }
        return doc
    }

    private fun rootDocument(folder: NetworkSharedFolder): DocumentFile? =
        DocumentFile.fromTreeUri(context, folder.uri)?.takeIf { it.isDirectory }

    private fun xmlEscape(str: String): String = str.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun writeChunked(source: InputStream, output: OutputStream) {
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = source.read(buf)
            if (n < 0) break
            output.write("${n.toString(16)}\r\n".toByteArray(StandardCharsets.US_ASCII))
            output.write(buf, 0, n)
            output.write("\r\n".toByteArray(StandardCharsets.US_ASCII))
        }
        output.write("0\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
    }

    private fun readLine(input: InputStream, limit: Int): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (bytes.size() < limit) {
            val b = input.read()
            if (b < 0) return null
            if (b == 10) {
                val data = bytes.toByteArray()
                val end = if (data.isNotEmpty() && data.last() == 13.toByte()) data.size - 1 else data.size
                return String(data, 0, end, StandardCharsets.ISO_8859_1)
            }
            bytes.write(b)
        }
        return null
    }

    private fun error(out: OutputStream, status: Int, msg: String) {
        val body = msg.toByteArray(StandardCharsets.UTF_8)
        val statusText = when (status) {
            400 -> "Bad Request"; 403 -> "Forbidden"; 404 -> "Not Found"; 405 -> "Method Not Allowed"
            409 -> "Conflict"; 411 -> "Length Required"; 413 -> "Payload Too Large"; 431 -> "Request Header Fields Too Large"
            507 -> "Insufficient Storage"; else -> "Error"
        }
        writeHeaders(out, status, statusText, body.size.toLong(), mapOf("Content-Type" to "text/plain; charset=utf-8"))
        out.write(body)
        out.flush()
    }

    private fun writeHeaders(out: OutputStream, status: Int, label: String, size: Long, extra: Map<String, String> = emptyMap()) {
        val headers = buildString {
            append("HTTP/1.1 $status $label\r\n")
            append("Connection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n")
            if (size >= 0) append("Content-Length: $size\r\n") else append("Transfer-Encoding: chunked\r\n")
            extra.forEach { (key, value) -> append("$key: $value\r\n") }
            append("\r\n")
        }
        out.write(headers.toByteArray(StandardCharsets.US_ASCII))
        out.flush()
    }
}
