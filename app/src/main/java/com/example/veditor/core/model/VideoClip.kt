package com.example.veditor.core.model

/**
 * Reference to a segment of a source video.
 *
 * Example:
 * ```kotlin
 * val clip = VideoClip(
 *     id = "c1",
 *     sourceUri = "content://video/1",
 *     range = TimeRange(TimeMs(0), TimeMs(2_000))
 * )
 * ```
 *
 * @see TimeRange
 * @param id unique id
 * @param sourceUri source video URI
 * @param range sub-range of the source
 */
data class VideoClip(
    val id: String,
    val sourceUri: String,
    val range: TimeRange,
)
