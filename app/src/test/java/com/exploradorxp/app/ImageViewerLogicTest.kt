package com.exploradorxp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageViewerLogicTest {
    @Test fun scaleIsClamped() {
        assertEquals(1f, ImageViewerLogic.clampScale(.2f))
        assertEquals(3f, ImageViewerLogic.clampScale(3f))
        assertEquals(6f, ImageViewerLogic.clampScale(12f))
    }

    @Test fun doubleTapTogglesBetweenFitAndZoom() {
        assertEquals(2.5f, ImageViewerLogic.doubleTapScale(1f))
        assertEquals(1f, ImageViewerLogic.doubleTapScale(2f))
    }

    @Test fun removalKeepsNearestImageVisible() {
        assertEquals(2, ImageViewerLogic.replacementSourceIndex(1, 3))
        assertEquals(1, ImageViewerLogic.replacementSourceIndex(2, 3))
        assertNull(ImageViewerLogic.replacementSourceIndex(0, 1))
    }
}
