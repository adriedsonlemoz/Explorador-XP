package com.exploradorxp.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import okhttp3.Call
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.MediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

internal data class NetworkRemoteEntry(
    val name: String,
    val path: List<String>,
    val isDirectory: Boolean,
    val size: Long,
)

/** Explicit HTTPS URL and SHA-256 certificate pin; no HTTP or unsecured fallback. */
internal class NetworkDavClient(endpoint: String, fingerprint: String, private val password: String, private val username: String = "xp") {
    companion object { private const val MAX_TRANSFER = 4L * 1024 * 1024 * 1024 }
    private val baseUrl: HttpUrl
    private val client: OkHttpClient
    @Volatile private var activeCall: Call? = null

    init {
        val parsed = endpoint.trim().toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Informe https://IP:18843")
        require(parsed.scheme == "https" && parsed.username.isEmpty() && parsed.password.isEmpty() &&
            parsed.query == null && parsed.fragment == null) { "Somente HTTPS sem credenciais na URL." }
        // The feature is local-only. Never forward the session password to a public endpoint.
        val resolvedLocal = InetAddress.getAllByName(parsed.host).toList()
        require(resolvedLocal.isNotEmpty() && resolvedLocal.all { it.isSiteLocalAddress || it.isLinkLocalAddress }) {
            "Informe um IP da rede local (Wi-Fi/hotspot)."
        }
        require(password.isNotEmpty() && password.length <= 200 && username.isNotBlank() && username.length <= 100 &&
            username.none { it == ':' || it.code < 32 }) { "Informe usuário e senha válidos." }
        val prefix = if (parsed.encodedPath == "/") "/dav/" else parsed.encodedPath.trimEnd('/') + "/"
        baseUrl = parsed.newBuilder().encodedPath(prefix).build()
        val (ssl, trust) = NetworkTls.pinnedClientContext(fingerprint)
        client = OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, trust)
            .dns { hostname -> if (hostname == parsed.host) resolvedLocal else throw java.net.UnknownHostException("Host externo não autorizado") }
            // Safe exclusively with exact SHA-256 DER pin verified by the custom TrustManager.
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    fun cancel() { activeCall?.cancel() }

    private fun urlFor(path: List<String>, directory: Boolean = false): HttpUrl {
        require(path.size <= 23 && path.all(NetworkSharePaths::validName)) { "Caminho remoto inválido" }
        val builder = baseUrl.newBuilder()
        path.forEach { builder.addPathSegment(it) }
        if (directory && path.isNotEmpty()) builder.addPathSegment("")
        return builder.build()
    }

    private fun request(path: List<String>, directory: Boolean = false) = Request.Builder()
        .url(urlFor(path, directory))
        .header("Authorization", Credentials.basic(username, password))
        .header("Cache-Control", "no-store")

    private fun execute(request: Request): Response {
        val call = client.newCall(request)
        activeCall = call
        return try { call.execute() } catch (error: Exception) { activeCall = null; throw error }
    }

