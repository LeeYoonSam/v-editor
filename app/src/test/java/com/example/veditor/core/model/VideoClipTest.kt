package com.example.veditor.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoClipTest {

    @Test
    fun givenClip_whenConstructed_thenRetainsRange() {
        val r = TimeRange(TimeMs(100), TimeMs(300))
        val c = VideoClip("c1", "uri://x", r)
        assertEquals(200, r.durationMs)
        assertEquals(r, c.range)
    }
}
