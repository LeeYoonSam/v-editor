package com.example.veditor.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeTest {

    @Test
    fun givenNegativeValue_whenCreateTimeMs_thenThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            TimeMs(-1)
        }
    }

    @Test
    fun givenRanges_whenCheckContainsAndOverlaps_thenBehaveAsExpected() {
        val r = TimeRange(TimeMs(0), TimeMs(1000))
        assertTrue(r.contains(TimeMs(0)))
        assertTrue(r.contains(TimeMs(999)))
        assertFalse(r.contains(TimeMs(1000)))

        val adjacent = TimeRange(TimeMs(1000), TimeMs(2000))
        assertFalse(r.overlaps(adjacent))

        val overlap = TimeRange(TimeMs(500), TimeMs(1500))
        assertTrue(r.overlaps(overlap))
    }
}
