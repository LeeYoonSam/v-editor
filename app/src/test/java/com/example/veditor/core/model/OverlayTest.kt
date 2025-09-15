package com.example.veditor.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayTest {

    @Test
    fun given_music_when_default_created_then_volume_is_100() {
        val timeRange = TimeRange(TimeMs(0), TimeMs(1000))
        val music = Overlay.Music(id = "m1", timeRange = timeRange, sourceUri = "uri://m")
        assertEquals(100, music.volumePercent)
    }

    @Test
    fun given_sticker_when_default_created_then_has_transform_defaults() {
        val timeRange = TimeRange(TimeMs(0), TimeMs(1000))
        val sticker = Overlay.Sticker(id = "s1", timeRange = timeRange, assetId = "a", x = 0.5f, y = 0.5f)
        assertEquals(1f, sticker.scale, 0.0f)
        assertEquals(0f, sticker.rotationDeg, 0.0f)
    }
}
