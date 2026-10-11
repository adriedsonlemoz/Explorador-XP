package com.exploradorxp.app

import android.content.Context
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
)

/**
 * User-initiated and screen-scoped NSD/mDNS search. Resolves one service at a time for API 26
 * compatibility. Discovery results are untrusted: the user must separately enter the certificate
 * SHA-256 and session password before any HTTPS request can be made.
 */
internal class NetworkServiceDiscovery(context: Context) {
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

    fun start() {
        synchronized(gate) {
            if (active) return
            active = true
            epoch++
            started = true // discovery may start asynchronously; a quick Stop still cancels it
            resolving = false
            pending.clear()
            found.clear()
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
                        pending.addLast(serviceInfo)
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
            manager.discoverServices(NetworkServiceAdvertiser.TYPE, NsdManager.PROTOCOL_DNS_SD, discovery)
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
                        if (epoch == session) { resolving = false; resolveNext() }
                    }
                }
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    synchronized(gate) {
                        if (active && epoch == session) {
                            val ipv4 = serviceInfo.host as? Inet4Address
                            val url = ipv4?.hostAddress?.let { NetworkDiscoveryEndpoints.httpsAddress(it, serviceInfo.port) }
                            if (url != null) {
                                found[serviceInfo.serviceName] = NetworkDiscoveredServer(serviceInfo.serviceName, url)
                                publish()
                            }
                        }
                        if (epoch == session) {
                            resolving = false
                            resolveNext()
                        }
                    }
                }
            })
        } catch (error: Exception) {
            if (epoch == session) resolving = false
            android.util.Log.w("XP-Network", "Não foi possível resolver anúncio", error)
            if (epoch == session) resolveNext()
        }
    }

    private fun publish() {
        state.value = NetworkDiscoveryState(searching = active,
            servers = found.values.sortedBy { it.serviceName.lowercase() })
    }

    private fun fail(reason: String) {
        stop()
        state.value = NetworkDiscoveryState(error = reason)
    }

    fun stop() {
        val old: NsdManager.DiscoveryListener?
        val shouldStop: Boolean
        synchronized(gate) {
            active = false
            epoch++
            resolving = false
            old = listener
            shouldStop = started
            listener = null
            pending.clear()
            found.clear()
            state.value = NetworkDiscoveryState()
        }
        if (old != null && shouldStop) runCatching { manager.stopServiceDiscovery(old) }
        runCatching { multicast?.release() }
        multicast = null
    }
}
