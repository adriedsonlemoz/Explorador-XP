package com.exploradorxp.app

import android.os.FileObserver
import java.io.File

/**
 * Observa somente a pasta atualmente aberta no Explorer e sinaliza alterações
 * que podem mudar a listagem. O debounce e a releitura ficam no ViewModel.
 */
@Suppress("DEPRECATION")
internal class CurrentDirectoryObserver(
    private val onDirectoryChanged: () -> Unit,
) {
    private var observer: FileObserver? = null
    private var observedPath: String? = null

    fun watch(directory: File?) {
        val target = directory?.absoluteFile
        val targetPath = target?.absolutePath

        if (targetPath != null && targetPath == observedPath && observer != null) return

        stop()
        if (target == null || !target.isDirectory) return
        val pathToWatch = target.absolutePath

        val nextObserver = object : FileObserver(pathToWatch, WATCH_MASK) {
            override fun onEvent(event: Int, path: String?) {
                if ((event and WATCH_MASK) != 0) onDirectoryChanged()
            }
        }

        runCatching {
            nextObserver.startWatching()
            observer = nextObserver
            observedPath = pathToWatch
        }.onFailure {
            runCatching { nextObserver.stopWatching() }
            observer = null
            observedPath = null
        }
    }

    fun stop() {
        observer?.let { current -> runCatching { current.stopWatching() } }
        observer = null
        observedPath = null
    }

    private companion object {
        const val WATCH_MASK: Int =
            FileObserver.CREATE or
                FileObserver.CLOSE_WRITE or
                FileObserver.DELETE or
                FileObserver.MOVED_FROM or
                FileObserver.MOVED_TO or
                FileObserver.DELETE_SELF or
                FileObserver.MOVE_SELF
    }
}
