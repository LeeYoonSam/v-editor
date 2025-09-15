package com.example.veditor.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.veditor.core.model.Overlay
import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip

@Composable
fun EditorUi(presenter: EditorPresenter) {
    val state by presenter.state.collectAsState()
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
    onUpdateMusic: (volumePercent: Int? , sourceUri: String?) -> Unit,
    // time controls
    onUpdateTime: (startMs: Long?, durationMs: Long?) -> Unit = { _, _ -> },
    onUpdateTimeForOverlay: (overlayId: String, startMs: Long?, endMs: Long?) -> Unit = { _, _, _ -> },
    // subtitle style/position
    onUpdateSubtitleStyle: (textSizeSp: Float?, colorArgb: Long?) -> Unit = { _, _ -> },
    onUpdateSubtitlePosition: (x: Float?, y: Float?) -> Unit = { _, _ -> },
    onEditOverlay: (overlayId: String) -> Unit = {},
    onDeleteSelectedOverlay: () -> Unit = {},
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PreviewArea(
                    timeline = state.timeline,
                    overlaySheet = state.overlaySheet,
                    overlayDraft = state.overlayDraft,
                    onDragSticker = { x, y -> onUpdateSticker(null, x, y, null, null) },
                    onDragSubtitle = { x, y -> onUpdateSubtitlePosition(x, y) },
                )
                OverlayPalette(
                    onAddSticker = onAddSticker,
                    onAddSubtitle = onAddSubtitle,
                    onAddMusic = onAddMusic,
                    onExport = onExport,
                )
                OverlayList(
                    timeline = state.timeline,
                    onDragStartHandle = { id, newStart -> onUpdateTimeForOverlay(id, newStart, null) },
                    onDragEndHandle = { id, newEnd -> onUpdateTimeForOverlay(id, null, newEnd) },
                    selectedOverlayId = state.selectedOverlayId,
                    onClickOverlay = { id -> onEditOverlay(id) },
                )
                
            }
        }

        when (state.overlaySheet) {
            is EditorOverlaySheet.Sticker -> {
                ModalBottomSheet(onDismissRequest = onCloseSheet) {
                    val draft = state.overlayDraft as? OverlayDraft.Sticker
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                        Text("Sticker settings")
                        StickerAssetGrid(onSelect = { id -> onUpdateSticker(id, null, null, null, null) })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCloseSheet) { Text("취소") }
                            TextButton(onClick = onConfirmOverlay) { Text("확인") }
                            if (state.selectedOverlayId != null) {
                                TextButton(onClick = onDeleteSelectedOverlay) { Text("삭제") }
                            }
                        }
                    }
                }
            }
            is EditorOverlaySheet.Subtitle -> {
                ModalBottomSheet(onDismissRequest = onCloseSheet) {
                    val draft = state.overlayDraft as? OverlayDraft.Subtitle
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                        Text("Subtitle settings")
                        TextField(value = draft?.text ?: "", onValueChange = onUpdateSubtitle, label = { Text("Text") })
                        Text("Color")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF).forEach { c ->
                                Box(modifier = Modifier
                                    .width(32.dp)
                                    .height(24.dp)
                                    .background(Color((c and 0xFFFFFFFF).toInt()))
                                    .clickable { onUpdateSubtitleStyle(null, c) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCloseSheet) { Text("취소") }
                            TextButton(onClick = onConfirmOverlay) { Text("확인") }
                            if (state.selectedOverlayId != null) {
                                TextButton(onClick = onDeleteSelectedOverlay) { Text("삭제") }
                            }
                        }
                    }
                }
            }
            is EditorOverlaySheet.Music -> {
                ModalBottomSheet(onDismissRequest = onCloseSheet) {
                    val draft = state.overlayDraft as? OverlayDraft.Music
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                        Text("Music settings")
                        Text("Volume: ${draft?.volumePercent ?: 100}")
                        Slider(
                            value = (draft?.volumePercent ?: 100).toFloat(),
                            onValueChange = { onUpdateMusic(it.toInt(), null) },
                            valueRange = 0f..100f,
                        )
                        val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                            if (uri != null) onUpdateMusic(null, uri.toString())
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { picker.launch("audio/*") }) { Text("오디오 선택") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCloseSheet) { Text("취소") }
                            TextButton(onClick = onConfirmOverlay) { Text("확인") }
                            if (state.selectedOverlayId != null) {
                                TextButton(onClick = onDeleteSelectedOverlay) { Text("삭제") }
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
        Text(text = "Preview", color = Color.White)

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
                            .pointerInput(boxWidthPx, boxHeightPx, d.x, d.y) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newX = clamp01(((posX + dragAmount.x) / boxWidthPx).toFloat())
                                    val newY = clamp01(((posY + dragAmount.y) / boxHeightPx).toFloat())
                                    onDragSticker(newX, newY)
                                }
                            }
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
                        Icon(icon, contentDescription = o.assetId, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                }
                is Overlay.Subtitle -> {
                    val px = (o.x.coerceIn(0f, 1f) * boxWidthPx).toInt()
                    val py = (o.y.coerceIn(0f, 1f) * boxHeightPx).toInt()
                    Text(
                        text = o.text,
                        color = Color((o.colorArgb and 0xFFFFFFFF).toInt()),
                        fontSize = o.textSizeSp.sp,
                        modifier = Modifier.padding(start = with(density) { px.toDp() }, top = with(density) { py.toDp() }),
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
        Button(onClick = onAddSticker) { Text("Sticker") }
        Button(onClick = onAddSubtitle) { Text("Subtitle") }
        Button(onClick = onAddMusic) { Text("Music") }
        Button(onClick = onExport) { Text("Export") }
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
) {
    if (timeline == null) return
    val totalMs = timeline.clips.last().range.endMs.value
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    // Scale: 1s = 300dp → dpPerMs = 0.3f (조작 용이성 향상)
    val dpPerMs = 0.3f
    val timelineWidthDp = (totalMs * dpPerMs).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Overlays")
        timeline.overlays.forEach { ov ->
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
                        .horizontalScroll(scrollState, enabled = !isRowSelected),
                ) {
                    val timelineWidthPx = with(density) { timelineWidthDp.toPx() }
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
                                if (isSelected) Modifier.pointerInput(ov.id, timelineWidthPx, startPx, endPx) {
                                    var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                                    val widthPx = endPx - startPx
                                    detectDragGestures(
                                        onDragStart = { accumulated = 0f },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            accumulated += drag.x
                                            val newStartPx = (startPx + accumulated).coerceIn(0f, timelineWidthPx - widthPx)
                                            val newEndPx = (newStartPx + widthPx).coerceIn(widthPx, timelineWidthPx)
                                            val newStartMs = ((newStartPx / timelineWidthPx) * totalMs).toLong()
                                            val newEndMs = ((newEndPx / timelineWidthPx) * totalMs).toLong()
                                            onDragStartHandle(ov.id, newStartMs)
                                            onDragEndHandle(ov.id, newEndMs)
                                        },
                                    )
                                } else Modifier
                            )
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
                                if (isSelected) Modifier.pointerInput(ov.id, timelineWidthPx, startPx, endPx) {
                                    var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                                    detectDragGestures(
                                        onDragStart = { accumulated = 0f },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            accumulated += drag.x
                                            val newStartPx = (startPx + accumulated).coerceIn(0f, endPx - 8f)
                                            val newStartMs = ((newStartPx / timelineWidthPx) * totalMs).toLong()
                                            onDragStartHandle(ov.id, newStartMs)
                                        },
                                    )
                                } else Modifier
                            ),
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
                                if (isSelected) Modifier.pointerInput(ov.id, timelineWidthPx, startPx, endPx) {
                                    var accumulated by androidx.compose.runtime.mutableStateOf(0f)
                                    detectDragGestures(
        								onDragStart = { accumulated = 0f },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            accumulated += drag.x
                                            val newEndPx = (endPx + accumulated).coerceIn(startPx + 8f, timelineWidthPx)
                                            val newEndMs = ((newEndPx / timelineWidthPx) * totalMs).toLong()
                                            onDragEndHandle(ov.id, newEndMs)
                                        },
                                    )
                                } else Modifier
                            ),
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
