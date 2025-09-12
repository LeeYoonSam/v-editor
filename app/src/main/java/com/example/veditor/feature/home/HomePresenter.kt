package com.example.veditor.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veditor.core.domain.GetDeviceVideosUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomePresenter(
    getDeviceVideos: GetDeviceVideosUseCase,
) : ViewModel() {

    val state = getDeviceVideos()
        .map { videos -> HomeState(videos = videos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeState(videos = emptyList()),
        )
}


