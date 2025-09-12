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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.veditor.core.domain.GetDeviceVideosUseCase
import com.example.veditor.core.media.DeviceVideo
import com.example.veditor.core.media.FakeMediaRepository
import com.example.veditor.core.model.TimeMs
import com.example.veditor.feature.editor.EditorPresenter
import com.example.veditor.feature.editor.EditorUi
import com.example.veditor.feature.home.HomePresenter
import com.example.veditor.feature.home.HomeState
import com.example.veditor.feature.home.HomeUi

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
    var showEditor by rememberSaveable { mutableStateOf(false) }

    if (showEditor) {
        val presenter = EditorPresenter()
        EditorUi(presenter = presenter)
        return
    }

    val repo = FakeMediaRepository(
        listOf(
            DeviceVideo("content://1", "샘플1", TimeMs(1_000)),
            DeviceVideo("content://2", "샘플2", TimeMs(2_000)),
        ),
    )
    val presenter = HomePresenter(GetDeviceVideosUseCase(repo))
    val state: HomeState by presenter.state.collectAsState()
    HomeUi(state = state, onCreateNewVideo = { showEditor = true })
}
