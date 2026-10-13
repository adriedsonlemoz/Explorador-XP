package com.exploradorxp.app

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * A única instância do player de música pertence ao Service e não à janela do arquivo.
 * O próprio Media3 publica/atualiza a notificação de mídia e seus controles do sistema.
 */
internal const val ACTION_OPEN_MUSIC_PLAYER = "com.exploradorxp.app.OPEN_MUSIC_PLAYER"

/** Indicador apenas em memória: reconectar à sessão se a Activity for recriada pelo launcher. */
internal object MusicPlaybackStatus {
    @Volatile var hasSession: Boolean = false
}

class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var audioPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        MusicPlaybackStatus.hasSession = true
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        audioPlayer = player
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_MUSIC_PLAYER
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        audioPlayer?.release()
        audioPlayer = null
        MusicPlaybackStatus.hasSession = false
        super.onDestroy()
    }
}
