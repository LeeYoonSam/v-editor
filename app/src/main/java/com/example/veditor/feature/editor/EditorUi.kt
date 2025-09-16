package com.example.veditor.feature.editor

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.veditor.core.model.Overlay
import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

// 간단한 메모리 썸네일 캐시 (바이트 수 기준)
private object ThumbMemoryCache {
    private val lru = object : LruCache<String, Bitmap>(8 * 1024 * 1024) { // 8MB
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    fun get(key: String): Bitmap? = lru.get(key)
    fun put(key: String, value: Bitmap) { lru.put(key, value) }
}

@Composable
fun EditorUi(presenter: EditorPresenter) {
    val state by presenter.state.collectAsState()
    val context = LocalContext.current

    // Ensure timeline clip durations match real media durations
    LaunchedEffect(state.timeline?.clips?.map { it.sourceUri }) {
        val tl = state.timeline ?: return@LaunchedEffect
        if (tl.clips.isEmpty()) return@LaunchedEffect
        val measured: List<Long> = withContext(kotlinx.coroutines.Dispatchers.IO) {
            tl.clips.map { clip ->
                val mmr = android.media.MediaMetadataRetriever()
                try {
                    mmr.setDataSource(context, clip.sourceUri.toUri())
                    val durMs = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    durMs ?: (clip.range.endMs.value - clip.range.startMs.value)
                } finally {
                    mmr.release()
                }
            }
        }
        // Rebuild sequential ranges using measured durations
        var start = 0L
        val rebuilt = tl.clips.mapIndexed { index, clip ->
            val d = measured.getOrNull(index) ?: (clip.range.endMs.value - clip.range.startMs.value)
            val newClip = VideoClip(
                id = clip.id,
                sourceUri = clip.sourceUri,
                range = TimeRange(TimeMs(start), TimeMs(start + d)),
            )
            start += d
            newClip
        }
        val oldTotal = tl.clips.last().range.endMs.value
        val newTotal = rebuilt.last().range.endMs.value
        if (newTotal != oldTotal) {
            presenter.setTimeline(tl.copy(clips = rebuilt))
        }
    }
    EditorContent(
        state = state,
        onImport = presenter::onImportRequested,
        onCloseSheet = presenter::onCloseSheet,
        onConfirmOverlay = presenter::confirmOverlay,
        onUpdateSticker = presenter::updateStickerDraft,
        onUpdateSubtitle = presenter::updateSubtitleDraft,
        onUpdateMusic = presenter::updateMusicDraft,
        onUpdateSubtitleStyle = presenter::updateSubtitleStyle,
        onUpdateSubtitlePosition = presenter::updateSubtitlePosition,
        onPlayPause = presenter::setPlaying,
        onSeek = presenter::seekTo,
        onPlaybackTick = presenter::onPlaybackPosition,
        onPlaybackComplete = {
            val end = presenter.state.value.timeline?.clips?.lastOrNull()?.range?.endMs?.value ?: 0L
            presenter.seekTo(end)
            presenter.setPlaying(false)
        },
        onDeleteSelectedOverlay = presenter::deleteSelectedOverlay,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorContent(
    state: EditorState,
    onImport: () -> Unit,
    onCloseSheet: () -> Unit,
    onConfirmOverlay: () -> Unit,
    onUpdateSticker: (assetId: String?, x: Float?, y: Float?, scale: Float?, rotationDeg: Float?) -> Unit,
    onUpdateSubtitle: (text: String) -> Unit,
    onUpdateMusic: (volumePercent: Int?, sourceUri: String?) -> Unit,
    onUpdateSubtitleStyle: (textSizeSp: Float?, colorArgb: Long?) -> Unit = { _, _ -> },
    onUpdateSubtitlePosition: (x: Float?, y: Float?) -> Unit = { _, _ -> },
    onDeleteSelectedOverlay: () -> Unit = {},
    onPlayPause: (Boolean) -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onPlaybackTick: (Long) -> Unit = {},
    onPlaybackComplete: () -> Unit = {},
) {
    Scaffold(
        topBar = { EditorTopBar() },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        if (state.timeline == null) {
            EmptyEditor(
                onImport = onImport,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            val vScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(vScroll),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewArea(
                    timeline = state.timeline,
                    overlaySheet = state.overlaySheet,
                    overlayDraft = state.overlayDraft,
                    isPlaying = state.isPlaying,
                    currentPositionMs = state.currentPositionMs,
                    onPlaybackTick = onPlaybackTick,
                    onPlaybackComplete = onPlaybackComplete,
                    onDragSticker = { x, y -> onUpdateSticker(null, x, y, null, null) },
                    onDragSubtitle = { x, y -> onUpdateSubtitlePosition(x, y) },
                )
                TransportStrip(
                    isPlaying = state.isPlaying,
                    onToggle = {
                        val end = state.timeline.clips.last().range.endMs.value
                        if (!state.isPlaying && state.currentPositionMs >= end) {
                            onSeek(0L)
                        }
                        onPlayPause(!state.isPlaying)
                    },
                    timeline = state.timeline,
                    currentPositionMs = state.currentPositionMs,
                    onSeek = onSeek,
                    onScrubStart = { onPlayPause(false) },
                )
            }
        }

        when (state.overlaySheet) {
            is EditorOverlaySheet.Sticker -> {
                ModalBottomSheet(onDismissRequest = onCloseSheet) {
                    val draft = state.overlayDraft as? OverlayDraft.Sticker
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp).testTag("sheet_sticker"),
                    ) {
                        Text("Sticker settings")
                        StickerAssetGrid(onSelect = { id -> onUpdateSticker(id, null, null, null, null) })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onCloseSheet,
                                modifier = Modifier.testTag("sheet_btn_cancel"),
                            ) { Text("취소") }
                            TextButton(
                                onClick = onConfirmOverlay,
                                modifier = Modifier.testTag("sheet_btn_confirm"),
                            ) { Text("확인") }
                            if (state.selectedOverlayId != null) {
                                TextButton(
                                    onClick = onDeleteSelectedOverlay,
                                    modifier = Modifier.testTag("sheet_btn_delete"),
                                ) { Text("삭제") }
                            }
                        }
                    }
                }
            }
            is EditorOverlaySheet.Subtitle -> {
                ModalBottomSheet(onDismissRequest = onCloseSheet) {
                    val draft = state.overlayDraft as? OverlayDraft.Subtitle
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp).testTag("sheet_subtitle"),
                    ) {
                        Text("Subtitle settings")
                        TextField(value = draft?.text ?: "", onValueChange = onUpdateSubtitle, label = { Text("Text") })
                        Text("Color")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF).forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(24.dp)
                                        .background(Color((c and 0xFFFFFFFF).toInt()))
                                        .clickable { onUpdateSubtitleStyle(null, c) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onCloseSheet,
                                modifier = Modifier.testTag("sheet_btn_cancel"),
                            ) { Text("취소") }
                            TextButton(
                                onClick = onConfirmOverlay,
                                modifier = Modifier.testTag("sheet_btn_confirm"),
                            ) { Text("확인") }
                            if (state.selectedOverlayId != null) {
                                TextButton(
                                    onClick = onDeleteSelectedOverlay,
                                    modifier = Modifier.testTag("sheet_btn_delete"),
                                ) { Text("삭제") }
                            }
                        }
                    }
                }
            }
            is EditorOverlaySheet.Music -> {
                ModalBottomSheet(onDismissRequest = onCloseSheet) {
                    val draft = state.overlayDraft as? OverlayDraft.Music
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp).testTag("sheet_music"),
                    ) {
                        Text("Music settings")
                        Text("Volume: ${draft?.volumePercent ?: 100}")
                        Slider(
                            value = (draft?.volumePercent ?: 100).toFloat(),
                            onValueChange = { onUpdateMusic(it.toInt(), null) },
                            valueRange = 0f..100f,
                        )
                        val picker =
                            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                                if (uri != null) onUpdateMusic(null, uri.toString())
                            }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { picker.launch("audio/*") }) { Text("오디오 선택") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onCloseSheet,
                                modifier = Modifier.testTag("sheet_btn_cancel"),
                            ) { Text("취소") }
                            TextButton(
                                onClick = onConfirmOverlay,
                                modifier = Modifier.testTag("sheet_btn_confirm"),
                            ) { Text("확인") }
                            if (state.selectedOverlayId != null) {
                                TextButton(
                                    onClick = onDeleteSelectedOverlay,
                                    modifier = Modifier.testTag("sheet_btn_delete"),
                                ) { Text("삭제") }
                            }
                        }
                    }
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun EditorTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Editor", style = MaterialTheme.typography.titleLarge)
        Text(text = "MVP", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun EmptyEditor(onImport: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "프로젝트가 비어 있어요", style = MaterialTheme.typography.titleMedium)
        Text(text = "동영상을 가져와 타임라인을 시작하세요", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onImport, modifier = Modifier.padding(top = 16.dp)) { Text("동영상 가져오기") }
    }
}

