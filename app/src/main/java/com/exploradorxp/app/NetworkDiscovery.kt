package com.exploradorxp.app

import android.content.Context
import android.os.Build
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.Inet4Address
import java.util.ArrayDeque

/** Only advertises the HTTPS port. Never broadcast credentials, certificate pins or shared folder names. */
internal class NetworkServiceAdvertiser(context: Context) {
    companion object { const val TYPE = "_webdavs._tcp." }
    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.RegistrationListener? = null
    private var registered = false

    fun start(port: Int) {
        if (listener != null) return
        val registration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) { registered = true }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                registered = false
                android.util.Log.w("XP-Network", "Anúncio mDNS indisponível: $errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) { registered = false }
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                android.util.Log.w("XP-Network", "Falha ao remover anúncio: $errorCode")
            }
        }
        listener = registration
        try {
            val info = NsdServiceInfo().apply {
                serviceName = "Explorador XP"
                serviceType = TYPE
                this.port = port
            }
            manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration)
        } catch (error: Exception) {
            listener = null
            android.util.Log.w("XP-Network", "Não foi possível anunciar servidor", error)
        }
    }

    fun stop() {
        val registration = listener ?: return
        listener = null
        // On some Android versions unregister may throw if registration never completed.
        runCatching { manager.unregisterService(registration) }
        registered = false
    }
}

internal data class NetworkDiscoveredServer(val serviceName: String, val url: String)
internal data class NetworkDiscoveryState(
    val searching: Boolean = false,
    val servers: List<NetworkDiscoveredServer> = emptyList(),
    val error: String? = null,
    val announcements: Int = 0,
    val addressFailures: Int = 0,
    val resolveFailures: Int = 0,
)

/**
 * User-initiated and screen-scoped NSD/mDNS search. Resolves one service at a time for API 26
 * compatibility. Discovery results are untrusted: the user must separately enter the certificate
 * SHA-256 and session password before any HTTPS request can be made.
 */
internal class NetworkServiceDiscovery(context: Context, private val serviceType: String = NetworkServiceAdvertiser.TYPE) {
    private val manager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val gate = Any()
    val state = MutableStateFlow(NetworkDiscoveryState())
    private var listener: NsdManager.DiscoveryListener? = null
    private var started = false
    private var epoch = 0L
    private var active = false
    private var resolving = false
    private val pending = ArrayDeque<NsdServiceInfo>()
    private val found = linkedMapOf<String, NetworkDiscoveredServer>()
    private var multicast: WifiManager.MulticastLock? = null
    private var announcements = 0
    private var addressFailures = 0
    private var resolveFailures = 0

    fun start() {
        synchronized(gate) {
            if (active) return
            active = true
            epoch++
            started = true // discovery may start asynchronously; a quick Stop still cancels it
            resolving = false
            pending.clear()
            found.clear()
            announcements = 0
            addressFailures = 0
            resolveFailures = 0
            state.value = NetworkDiscoveryState(searching = true)
        }
        val session = synchronized(gate) { epoch }
        try {
            // On legacy Android devices mDNS responses may be suppressed without this lock.
            multicast = wifi?.createMulticastLock("xp-nsd-discovery")?.apply {
                setReferenceCounted(false)
                acquire()
            }
            val discovery = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {
                    synchronized(gate) { if (active && epoch == session) started = true }
                }
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    synchronized(gate) {
                        if (!active || epoch != session || pending.size >= 64) return
                        // Um anúncio apenas indica que há um serviço, não que o endereço seja válido.
                        announcements++
                        pending.addLast(serviceInfo)
                        publish()
                        resolveNext()
                    }
                }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    synchronized(gate) {
                        if (active && epoch == session) {
                            found.remove(serviceInfo.serviceName)
                            publish()
                        }
                    }
                }
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    if (synchronized(gate) { epoch == session }) {
                        fail("Não foi possível pesquisar dispositivos (código $errorCode).")
                    }
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    android.util.Log.w("XP-Network", "Falha ao parar busca mDNS: $errorCode")
                }
            }
            synchronized(gate) { listener = discovery }
            manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discovery)
        } catch (error: Exception) { fail(error.localizedMessage ?: "Descoberta indisponível") }
    }

    /** Called within gate; never resolves two services concurrently on older Android devices. */
    private fun resolveNext() {
        if (!active || resolving || pending.isEmpty()) return
        resolving = true
        val session = epoch
        val info = pending.removeFirst()
        try {
            manager.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    synchronized(gate) {
                        if (epoch == session) {
                            resolveFailures++
                            publish()
                            resolving = false
                            resolveNext()
                        }
                    }
                }
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    synchronized(gate) {
                        if (active && epoch == session) {
                            // Em Android 14+, o DNS-SD pode devolver IPv6 como host principal
                            // e IPv4 na lista de endereços. Antes descartávamos o anúncio todo.
                            val addresses = if (Build.VERSION.SDK_INT >= 34) {
                                serviceInfo.hostAddresses.orEmpty() + listOfNotNull(serviceInfo.host)
                            } else listOfNotNull(serviceInfo.host)
                            val ips = addresses.mapNotNull { (it as? Inet4Address)?.hostAddress }
                            val url = if (serviceType == "_smb._tcp.") {
                                // Apenas anúncios SMB na porta padrão, com IPv4 privado.
                                if (serviceInfo.port == 445) ips.firstOrNull(NetworkSmbAddress::privateIpv4) else null
                            } else NetworkDiscoveryEndpoints.firstHttpsAddress(ips, serviceInfo.port)
                            if (url != null) {
                                found[serviceInfo.serviceName] = NetworkDiscoveredServer(serviceInfo.serviceName, url)
                            } else addressFailures++
                            publish()
                        }
                        if (epoch == session) {
                            resolving = false
                            resolveNext()
                        }
                    }
                }
            })
        } catch (error: Exception) {
            if (epoch == session) {
                resolveFailures++
                publish()
                resolving = false
            }
            android.util.Log.w("XP-Network", "Não foi possível resolver anúncio", error)
            if (epoch == session) resolveNext()
        }
    }

    private fun publish() {
        state.value = NetworkDiscoveryState(
            searching = active, servers = found.values.sortedBy { it.serviceName.lowercase() },
            announcements = announcements, addressFailures = addressFailures,
            resolveFailures = resolveFailures
        )
    }

    private fun fail(reason: String) {
        stop()
        state.value = NetworkDiscoveryState(error = reason)
    }

    fun stop() {
        val old: NsdManager.DiscoveryListener?
        val shouldStop: Boolean
        val snapshot: NetworkDiscoveryState
        synchronized(gate) {
            active = false
            epoch++
            resolving = false
            old = listener
            shouldStop = started
            listener = null
            pending.clear()
            // Preservar resultados visíveis depois de tocar Parar busca.
            snapshot = NetworkDiscoveryState(
                searching = false, servers = found.values.sortedBy { it.serviceName.lowercase() },
                announcements = announcements, addressFailures = addressFailures,
                resolveFailures = resolveFailures
            )
            state.value = snapshot
        }
        if (old != null && shouldStop) runCatching { manager.stopServiceDiscovery(old) }
        runCatching { multicast?.release() }
        multicast = null
    }
}
