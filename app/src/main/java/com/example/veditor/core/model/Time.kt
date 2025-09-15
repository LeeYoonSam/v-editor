package com.example.veditor.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Milliseconds wrapper to make time units explicit across the codebase.
 *
 * Example:
 * ```kotlin
 * val t0 = TimeMs(0)
 * val t1 = TimeMs(1_000)
 * ```
 *
 * @see TimeRange
 * @param value non-negative milliseconds
 */
@JvmInline
@Serializable(with = TimeMsAsLongSerializer::class)
value class TimeMs(val value: Long) {
    init {
        require(value >= 0) { "time must be >= 0" }
    }
}

object TimeMsAsLongSerializer : KSerializer<TimeMs> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TimeMs", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: TimeMs) { encoder.encodeLong(value.value) }
    override fun deserialize(decoder: Decoder): TimeMs = TimeMs(decoder.decodeLong())
}

/**
 * Half-open time interval [startMs, endMs).
 *
 * Example:
 * ```kotlin
 * val range = TimeRange(TimeMs(0), TimeMs(2_000))
 * range.contains(TimeMs(1_999)) // true
 * range.contains(TimeMs(2_000)) // false
 * ```
 *
 * @see TimeMs
 * @param startMs inclusive start
 * @param endMs exclusive end, must be > start
 * @return durationMs computed as end-start
 */
@Serializable
data class TimeRange(
    val startMs: TimeMs,
    val endMs: TimeMs,
) {
    init {
        require(endMs.value > startMs.value) { "end must be > start" }
    }
    val durationMs: Long get() = endMs.value - startMs.value
    fun overlaps(other: TimeRange): Boolean = startMs.value < other.endMs.value && other.startMs.value < endMs.value
    fun contains(time: TimeMs): Boolean = time.value in startMs.value until endMs.value
}
