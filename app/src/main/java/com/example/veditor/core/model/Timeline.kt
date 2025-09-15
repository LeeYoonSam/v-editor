package com.example.veditor.core.model

import kotlinx.serialization.Serializable

/**
 * Non-destructive edit model composed of ordered [clips] and timed [overlays].
 *
 * Example:
 * ```kotlin
 * val clipA = VideoClip("a", "content://a", TimeRange(TimeMs(0), TimeMs(1000)))
 * val clipB = VideoClip("b", "content://b", TimeRange(TimeMs(1000), TimeMs(2000)))
 * val timeline = Timeline(clips = listOf(clipA, clipB), overlays = emptyList())
 * ```
 *
 * @see VideoClip
 * @see Overlay
 * @param clips ordered, non-overlapping clips
 * @param overlays timed overlays
 */
@Serializable
data class Timeline(
    val clips: List<VideoClip>,
    val overlays: List<Overlay>,
) {
    init {
        require(clips.isNotEmpty()) { "timeline requires at least one clip" }
        require(!hasOverlap(clips.map { it.range })) { "clip ranges must not overlap" }
        // Overlays are allowed to overlap in time by design
    }

    private fun hasOverlap(ranges: List<TimeRange>): Boolean {
        val sorted = ranges.sortedBy { it.startMs.value }
        for (i in 1 until sorted.size) {
            if (sorted[i - 1].overlaps(sorted[i])) return true
        }
        return false
    }
}