    fun list(path: List<String>): List<NetworkRemoteEntry> {
        val body = """<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:allprop/></d:propfind>"""
        val request = request(path, directory = true)
            .header("Depth", "1")
            .method("PROPFIND", body.toRequestBody("application/xml".toMediaType())).build()
        return execute(request).use { response ->
            if (response.code != 207) throw IOException("Listagem recusada (HTTP ${response.code}).")
            val stream = response.body?.byteStream() ?: throw IOException("Resposta WebDAV vazia")
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(16_384)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                if (bytes.size() + n > 4 * 1024 * 1024) throw IOException("Listagem remota muito grande")
                bytes.write(buffer, 0, n)
            }
            parseMultistatus(bytes.toByteArray(), path)
        }.also { activeCall = null }
    }

    private fun parseMultistatus(xml: ByteArray, parentPath: List<String>): List<NetworkRemoteEntry> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
            runCatching { setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "") }
            runCatching { setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "") }
        }
        val doc = factory.newDocumentBuilder().parse(xml.inputStream())
        val nodes = doc.getElementsByTagNameNS("DAV:", "response")
        val result = mutableListOf<NetworkRemoteEntry>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            val hrefElement = element.getElementsByTagNameNS("DAV:", "href").item(0) ?: continue
            val href = hrefElement.textContent ?: continue
            val parsedHref = runCatching { URI(href) }.getOrNull() ?: continue
            // Absolute URLs are accepted only if they refer to the same endpoint.
            if (parsedHref.isAbsolute && (parsedHref.host != baseUrl.host ||
                (if (parsedHref.port == -1) 443 else parsedHref.port) != baseUrl.port || parsedHref.scheme != "https")) continue
            val raw = parsedHref.rawPath ?: continue
            if (!raw.startsWith(baseUrl.encodedPath)) continue
            val parts = NetworkSharePaths.parseRequestPath("/dav/" + raw.removePrefix(baseUrl.encodedPath)) ?: continue
            if (parts.size != parentPath.size + 1 || parts.take(parentPath.size) != parentPath) continue
            val name = parts.last()
            val resourceType = element.getElementsByTagNameNS("DAV:", "resourcetype").item(0) as? Element
            val isDir = (resourceType?.getElementsByTagNameNS("DAV:", "collection")?.length ?: 0) > 0
            val lengthText = element.getElementsByTagNameNS("DAV:", "getcontentlength").item(0)?.textContent
            result += NetworkRemoteEntry(name, parts, isDir, lengthText?.toLongOrNull() ?: 0L)
        }
        return result.distinctBy { it.path }.sortedWith(compareByDescending<NetworkRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    /** Destination is a new document selected through CreateDocument; remove it if transfer fails. */
    fun download(context: Context, item: NetworkRemoteEntry, destination: Uri, progress: (Long, Long) -> Unit) {
        require(!item.isDirectory)
        var success = false
        try {
            execute(request(item.path).get().build()).use { response ->
                if (!response.isSuccessful) throw IOException("Download recusado (HTTP ${response.code}).")
                val body = response.body ?: throw IOException("Arquivo sem conteúdo")
                val total = body.contentLength()
                if (total > MAX_TRANSFER) throw IOException("Download acima do limite de 4 GiB")
                val input = body.byteStream()
                context.contentResolver.openOutputStream(destination, "wt")?.use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        done += count
                        if (done > MAX_TRANSFER) throw IOException("Download acima do limite")
                        output.write(buffer, 0, count)
                        progress(done, total)
                    }
                    output.flush()
                    if (total >= 0 && done != total) throw IOException("Download incompleto")
                } ?: throw IOException("Não foi possível gravar o arquivo selecionado")
                success = true
            }
        } finally {
            activeCall = null
            if (!success) runCatching { DocumentsContract.deleteDocument(context.contentResolver, destination) }
        }
    }

    /** Upload is only for a new file; server rejects overwriting existing user data. */
    fun upload(context: Context, source: Uri, destinationDir: List<String>, progress: (Long, Long) -> Unit) {
        val resolver = context.contentResolver
        val cursor = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?: throw IOException("Não foi possível ler informações do arquivo")
        val (name, length) = cursor.use {
            if (!it.moveToFirst()) throw IOException("Arquivo não encontrado")
            val display = it.getString(0) ?: ""
            val size = if (it.isNull(1)) -1L else it.getLong(1)
            display to size
        }
        require(NetworkSharePaths.validName(name)) { "Nome de arquivo inválido" }
        require(length in 0..MAX_TRANSFER) { "Envio exige tamanho conhecido e no máximo 4 GiB." }
        val requestBody = object : RequestBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = length
            override fun writeTo(sink: BufferedSink) {
                val input = resolver.openInputStream(source) ?: throw IOException("Falha ao abrir arquivo")
                input.use {
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (done < length) {
                        val count = it.read(buffer, 0, minOf(buffer.size.toLong(), length - done).toInt())
                        if (count < 0) throw IOException("Arquivo incompleto durante envio")
                        sink.write(buffer, 0, count)
                        done += count
                        progress(done, length)
                    }
                }
            }
        }
        val req = request(destinationDir + name).header("If-None-Match", "*").put(requestBody).build()
        try {
            execute(req).use { response ->
                if (response.code !in listOf(200, 201, 204)) throw IOException("Envio recusado (HTTP ${response.code}). O arquivo pode já existir ou a escrita está desativada.")
            }
        } finally { activeCall = null }
    }
}
