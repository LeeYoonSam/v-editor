package com.example.veditor.feature.editor

import androidx.lifecycle.ViewModel
import com.example.veditor.core.model.Timeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorState(
    val timeline: Timeline?,
    val isExporting: Boolean = false,
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

    fun onAddStickerClicked() { /* no-op for skeleton */ }
    fun onAddSubtitleClicked() { /* no-op for skeleton */ }
    fun onAddMusicClicked() { /* no-op for skeleton */ }
    fun onExportClicked() { /* no-op for skeleton */ }

    fun setTimeline(timeline: Timeline) {
        _state.value = _state.value.copy(timeline = timeline)
    }
}


