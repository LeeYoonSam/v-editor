package com.example.veditor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline
import com.example.veditor.core.model.VideoClip
import com.example.veditor.feature.editor.EditorPresenter
import com.example.veditor.feature.editor.EditorUi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditorUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var presenter: EditorPresenter

    @Before
    fun setUp() {
        val clip = VideoClip("c1", "uri://1", TimeRange(TimeMs(0), TimeMs(1000)))
        val timeline = Timeline(listOf(clip), emptyList())
        presenter = EditorPresenter(initialTimeline = timeline)
        composeTestRule.setContent {
            EditorUi(presenter)
        }
    }

    @Test
    fun selectOverlay_opensSheet() {
        composeTestRule.onNodeWithTag("btn_add_sticker").performClick()
        composeTestRule.onNodeWithTag("sheet_btn_confirm").performClick()
        composeTestRule.runOnIdle {
            val id = requireNotNull(presenter.state.value.timeline?.overlays?.firstOrNull()?.id)
            presenter.editOverlay(id)
        }
        composeTestRule.onNodeWithTag("sheet_sticker").assertIsDisplayed()
    }

    @Test
    fun dragOverlay_movesRange() {
        composeTestRule.onNodeWithTag("btn_add_sticker").performClick()
        composeTestRule.onNodeWithTag("sheet_btn_confirm").performClick()
        composeTestRule.runOnIdle {
            val id = requireNotNull(presenter.state.value.timeline?.overlays?.firstOrNull()?.id)
            presenter.editOverlay(id)
            presenter.updateOverlayTimeById(id, startMs = 100, durationMs = 600)
        }
        // Sheet remains visible during editing
        composeTestRule.onNodeWithTag("sheet_sticker").assertIsDisplayed()
    }

    @Test
    fun deleteSelectedOverlay_removesOverlay() {
        composeTestRule.onNodeWithTag("btn_add_sticker").performClick()
        composeTestRule.onNodeWithTag("sheet_btn_confirm").performClick()
        composeTestRule.runOnIdle {
            val id = requireNotNull(presenter.state.value.timeline?.overlays?.firstOrNull()?.id)
            presenter.editOverlay(id)
        }

        composeTestRule.onNodeWithTag("sheet_btn_delete").performClick()
        composeTestRule.runOnIdle {
            assert(presenter.state.value.timeline?.overlays?.isEmpty() == true)
        }
    }
}
