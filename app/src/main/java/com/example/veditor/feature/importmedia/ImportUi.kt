package com.example.veditor.feature.importmedia

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import com.example.veditor.core.media.DeviceVideo

@Composable
fun ImportUi(presenter: ImportPresenter, onConfirm: (selectedUris: List<String>) -> Unit, onClose: () -> Unit) {
    val state by presenter.state.collectAsState()
    ImportContent(
        state = state,
        onToggle = presenter::toggleSelection,
        onConfirm = { onConfirm(state.selectedUris.toList()) },
        onClose = onClose,
    )
}

@Composable
private fun ImportContent(state: ImportState, onToggle: (String) -> Unit, onConfirm: () -> Unit, onClose: () -> Unit) {
    Scaffold(
        topBar = { ImportTopBar(onClose = onClose) },
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            ImportBottomBar(
                selectedCount = state.selectedUris.size,
                onConfirm = onConfirm,
            )
        },
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.videos, key = { it.uri }) { video ->
                ImportGridItem(video = video, isSelected = video.uri in state.selectedUris) {
                    onToggle(video.uri)
                }
            }
        }
    }
}

@Composable
private fun ImportTopBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = "Import", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable { onClose() })
    }
}

@Composable
private fun ImportBottomBar(selectedCount: Int, onConfirm: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
    ) {
        Button(onClick = onConfirm, enabled = selectedCount > 0, modifier = Modifier.align(Alignment.Center)) {
            Text("가져오기($selectedCount)")
        }
    }
}

@Composable
private fun ImportGridItem(video: DeviceVideo, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = video.displayName, style = MaterialTheme.typography.bodyMedium)
    }
}
