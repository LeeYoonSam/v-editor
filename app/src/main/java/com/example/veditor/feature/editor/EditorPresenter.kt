package com.example.veditor.feature.editor

import androidx.lifecycle.ViewModel
import com.example.veditor.core.model.Timeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorState(
    val timeline: Timeline?,
    val isExporting: Boolean = false,
    val overlaySheet: EditorOverlaySheet? = null,
)

class EditorPresenter(
    initialTimeline: Timeline? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditorState(
            timeline = initialTimeline,
        ),
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    fun onImportRequested() { /* SAF/MediaStore 연결 예정 */ }

    fun onAddStickerClicked() { _state.value = _state.value.copy(overlaySheet = EditorOverlaySheet.Sticker) }
    fun onAddSubtitleClicked() { _state.value = _state.value.copy(overlaySheet = EditorOverlaySheet.Subtitle) }
    fun onAddMusicClicked() { _state.value = _state.value.copy(overlaySheet = EditorOverlaySheet.Music) }
    fun onExportClicked() { /* no-op for skeleton */ }

    fun onCloseSheet() {
        _state.value = _state.value.copy(overlaySheet = null)
    }

    fun setTimeline(timeline: Timeline) {
        _state.value = _state.value.copy(timeline = timeline)
    }
}

sealed class EditorOverlaySheet {
    data object Sticker : EditorOverlaySheet()
    data object Subtitle : EditorOverlaySheet()
    data object Music : EditorOverlaySheet()
}
