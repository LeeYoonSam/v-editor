package com.example.veditor.feature.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veditor.core.model.Overlay
import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.core.net.toUri
import kotlin.math.ceil
import kotlinx.coroutines.withContext

private const val SNAP_THRESHOLD_PX = 10f

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
        onAddSticker = presenter::onAddStickerClicked,
        onAddSubtitle = presenter::onAddSubtitleClicked,
        onAddMusic = presenter::onAddMusicClicked,
        onExport = presenter::onExportClicked,
        onImport = presenter::onImportRequested,
        onCloseSheet = presenter::onCloseSheet,
        onConfirmOverlay = presenter::confirmOverlay,
        onUpdateSticker = presenter::updateStickerDraft,
        onUpdateSubtitle = presenter::updateSubtitleDraft,
        onUpdateMusic = presenter::updateMusicDraft,
        onUpdateTime = presenter::updateOverlayTime,
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
        onJumpPrev = presenter::jumpToPrevious,
        onJumpNext = presenter::jumpToNext,
        onUpdateTimeForOverlay = { id, start, end ->
            val tlEnd = presenter.state.value.timeline?.clips?.lastOrNull()?.range?.endMs?.value ?: 0L
            val current = presenter.state.value.timeline?.overlays?.firstOrNull { it.id == id }
            val curStart = current?.timeRange?.startMs?.value ?: 0L
            presenter.updateOverlayTimeById(
                overlayId = id,
                startMs = start,
                durationMs = end?.let { e -> (e - curStart).coerceAtLeast(100L).coerceAtMost(tlEnd) },
            )
        },
        onEditOverlay = presenter::editOverlay,
        onDeleteSelectedOverlay = presenter::deleteSelectedOverlay,
        onTrimClipStart = presenter::trimClipStart,
        onTrimClipEnd = presenter::trimClipEnd,
        onAdjustTrimStartMs = presenter::adjustTrimStart,
        onAdjustTrimEndMs = presenter::adjustTrimEnd,
        onConfirmTrim = presenter::confirmTrimSelection,
        onChangeZoom = presenter::setZoomDpPerMs,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorContent(
    state: EditorState,
    onAddSticker: () -> Unit,
    onAddSubtitle: () -> Unit,
    onAddMusic: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onCloseSheet: () -> Unit,
    onConfirmOverlay: () -> Unit,
    onUpdateSticker: (assetId: String?, x: Float?, y: Float?, scale: Float?, rotationDeg: Float?) -> Unit,
    onUpdateSubtitle: (text: String) -> Unit,
    onUpdateMusic: (volumePercent: Int?, sourceUri: String?) -> Unit,
    // time controls
    onUpdateTime: (startMs: Long?, durationMs: Long?) -> Unit = { _, _ -> },
    onUpdateTimeForOverlay: (overlayId: String, startMs: Long?, endMs: Long?) -> Unit = { _, _, _ -> },
    // subtitle style/position
    onUpdateSubtitleStyle: (textSizeSp: Float?, colorArgb: Long?) -> Unit = { _, _ -> },
    onUpdateSubtitlePosition: (x: Float?, y: Float?) -> Unit = { _, _ -> },
    onEditOverlay: (overlayId: String) -> Unit = {},
    onDeleteSelectedOverlay: () -> Unit = {},
    onPlayPause: (Boolean) -> Unit = {},
    onSeek: (Long) -> Unit = {},
    onPlaybackTick: (Long) -> Unit = {},
    onPlaybackComplete: () -> Unit = {},
    onJumpPrev: () -> Unit = {},
    onJumpNext: () -> Unit = {},
    onTrimClipStart: (clipIndex: Int, newStartMs: Long) -> Unit = { _, _ -> },
    onTrimClipEnd: (clipIndex: Int, newEndMs: Long) -> Unit = { _, _ -> },
    onAdjustTrimStartMs: (Long) -> Unit = {},
    onAdjustTrimEndMs: (Long) -> Unit = {},
    onConfirmTrim: () -> Unit = {},
    onChangeZoom: (Float) -> Unit = {},
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallPlayPauseButton(isPlaying = state.isPlaying, onToggle = {
                        val end = state.timeline.clips.last().range.endMs.value
                        if (!state.isPlaying && state.currentPositionMs >= end) {
                            onSeek(0L)
                        }
                        onPlayPause(!state.isPlaying)
                    })
                    Box(modifier = Modifier.weight(1f).height(48.dp)) {
                        TimelineThumbnailsBar(
                            timeline = state.timeline,
                            currentPositionMs = state.currentPositionMs,
                            onSeek = { pos -> onSeek(pos) },
                            onScrubStart = { onPlayPause(false) },
                        )
                    }
                }
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
private fun ZoomSlider(value: Float, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Zoom")
        Slider(value = value, onValueChange = onChange, valueRange = 0.05f..1.0f)
        Text(String.format("%.2f dp/ms", value))
    }
}

