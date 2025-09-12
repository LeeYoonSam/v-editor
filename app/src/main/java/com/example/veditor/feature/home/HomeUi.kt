package com.example.veditor.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.veditor.core.media.DeviceVideo
import com.example.veditor.core.model.TimeMs

data class HomeState(
    val videos: List<DeviceVideo>,
)

@Composable
fun HomeUi(state: HomeState, onCreateNewVideo: () -> Unit) {
    Scaffold(
        topBar = {
            SimpleAppBar(title = "V-Editor", onAddClick = onCreateNewVideo)
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNewVideo) {
                Icon(Icons.Filled.Add, contentDescription = "새 비디오")
            }
        },
    ) { innerPadding ->
        VideoList(
            videos = state.videos,
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        )
    }
}
@Composable
private fun SimpleAppBar(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = "새 비디오")
        }
    }
}


@Composable
private fun VideoList(
    videos: List<DeviceVideo>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacingSmall),
    ) {
        items(videos, key = { it.uri }) { video ->
            VideoItem(video)
        }
    }
}

@Composable
private fun VideoItem(video: DeviceVideo) {
    Column {
        Text(text = video.displayName, style = MaterialTheme.typography.titleMedium)
        Text(text = "${video.durationMs.value} ms", style = MaterialTheme.typography.bodySmall)
    }
}

private val MaterialTheme.spacingSmall
    get() = 8.dp

@Preview(showSystemUi = true)
@Composable
private fun HomeUiPreview() {
    HomeUi(
        state = HomeState(
            videos = listOf(
                DeviceVideo("content://1", "샘플1", TimeMs(1_000)),
                DeviceVideo("content://2", "샘플2", TimeMs(2_000)),
            ),
        ),
        onCreateNewVideo = {},
    )
}


