package com.exploradorxp.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

internal class ArchiveProgressNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "archive_extraction"
    private val notificationId = 4301
    private var active = false
    private var title = "Extraindo arquivo"

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Extração de arquivos",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Progresso de extração de arquivos compactados"
                    setShowBadge(false)
                }
            )
        }
    }

    fun start(fileName: String) {
        title = "Extraindo $fileName"
        active = canNotify()
        if (!active) return
        manager.notify(notificationId, buildNotification(0, "Preparando extração..."))
    }

    fun update(progress: ArchiveProgress) {
        if (!active) return
        val text = "${progress.percent}% • ${progress.completedEntries}/${progress.totalEntries} itens"
        manager.notify(notificationId, buildNotification(progress.percent, text))
    }

    fun finish() {
        if (active) manager.cancel(notificationId)
        active = false
    }

    private fun buildNotification(percent: Int, text: String) = NotificationCompat.Builder(appContext, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(text)
        .setOnlyAlertOnce(true)
        .setOngoing(percent in 0..99)
        .setProgress(100, percent.coerceIn(0, 100), false)
        .build()

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
