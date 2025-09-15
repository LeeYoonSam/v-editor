package com.example.veditor.core.model

import androidx.annotation.IntRange
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Timeline overlays.
 *
 * Example:
 * ```kotlin
 * val r = TimeRange(TimeMs(0), TimeMs(1000))
 * val subtitle = Overlay.Subtitle("s1", r, text = "Hello")
 * val music = Overlay.Music("m1", r, sourceUri = "content://audio")
 * val sticker = Overlay.Sticker("k1", r, assetId = "star", x = 0.5f, y = 0.3f)
 * ```
 *
 * @see TimeRange
 */
@Serializable
sealed class Overlay {
    abstract val id: String
    abstract val timeRange: TimeRange

    /** Image-like overlay with transform.
     * @param id unique id
     * @param timeRange when this overlay is visible
     * @param assetId asset key
     * @param x normalized x [0,1]
     * @param y normalized y [0,1]
     * @param scale scale factor
     * @param rotationDeg rotation in degrees
     */
    @Serializable
    @SerialName("sticker")
    data class Sticker(
        override val id: String,
        override val timeRange: TimeRange,
        val assetId: String,
        val x: Float,
        val y: Float,
        val scale: Float = 1f,
        val rotationDeg: Float = 0f,
    ) : Overlay()

    /** Timed text overlay.
     * @param id unique id
     * @param timeRange visibility window
     * @param text subtitle text
     * @param x normalized x [0,1]
     * @param y normalized y [0,1]
     * @param textSizeSp font size in sp
     * @param colorArgb packed ARGB color (0xAARRGGBB)
     */
    @Serializable
    @SerialName("subtitle")
    data class Subtitle(
        override val id: String,
        override val timeRange: TimeRange,
        val text: String,
        val x: Float,
        val y: Float,
        val textSizeSp: Float,
        val colorArgb: Long,
    ) : Overlay()

    /** Background music segment.
     * @param id unique id
     * @param timeRange audible window
     * @param sourceUri audio source
     * @param volumePercent 0..100
     */
    @Serializable
    @SerialName("music")
    data class Music(
        override val id: String,
        override val timeRange: TimeRange,
        val sourceUri: String,
        @param:IntRange(from = 0, to = 100) val volumePercent: Int = 100,
    ) : Overlay()
}
