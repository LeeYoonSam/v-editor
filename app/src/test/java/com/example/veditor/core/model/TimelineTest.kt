package com.example.veditor.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimelineTest {

    @Test
    fun givenSameStartAndEnd_whenCreateTimeRange_thenThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            val startInclusive = TimeMs(10)
            val endExclusive = TimeMs(10)
            TimeRange(startInclusive, endExclusive)
        }
    }

    @Test
    fun givenOverlappingClipRanges_whenCreateTimeline_thenThrows() {
        val firstClipRange = TimeRange(TimeMs(0), TimeMs(1000))
        val overlappingClipRange = TimeRange(TimeMs(500), TimeMs(1500))
        val firstClip = VideoClip("firstClip", "uri://first", firstClipRange)
        val overlappingClip = VideoClip("overlappingClip", "uri://second", overlappingClipRange)
        assertThrows(IllegalArgumentException::class.java) {
            Timeline(clips = listOf(firstClip, overlappingClip), overlays = emptyList())
        }
    }

    @Test
    fun givenOverlappingOverlayRanges_whenCreateTimeline_thenThrows() {
        val baseClipRange = TimeRange(TimeMs(0), TimeMs(1000))
        val overlappingOverlayRange = TimeRange(TimeMs(500), TimeMs(1500))
        val baseClip = VideoClip("baseClip", "uri://clip", baseClipRange)
        val firstSubtitle = Overlay.Subtitle("subtitle1", baseClipRange, "hello")
        val overlappingSubtitle = Overlay.Subtitle("subtitle2", overlappingOverlayRange, "world")
        assertThrows(IllegalArgumentException::class.java) {
            Timeline(clips = listOf(baseClip), overlays = listOf(firstSubtitle, overlappingSubtitle))
        }
    }

    @Test
    fun givenAdjacentClipRanges_whenCreateTimeline_thenSucceeds() {
        val firstRange = TimeRange(TimeMs(0), TimeMs(1000))
        val secondRange = TimeRange(TimeMs(1000), TimeMs(2000))
        val firstClip = VideoClip("firstClip", "uri://first", firstRange)
        val secondClip = VideoClip("secondClip", "uri://second", secondRange)
        val timeline = Timeline(clips = listOf(firstClip, secondClip), overlays = emptyList())
        assertEquals(2, timeline.clips.size)
    }
}
