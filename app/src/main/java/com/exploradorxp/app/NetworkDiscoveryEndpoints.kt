package com.exploradorxp.app

/** Pure IPv4 validation for untrusted mDNS discoveries. No DNS lookups or implicit trust. */
internal object NetworkDiscoveryEndpoints {
    /** Prefere qualquer endereço IPv4 privado utilizável no anúncio NSD, não só o primeiro. */
    fun firstHttpsAddress(addresses: List<String>, port: Int): String? =
        addresses.asSequence().mapNotNull { httpsAddress(it, port) }.firstOrNull()

    fun httpsAddress(ip: String, port: Int): String? {
        if (port !in 1..65535 || ip.length > 15) return null
        val parts = ip.split('.')
        if (parts.size != 4 || parts.any { it.isEmpty() || it.length > 3 || (it.length > 1 && it.startsWith("0")) || it.any { c -> c !in '0'..'9' } }) return null
        val octets = parts.map { it.toIntOrNull() ?: return null }
        if (octets.any { it !in 0..255 }) return null
        val local = octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 169 && octets[1] == 254)
        if (!local) return null
        return "https://${octets.joinToString(".")}:$port"
    }
}
