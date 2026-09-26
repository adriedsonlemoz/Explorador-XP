package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FileIconMapperTest {
    @Test fun `common extensions use coherent category icons`() {
        assertEquals(R.drawable.xp_file_document, FileIconMapper.iconForExtension("docx"))
        assertEquals(R.drawable.xp_file_image, FileIconMapper.iconForExtension("jpg"))
        assertEquals(R.drawable.xp_file_audio, FileIconMapper.iconForExtension("mp3"))
        assertEquals(R.drawable.xp_file_video, FileIconMapper.iconForExtension("mp4"))
        assertEquals(R.drawable.xp_file_archive, FileIconMapper.iconForExtension("zip"))
        assertEquals(R.drawable.xp_file_code, FileIconMapper.iconForExtension("kt"))
        assertEquals(R.drawable.xp_file_app, FileIconMapper.iconForExtension("apk"))
        assertEquals(R.drawable.xp_file_unknown, FileIconMapper.iconForExtension("semformato"))
    }

    @Test fun `well known folders keep semantic vector categories`() {
        assertEquals(R.drawable.xp_folder_download, FileIconMapper.folderIconFor("Download"))
        assertEquals(R.drawable.xp_folder_image, FileIconMapper.folderIconFor("DCIM"))
        assertEquals(R.drawable.xp_folder_music, FileIconMapper.folderIconFor("Music"))
        assertEquals(R.drawable.xp_folder_video, FileIconMapper.folderIconFor("Movies"))
        assertEquals(R.drawable.xp_folder, FileIconMapper.folderIconFor("Projetos"))
    }
}