@Composable
private fun PreviewArea(
    timeline: Timeline?,
    overlaySheet: EditorOverlaySheet?,
    overlayDraft: OverlayDraft?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onPlaybackTick: (Long) -> Unit,
    onPlaybackComplete: () -> Unit = {},
    onDragSticker: (x: Float, y: Float) -> Unit,
    onDragSubtitle: (x: Float, y: Float) -> Unit,
) {
    var boxWidthPx by remember { mutableStateOf(0) }
    var boxHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    fun clamp01(v: Float) = v.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Black.copy(alpha = 0.8f))
            .onSizeChanged { size ->
                boxWidthPx = size.width
                boxHeightPx = size.height
            },
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val player = remember {
            ExoPlayer.Builder(context).build()
        }
        DisposableEffect(Unit) {
            onDispose { player.release() }
        }
        val firstClip = timeline?.clips?.firstOrNull()
        if (firstClip != null) {
            LaunchedEffect(firstClip.sourceUri) {
                player.setMediaItem(MediaItem.fromUri(firstClip.sourceUri))
                player.prepare()
            }
            LaunchedEffect(isPlaying) {
                player.playWhenReady = isPlaying
            }
            LaunchedEffect(currentPositionMs, isPlaying) {
                // 재생 중이라도 큰 점프(사용자 시킹/처음으로 이동)일 때는 시킹 허용
                val diff = abs(player.currentPosition - currentPositionMs)
                if (!isPlaying || diff > 200) {
                    player.seekTo(currentPositionMs)
                }
            }
            LaunchedEffect(isPlaying) {
                while (isPlaying) {
                    val pos = player.currentPosition
                    onPlaybackTick(pos)
                    val total = (player.duration.takeIf { it > 0 } ?: timeline.clips.last().range.endMs.value).toLong()
                    if (pos >= total) {
                        onPlaybackComplete()
                        break
                    }
                    // 30fps 정도의 UI 업데이트 간격(33ms)로 줄여 반응성 개선
                    delay(33)
                }
            }
            AndroidView(factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                }
            }, modifier = Modifier.fillMaxWidth().height(200.dp))
        } else {
            Text(text = "Preview", color = Color.White)
        }

        when (overlaySheet) {
            is EditorOverlaySheet.Sticker -> {
                val d = overlayDraft as? OverlayDraft.Sticker
                if (d != null && boxWidthPx > 0 && boxHeightPx > 0) {
                    val posX = d.x * boxWidthPx
                    val posY = d.y * boxHeightPx
                    Box(
                        modifier = Modifier
                            .padding(0.dp)
                            .height(48.dp)
                            .width(48.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                            .testTag("preview_sticker_draft")
                            .pointerInput(boxWidthPx, boxHeightPx, d.x, d.y) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newX = clamp01(((posX + dragAmount.x) / boxWidthPx).toFloat())
                                    val newY = clamp01(((posY + dragAmount.y) / boxHeightPx).toFloat())
                                    onDragSticker(newX, newY)
                                }
                            },
                    )
                }
            }
            is EditorOverlaySheet.Subtitle -> {
                val d = overlayDraft as? OverlayDraft.Subtitle
                if (d != null && boxWidthPx > 0 && boxHeightPx > 0) {
                    val posX = d.x * boxWidthPx
                    val posY = d.y * boxHeightPx
                    Text(
                        text = if (d.text.isBlank()) "Aa" else d.text,
                        color = Color((d.colorArgb and 0xFFFFFFFF).toInt()),
                        modifier = Modifier
                            .padding(0.dp)
                            .testTag("preview_subtitle_draft")
                            .pointerInput(boxWidthPx, boxHeightPx, d.x, d.y) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newX = clamp01(((posX + dragAmount.x) / boxWidthPx).toFloat())
                                    val newY = clamp01(((posY + dragAmount.y) / boxHeightPx).toFloat())
                                    onDragSubtitle(newX, newY)
                                }
                            },
                    )
                }
            }
            else -> Unit
        }

        // Render confirmed overlays from timeline for visual feedback after confirm
        val overlays = timeline?.overlays.orEmpty()
        overlays.forEach { o ->
            when (o) {
                is Overlay.Sticker -> {
                    val px = (o.x.coerceIn(0f, 1f) * boxWidthPx).toInt()
                    val py = (o.y.coerceIn(0f, 1f) * boxHeightPx).toInt()
                    Box(
                        modifier = Modifier
                            .padding(start = with(density) { px.toDp() }, top = with(density) { py.toDp() })
                            .width(32.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.15f)),
                    ) {
                        // Icon mapping fallback
                        val icon: ImageVector = when (o.assetId) {
                            "star" -> Icons.Filled.Star
                            "heart" -> Icons.Filled.Favorite
                            "face" -> Icons.Filled.Face
                            "pet" -> Icons.Filled.Pets
                            "party" -> Icons.Filled.Celebration
                            else -> Icons.Filled.Star
                        }
                        Icon(
                            icon,
                            contentDescription = o.assetId,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                is Overlay.Subtitle -> {
                    val px = (o.x.coerceIn(0f, 1f) * boxWidthPx).toInt()
                    val py = (o.y.coerceIn(0f, 1f) * boxHeightPx).toInt()
                    Text(
                        text = o.text,
                        color = Color((o.colorArgb and 0xFFFFFFFF).toInt()),
                        fontSize = o.textSizeSp.sp,
                        modifier = Modifier.padding(
                            start = with(density) { px.toDp() },
                            top = with(density) { py.toDp() },
                        ),
                    )
                }
                is Overlay.Music -> {
                    // Music overlays are non-visual in preview for now
                }
            }
        }
    }
}

