package com.example.veditor.core.domain

import com.example.veditor.core.media.DeviceVideo
import com.example.veditor.core.media.FakeMediaRepository
import com.example.veditor.core.model.TimeMs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetDeviceVideosUseCaseTest {

    @Test
    fun given_repo_when_invoke_then_emits_videos() = runBlocking {
        val repo = FakeMediaRepository(
            listOf(
                DeviceVideo("content://1", "v1", TimeMs(1000)),
                DeviceVideo("content://2", "v2", TimeMs(2000)),
            ),
        )
        val useCase = GetDeviceVideosUseCase(repo)

        val first = useCase().first()

        assertEquals(2, first.size)
        assertEquals("v1", first.first().displayName)
    }
}
