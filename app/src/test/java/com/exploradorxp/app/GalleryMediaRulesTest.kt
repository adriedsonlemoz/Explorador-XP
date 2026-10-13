package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryMediaRulesTest {
    @Test fun filtersRecognizeAndroidMediaFolders() {
        assertTrue(MediaGalleryRules.matches("/storage/emulated/0/DCIM/Camera/photo.jpg", MediaGalleryOrigin.CAMERA))
        assertTrue(MediaGalleryRules.matches("/storage/emulated/0/Pictures/Screenshots/a.png", MediaGalleryOrigin.SCREENSHOTS))
        assertTrue(MediaGalleryRules.matches("/storage/emulated/0/Download/video.mp4", MediaGalleryOrigin.DOWNLOADS))
        assertFalse(MediaGalleryRules.matches("/storage/emulated/0/Movies/a.mp4", MediaGalleryOrigin.CAMERA))
    }

    @Test fun otherExcludesKnownFoldersAndAllIncludesEverything() {
        assertTrue(MediaGalleryRules.matches("/sdcard/Movies/custom.mkv", MediaGalleryOrigin.OTHER))
        assertFalse(MediaGalleryRules.matches("/sdcard/Camera/clip.mov", MediaGalleryOrigin.OTHER))
        assertTrue(MediaGalleryRules.matches("/sdcard/Movies/clip.mov", MediaGalleryOrigin.ALL))
        assertTrue(MediaGalleryRules.matches("C:\\Download\\test.mp4", MediaGalleryOrigin.DOWNLOADS))
    }

    @Test fun orderingContainsAllFiveModes() {
        assertEquals(5, MediaGallerySort.values().size)
        assertEquals(5, MediaGalleryOrigin.values().size)
    }
    @Test fun folderMembershipUsesFullParentPathNotOnlyFolderName() {
        assertTrue(MediaGalleryRules.isInDirectory("/storage/emulated/0/DCIM/Camera/one.jpg", "/storage/emulated/0/DCIM/Camera"))
        assertFalse(MediaGalleryRules.isInDirectory("/storage/emulated/0/Pictures/Camera/two.jpg", "/storage/emulated/0/DCIM/Camera"))
        assertFalse(MediaGalleryRules.isInDirectory("/storage/emulated/0/DCIM/Camera/nested/three.jpg", "/storage/emulated/0/DCIM/Camera"))
        assertFalse(MediaGalleryRules.isInDirectory("/storage/emulated/0/DCIM/Camera2/other.jpg", "/storage/emulated/0/DCIM/Camera"))
    }

}
