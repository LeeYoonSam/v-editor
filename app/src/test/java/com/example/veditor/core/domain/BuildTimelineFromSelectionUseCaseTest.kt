package com.example.veditor.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildTimelineFromSelectionUseCaseTest {

    @Test
    fun given_single_selection_when_build_timeline_then_timeline_has_one_clip() {
        val selectedUris = listOf("content://video/1")
        val build = BuildTimelineFromSelectionUseCase()
        val timeline = build(selectedUris)
        assertEquals(1, timeline.clips.size)
    }

    @Test
    fun given_multiple_selections_when_build_timeline_then_clips_are_adjacent() {
        val selectedUris = listOf("content://video/1", "content://video/2")
        val build = BuildTimelineFromSelectionUseCase(
            defaultClipDurationMs = 1_000,
        )
        val timeline = build(selectedUris)
        val first = timeline.clips[0].range
        val second = timeline.clips[1].range
        assertEquals(first.endMs.value, second.startMs.value)
    }
}
