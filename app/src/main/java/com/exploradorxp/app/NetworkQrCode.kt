package com.exploradorxp.app

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/** Dados efêmeros, jamais persistidos em arquivos, preferências ou logs. */
internal data class NetworkQrConnection(val host: String, val username: String, val password: String, val fingerprint: String) {
    val endpoint: String get() = "https://$host:${NetworkDavServer.PORT}"
}

internal object NetworkQrCode {
    private const val SCHEME = "exploradorxp"
    private const val AUTHORITY = "webdav"
    private const val MAX_LENGTH = 512
    private val HEX = Regex("[A-Fa-f0-9]{64}")
    private val PASSWORD = Regex("[A-Za-z0-9_-]{32}")
    private fun encodePart(value: String) = URLEncoder.encode(value, "UTF-8")

    fun create(connection: NetworkQrConnection): String {
        require(NetworkSmbAddress.privateIpv4(connection.host) && connection.username == "xp" &&
            PASSWORD.matches(connection.password) && HEX.matches(connection.fingerprint)) { "Dados inválidos para o QR local" }
        return "$SCHEME://$AUTHORITY?ip=${connection.host}&port=${NetworkDavServer.PORT}&user=xp" +
            "&secret=${encodePart(connection.password)}&pin=${connection.fingerprint.uppercase()}"
    }

    /** Não executa uma conexão: apenas valida o QR lido antes de pedir a confirmação do usuário. */
    fun parse(raw: String): NetworkQrConnection {
        require(raw.length in 40..MAX_LENGTH) { "QR inválido ou excessivamente longo." }
        val uri = URI(raw)
        require(uri.scheme == SCHEME && uri.rawAuthority == AUTHORITY && uri.rawFragment == null &&
            (uri.rawPath.isNullOrEmpty()) && !uri.rawQuery.isNullOrEmpty()) { "QR não pertence ao Explorador XP." }
        val entries = uri.rawQuery!!.split('&').map { entry ->
            val pieces = entry.split('=', limit = 2)
            require(pieces.size == 2) { "Campo de QR inválido." }
            URLDecoder.decode(pieces[0], "UTF-8") to URLDecoder.decode(pieces[1], "UTF-8")
        }
        require(entries.size == 5 && entries.map { it.first }.toSet() == setOf("ip", "port", "user", "secret", "pin")) {
            "Campos QR inesperados ou repetidos."
        }
        val data = entries.toMap()
        val host = data.getValue("ip")
        val port = data.getValue("port")
        val user = data.getValue("user")
        val password = data.getValue("secret")
        val fingerprint = data.getValue("pin")
        require(NetworkSmbAddress.privateIpv4(host) && port == NetworkDavServer.PORT.toString() &&
            user == "xp" && PASSWORD.matches(password) && HEX.matches(fingerprint)) {
            "QR contém endereço, senha ou certificado inválido."
        }
        return NetworkQrConnection(host, user, password, fingerprint.uppercase())
    }

}
