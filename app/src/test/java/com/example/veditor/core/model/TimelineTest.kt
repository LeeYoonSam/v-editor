package com.example.veditor.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimelineTest {

    @Test
    fun given_same_start_and_end_when_create_time_range_then_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            val startInclusive = TimeMs(10)
            val endExclusive = TimeMs(10)
            TimeRange(startInclusive, endExclusive)
        }
    }

    @Test
    fun given_overlapping_clip_ranges_when_create_timeline_then_throws() {
        val firstClipRange = TimeRange(TimeMs(0), TimeMs(1000))
        val overlappingClipRange = TimeRange(TimeMs(500), TimeMs(1500))
        val firstClip = VideoClip("firstClip", "uri://first", firstClipRange)
        val overlappingClip = VideoClip("overlappingClip", "uri://second", overlappingClipRange)
        assertThrows(IllegalArgumentException::class.java) {
            Timeline(clips = listOf(firstClip, overlappingClip), overlays = emptyList())
        }
    }

    // Overlays can overlap now; ensure it's allowed
    @Test
    fun given_overlapping_overlay_ranges_when_create_timeline_then_allowed() {
        val baseClipRange = TimeRange(TimeMs(0), TimeMs(1000))
        val overlappingOverlayRange = TimeRange(TimeMs(500), TimeMs(1500))
        val baseClip = VideoClip("baseClip", "uri://clip", baseClipRange)
        val firstSubtitle = Overlay.Subtitle(
            "subtitle1",
            baseClipRange,
            "hello",
            x = 0.5f,
            y = 0.9f,
            textSizeSp = 16f,
            colorArgb = 0xFFFFFFFF,
        )
        val overlappingSubtitle = Overlay.Subtitle(
            "subtitle2",
            overlappingOverlayRange,
            "world",
            x = 0.5f,
            y = 0.8f,
            textSizeSp = 16f,
            colorArgb = 0xFFFFFFFF,
        )
        val timeline = Timeline(clips = listOf(baseClip), overlays = listOf(firstSubtitle, overlappingSubtitle))
        assertEquals(2, timeline.overlays.size)
    }

    @Test
    fun given_adjacent_clip_ranges_when_create_timeline_then_succeeds() {
        val firstRange = TimeRange(TimeMs(0), TimeMs(1000))
        val secondRange = TimeRange(TimeMs(1000), TimeMs(2000))
        val firstClip = VideoClip("firstClip", "uri://first", firstRange)
        val secondClip = VideoClip("secondClip", "uri://second", secondRange)
        val timeline = Timeline(clips = listOf(firstClip, secondClip), overlays = emptyList())
        assertEquals(2, timeline.clips.size)
    }
}
