package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FileIconMapperTest {
    @Test
    fun mapsExtensionsToVectorCategories() {
        assertEquals(FileIconKind.PDF, FileIconMapper.iconFor(File("a.pdf"), false).kind)
        assertEquals(FileIconKind.IMAGE, FileIconMapper.iconFor(File("a.jpg"), false).kind)
        assertEquals(FileIconKind.ARCHIVE, FileIconMapper.iconFor(File("a.zip"), false).kind)
        assertEquals(FileIconKind.CODE, FileIconMapper.iconFor(File("a.kt"), false).kind)
        assertEquals(FileIconKind.APK, FileIconMapper.iconFor(File("a.apk"), false).kind)
    }

    @Test
    fun createsShortBadgesWithoutBitmapResources() {
        assertEquals("DOCX", FileIconMapper.iconFor(File("a.docx"), false).badge)
        assertEquals("JPG", FileIconMapper.iconFor(File("a.jpeg"), false).badge)
        assertEquals("DB", FileIconMapper.iconFor(File("a.sqlite"), false).badge)
        assertEquals("SUPRA", FileIconMapper.iconFor(File("a.supralongextension"), false).badge)
    }

    @Test
    fun foldersDoNotNeedExtensionArtwork() {
        assertEquals(FileIconKind.FOLDER, FileIconMapper.iconFor(File("Pictures"), true).kind)
        assertEquals(FileIconKind.DOWNLOADS_FOLDER, FileIconMapper.iconFor(File("Download"), true).kind)
    }
}
