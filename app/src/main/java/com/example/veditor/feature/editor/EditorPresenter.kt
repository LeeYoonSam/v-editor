package com.example.veditor.feature.editor

import androidx.lifecycle.ViewModel
import com.example.veditor.core.domain.ExportParams
import com.example.veditor.core.domain.ExportUseCase
import com.example.veditor.core.domain.TrimClipUseCase
import com.example.veditor.core.model.Overlay
import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorState(
    val timeline: Timeline?,
    val isExporting: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val overlaySheet: EditorOverlaySheet? = null,
    val overlayDraft: OverlayDraft? = null,
    val selectedOverlayId: String? = null,
    val zoomDpPerMs: Float = 0.3f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    // 컷 적용 후 썸네일/스크러빙에 사용되는 뷰포트 범위(초기값: 전체 타임라인)
    val viewportStartMs: Long = 0L,
    val viewportEndMs: Long = 0L,
    val isTrimEditing: Boolean = false,
)

class EditorPresenter(
    initialTimeline: Timeline? = null,
    private val exportUseCase: ExportUseCase? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditorState(
            timeline = initialTimeline,
            trimStartMs = 0L,
            trimEndMs = initialTimeline?.clips?.lastOrNull()?.range?.endMs?.value ?: 0L,
            viewportStartMs = 0L,
            viewportEndMs = initialTimeline?.clips?.lastOrNull()?.range?.endMs?.value ?: 0L,
            isTrimEditing = false,
        ),
    )
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val trimClipUseCase = TrimClipUseCase()

    fun onImportRequested() { /* SAF/MediaStore 연결 예정 */ }

    fun onAddStickerClicked() {
        _state.value = _state.value.copy(
            selectedOverlayId = null,
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

    fun onAddMusicClicked() {
        _state.value = _state.value.copy(
            selectedOverlayId = null,
            overlaySheet = EditorOverlaySheet.Music,
            overlayDraft = OverlayDraft.Music(
                volumePercent = 100,
                startMs = 0L,
                durationMs = 1_000L,
                sourceUri = null,
            ),
        )
    }
    private var exportJob: Job? = null

    fun onExportClicked() {
        val tl = _state.value.timeline ?: return
        if (exportJob != null) return
        _state.value = _state.value.copy(isExporting = true)
        exportJob = CoroutineScope(Dispatchers.Default).launch {
            val result = exportUseCase?.invoke(ExportParams(tl)) { _ -> /* progress hook */ }
            _state.value = _state.value.copy(isExporting = false)
            exportJob = null
        }
    }

    // Playback controls
    fun setPlaying(playing: Boolean) {
        _state.value = _state.value.copy(isPlaying = playing)
    }

    fun seekTo(positionMs: Long) {
        val tl = _state.value.timeline ?: return
        val fullEnd = tl.clips.lastOrNull()?.range?.endMs?.value ?: return
        val vpStart = _state.value.viewportStartMs
        val vpEnd = _state.value.viewportEndMs.takeIf { it > 0L } ?: fullEnd
        val effectiveStart = vpStart.coerceAtLeast(0L)
        val effectiveEnd = vpEnd.coerceIn(effectiveStart, fullEnd)
        val clamped = positionMs.coerceIn(effectiveStart, effectiveEnd)
        _state.value = _state.value.copy(currentPositionMs = clamped)
    }

    fun onPlaybackPosition(positionMs: Long) {
        if (_state.value.isPlaying) {
            seekTo(positionMs)
        }
    }

    fun onCloseSheet() {
        _state.value = _state.value.copy(overlaySheet = null, overlayDraft = null)
    }

    fun setTimeline(timeline: Timeline) {
        _state.value = _state.value.copy(
            timeline = timeline,
            trimStartMs = 0L,
            trimEndMs = timeline.clips.lastOrNull()?.range?.endMs?.value ?: 0L,
            viewportStartMs = 0L,
            viewportEndMs = timeline.clips.lastOrNull()?.range?.endMs?.value ?: 0L,
            isTrimEditing = false,
        )
    }

    fun setTrimEditing(editing: Boolean) {
        _state.value = _state.value.copy(isTrimEditing = editing)
    }

    // 뷰포트 범위 업데이트(편집 모드 재생 범위)
    fun updateViewportRange(startMs: Long? = null, endMs: Long? = null, moveByMs: Long? = null) {
        val tlEnd = _state.value.timeline?.clips?.lastOrNull()?.range?.endMs?.value ?: return
        var vs = _state.value.viewportStartMs
        var ve = _state.value.viewportEndMs.takeIf { it > 0L } ?: tlEnd
        if (moveByMs != null) {
            val length = (ve - vs).coerceAtLeast(100L)
            val nvs = (vs + moveByMs).coerceIn(0L, tlEnd - length)
            vs = nvs
            ve = nvs + length
        }
        if (startMs != null) vs = startMs.coerceIn(0L, ve - 100L)
        if (endMs != null) ve = endMs.coerceIn(vs + 100L, tlEnd)

        // 뷰포트가 바뀌면, 현재 위치가 벗어나지 않도록 보정
        val cur = _state.value.currentPositionMs
        val clampedCur = cur.coerceIn(vs, ve)
        _state.value = _state.value.copy(
            viewportStartMs = vs,
            viewportEndMs = ve,
            currentPositionMs = clampedCur,
        )
    }

    // 트림 선택 범위 업데이트: 좌/우 핸들 드래그 또는 내부 드래그로 이동
    fun updateTrimSelection(startMs: Long? = null, endMs: Long? = null, moveByMs: Long? = null) {
        val tlEnd = _state.value.timeline?.clips?.lastOrNull()?.range?.endMs?.value ?: return
        var s = _state.value.trimStartMs
        var e = _state.value.trimEndMs
        if (moveByMs != null) {
            val length = (e - s).coerceAtLeast(1L)
            val ns = (s + moveByMs).coerceIn(0L, tlEnd - length)
            s = ns
            e = ns + length
        }
        if (startMs != null) s = startMs.coerceIn(0L, e - 1)
        if (endMs != null) e = endMs.coerceIn(s + 1, tlEnd)
        _state.value = _state.value.copy(trimStartMs = s, trimEndMs = e)
    }

    // 컷 버튼: 선택 범위를 기반으로 타임라인을 해당 구간으로 트림하고 뷰포트 갱신
    fun applyCutToTimeline() {
        val current = _state.value
        val tl = current.timeline ?: return
        val s = current.trimStartMs
        val e = current.trimEndMs
        if (e <= s) return
        if (tl.clips.isEmpty()) return
        // 선택 구간을 완전히 포함하는 단일 클립을 찾는다.
        val idx = tl.clips.indexOfFirst { clip ->
            val cs = clip.range.startMs.value
            val ce = clip.range.endMs.value
            s >= cs && e <= ce
        }
        if (idx >= 0) {
            // 단일 클립 내부 트림 + 소스 교체(선택 구간만 남기는 새로운 타임라인)
            val target = tl.clips[idx]
            val newClip = target.copy(
                range = TimeRange(TimeMs(0), TimeMs(e - s)),
            )
            // 선택 범위와 겹치는 오버레이만 남기고, 시작 기준을 0으로 이동
            val keptOverlays = tl.overlays.mapNotNull { o ->
                val os = o.timeRange.startMs.value
                val oe = o.timeRange.endMs.value
                val isectStart = maxOf(s, os)
                val isectEnd = minOf(e, oe)
                if (isectEnd <= isectStart) return@mapNotNull null
                val shifted = TimeRange(TimeMs(isectStart - s), TimeMs(isectEnd - s))
                when (o) {
                    is Overlay.Sticker -> o.copy(timeRange = shifted)
                    is Overlay.Subtitle -> o.copy(timeRange = shifted)
                    is Overlay.Music -> o.copy(timeRange = shifted)
                }
            }
            val newTimeline = Timeline(clips = listOf(newClip), overlays = keptOverlays)
            setTimeline(newTimeline)
            _state.value = _state.value.copy(
                currentPositionMs = 0L,
                viewportStartMs = 0L,
                viewportEndMs = newClip.range.endMs.value,
                isTrimEditing = false,
            )
        } else {
            // 여러 클립에 걸친 선택: 타임라인은 보존하고 뷰포트만 설정
            _state.value = _state.value.copy(
                currentPositionMs = s,
                viewportStartMs = s,
                viewportEndMs = e,
                isTrimEditing = false,
            )
        }
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
        val selectedId = _state.value.selectedOverlayId

        val timelineEnd = currentTimeline.clips.lastOrNull()?.range?.endMs?.value ?: return
        val (start, duration) = when (draft) {
            is OverlayDraft.Sticker -> draft.startMs to draft.durationMs
            is OverlayDraft.Subtitle -> draft.startMs to draft.durationMs
            is OverlayDraft.Music -> draft.startMs to draft.durationMs
        }
        val end = (start + duration).coerceAtMost(timelineEnd)
        if (end <= start) return
        val placeRange = TimeRange(TimeMs(start), TimeMs(end))

        if (selectedId != null) {
            // Update existing overlay
            val idx = currentTimeline.overlays.indexOfFirst { it.id == selectedId }
            if (idx != -1) {
                val target = currentTimeline.overlays[idx]
                val updatedOverlay: Overlay? = when {
                    target is Overlay.Sticker && draft is OverlayDraft.Sticker -> target.copy(
                        timeRange = placeRange,
                        assetId = draft.assetId,
                        x = draft.x,
                        y = draft.y,
                        scale = draft.scale,
                        rotationDeg = draft.rotationDeg,
                    )
                    target is Overlay.Subtitle && draft is OverlayDraft.Subtitle -> target.copy(
                        timeRange = placeRange,
                        text = draft.text,
                        x = draft.x,
                        y = draft.y,
                        textSizeSp = draft.textSizeSp,
                        colorArgb = draft.colorArgb,
                    )
                    target is Overlay.Music && draft is OverlayDraft.Music -> target.copy(
                        timeRange = placeRange,
                        sourceUri = draft.sourceUri ?: target.sourceUri,
                        volumePercent = draft.volumePercent,
                    )
                    else -> null
                }
                if (updatedOverlay != null) {
                    val newList = currentTimeline.overlays.toMutableList()
                    newList[idx] = updatedOverlay
                    _state.value = _state.value.copy(
                        timeline = currentTimeline.copy(overlays = newList),
                        overlaySheet = null,
                        overlayDraft = null,
                    )
                    return
                }
            }
        }

        // Add new overlay
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
        _state.value = _state.value.copy(
            timeline = updated,
            overlaySheet = null,
            overlayDraft = null,
            selectedOverlayId = newOverlay.id,
        )
    }

    fun editOverlay(overlayId: String) {
        val currentTimeline = _state.value.timeline ?: return
        val target = currentTimeline.overlays.firstOrNull { it.id == overlayId } ?: return
        when (target) {
            is Overlay.Sticker -> {
                _state.value = _state.value.copy(
                    selectedOverlayId = overlayId,
                    overlaySheet = EditorOverlaySheet.Sticker,
                    overlayDraft = OverlayDraft.Sticker(
                        assetId = target.assetId,
                        x = target.x,
                        y = target.y,
                        scale = target.scale,
                        rotationDeg = target.rotationDeg,
                        startMs = target.timeRange.startMs.value,
                        durationMs = target.timeRange.endMs.value - target.timeRange.startMs.value,
                    ),
                )
            }
            is Overlay.Subtitle -> {
                _state.value = _state.value.copy(
                    selectedOverlayId = overlayId,
                    overlaySheet = EditorOverlaySheet.Subtitle,
                    overlayDraft = OverlayDraft.Subtitle(
                        text = target.text,
                        startMs = target.timeRange.startMs.value,
                        durationMs = target.timeRange.endMs.value - target.timeRange.startMs.value,
                        x = target.x,
                        y = target.y,
                        textSizeSp = target.textSizeSp,
                        colorArgb = target.colorArgb,
                    ),
                )
            }
            is Overlay.Music -> {
                _state.value = _state.value.copy(
                    selectedOverlayId = overlayId,
                    overlaySheet = EditorOverlaySheet.Music,
                    overlayDraft = OverlayDraft.Music(
                        volumePercent = target.volumePercent,
                        sourceUri = target.sourceUri,
                        startMs = target.timeRange.startMs.value,
                        durationMs = target.timeRange.endMs.value - target.timeRange.startMs.value,
                    ),
                )
            }
        }
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

    fun deleteSelectedOverlay() {
        val currentTimeline = _state.value.timeline ?: return
        val selected = _state.value.selectedOverlayId ?: return
        val newList = currentTimeline.overlays.filterNot { it.id == selected }
        _state.value = _state.value.copy(
            timeline = currentTimeline.copy(overlays = newList),
            overlaySheet = null,
            overlayDraft = null,
            selectedOverlayId = null,
        )
    }

    fun deleteOverlayById(overlayId: String) {
        val currentTimeline = _state.value.timeline ?: return
        val newList = currentTimeline.overlays.filterNot { it.id == overlayId }
        _state.value = _state.value.copy(timeline = currentTimeline.copy(overlays = newList))
        if (_state.value.selectedOverlayId == overlayId) {
            _state.value = _state.value.copy(overlaySheet = null, overlayDraft = null, selectedOverlayId = null)
        }
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
