package com.exploradorxp.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Base64

internal data class NetworkServerState(
    val active: Boolean = false,
    val starting: Boolean = false,
    val password: String = "",
    val fingerprint: String = "",
    val uploadEnabled: Boolean = false,
    val addresses: List<String> = emptyList(),
    val error: String? = null,
)

internal object NetworkShareStatus {
    val state = MutableStateFlow(NetworkServerState())

    fun localAddresses(): List<String> = runCatching {
        // Preferir interfaces Wi-Fi/hotspot. Em alguns aparelhos VPN/dados móveis
        // também têm IP privado e não servem para conectar outro celular via Wi-Fi.
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { network -> network.inetAddresses.toList().filterIsInstance<Inet4Address>()
                .filter { it.isSiteLocalAddress }
                .map { network.name.orEmpty() to (it.hostAddress ?: "") } }
            .filter { it.second.isNotBlank() }
            .sortedBy { (name, _) ->
                if (name.startsWith("wlan") || name.startsWith("swlan") ||
                    name.startsWith("ap") || name.startsWith("eth")) 0 else 1
            }
            .map { it.second }.distinct()
    }.getOrDefault(emptyList())
}

/** User-started foreground server. The password is ephemeral and erased whenever it stops. */
class NetworkShareService : Service() {
    companion object {
        private const val CHANNEL = "xp_network_share"
        private const val NOTIFICATION_ID = 95
        private const val START = "com.exploradorxp.app.NETWORK_START"
        private const val STOP = "com.exploradorxp.app.NETWORK_STOP"
        private const val UPLOAD = "allow_upload"

        fun start(context: Context, allowUploads: Boolean) {
            NetworkShareStatus.state.value = NetworkServerState(starting = true)
            val intent = Intent(context, NetworkShareService::class.java).setAction(START).putExtra(UPLOAD, allowUploads)
            try { androidx.core.content.ContextCompat.startForegroundService(context, intent) }
            catch (e: Exception) { NetworkShareStatus.state.value = NetworkServerState(error = e.localizedMessage ?: "Servidor não iniciou") }
        }
        fun stop(context: Context) {
            context.startService(Intent(context, NetworkShareService::class.java).setAction(STOP))
        }
    }

    private var server: NetworkDavServer? = null
    private var advertiser: NetworkServiceAdvertiser? = null

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { shutdown(); stopSelf(); return START_NOT_STICKY }
        if (intent?.action != START || server != null) return START_NOT_STICKY
        try {
            val folders = NetworkSharedFolders(this).list()
            check(folders.isNotEmpty()) { "Escolha uma pasta para compartilhar." }
            val uploads = intent.getBooleanExtra(UPLOAD, false)
            val token = ByteArray(24).also { SecureRandom().nextBytes(it) }
            val password = Base64.getUrlEncoder().withoutPadding().encodeToString(token)
            val (tls, fingerprint) = NetworkTls.serverContext()
            createNotificationChannel()
            val note = NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Explorador XP: compartilhando arquivos")
                .setContentText(if (uploads) "Rede local • leitura e envio ativados" else "Rede local • somente leitura")
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .build()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else startForeground(NOTIFICATION_ID, note)
            val newServer = NetworkDavServer(this, tls, folders, password, uploads)
            newServer.start()
            server = newServer
            // Best-effort discovery: an mDNS failure must not disable manual HTTPS access.
            advertiser = NetworkServiceAdvertiser(this).also { it.start(NetworkDavServer.PORT) }
            NetworkShareStatus.state.value = NetworkServerState(
                active = true, password = password, fingerprint = fingerprint,
                uploadEnabled = uploads, addresses = NetworkShareStatus.localAddresses()
            )
        } catch (error: Exception) {
            android.util.Log.e("XP-Network", "Não foi possível ativar compartilhamento", error)
            shutdown(error.localizedMessage ?: "Falha na inicialização")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(NotificationChannel(
            CHANNEL, "Compartilhamento de arquivos", NotificationManager.IMPORTANCE_LOW
        ))
    }

    private fun shutdown(reason: String? = null) {
        advertiser?.stop()
        advertiser = null
        server?.stop()
        server = null
        NetworkShareStatus.state.value = NetworkServerState(error = reason)
    }

    override fun onDestroy() {
        shutdown(NetworkShareStatus.state.value.error)
        super.onDestroy()
    }

    // Android 15 target 35: dataSync FGS is limited to six hours per rolling 24h.
    override fun onTimeout(startId: Int, fgsType: Int) {
        shutdown("O Android encerrou o compartilhamento por limite de tempo.")
        stopSelf(startId)
    }
}
