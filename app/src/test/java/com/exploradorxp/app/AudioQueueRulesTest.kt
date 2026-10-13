package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AudioQueueRulesTest {
    @Test fun recognizesSupportedAudioExtensionsIgnoringCase() {
        for (ext in AudioQueueRules.extensions) assertTrue(AudioQueueRules.isAudio("music.$ext"))
        assertTrue(AudioQueueRules.isAudio("MUSIC.OPUS"))
        assertFalse(AudioQueueRules.isAudio("music.mp4"))
        assertFalse(AudioQueueRules.isAudio("music.mp3.exe"))
        assertFalse(AudioQueueRules.isAudio("README"))
    }

    @Test fun onlyIncludesReadableAudioFromSameFolder() {
        val dir = Files.createTempDirectory("xp-audio-").toFile()
        try {
            val first = File(dir, "02 track.MP3").apply { writeText("fake") }
            val second = File(dir, "01 track.flac").apply { writeText("fake") }
            File(dir, "movie.mp4").writeText("fake")
            File(dir, "empty.wav").createNewFile()
            val child = File(dir, "sub").apply { mkdir() }
            File(child, "03 track.mp3").writeText("fake")
            assertEquals(listOf(second.name, first.name), AudioQueueRules.buildQueue(first).map { it.name })
        } finally { dir.deleteRecursively() }
    }

    @Test fun keepsSelectedWhenLimitIsReached() {
        val dir = Files.createTempDirectory("xp-audio-limit-").toFile()
        try {
            val files = (1..8).map { i -> File(dir, "%02d.mp3".format(i)).apply { writeText("fake") } }
            val queue = AudioQueueRules.buildQueue(files.last(), 3)
            assertEquals(3, queue.size)
            assertTrue(queue.any { it.absolutePath == files.last().absolutePath })
            assertEquals(queue.map { it.name }.sorted(), queue.map { it.name })
        } finally { dir.deleteRecursively() }
    }

    @Test fun missingDirectoryStillPreservesSelectedTrack() {
        val input = File("/this/path/does/not/exist/song.opus")
        assertEquals(listOf(input), AudioQueueRules.buildQueue(input))
    }

    @Test fun trimsExtensionButPreservesMultipleDots() {
        assertEquals("Band - Track.live", AudioQueueRules.readableTitle("Band - Track.live.flac"))
        assertEquals("Track", AudioQueueRules.readableTitle("Track"))
    }
}
