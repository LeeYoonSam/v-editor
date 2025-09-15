package com.example.veditor.core.data

import com.example.veditor.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class EditSessionRepositoryTest {

    @Test
    fun given_timeline_when_save_and_load_then_equal() {
        // Arrange
        val clip = VideoClip("c1", "content://1", TimeRange(TimeMs(0), TimeMs(1_000)))
        val overlays = listOf(
            Overlay.Sticker("s1", TimeRange(TimeMs(0), TimeMs(500)), assetId = "star", x = 0.5f, y = 0.5f),
            Overlay.Subtitle("t1", TimeRange(TimeMs(200), TimeMs(900)), text = "Hi", x = 0.4f, y = 0.8f, textSizeSp = 16f, colorArgb = 0xFFFFFFFF),
            Overlay.Music("m1", TimeRange(TimeMs(0), TimeMs(1_000)), sourceUri = "content://audio", volumePercent = 80),
        )
        val timeline = Timeline(clips = listOf(clip), overlays = overlays)
        val repo = FileEditSessionRepository()
        val tmp = File.createTempFile("timeline", ".json")

        // Act
        repo.save(timeline, tmp)
        val loaded = repo.load(tmp)

        // Assert
        assertEquals(timeline, loaded)
    }
}


