package com.example.veditor.feature.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.example.veditor.core.model.Timeline
import com.example.veditor.R
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun TimelineStrip(
    timeline: Timeline?,
    currentPositionMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    viewportStartMs: Long,
    viewportEndMs: Long,
    isEditing: Boolean,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit = {},
    onUpdateTrim: (startMs: Long?, endMs: Long?, moveByMs: Long?) -> Unit,
    onEnterTrimEdit: () -> Unit,
    onUpdateViewport: (startMs: Long?, endMs: Long?, moveByMs: Long?) -> Unit = { _, _, _ -> },
) {
    if (timeline == null) return
    val totalMs = timeline.clips.last().range.endMs.value
    val density = LocalDensity.current
    val context = LocalContext.current
    var stripWidthPx by remember { mutableIntStateOf(0) }
    val sidePaddingDp = 10.dp
    val sidePaddingPx = with(density) { sidePaddingDp.toPx() }

    val videoUri = timeline.clips.first().sourceUri

    val displayStartMs = if (viewportEndMs > viewportStartMs) viewportStartMs else 0L
    val displayTotalMs = if (viewportEndMs > viewportStartMs) (viewportEndMs - viewportStartMs) else totalMs

    var thumbs by remember(videoUri, stripWidthPx, displayTotalMs, displayStartMs) { mutableStateOf<List<androidx.compose.ui.graphics.ImageBitmap>>(emptyList()) }
    var generating by remember(videoUri, stripWidthPx, displayTotalMs, displayStartMs) { mutableStateOf(false) }
    LaunchedEffect(videoUri, displayTotalMs, stripWidthPx, displayStartMs) {
        if (stripWidthPx <= 0 || generating) return@LaunchedEffect
        generating = true
        val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
        val totalSec = (displayTotalMs / 1000f).coerceAtLeast(1f)
        val finalFrameCount = ceil(totalSec).toInt().coerceAtLeast(1)
        val finalThumbWidthPx = (effectivePx / finalFrameCount).coerceAtLeast(1f)

        suspend fun extractFrames(frameCount: Int, option: Int, scaleW: Int): List<androidx.compose.ui.graphics.ImageBitmap> =
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val mmr = android.media.MediaMetadataRetriever()
                try {
                    mmr.setDataSource(context, videoUri.toUri())
                    val msPerFrame = displayTotalMs.toFloat() / frameCount
                    val list = ArrayList<androidx.compose.ui.graphics.ImageBitmap>(frameCount)
                    for (idx in 0 until frameCount) {
                        val tsMs = (idx * msPerFrame) + displayStartMs
                        val tsUs = (tsMs * 1000L).toLong().coerceAtLeast(0L)
                        val key = "${videoUri}|${displayStartMs}|${displayTotalMs}|${scaleW}|${tsUs}"
                        val cached = ThumbMemoryCache.get(key)
                        if (cached != null) {
                            list.add(cached.asImageBitmap())
                            continue
                        }
                        val raw = mmr.getFrameAtTime(tsUs, option) ?: continue
                        val targetH = (48f * scaleW / finalThumbWidthPx).roundToInt().coerceAtLeast(24)
                        val scaled = if (raw.width > scaleW) raw.scale(scaleW, targetH) else raw
                        ThumbMemoryCache.put(key, scaled)
                        list.add(scaled.asImageBitmap())
                    }
                    list
                } finally {
                    mmr.release()
                }
            }

        val fastScaleW = (finalThumbWidthPx / 4f).roundToInt().coerceAtLeast(16)
        val fast = extractFrames(finalFrameCount, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, fastScaleW)
        thumbs = fast

        val preciseScaleW = finalThumbWidthPx.roundToInt().coerceAtLeast(fastScaleW)
        val precise = extractFrames(finalFrameCount, android.media.MediaMetadataRetriever.OPTION_CLOSEST, preciseScaleW)
        if (precise.isNotEmpty()) thumbs = precise

        generating = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { stripWidthPx = it.width },
    ) {
        // When editing, draw a subtle border to highlight the strip area
        if (isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color.Transparent)
                    .zIndex(0.25f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = sidePaddingDp)) {
            if (thumbs.isEmpty()) {
                val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
                val totalSec = (displayTotalMs / 1000f).coerceAtLeast(1f)
                val frameCount = ceil(totalSec).toInt().coerceAtLeast(1)
                val placeholders = frameCount
                repeat(placeholders) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Color.DarkGray.copy(alpha = 0.3f)),
                    )
                }
            } else {
                thumbs.forEach { img ->
                    Image(
                        bitmap = img,
                        contentDescription = null,
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }

        // Scrub layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .zIndex(0.5f)
                .pointerInput(displayTotalMs, stripWidthPx, displayStartMs) {
                    detectDragGestures(
                        onDragStart = { onScrubStart() },
                        onDrag = { change, _ ->
                            change.consume()
                            val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
                            val x = (change.position.x - sidePaddingPx).coerceIn(0f, effectivePx)
                            val withinViewport = ((x / effectivePx) * displayTotalMs).toLong()
                            val absolute = withinViewport + displayStartMs
                            onSeek(absolute)
                            onEnterTrimEdit()
                        },
                    )
                },
        )

        val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
        fun timeToPxAbs(t: Long): Float = sidePaddingPx + ((t.toFloat() / totalMs).coerceIn(0f, 1f)) * effectivePx
        fun timeToPxViewport(t: Long): Float = sidePaddingPx + ((t.toFloat() / displayTotalMs).coerceIn(0f, 1f)) * effectivePx

        // Playhead (absolute relative to full timeline)
        val playheadPx = timeToPxAbs(currentPositionMs)
        Box(
            modifier = Modifier
                .padding(start = with(density) { playheadPx.toDp() })
                .width(6.dp)
                .height(52.dp)
                .background(Color.White)
                .zIndex(1f),
        )

        // Trim selection overlay (absolute before cut)
        val selStartPx = timeToPxAbs(trimStartMs)
        val selEndPx = timeToPxAbs(trimEndMs)
        val selWidthPx = (selEndPx - selStartPx).coerceAtLeast(1f)
        val accent = Color(0xFFFFD54F)
        // Dim non-selected regions when editing
        if (isEditing) {
            // Left dim
            val leftWidth = (selStartPx - sidePaddingPx).coerceAtLeast(0f)
            if (leftWidth > 0f) {
                Box(
                    modifier = Modifier
                        .padding(start = with(density) { sidePaddingPx.toDp() })
                        .width(with(density) { leftWidth.toDp() })
                        .height(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .zIndex(1.5f),
                )
            }
            // Right dim
            val rightWidth = (sidePaddingPx + effectivePx - selEndPx).coerceAtLeast(0f)
            if (rightWidth > 0f) {
                Box(
                    modifier = Modifier
                        .padding(start = with(density) { selEndPx.toDp() })
                        .width(with(density) { rightWidth.toDp() })
                        .height(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .zIndex(1.5f),
                )
            }
        }
        // selection fill (light accent background)
        Box(
            modifier = Modifier
                .padding(start = with(density) { selStartPx.toDp() })
                .width(with(density) { selWidthPx.toDp() })
                .height(48.dp)
                .background(Color(0xFFFFD54F).copy(alpha = 0.18f))
                .zIndex(1.8f),
        )
        // selection box
        var wholeAccumMs by remember(displayTotalMs, stripWidthPx, trimStartMs, trimEndMs) { mutableStateOf(0f) }
        val msPerPixelViewport = (displayTotalMs / effectivePx).coerceAtLeast(1f)
        val emitThresholdMs = (msPerPixelViewport * 2f).coerceAtLeast(5f) // 최소 5ms, 보통 2px 수준
        Box(
            modifier = Modifier
                .padding(start = with(density) { selStartPx.toDp() })
                .width(with(density) { selWidthPx.toDp() })
                .height(48.dp)
                .background(Color.Transparent)
                .zIndex(2f)
                .pointerInput(trimStartMs, trimEndMs, totalMs, stripWidthPx) {
                    // drag whole selection
                    detectDragGestures { change, drag ->
                        change.consume()
                        wholeAccumMs += (drag.x / effectivePx) * displayTotalMs
                        if (kotlin.math.abs(wholeAccumMs) >= emitThresholdMs) {
                            val step = wholeAccumMs.toLong()
                            wholeAccumMs -= step.toFloat()
                            if (step != 0L) onUpdateTrim(null, null, step)
                        }
                        onEnterTrimEdit()
                    }
                },
        )
        // selection border (top + bottom lines)
        Box(
            modifier = Modifier
                .padding(start = with(density) { selStartPx.toDp() })
                .width(with(density) { selWidthPx.toDp() })
                .height(2.dp)
                .background(accent)
                .zIndex(2f),
        )
        Box(
            modifier = Modifier
                .padding(start = with(density) { selStartPx.toDp() })
                .width(with(density) { selWidthPx.toDp() })
                .padding(top = 46.dp)
                .height(2.dp)
                .background(accent)
                .zIndex(2f),
        )
        // selection border (left/right lines)
        Box(
            modifier = Modifier
                .padding(start = with(density) { selStartPx.toDp() })
                .width(2.dp)
                .height(48.dp)
                .background(accent)
                .zIndex(2f),
        )
        Box(
            modifier = Modifier
                .padding(start = with(density) { (selEndPx - 2f).coerceAtLeast(0f).toDp() })
                .width(2.dp)
                .height(48.dp)
                .background(accent)
                .zIndex(2f),
        )
        // Left handle
        var leftAccumMs by remember(displayTotalMs, stripWidthPx, trimStartMs) { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .padding(start = with(density) { (selStartPx - 12f).coerceAtLeast(0f).toDp() })
                .width(24.dp)
                .height(48.dp)
                .background(Color.Transparent)
                .zIndex(3f)
                .pointerInput(trimStartMs, totalMs, stripWidthPx) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        leftAccumMs += (drag.x / effectivePx) * displayTotalMs
                        if (kotlin.math.abs(leftAccumMs) >= emitThresholdMs) {
                            val step = leftAccumMs.toLong()
                            leftAccumMs -= step.toFloat()
                            val newStart = (trimStartMs + step)
                            onUpdateTrim(newStart, null, null)
                        }
                        onEnterTrimEdit()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_left),
                contentDescription = "트림 시작 조절",
                modifier = Modifier.width(18.dp).height(18.dp),
                contentScale = ContentScale.Fit,
            )
        }
        // Right handle
        var rightAccumMs by remember(displayTotalMs, stripWidthPx, trimEndMs) { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .padding(start = with(density) { (selEndPx - 12f).coerceAtLeast(0f).toDp() })
                .width(24.dp)
                .height(48.dp)
                .background(Color.Transparent)
                .zIndex(3f)
                .pointerInput(trimEndMs, totalMs, stripWidthPx) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        rightAccumMs += (drag.x / effectivePx) * displayTotalMs
                        if (kotlin.math.abs(rightAccumMs) >= emitThresholdMs) {
                            val step = rightAccumMs.toLong()
                            rightAccumMs -= step.toFloat()
                            val newEnd = (trimEndMs + step)
                            onUpdateTrim(null, newEnd, null)
                        }
                        onEnterTrimEdit()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "트림 끝 조절",
                modifier = Modifier.width(18.dp).height(18.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}


