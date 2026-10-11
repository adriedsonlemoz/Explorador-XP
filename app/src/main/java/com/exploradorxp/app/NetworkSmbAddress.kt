package com.exploradorxp.app

/** Regras puras: conexão SMB apenas a endereços IPv4 privados da rede local. */
internal object NetworkSmbAddress {
    fun privateIpv4(text: String): Boolean {
        val parts = text.trim().split('.')
        if (parts.size != 4 || parts.any { it.isEmpty() || it.length > 3 || (it.length > 1 && it.startsWith('0')) || !it.all(Char::isDigit) }) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        return octets[0] == 10 || (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168)
    }

    fun validShare(value: String): Boolean = value.isNotBlank() && value.length <= 80 &&
        value != "." && value != ".." && value.none { it in "\\/:*?\"<>|" || it.code < 32 } &&
        !value.endsWith(' ') && !value.endsWith('.')

    fun remotePath(parts: List<String>): String {
        require(parts.size <= 32 && parts.all(::validComponent)) { "Caminho de rede inválido" }
        return parts.joinToString("\\")
    }

    fun validComponent(value: String): Boolean = value.isNotBlank() && value.length <= 255 &&
        value != "." && value != ".." && value.none { it in "\\/:*?\"<>|" || it.code < 32 } &&
        !value.endsWith('.') && !value.endsWith(' ')
}

