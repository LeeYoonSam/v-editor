package com.example.veditor.feature.editor

import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorPresenterTest {

    @Test
    fun given_presenter_when_add_sticker_then_sheet_is_sticker() {
        val presenter = EditorPresenter()
        presenter.onAddStickerClicked()
        val sheet = presenter.state.value.overlaySheet
        assertEquals(EditorOverlaySheet.Sticker, sheet)
    }

    @Test
    fun given_presenter_when_close_sheet_then_sheet_is_null() {
        val presenter = EditorPresenter()
        presenter.onAddMusicClicked()
        presenter.onCloseSheet()
        val sheet = presenter.state.value.overlaySheet
        assertEquals(null, sheet)
    }

    @Test
    fun given_presenter_when_set_timeline_then_state_updates() {
        val presenter = EditorPresenter()
        val clip = VideoClip("c1", "uri://1", TimeRange(TimeMs(0), TimeMs(1000)))
        val timeline = Timeline(listOf(clip), emptyList())
        presenter.setTimeline(timeline)
        assertEquals(timeline, presenter.state.value.timeline)
    }

    @Test
    fun given_draft_and_timeline_when_confirm_overlay_then_overlay_added_and_sheet_closed() {
        val clip = VideoClip("c1", "uri://1", TimeRange(TimeMs(0), TimeMs(1000)))
        val timeline = Timeline(listOf(clip), emptyList())
        val presenter = EditorPresenter(initialTimeline = timeline)

        presenter.onAddStickerClicked()
        presenter.updateStickerDraft(x = 0.7f, y = 0.2f, assetId = null, scale = null, rotationDeg = null)
        presenter.confirmOverlay()

        val state = presenter.state.value
        assertEquals(1, state.timeline?.overlays?.size)
        assertEquals(null, state.overlaySheet)
        assertEquals(null, state.overlayDraft)
    }
}
