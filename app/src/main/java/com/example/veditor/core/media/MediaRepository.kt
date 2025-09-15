package com.example.veditor.core.media

import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.VideoClip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Abstraction over device media access.
 * MVP에서는 로컬 기기에서 비디오 목록을 제공하고, 선택된 소스에서 부분 범위를 클립으로 만든다.
 * Circuit Presenter에서의 반응형 업데이트를 위해 목록은 Flow로 노출한다.
 */
interface MediaRepository {
    fun listDeviceVideos(): Flow<List<DeviceVideo>>
    suspend fun buildClipFromSource(sourceUri: String, range: TimeRange): VideoClip
}

data class DeviceVideo(
    val uri: String,
    val displayName: String,
    val durationMs: TimeMs,
)

class FakeMediaRepository(
    private val videos: List<DeviceVideo> = emptyList(),
) : MediaRepository {
    override fun listDeviceVideos(): Flow<List<DeviceVideo>> = flow { emit(videos) }

    override suspend fun buildClipFromSource(sourceUri: String, range: TimeRange): VideoClip {
        return VideoClip(
            id = "clip_${sourceUri.hashCode()}_${range.startMs.value}",
            sourceUri = sourceUri,
            range = range,
        )
    }
}
