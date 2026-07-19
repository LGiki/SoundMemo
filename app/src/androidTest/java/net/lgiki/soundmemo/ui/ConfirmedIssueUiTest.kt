package net.lgiki.soundmemo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.data.model.RecordingSort
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.ui.library.SortRow
import net.lgiki.soundmemo.ui.recorder.AudioInputLine
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfirmedIssueUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sortRow_exposesLongestAndShortest() {
        var selected = RecordingSort.Newest
        composeRule.setContent {
            MaterialTheme {
                SortRow(sort = selected, onSort = { selected = it })
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.sort_longest)).performClick()
        assertEquals(RecordingSort.Longest, selected)
        composeRule.onNodeWithText(context.getString(R.string.sort_shortest)).assertExists()
    }

    @Test
    fun inactiveAudioInput_isButtonWithMaterialTouchTarget() {
        composeRule.setContent {
            MaterialTheme {
                AudioInputLine(
                    status = RecorderStatus.Idle,
                    preferredAudioInput = null,
                    actualAudioInput = null,
                    onPreferredAudioInputClick = {},
                )
            }
        }

        composeRule.onNode(hasClickAction())
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }
}
