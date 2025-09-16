package com.example.veditor

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
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
import com.example.veditor.feature.navigation.EditorScreen
import com.example.veditor.feature.navigation.HomeScreen
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

    val context = LocalContext.current
    // Photo Picker: multiple video selection
    val pickVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                val asStrings = ArrayList(uris.map { it.toString() })
                navigator.goTo(EditorScreen(selectedUris = asStrings))
            }
        },
    )
    // Fallback: SAF GetMultipleContents for videos
    val pickVideosFallback = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                val asStrings = ArrayList(uris.map { it.toString() })
                navigator.goTo(EditorScreen(selectedUris = asStrings))
            }
        },
    )

    when (val screen = backStack.topRecord?.screen) {
        is HomeScreen -> {
            val presenter = HomePresenter(GetDeviceVideosUseCase(repo))
            val state: HomeState by presenter.state.collectAsState()
            HomeUi(
                state = state,
                onCreateNewVideo = {
                    if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                        pickVideosLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                        )
                    } else {
                        pickVideosFallback.launch("video/*")
                    }
                },
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
