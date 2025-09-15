package com.example.veditor.feature.editor

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
                PreviewArea()
                OverlayPalette(
                    onAddSticker = onAddSticker,
                    onAddSubtitle = onAddSubtitle,
                    onAddMusic = onAddMusic,
                    onExport = onExport,
                )
                TimelineRangeEditor(
                    timeline = state.timeline,
                    overlayDraft = state.overlayDraft,
                    onUpdateTime = onUpdateTime,
                )
            }
        }

        when (state.overlaySheet) {
            is EditorOverlaySheet.Sticker -> {
                ModalBottomSheet(onDismissRequest = onCloseSheet) {
                    val draft = state.overlayDraft as? OverlayDraft.Sticker
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                        Text("Sticker settings")
                        Text("X")
                        Slider(value = draft?.x ?: 0.5f, onValueChange = { onUpdateSticker(null, it, null, null, null) }, valueRange = 0f..1f)
                        Text("Y")
                        Slider(value = draft?.y ?: 0.5f, onValueChange = { onUpdateSticker(null, null, it, null, null) }, valueRange = 0f..1f)
                        Text("Scale")
                        Slider(value = draft?.scale ?: 1f, onValueChange = { onUpdateSticker(null, null, null, it, null) }, valueRange = 0.5f..2f)
                        Text("Rotation")
                        Slider(value = draft?.rotationDeg ?: 0f, onValueChange = { onUpdateSticker(null, null, null, null, it) }, valueRange = -180f..180f)
                        Text("Start (ms): ${draft?.startMs ?: 0}")
                        Slider(value = (draft?.startMs ?: 0).toFloat(), onValueChange = { onUpdateTime(it.toLong(), null) }, valueRange = 0f..(state.timeline?.clips?.lastOrNull()?.range?.endMs?.value?.toFloat() ?: 0f))
                        Text("Duration (ms): ${draft?.durationMs ?: 1000}")
                        Slider(value = (draft?.durationMs ?: 1000).toFloat(), onValueChange = { onUpdateTime(null, it.toLong()) }, valueRange = 100f..5_000f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCloseSheet) { Text("취소") }
                            TextButton(onClick = onConfirmOverlay) { Text("확인") }
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
                        Text("Start (ms): ${draft?.startMs ?: 0}")
                        Slider(value = (draft?.startMs ?: 0).toFloat(), onValueChange = { onUpdateTime(it.toLong(), null) }, valueRange = 0f..(state.timeline?.clips?.lastOrNull()?.range?.endMs?.value?.toFloat() ?: 0f))
                        Text("Duration (ms): ${draft?.durationMs ?: 1000}")
                        Slider(value = (draft?.durationMs ?: 1000).toFloat(), onValueChange = { onUpdateTime(null, it.toLong()) }, valueRange = 100f..5_000f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCloseSheet) { Text("취소") }
                            TextButton(onClick = onConfirmOverlay) { Text("확인") }
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
                        Text("Start (ms): ${draft?.startMs ?: 0}")
                        Slider(value = (draft?.startMs ?: 0).toFloat(), onValueChange = { onUpdateTime(it.toLong(), null) }, valueRange = 0f..(state.timeline?.clips?.lastOrNull()?.range?.endMs?.value?.toFloat() ?: 0f))
                        Text("Duration (ms): ${draft?.durationMs ?: 1000}")
                        Slider(value = (draft?.durationMs ?: 1000).toFloat(), onValueChange = { onUpdateTime(null, it.toLong()) }, valueRange = 100f..5_000f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onCloseSheet) { Text("취소") }
                            TextButton(onClick = onConfirmOverlay) { Text("확인") }
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
private fun PreviewArea() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Preview", color = Color.White)
    }
}

@Composable
private fun OverlayPalette(
    onAddSticker: () -> Unit,
    onAddSubtitle: () -> Unit,
    onAddMusic: () -> Unit,
    onExport: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onAddSticker) { Text("Sticker") }
        Button(onClick = onAddSubtitle) { Text("Subtitle") }
        Button(onClick = onAddMusic) { Text("Music") }
        Button(onClick = onExport) { Text("Export") }
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
private fun TimelineRangeEditor(
    timeline: Timeline?,
    overlayDraft: OverlayDraft?,
    onUpdateTime: (startMs: Long?, durationMs: Long?) -> Unit,
) {
    if (timeline == null) return
    val totalMs = timeline.clips.last().range.endMs.value
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0) }
    val height = 64.dp
    val handleWidth = 12.dp

    fun msToPx(ms: Long): Float {
        if (totalMs <= 0) return 0f
        return (ms.toFloat() / totalMs.toFloat()) * widthPx
    }
    fun pxToMs(px: Float): Long {
        if (widthPx <= 0) return 0L
        val ratio = (px / widthPx).coerceIn(0f, 1f)
        return (totalMs * ratio).toLong()
    }

    val startMs = when (overlayDraft) {
        is OverlayDraft.Sticker -> overlayDraft.startMs
        is OverlayDraft.Subtitle -> overlayDraft.startMs
        is OverlayDraft.Music -> overlayDraft.startMs
        null -> 0L
    }
    val durationMs = when (overlayDraft) {
        is OverlayDraft.Sticker -> overlayDraft.durationMs
        is OverlayDraft.Subtitle -> overlayDraft.durationMs
        is OverlayDraft.Music -> overlayDraft.durationMs
        null -> 1_000L
    }
    val endMs = (startMs + durationMs).coerceAtMost(totalMs)

    val startPx = msToPx(startMs)
    val endPx = msToPx(endMs)
    val contentWidthPx = widthPx.toFloat().coerceAtLeast(0f)
    val handleWidthPx = with(density) { handleWidth.toPx() }
    val maxStartX = (contentWidthPx - handleWidthPx).coerceAtLeast(0f)
    val leftX = startPx.coerceIn(0f, maxStartX)
    val rightX = (endPx - handleWidthPx).coerceIn(0f, maxStartX)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color.LightGray.copy(alpha = 0.3f))
            .onSizeChanged { widthPx = it.width },
    ) {
        // Selection area (visual)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
                .background(Color.Transparent)
        )

        // Left handle
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(handleWidth)
                .padding(start = with(density) { leftX.toDp() })
                .background(Color.Red.copy(alpha = 0.7f))
                .pointerInput(totalMs, widthPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val upper = (endPx - 10f).coerceAtLeast(0f)
                        val newStartPx = (startPx + dragAmount.x).coerceIn(0f, upper)
                        val newStartMs = pxToMs(newStartPx)
                        val newDuration = (endMs - newStartMs).coerceAtLeast(100L)
                        onUpdateTime(newStartMs, newDuration)
                    }
                },
        )

        // Right handle
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(handleWidth)
                .padding(start = with(density) { rightX.toDp() })
                .background(Color.Blue.copy(alpha = 0.7f))
                .pointerInput(totalMs, widthPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val lower = (startPx + 10f).coerceAtLeast(0f)
                        val newEndPx = (endPx + dragAmount.x).coerceIn(lower, contentWidthPx)
                        val newEndMs = pxToMs(newEndPx)
                        val newDuration = (newEndMs - startMs).coerceAtLeast(100L)
                        onUpdateTime(null, newDuration)
                    }
                },
        )
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
