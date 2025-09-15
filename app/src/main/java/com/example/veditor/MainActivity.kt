package com.example.veditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.veditor.core.domain.BuildTimelineFromSelectionUseCase
import com.example.veditor.core.domain.GetDeviceVideosUseCase
import com.example.veditor.core.media.DeviceVideo
import com.example.veditor.core.media.FakeMediaRepository
import com.example.veditor.core.model.TimeMs
import com.example.veditor.feature.editor.EditorPresenter
import com.example.veditor.feature.editor.EditorUi
import com.example.veditor.feature.home.HomePresenter
import com.example.veditor.feature.home.HomeState
import com.example.veditor.feature.home.HomeUi
import com.example.veditor.feature.importmedia.ImportPresenter
import com.example.veditor.feature.importmedia.ImportUi
import com.example.veditor.feature.navigation.EditorScreen
import com.example.veditor.feature.navigation.HomeScreen
import com.example.veditor.feature.navigation.ImportScreen
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.rememberCircuitNavigator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }
}

@Composable
private fun App() {
    val repo = FakeMediaRepository(
        listOf(
            DeviceVideo("content://1", "샘플1", TimeMs(1_000)),
            DeviceVideo("content://2", "샘플2", TimeMs(2_000)),
            DeviceVideo("content://3", "샘플3", TimeMs(3_000)),
        ),
    )

    val backStack = rememberSaveableBackStack(root = HomeScreen)
    val navigator = rememberCircuitNavigator(backStack)

    when (val screen = backStack.topRecord?.screen) {
        is HomeScreen -> {
            val presenter = HomePresenter(GetDeviceVideosUseCase(repo))
            val state: HomeState by presenter.state.collectAsState()
            HomeUi(state = state, onCreateNewVideo = { navigator.goTo(ImportScreen) })
        }
        is ImportScreen -> {
            val presenter = ImportPresenter(GetDeviceVideosUseCase(repo))
            ImportUi(
                presenter = presenter,
                onConfirm = { selected -> navigator.goTo(EditorScreen(selectedUris = ArrayList(selected))) },
                onClose = { navigator.pop() },
            )
        }
        is EditorScreen -> {
            val timeline = BuildTimelineFromSelectionUseCase(defaultClipDurationMs = 1_000)(screen.selectedUris)
            val presenter = EditorPresenter(initialTimeline = timeline)
            EditorUi(presenter = presenter)
        }
        else -> {}
    }
}
