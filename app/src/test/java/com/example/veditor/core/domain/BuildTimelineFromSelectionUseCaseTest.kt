package com.example.veditor.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildTimelineFromSelectionUseCaseTest {

    @Test
    fun givenSingleSelection_whenBuildTimeline_thenTimelineHasOneClip() {
        val selectedUris = listOf("content://video/1")
        val build = BuildTimelineFromSelectionUseCase()
        val timeline = build(selectedUris)
        assertEquals(1, timeline.clips.size)
    }

    @Test
    fun givenMultipleSelections_whenBuildTimeline_thenClipsAreAdjacent() {
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
