package com.example.veditor.feature.editor

import androidx.lifecycle.ViewModel
import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorState(
    val timeline: Timeline?,
    val isExporting: Boolean = false,
)

class EditorPresenter : ViewModel() {

    private val _state = MutableStateFlow(
        EditorState(
            timeline = null,
        ),
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    fun onImportRequested() { /* SAF/MediaStore 연결 예정 */ }

    fun onAddStickerClicked() { /* no-op for skeleton */ }
    fun onAddSubtitleClicked() { /* no-op for skeleton */ }
    fun onAddMusicClicked() { /* no-op for skeleton */ }
    fun onExportClicked() { /* no-op for skeleton */ }
}


