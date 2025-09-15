package com.example.veditor.feature.editor

import androidx.lifecycle.ViewModel
import com.example.veditor.core.model.Overlay
import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EditorState(
    val timeline: Timeline?,
    val isExporting: Boolean = false,
    val overlaySheet: EditorOverlaySheet? = null,
    val overlayDraft: OverlayDraft? = null,
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

    fun onAddStickerClicked() {
        _state.value = _state.value.copy(
            overlaySheet = EditorOverlaySheet.Sticker,
            overlayDraft = OverlayDraft.Sticker(
                assetId = "star",
                x = 0.5f,
                y = 0.5f,
                scale = 1f,
                rotationDeg = 0f,
                startMs = 0L,
                durationMs = 1_000L,
            ),
        )
    }
    fun onAddSubtitleClicked() {
        _state.value = _state.value.copy(
            overlaySheet = EditorOverlaySheet.Subtitle,
            overlayDraft = OverlayDraft.Subtitle(
                text = "",
                startMs = 0L,
                durationMs = 1_000L,
                x = 0.5f,
                y = 0.8f,
                textSizeSp = 18f,
                colorArgb = 0xFFFFFFFF,
            ),
        )
    }
    fun onAddMusicClicked() {
        _state.value = _state.value.copy(
            overlaySheet = EditorOverlaySheet.Music,
            overlayDraft = OverlayDraft.Music(
                volumePercent = 100,
                startMs = 0L,
                durationMs = 1_000L,
                sourceUri = null,
            ),
        )
    }
    fun onExportClicked() { /* no-op for skeleton */ }

    fun onCloseSheet() {
        _state.value = _state.value.copy(overlaySheet = null, overlayDraft = null)
    }

    fun setTimeline(timeline: Timeline) {
        _state.value = _state.value.copy(timeline = timeline)
    }

    fun updateStickerDraft(
        assetId: String? = null,
        x: Float? = null,
        y: Float? = null,
        scale: Float? = null,
        rotationDeg: Float? = null,
    ) {
        val current = _state.value.overlayDraft as? OverlayDraft.Sticker ?: return
        _state.value = _state.value.copy(
            overlayDraft = current.copy(
                assetId = assetId ?: current.assetId,
                x = x ?: current.x,
                y = y ?: current.y,
                scale = scale ?: current.scale,
                rotationDeg = rotationDeg ?: current.rotationDeg,
            ),
        )
    }

    fun updateSubtitleDraft(text: String) {
        val current = _state.value.overlayDraft as? OverlayDraft.Subtitle ?: return
        _state.value = _state.value.copy(overlayDraft = current.copy(text = text))
    }

    fun updateSubtitlePosition(x: Float? = null, y: Float? = null) {
        val current = _state.value.overlayDraft as? OverlayDraft.Subtitle ?: return
        _state.value = _state.value.copy(
            overlayDraft = current.copy(
                x = (x ?: current.x).coerceIn(0f, 1f),
                y = (y ?: current.y).coerceIn(0f, 1f),
            ),
        )
    }

    fun updateSubtitleStyle(textSizeSp: Float? = null, colorArgb: Long? = null) {
        val current = _state.value.overlayDraft as? OverlayDraft.Subtitle ?: return
        _state.value = _state.value.copy(
            overlayDraft = current.copy(
                textSizeSp = (textSizeSp ?: current.textSizeSp).coerceIn(8f, 72f),
                colorArgb = colorArgb ?: current.colorArgb,
            ),
        )
    }

    fun updateMusicDraft(volumePercent: Int? = null, sourceUri: String? = null) {
        val current = _state.value.overlayDraft as? OverlayDraft.Music ?: return
        _state.value = _state.value.copy(
            overlayDraft = current.copy(
                volumePercent = volumePercent ?: current.volumePercent,
                sourceUri = sourceUri ?: current.sourceUri,
            ),
        )
    }

    fun updateOverlayTime(startMs: Long? = null, durationMs: Long? = null) {
        val tlEnd = _state.value.timeline?.clips?.lastOrNull()?.range?.endMs?.value ?: return
        fun clampStart(value: Long): Long = value.coerceIn(0L, tlEnd)
        fun clampDuration(start: Long, value: Long): Long {
            val maxDur = (tlEnd - start).coerceAtLeast(1L)
            return value.coerceIn(100L, maxDur)
        }
        when (val d = _state.value.overlayDraft) {
            is OverlayDraft.Sticker -> {
                val newStart = clampStart(startMs ?: d.startMs)
                val newDur = clampDuration(newStart, durationMs ?: d.durationMs)
                _state.value = _state.value.copy(overlayDraft = d.copy(startMs = newStart, durationMs = newDur))
            }
            is OverlayDraft.Subtitle -> {
                val newStart = clampStart(startMs ?: d.startMs)
                val newDur = clampDuration(newStart, durationMs ?: d.durationMs)
                _state.value = _state.value.copy(overlayDraft = d.copy(startMs = newStart, durationMs = newDur))
            }
            is OverlayDraft.Music -> {
                val newStart = clampStart(startMs ?: d.startMs)
                val newDur = clampDuration(newStart, durationMs ?: d.durationMs)
                _state.value = _state.value.copy(overlayDraft = d.copy(startMs = newStart, durationMs = newDur))
            }
            null -> return
        }
    }

    fun confirmOverlay() {
        val currentTimeline = _state.value.timeline ?: return
        val draft = _state.value.overlayDraft ?: return

        val timelineEnd = currentTimeline.clips.lastOrNull()?.range?.endMs?.value ?: return
        val (start, duration) = when (draft) {
            is OverlayDraft.Sticker -> draft.startMs to draft.durationMs
            is OverlayDraft.Subtitle -> draft.startMs to draft.durationMs
            is OverlayDraft.Music -> draft.startMs to draft.durationMs
        }
        val end = (start + duration).coerceAtMost(timelineEnd)
        if (end <= start) return
        val placeRange = TimeRange(TimeMs(start), TimeMs(end))

        val newOverlay: Overlay = when (draft) {
            is OverlayDraft.Sticker -> Overlay.Sticker(
                id = generateOverlayId(),
                timeRange = placeRange,
                assetId = draft.assetId,
                x = draft.x,
                y = draft.y,
                scale = draft.scale,
                rotationDeg = draft.rotationDeg,
            )
            is OverlayDraft.Subtitle -> Overlay.Subtitle(
                id = generateOverlayId(),
                timeRange = placeRange,
                text = draft.text,
                x = draft.x,
                y = draft.y,
                textSizeSp = draft.textSizeSp,
                colorArgb = draft.colorArgb,
            )
            is OverlayDraft.Music -> Overlay.Music(
                id = generateOverlayId(),
                timeRange = placeRange,
                sourceUri = draft.sourceUri ?: "",
                volumePercent = draft.volumePercent,
            )
        }
        val updated = currentTimeline.copy(overlays = currentTimeline.overlays + newOverlay)
        _state.value = _state.value.copy(timeline = updated, overlaySheet = null, overlayDraft = null)
    }

    fun updateOverlayTimeById(overlayId: String, startMs: Long? = null, durationMs: Long? = null) {
        val currentTimeline = _state.value.timeline ?: return
        val tlEnd = currentTimeline.clips.lastOrNull()?.range?.endMs?.value ?: return
        val idx = currentTimeline.overlays.indexOfFirst { it.id == overlayId }
        if (idx == -1) return
        val target = currentTimeline.overlays[idx]
        val curStart = target.timeRange.startMs.value
        val curDur = target.timeRange.endMs.value - curStart
        val newStart = (startMs ?: curStart).coerceIn(0L, tlEnd)
        val newEnd = (newStart + (durationMs ?: curDur)).coerceAtMost(tlEnd)
        if (newEnd <= newStart) return
        val newRange = TimeRange(TimeMs(newStart), TimeMs(newEnd))
        val updatedOverlay = when (target) {
            is Overlay.Sticker -> target.copy(timeRange = newRange)
            is Overlay.Subtitle -> target.copy(timeRange = newRange)
            is Overlay.Music -> target.copy(timeRange = newRange)
        }
        val newList = currentTimeline.overlays.toMutableList()
        newList[idx] = updatedOverlay
        _state.value = _state.value.copy(timeline = currentTimeline.copy(overlays = newList))
    }

    private fun generateOverlayId(): String = "ov_" + System.currentTimeMillis()
}

sealed class EditorOverlaySheet {
    data object Sticker : EditorOverlaySheet()
    data object Subtitle : EditorOverlaySheet()
    data object Music : EditorOverlaySheet()
}

sealed class OverlayDraft {
    data class Sticker(
        val assetId: String,
        val x: Float,
        val y: Float,
        val scale: Float,
        val rotationDeg: Float,
        val startMs: Long,
        val durationMs: Long,
    ) : OverlayDraft()

    data class Subtitle(
        val text: String,
        val startMs: Long,
        val durationMs: Long,
        val x: Float,
        val y: Float,
        val textSizeSp: Float,
        val colorArgb: Long,
    ) : OverlayDraft()

    data class Music(
        val volumePercent: Int,
        val sourceUri: String?,
        val startMs: Long,
        val durationMs: Long,
    ) : OverlayDraft()
}
