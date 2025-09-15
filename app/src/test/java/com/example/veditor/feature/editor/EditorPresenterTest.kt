package com.example.veditor.feature.editor

import com.example.veditor.core.model.Overlay
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

    @Test
    fun given_timeline_with_overlay_when_delete_selected_then_overlay_removed_and_state_cleared() {
        val clip = VideoClip("c1", "uri://1", TimeRange(TimeMs(0), TimeMs(1000)))
        val overlay = Overlay.Subtitle(
            id = "ov_test",
            timeRange = TimeRange(TimeMs(100), TimeMs(400)),
            text = "Hello",
            x = 0.5f,
            y = 0.8f,
            textSizeSp = 16f,
            colorArgb = 0xFFFFFFFF,
        )
        val timeline = Timeline(listOf(clip), listOf(overlay))
        val presenter = EditorPresenter(initialTimeline = timeline)

        presenter.editOverlay(overlay.id)
        presenter.deleteSelectedOverlay()

        val state = presenter.state.value
        assertEquals(0, state.timeline?.overlays?.size)
        assertEquals(null, state.overlaySheet)
        assertEquals(null, state.overlayDraft)
        assertEquals(null, state.selectedOverlayId)
    }

    @Test
    fun given_timeline_with_overlay_when_delete_by_id_then_overlay_removed() {
        val clip = VideoClip("c1", "uri://1", TimeRange(TimeMs(0), TimeMs(1000)))
        val overlay = Overlay.Sticker(
            id = "ov_test2",
            timeRange = TimeRange(TimeMs(0), TimeMs(200)),
            assetId = "star",
            x = 0.2f,
            y = 0.3f,
            scale = 1f,
            rotationDeg = 0f,
        )
        val timeline = Timeline(listOf(clip), listOf(overlay))
        val presenter = EditorPresenter(initialTimeline = timeline)

        presenter.deleteOverlayById(overlay.id)

        val state = presenter.state.value
        assertEquals(0, state.timeline?.overlays?.size)
    }

    @Test
    fun given_music_draft_when_confirm_then_music_overlay_added() {
        val clip = VideoClip("c1", "uri://1", TimeRange(TimeMs(0), TimeMs(1000)))
        val timeline = Timeline(listOf(clip), emptyList())
        val presenter = EditorPresenter(initialTimeline = timeline)

        presenter.onAddMusicClicked()
        presenter.updateMusicDraft(volumePercent = 42, sourceUri = "content://audio")
        presenter.confirmOverlay()

        val overlays = presenter.state.value.timeline?.overlays.orEmpty()
        assertEquals(1, overlays.size)
        val music = overlays.first() as Overlay.Music
        assertEquals(42, music.volumePercent)
        assertEquals("content://audio", music.sourceUri)
    }

    @Test
    fun given_existing_music_when_edit_and_confirm_then_overlay_updated_not_added() {
        val clip = VideoClip("c1", "uri://1", TimeRange(TimeMs(0), TimeMs(2000)))
        val initial = Overlay.Music(
            id = "m1",
            timeRange = TimeRange(TimeMs(0), TimeMs(1000)),
            sourceUri = "content://old",
            volumePercent = 80,
        )
        val timeline = Timeline(listOf(clip), listOf(initial))
        val presenter = EditorPresenter(initialTimeline = timeline)

        presenter.editOverlay("m1")
        presenter.updateOverlayTime(startMs = 100, durationMs = 1200)
        presenter.updateMusicDraft(volumePercent = 55, sourceUri = "content://new")
        presenter.confirmOverlay()

        val overlays = presenter.state.value.timeline?.overlays.orEmpty()
        assertEquals(1, overlays.size)
        val updated = overlays.first() as Overlay.Music
        assertEquals(55, updated.volumePercent)
        assertEquals("content://new", updated.sourceUri)
        assertEquals(100L, updated.timeRange.startMs.value)
        assertEquals(1300L, updated.timeRange.endMs.value)
    }
}
