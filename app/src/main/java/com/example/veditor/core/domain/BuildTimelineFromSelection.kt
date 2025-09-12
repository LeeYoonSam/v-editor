package com.example.veditor.core.domain

import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip

/**
 * 선택된 동영상 URI 리스트를 임시 타임라인으로 변환한다.
 * 각 클립은 [defaultClipDurationMs] 길이로 순차 배치한다.
 * 실제 길이는 후속 단계에서 MediaMetadata로 대체한다.
 */
class BuildTimelineFromSelectionUseCase(
    private val defaultClipDurationMs: Long = 2_000,
) {
    operator fun invoke(selectedUris: List<String>): Timeline {
        require(selectedUris.isNotEmpty()) { "selection must not be empty" }
        val clips = selectedUris.mapIndexed { index, uri ->
            val startOffset = index * defaultClipDurationMs
            val start = TimeMs(startOffset)
            val end = TimeMs(startOffset + defaultClipDurationMs)
            VideoClip(
                id = "clip-$index",
                sourceUri = uri,
                range = TimeRange(start, end),
            )
        }
        return Timeline(clips = clips, overlays = emptyList())
    }
}


