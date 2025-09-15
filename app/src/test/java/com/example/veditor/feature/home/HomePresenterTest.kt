package com.example.veditor.feature.home

import app.cash.turbine.test
import com.example.veditor.core.domain.GetDeviceVideosUseCase
import com.example.veditor.core.media.DeviceVideo
import com.example.veditor.core.media.FakeMediaRepository
import com.example.veditor.core.model.TimeMs
import com.example.veditor.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomePresenterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenRepo_whenInit_stateContainsVideos() = runTest {
        val repo = FakeMediaRepository(
            listOf(
                DeviceVideo("content://1", "v1", TimeMs(1000)),
                DeviceVideo("content://2", "v2", TimeMs(2000)),
            ),
        )
        val presenter = HomePresenter(GetDeviceVideosUseCase(repo))

        val first = presenter.state.first()
        assertEquals(2, first.videos.size)
        assertEquals("v1", first.videos.first().displayName)
    }

    @Test
    fun state_emitsLatestVideos() = runTest {
        val repo = FakeMediaRepository(
            listOf(
                DeviceVideo("content://1", "v1", TimeMs(1000)),
            ),
        )
        val presenter = HomePresenter(GetDeviceVideosUseCase(repo))

        presenter.state.test {
            val initial = awaitItem()
            assertEquals(1, initial.videos.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
