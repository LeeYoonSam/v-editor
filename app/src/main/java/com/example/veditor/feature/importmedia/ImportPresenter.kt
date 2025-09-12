package com.example.veditor.feature.importmedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veditor.core.domain.GetDeviceVideosUseCase
import com.example.veditor.core.media.DeviceVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ImportState(
    val videos: List<DeviceVideo>,
    val selectedUris: Set<String>,
)

class ImportPresenter(
    getDeviceVideos: GetDeviceVideosUseCase,
) : ViewModel() {

    private val selectedUrisFlow = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<ImportState> = combine(
        getDeviceVideos(),
        selectedUrisFlow,
    ) { videos, selected ->
        ImportState(videos = videos, selectedUris = selected)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ImportState(videos = emptyList(), selectedUris = emptySet()),
    )

    fun toggleSelection(uri: String) {
        val current = selectedUrisFlow.value
        selectedUrisFlow.value = if (uri in current) current - uri else current + uri
    }

    fun clearSelection() {
        selectedUrisFlow.value = emptySet()
    }
}