@Composable
private fun TrimSelector(
    trimStartMs: Long,
    trimEndMs: Long,
    totalMs: Long,
    onLeftNudge: () -> Unit,
    onRightNudge: () -> Unit,
    onAdjustStartMs: (Long) -> Unit,
    onAdjustEndMs: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Arrow nudges
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onLeftNudge) { Icon(Icons.Filled.ChevronLeft, contentDescription = "왼쪽") }
            IconButton(onClick = onRightNudge) { Icon(Icons.Filled.ChevronRight, contentDescription = "오른쪽") }
        }
        // Range sliders (start/end)
        Text("시작: ${formatMs(trimStartMs)}  끝: ${formatMs(trimEndMs)}")
        Slider(
            value = (trimStartMs.toFloat() / totalMs).coerceIn(0f, 1f),
            onValueChange = { f ->
                val target = (f * totalMs).toFloat()
                onAdjustStartMs(target.toLong() - trimStartMs)
            },
            valueRange = 0f..(trimEndMs.toFloat() / totalMs).coerceIn(0.01f, 1f),
        )
        Slider(
            value = (trimEndMs.toFloat() / totalMs).coerceIn(0f, 1f),
            onValueChange = { f ->
                val target = (f * totalMs).toFloat()
                onAdjustEndMs(target.toLong() - trimEndMs)
            },
            valueRange = (trimStartMs.toFloat() / totalMs).coerceIn(0f, 0.99f)..1f,
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("선택 길이: ${formatMs(trimEndMs - trimStartMs)}")
            OutlinedButton(onClick = onConfirm) { Text("자르기") }
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
            LaunchedEffect(currentPositionMs) {
                player.seekTo(currentPositionMs)
            }
            LaunchedEffect(isPlaying) {
                while (isPlaying) {
                    onPlaybackTick(player.currentPosition)
                    delay(100)
                    val total = (player.duration.takeIf { it > 0 } ?: timeline.clips.last().range.endMs.value).toLong()
                    if (player.currentPosition >= total) {
                        onPlaybackComplete()
                        break
                    }
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
private fun VideoControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onPlayPause: (Boolean) -> Unit,
    onJumpPrev: () -> Unit,
    onJumpNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onJumpPrev) { Text("이전") }
        OutlinedButton(onClick = { onPlayPause(!isPlaying) }) { Text(if (isPlaying) "일시정지" else "재생") }
        OutlinedButton(onClick = onJumpNext) { Text("다음") }
        val pos = formatMs(currentPositionMs)
        val total = formatMs(totalDurationMs)
        Text("$pos / $total")
    }
}

@Composable
private fun SimplePlayControl(isPlaying: Boolean, onPlayPause: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onPlayPause(!isPlaying) }) {
            if (isPlaying) Icon(Icons.Filled.Pause, contentDescription = "일시정지")
            else Icon(Icons.Filled.PlayArrow, contentDescription = "재생")
        }
        // Thumbnail strip container will be placed by caller
    }
}