@Composable
private fun TransportStrip(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    timeline: Timeline?,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFBDBDBD).copy(alpha = 0.6f)),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left control pad
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(Color(0xFFBDBDBD))
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onToggle) {
                if (isPlaying) Icon(Icons.Filled.Pause, contentDescription = "일시정지", tint = Color.White)
                else Icon(Icons.Filled.PlayArrow, contentDescription = "재생", tint = Color.White)
            }
        }
        // Divider
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.6f)))
        // Thumbnail strip area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            TimelineThumbnailsBar(
                timeline = timeline,
                currentPositionMs = currentPositionMs,
                onSeek = onSeek,
                onScrubStart = onScrubStart,
            )
        }
    }
}

@Composable
private fun TimelineThumbnailsBar(
    timeline: Timeline?,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit = {},
) {
    if (timeline == null) return
    val totalMs = timeline.clips.last().range.endMs.value
    val density = LocalDensity.current
    val context = LocalContext.current
    var stripWidthPx by remember { mutableIntStateOf(0) }
    val sidePaddingDp = 10.dp
    val sidePaddingPx = with(density) { sidePaddingDp.toPx() }

    val videoUri = timeline.clips.first().sourceUri
    // Precompute frames once per (uri, width, duration)
    var thumbs by remember(videoUri, stripWidthPx, totalMs) { mutableStateOf<List<androidx.compose.ui.graphics.ImageBitmap>>(emptyList()) }
    // 프레임 추출 중 과도한 CPU 사용을 피하기 위한 가드
    var generating by remember(videoUri, stripWidthPx, totalMs) { mutableStateOf(false) }
    LaunchedEffect(videoUri, totalMs, stripWidthPx) {
        if (stripWidthPx <= 0 || generating) return@LaunchedEffect
        generating = true
        val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
        val totalSec = (totalMs / 1000f).coerceAtLeast(1f)
        // 최종 프레임 수와 목표 폭을 명시적으로 계산: 초당 1프레임 기준
        val finalFrameCount = ceil(totalSec).toInt().coerceAtLeast(1)
        val finalThumbWidthPx = (effectivePx / finalFrameCount).coerceAtLeast(1f)

        suspend fun extractFrames(frameCount: Int, option: Int, scaleW: Int): List<androidx.compose.ui.graphics.ImageBitmap> =
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val mmr = android.media.MediaMetadataRetriever()
                try {
                    mmr.setDataSource(context, videoUri.toUri())
                    val msPerFrame = totalMs.toFloat() / frameCount
                    val list = ArrayList<androidx.compose.ui.graphics.ImageBitmap>(frameCount)
                    for (idx in 0 until frameCount) {
                        val tsMs = (idx * msPerFrame)
                        val tsUs = (tsMs * 1000L).toLong().coerceIn(0L, totalMs * 1000L - 1_000L)
                        val key = "${videoUri}|${totalMs}|${scaleW}|${tsUs}"
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

        // 1차: 최종 썸네일 폭의 1/4 크기로 빠른 표시
        val fastScaleW = (finalThumbWidthPx / 4f).roundToInt().coerceAtLeast(16)
        val fast = extractFrames(finalFrameCount, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC, fastScaleW)
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            thumbs = fast
        }

        // 2차: 최종 폭으로 정밀 교체
        val preciseScaleW = finalThumbWidthPx.roundToInt().coerceAtLeast(fastScaleW)
        val precise = extractFrames(finalFrameCount, android.media.MediaMetadataRetriever.OPTION_CLOSEST, preciseScaleW)
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            if (precise.isNotEmpty()) thumbs = precise
        }

        generating = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { stripWidthPx = it.width },
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = sidePaddingDp)) {
            if (thumbs.isEmpty()) {
                val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
                val totalSec = (totalMs / 1000f).coerceAtLeast(1f)
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
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            }
        }
        // Seek by tap/drag within the strip (no scroll)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(totalMs, stripWidthPx) {
                    detectDragGestures(
                        onDragStart = { onScrubStart() },
                        onDragEnd = {
                            // 드래그 종료 후에도 비디오가 끝나있지 않다면 그대로 유지
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
                            val x = (change.position.x - sidePaddingPx).coerceIn(0f, effectivePx)
                            val raw = ((x / effectivePx) * totalMs).toLong()
                            onSeek(raw)
                        },
                    )
                },
        )
        val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
        val playheadPx = sidePaddingPx + ((currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f)) * effectivePx
        Box(
            modifier = Modifier
                .padding(start = with(density) { playheadPx.toDp() })
                .width(6.dp)
                .height(52.dp)
                .background(Color.White)
                .zIndex(1f),
        )
    }
}

