package com.example.veditor.core.domain

import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimelineEditUseCasesTest {

    private fun sampleTimeline(): Timeline {
        val c1 = VideoClip("c1", "content://a", TimeRange(TimeMs(0), TimeMs(1000)))
        val c2 = VideoClip("c2", "content://a", TimeRange(TimeMs(1000), TimeMs(2000)))
        return Timeline(clips = listOf(c1, c2), overlays = emptyList())
    }

    @Test
    fun given_clip_and_newRange_within_original_when_trim_then_updates_range() {
        val timeline = sampleTimeline()
        val trim = TrimClipUseCase()

        val result = trim(timeline, clipIndex = 0, newRange = TimeRange(TimeMs(100), TimeMs(900)))

        assertEquals(2, result.clips.size)
        assertEquals(TimeMs(100), result.clips[0].range.startMs)
        assertEquals(TimeMs(900), result.clips[0].range.endMs)
        // 다음 클립은 그대로
        assertEquals(TimeMs(1000), result.clips[1].range.startMs)
    }

    @Test
    fun given_clip_and_newRange_touching_neighbors_when_trim_then_throws() {
        val timeline = sampleTimeline()
        val trim = TrimClipUseCase()

        assertThrows(IllegalArgumentException::class.java) {
            // new end goes beyond neighbor start
            trim(timeline, clipIndex = 0, newRange = TimeRange(TimeMs(100), TimeMs(1001)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            // new start goes before previous (none, but invalid when decreasing order)
            trim(timeline, clipIndex = 1, newRange = TimeRange(TimeMs(999), TimeMs(2000)))
        }
    }

    @Test
    fun given_clip_and_newRange_outside_original_when_trim_then_throws() {
        val timeline = sampleTimeline()
        val trim = TrimClipUseCase()

        assertThrows(IllegalArgumentException::class.java) {
            trim(timeline, clipIndex = 0, newRange = TimeRange(TimeMs(0), TimeMs(1100)))
        }
    }

    @Test
    fun given_clip_when_split_at_middle_then_two_adjacent_clips() {
        val timeline = sampleTimeline()
        val split = SplitClipUseCase()

        val result = split(timeline, clipIndex = 0, splitAt = TimeMs(400))

        assertEquals(3, result.clips.size)
        val first = result.clips[0].range
        val second = result.clips[1].range
        assertEquals(TimeMs(0), first.startMs)
        assertEquals(TimeMs(400), first.endMs)
        assertEquals(TimeMs(400), second.startMs)
        assertEquals(TimeMs(1000), second.endMs)
        // 뒤 클립은 기존 위치 보전
        assertEquals(TimeMs(1000), result.clips[2].range.startMs)
    }

    @Test
    fun given_clip_when_split_at_bounds_then_throws() {
        val timeline = sampleTimeline()
        val split = SplitClipUseCase()
        assertThrows(IllegalArgumentException::class.java) {
            split(timeline, clipIndex = 0, splitAt = TimeMs(0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            split(timeline, clipIndex = 0, splitAt = TimeMs(1000))
        }
    }

    @Test
    fun given_adjacent_same_source_when_merge_then_combined_range() {
        val timeline = sampleTimeline()
        val merge = MergeAdjacentClipsUseCase()

        val result = merge(timeline, firstIndex = 0)

        assertEquals(1, result.clips.size)
        val range = result.clips[0].range
        assertEquals(TimeMs(0), range.startMs)
        assertEquals(TimeMs(2000), range.endMs)
    }

    @Test
    fun given_adjacent_different_source_when_merge_then_throws() {
        val c1 = VideoClip("c1", "content://a", TimeRange(TimeMs(0), TimeMs(1000)))
        val c2 = VideoClip("c2", "content://b", TimeRange(TimeMs(1000), TimeMs(2000)))
        val timeline = Timeline(clips = listOf(c1, c2), overlays = emptyList())
        val merge = MergeAdjacentClipsUseCase()

        assertThrows(IllegalArgumentException::class.java) {
            merge(timeline, firstIndex = 0)
        }
    }

    @Test
    fun given_non_adjacent_when_merge_then_throws() {
        val c1 = VideoClip("c1", "content://a", TimeRange(TimeMs(0), TimeMs(1000)))
        val c2 = VideoClip("c2", "content://a", TimeRange(TimeMs(1200), TimeMs(2000)))
        val timeline = Timeline(clips = listOf(c1, c2), overlays = emptyList())
        val merge = MergeAdjacentClipsUseCase()

        assertThrows(IllegalArgumentException::class.java) {
            merge(timeline, firstIndex = 0)
        }
    }
}