@Composable
private fun SmallPlayPauseButton(isPlaying: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onToggle, modifier = Modifier.width(40.dp).height(40.dp)) {
            if (isPlaying) Icon(Icons.Filled.Pause, contentDescription = "일시정지")
            else Icon(Icons.Filled.PlayArrow, contentDescription = "재생")
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun TimelineProgressBar(
    timeline: Timeline?,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
) {
    if (timeline == null) return
    val totalMs = timeline.clips.last().range.endMs.value
    val density = LocalDensity.current
    val widthDp = 300.dp
    val widthPx = with(density) { widthDp.toPx() }
    val playheadPx = (currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f) * widthPx

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .width(widthDp)
                .height(24.dp)
                .background(Color.White.copy(alpha = 0.06f))
                .pointerInput(totalMs, widthPx) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val x = (change.position.x).coerceIn(0f, widthPx)
                        val newPos = ((x / widthPx) * totalMs).toLong()
                        onSeek(newPos)
                    }
                },
        )
        Box(
            modifier = Modifier
                .padding(start = with(density) { playheadPx.toDp() })
                .width(2.dp)
                .height(24.dp)
                .background(Color.Red.copy(alpha = 0.9f)),
        )
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
    LaunchedEffect(videoUri, totalMs, stripWidthPx) {
        if (stripWidthPx <= 0) return@LaunchedEffect
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val mmr = android.media.MediaMetadataRetriever()
            try {
                mmr.setDataSource(context, videoUri.toUri())
                val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
                val totalSec = (totalMs / 1000f).coerceAtLeast(1f)
                val pxPerSec = effectivePx / totalSec
                val desiredSecondsPerThumb = 4f
                val targetPxPerThumb = (pxPerSec * desiredSecondsPerThumb).coerceIn(32f, 96f)
                val frameCount = ceil(effectivePx / targetPxPerThumb).toInt().coerceIn(1, 120)
                val frames = run {
                    val msPerFrame = totalMs.toFloat() / frameCount
                    val timestampsUs = (0 until frameCount).map { idx ->
                        val tsMs = (idx * msPerFrame)
                        (tsMs * 1000L).toLong().coerceIn(0L, totalMs * 1000L - 1_000L)
                    }
                    timestampsUs.mapNotNull { ts ->
                        val bmp = mmr.getFrameAtTime(ts, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                        bmp?.asImageBitmap()
                    }
                }
                thumbs = if (frames.isEmpty()) emptyList() else buildList {
                    addAll(frames)
                    while (size < frameCount) add(frames.last())
                }
            } finally {
                mmr.release()
            }
        }
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
                val pxPerSec = effectivePx / totalSec
                val desiredSecondsPerThumb = 4f
                val targetPxPerThumb = (pxPerSec * desiredSecondsPerThumb).coerceIn(32f, 96f)
                val placeholders = ceil(effectivePx / targetPxPerThumb).toInt().coerceAtLeast(1)
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
                    androidx.compose.foundation.Image(
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
                        onDrag = { change, _ ->
                        change.consume()
                        val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
                        val x = (change.position.x - sidePaddingPx).coerceIn(0f, effectivePx)
                        val raw = ((x / effectivePx) * totalMs).toLong()
                        onSeek(raw)
                    })
                },
        )
        val effectivePx = (stripWidthPx - (sidePaddingPx * 2)).coerceAtLeast(1f)
        val playheadPx = sidePaddingPx + ((currentPositionMs.toFloat() / totalMs).coerceIn(0f, 1f)) * effectivePx
        Box(
            modifier = Modifier
                .padding(start = with(density) { playheadPx.toDp() })
                .width(2.dp)
                .height(48.dp)
                .background(Color.Red.copy(alpha = 0.9f))
                .zIndex(1f),
        )
    }
}

