package com.example.veditor.feature.editor

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    )
}

@Composable
private fun EditorContent(
    state: EditorState,
    onAddSticker: () -> Unit,
    onAddSubtitle: () -> Unit,
    onAddMusic: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
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
                TimelineBar()
            }
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
    )
}
