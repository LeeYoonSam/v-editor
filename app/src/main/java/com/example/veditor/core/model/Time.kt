package com.example.veditor.core.model

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
value class TimeMs(val value: Long) {
    init {
        require(value >= 0) { "time must be >= 0" }
    }
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
