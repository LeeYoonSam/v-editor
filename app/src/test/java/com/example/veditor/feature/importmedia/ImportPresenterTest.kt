package com.example.veditor.feature.importmedia

import app.cash.turbine.test
import com.example.veditor.core.domain.GetDeviceVideosUseCase
import com.example.veditor.core.media.DeviceVideo
import com.example.veditor.core.media.FakeMediaRepository
import com.example.veditor.core.model.TimeMs
import com.example.veditor.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ImportPresenterTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun givenRepo_whenInit_stateContainsVideosAndEmptySelection() = runTest {
        val repo = FakeMediaRepository(
            listOf(DeviceVideo("content://1", "v1", TimeMs(1000))),
        )
        val presenter = ImportPresenter(GetDeviceVideosUseCase(repo))

        presenter.state.test {
            val initial = awaitItem()
            assertEquals(1, initial.videos.size)
            assertEquals(0, initial.selectedUris.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleSelection_updatesSelectedUris() = runTest {
        val repo = FakeMediaRepository(
            listOf(DeviceVideo("content://1", "v1", TimeMs(1000))),
        )
        val presenter = ImportPresenter(GetDeviceVideosUseCase(repo))

        presenter.state.test {
            awaitItem() // initial
            presenter.toggleSelection("content://1")
            val afterToggle = awaitItem()
            assertEquals(setOf("content://1"), afterToggle.selectedUris)

            presenter.toggleSelection("content://1")
            val afterSecondToggle = awaitItem()
            assertEquals(emptySet<String>(), afterSecondToggle.selectedUris)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
