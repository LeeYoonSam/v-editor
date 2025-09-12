package com.example.veditor.core.media

import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaRepositoryTest {

    @Test
    fun givenFake_whenListVideos_thenReturnsSeed() = runBlocking {
        val seed = listOf(
            DeviceVideo(uri = "content://video/1", displayName = "v1", durationMs = TimeMs(1000)),
            DeviceVideo(uri = "content://video/2", displayName = "v2", durationMs = TimeMs(2000)),
        )
        val repo = FakeMediaRepository(seed)

        val result = repo.listDeviceVideos().first()

        assertEquals(2, result.size)
        assertEquals("v1", result.first().displayName)
    }

    @Test
    fun givenSourceAndRange_whenBuildClip_thenUsesSourceAndRange() = runBlocking {
        val repo = FakeMediaRepository()
        val range = TimeRange(TimeMs(100), TimeMs(300))

        val clip = repo.buildClipFromSource("content://video/1", range)

        assertEquals("content://video/1", clip.sourceUri)
        assertEquals(range, clip.range)
    }
}


