package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class StorageDisplayRulesTest {
    private val files = listOf(
        StorageFileSummary(File("game/main.100.com.example.obb"), 1000, "Dados de jogo"),
        StorageFileSummary(File("Videos/video.mp4"), 500, "Vídeo"),
        StorageFileSummary(File("Pictures/foto.jpg"), 250, "Imagem"),
        StorageFileSummary(File("Downloads/app.apk"), 80, "APK"),
        StorageFileSummary(File("Documentos/outro.txt"), 10, "Outro"),
    )
    @Test fun recognizesObbAsSeparateCategory() {
        assertEquals("game_data" to "Dados de jogos (OBB)", FileTypeClassifier.storageCategory("ObB"))
    }
    @Test fun fileFiltersDoNotInventOrRescanEntries() {
        assertEquals(listOf("main.100.com.example.obb"),
            StorageDisplayRules.filterLargeFiles(files, LargeFileFilter.GAMES).map { it.file.name })
        assertEquals(listOf("video.mp4"),
            StorageDisplayRules.filterLargeFiles(files, LargeFileFilter.VIDEOS).map { it.file.name })
        assertEquals(listOf("outro.txt"),
            StorageDisplayRules.filterLargeFiles(files, LargeFileFilter.OTHER).map { it.file.name })
        assertEquals(files.map { it.file.name },
            StorageDisplayRules.filterLargeFiles(files, LargeFileFilter.ALL).map { it.file.name })
    }
}
