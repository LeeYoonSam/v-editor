package com.example.veditor.core.domain

import com.example.veditor.core.media.DeviceVideo
import com.example.veditor.core.media.MediaRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case: stream device videos for presentation layer.
 */
class GetDeviceVideosUseCase(
    private val mediaRepository: MediaRepository,
) {
    operator fun invoke(): Flow<List<DeviceVideo>> = mediaRepository.listDeviceVideos()
}