@Composable
private fun StickerAssetGrid(onSelect: (assetId: String) -> Unit) {
    val assets: List<Pair<String, ImageVector>> = listOf(
        "star" to Icons.Filled.Star,
        "heart" to Icons.Filled.Favorite,
        "face" to Icons.Filled.Face,
        "pet" to Icons.Filled.Pets,
        "party" to Icons.Filled.Celebration,
    )
    LazyVerticalGrid(columns = GridCells.Adaptive(64.dp), modifier = Modifier.height(120.dp)) {
        items(assets, key = { it.first }) { (id, icon) ->
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .width(48.dp)
                    .height(48.dp)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onSelect(id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = id, tint = Color.White)
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun EditorPreview_Empty() {
    val presenter = EditorPresenter()
    EditorUi(presenter = presenter)
}

@Preview(showSystemUi = true)
@Composable
private fun EditorPreview_Timeline() {
    val presenter = EditorPresenter()
    // 미리보기용으로 타임라인 상태를 가짜로 구성
    val clip = VideoClip("c1", "content://1", TimeRange(TimeMs(0), TimeMs(1_000)))
    val timeline = Timeline(listOf(clip), emptyList())
    EditorContent(
        state = EditorState(timeline = timeline),
        onImport = {},
        onCloseSheet = {},
        onConfirmOverlay = {},
        onUpdateSticker = { _, _, _, _, _ -> },
        onUpdateSubtitle = { _ -> },
        onUpdateMusic = { _, _ -> },
    )
}