@Composable
private fun ClipsTrack(
    timeline: Timeline?,
    onTrimStart: (clipIndex: Int, newStartMs: Long) -> Unit,
    onTrimEnd: (clipIndex: Int, newEndMs: Long) -> Unit,
    dpPerMs: Float,
    currentPositionMs: Long,
) {
    if (timeline == null) return
    val totalMs = timeline.clips.last().range.endMs.value
    val density = LocalDensity.current
    val timelineWidthDp = (totalMs * dpPerMs).dp
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .horizontalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Clips")
        Box(
            modifier = Modifier
                .width(timelineWidthDp)
                .height(36.dp)
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            val timelineWidthPx = with(density) { timelineWidthDp.toPx() }
            val boundaries = buildList {
                add(0L)
                timeline.clips.forEach { add(it.range.startMs.value); add(it.range.endMs.value) }
                timeline.overlays.forEach { add(it.timeRange.startMs.value); add(it.timeRange.endMs.value) }
            }.distinct().sorted()
            fun snapMsIfClose(candidateMs: Long): Long {
                val candidatePx = (candidateMs.toFloat() / totalMs) * timelineWidthPx
                // 플레이헤드와 경계에 스냅
                val targetsMs = boundaries + currentPositionMs
                var bestMs = candidateMs
                var bestDx = Float.MAX_VALUE
                targetsMs.forEach { tMs ->
                    val tx = (tMs.toFloat() / totalMs) * timelineWidthPx
                    val dx = abs(candidatePx - tx)
                    if (dx < bestDx) {
                        bestDx = dx
                        bestMs = tMs
                    }
                }
                return if (bestDx <= SNAP_THRESHOLD_PX) bestMs else candidateMs
            }

            timeline.clips.forEachIndexed { index, clip ->
                val startMs = clip.range.startMs.value
                val endMs = clip.range.endMs.value
                val startPx = (startMs.toFloat() / totalMs) * timelineWidthPx
                val endPx = (endMs.toFloat() / totalMs) * timelineWidthPx
                // Clip body
                Box(
                    modifier = Modifier
                        .padding(start = with(density) { startPx.toDp() })
                        .width(with(density) { (endPx - startPx).toDp() })
                        .height(24.dp)
                        .align(Alignment.CenterStart)
                        .background(Color(0xFF2A2A2A))
                        .border(1.dp, Color.White.copy(alpha = 0.06f)),
                )
                // Start handle
                Box(
                    modifier = Modifier
                        .padding(start = with(density) { (startPx - 12f).coerceAtLeast(0f).toDp() })
                        .width(24.dp)
                        .height(24.dp)
                        .align(Alignment.CenterStart)
                        .background(Color.Transparent)
                        .pointerInput(index, timelineWidthPx, startPx, endPx) {
                            var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                            detectDragGestures(
                                onDragStart = { accumulated = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    accumulated += drag.x
                                    val newStartPx = (startPx + accumulated).coerceIn(0f, endPx - 50f)
                                    val rawMs = ((newStartPx / timelineWidthPx) * totalMs).toLong()
                                    val snapped = snapMsIfClose(rawMs)
                                    onTrimStart(index, snapped)
                                },
                            )
                        },
                )
                // End handle
                Box(
                    modifier = Modifier
                        .padding(start = with(density) { (endPx - 12f).coerceAtLeast(0f).toDp() })
                        .width(24.dp)
                        .height(24.dp)
                        .align(Alignment.CenterStart)
                        .background(Color.Transparent)
                        .pointerInput(index, timelineWidthPx, startPx, endPx) {
                            var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                            detectDragGestures(
                                onDragStart = { accumulated = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    accumulated += drag.x
                                    val newEndPx = (endPx + accumulated).coerceIn(startPx + 50f, timelineWidthPx)
                                    val rawMs = ((newEndPx / timelineWidthPx) * totalMs).toLong()
                                    val snapped = snapMsIfClose(rawMs)
                                    onTrimEnd(index, snapped)
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun OverlayPalette(
    onAddSticker: () -> Unit,
    onAddSubtitle: () -> Unit,
    onAddMusic: () -> Unit,
    onExport: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onAddSticker, modifier = Modifier.testTag("btn_add_sticker")) { Text("Sticker") }
        Button(onClick = onAddSubtitle, modifier = Modifier.testTag("btn_add_subtitle")) { Text("Subtitle") }
        Button(onClick = onAddMusic, modifier = Modifier.testTag("btn_add_music")) { Text("Music") }
        Button(onClick = onExport, modifier = Modifier.testTag("btn_export")) { Text("Export") }
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

@Composable
private fun TimelineBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.Gray.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("Timeline")
    }
}

@Composable
private fun OverlayList(
    timeline: Timeline?,
    onDragStartHandle: (overlayId: String, newStartMs: Long) -> Unit,
    onDragEndHandle: (overlayId: String, newEndMs: Long) -> Unit,
    selectedOverlayId: String?,
    onClickOverlay: (overlayId: String) -> Unit,
    currentPositionMs: Long,
    dpPerMs: Float = 0.3f,
) {
    if (timeline == null) return
    val totalMs = timeline.clips.last().range.endMs.value
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val timelineWidthDp = (totalMs * dpPerMs).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Overlays")
        timeline.overlays.forEachIndexed { index, ov ->
            val label = when (ov) {
                is Overlay.Sticker -> "Sticker"
                is Overlay.Subtitle -> "Subtitle"
                is Overlay.Music -> "Music"
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Label column
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = Color(0xFF9E9E9E))
                }
                // Scrollable timeline area per overlay
                val isRowSelected = selectedOverlayId == ov.id
                val startMs = ov.timeRange.startMs.value
                val endMs = ov.timeRange.endMs.value
                val trackColor = Color(0xFF2A2A2A)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .horizontalScroll(scrollState, enabled = !isRowSelected)
                        .testTag("overlay_row_${ov.id}"),
                ) {
                    val timelineWidthPx = with(density) { timelineWidthDp.toPx() }
                    val boundaries = buildList {
                        add(0L)
                        timeline.clips.forEach { add(it.range.startMs.value); add(it.range.endMs.value) }
                        timeline.overlays.forEach { add(it.timeRange.startMs.value); add(it.timeRange.endMs.value) }
                    }.distinct().sorted()
                    fun snapMsIfClose(candidateMs: Long): Long {
                        val candidatePx = (candidateMs.toFloat() / totalMs) * timelineWidthPx
                        val targetsMs = boundaries + currentPositionMs
                        var bestMs = candidateMs
                        var bestDx = Float.MAX_VALUE
                        targetsMs.forEach { tMs ->
                            val tx = (tMs.toFloat() / totalMs) * timelineWidthPx
                            val dx = kotlin.math.abs(candidatePx - tx)
                            if (dx < bestDx) {
                                bestDx = dx
                                bestMs = tMs
                            }
                        }
                        return if (bestDx <= SNAP_THRESHOLD_PX) bestMs else candidateMs
                    }
                    val startPx = (startMs.toFloat() / totalMs) * timelineWidthPx
                    val endPx = (endMs.toFloat() / totalMs) * timelineWidthPx
                    // Base track line (subtle)
                    Box(
                        modifier = Modifier
                            .width(timelineWidthDp)
                            .height(2.dp)
                            .align(Alignment.CenterStart)
                            .background(Color.White.copy(alpha = 0.06f)),
                    )
                    // Red playhead line across tracks
                    val playheadPx = (currentPositionMs.toFloat() / totalMs) * timelineWidthPx
                    Box(
                        modifier = Modifier
                            .padding(start = with(density) { playheadPx.toDp() })
                            .width(2.dp)
                            .height(28.dp)
                            .align(Alignment.CenterStart)
                            .background(Color.Red.copy(alpha = 0.9f)),
                    )
                    // Range area (배경은 타입 색, 선택 시 노란 테두리)
                    val isSelected = isRowSelected
                    Box(
                        modifier = Modifier
                            .padding(start = with(density) { startPx.toDp() })
                            .width(with(density) { (endPx - startPx).toDp() })
                            .height(16.dp)
                            .align(Alignment.CenterStart)
                            .background(trackColor)
                            .then(if (isSelected) Modifier.border(2.dp, Color(0xFFFFD54F)) else Modifier)
                            .then(
                                if (isSelected) {
                                    Modifier.pointerInput(ov.id, timelineWidthPx, startPx, endPx) {
                                        var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                                        val widthPx = endPx - startPx
                                        detectDragGestures(
                                            onDragStart = { accumulated = 0f },
                                            onDrag = { change, drag ->
                                                change.consume()
                                                accumulated += drag.x
                                                val newStartPx = (startPx + accumulated).coerceIn(
                                                    0f,
                                                    timelineWidthPx - widthPx,
                                                )
                                                val newEndPx = (newStartPx + widthPx).coerceIn(widthPx, timelineWidthPx)
                                                val rawStartMs = ((newStartPx / timelineWidthPx) * totalMs).toLong()
                                                val rawEndMs = ((newEndPx / timelineWidthPx) * totalMs).toLong()
                                                onDragStartHandle(ov.id, snapMsIfClose(rawStartMs))
                                                onDragEndHandle(ov.id, snapMsIfClose(rawEndMs))
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .testTag("overlay_range_${ov.id}")
                            .testTag("overlay_range")
                            .then(if (index == 0) Modifier.testTag("overlay_range_first") else Modifier)
                            .clickable { onClickOverlay(ov.id) },
                    )
                    // Left handle (투명 핫스팟 + 누적 드래그) - 선택 시에만 활성화
                    Box(
                        modifier = Modifier
                            .padding(start = with(density) { (startPx - 12f).coerceAtLeast(0f).toDp() })
                            .width(24.dp)
                            .height(24.dp)
                            .align(Alignment.CenterStart)
                            .background(Color.Transparent)
                            .then(
                                if (isSelected) {
                                    Modifier.pointerInput(ov.id, timelineWidthPx, startPx, endPx) {
                                        var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                                        detectDragGestures(
                                            onDragStart = { accumulated = 0f },
                                            onDrag = { change, drag ->
                                                change.consume()
                                                accumulated += drag.x
                                                val newStartPx = (startPx + accumulated).coerceIn(0f, endPx - 8f)
                                                val rawMs = ((newStartPx / timelineWidthPx) * totalMs).toLong()
                                                onDragStartHandle(ov.id, snapMsIfClose(rawMs))
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .testTag("overlay_handle_start_${ov.id}")
                            .testTag("overlay_handle_start"),
                    )
                    // Right handle (투명 핫스팟 + 누적 드래그) - 선택 시에만 활성화
                    Box(
                        modifier = Modifier
                            .padding(start = with(density) { (endPx - 12f).coerceAtLeast(0f).toDp() })
                            .width(24.dp)
                            .height(24.dp)
                            .align(Alignment.CenterStart)
                            .background(Color.Transparent)
                            .then(
                                if (isSelected) {
                                    Modifier.pointerInput(ov.id, timelineWidthPx, startPx, endPx) {
                                        var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                                        detectDragGestures(
                                            onDragStart = { accumulated = 0f },
                                            onDrag = { change, drag ->
                                                change.consume()
                                                accumulated += drag.x
                                                val newEndPx = (endPx + accumulated).coerceIn(
                                                    startPx + 8f,
                                                    timelineWidthPx,
                                                )
                                                val rawMs = ((newEndPx / timelineWidthPx) * totalMs).toLong()
                                                onDragEndHandle(ov.id, snapMsIfClose(rawMs))
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .testTag("overlay_handle_end_${ov.id}")
                            .testTag("overlay_handle_end"),
                    )
                }
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
        onAddSticker = {},
        onAddSubtitle = {},
        onAddMusic = {},
        onExport = {},
        onImport = {},
        onCloseSheet = {},
        onConfirmOverlay = {},
        onUpdateSticker = { _, _, _, _, _ -> },
        onUpdateSubtitle = { _ -> },
        onUpdateMusic = { _, _ -> },
        onUpdateTime = { _, _ -> },
    )
}
