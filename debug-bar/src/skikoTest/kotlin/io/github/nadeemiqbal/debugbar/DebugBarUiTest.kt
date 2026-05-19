package io.github.nadeemiqbal.debugbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.github.nadeemiqbal.debugbar.sections.CustomSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DebugBarUiTest {

    @Test
    fun whenDisabled_drawerNotInTree() = runComposeUiTest {
        val state = DebugBarState(initialOpen = true)
        setContent {
            MaterialTheme {
                DebugBar(
                    sections = listOf(CustomSection("Test") { Text("hello") }),
                    enabled = false,
                    state = state,
                ) {
                    Text("app content")
                }
            }
        }
        waitForIdle()
        // Disabled = pass-through. Root tag should not exist.
        onNodeWithTag("debug_bar_root").assertDoesNotExist()
    }

    @Test
    fun whenEnabled_rootIsInTreeAndClosedByDefault() = runComposeUiTest {
        val state = DebugBarState()
        setContent {
            MaterialTheme {
                DebugBar(
                    sections = listOf(CustomSection("Test") { Text("hello") }),
                    state = state,
                ) {
                    Text("app content")
                }
            }
        }
        waitForIdle()
        onNodeWithTag("debug_bar_root").assertIsDisplayed()
        onNodeWithTag("debug_bar_drawer", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun toggle_opensAndClosesDrawer() = runComposeUiTest {
        val state = DebugBarState()
        setContent {
            MaterialTheme {
                DebugBar(
                    sections = listOf(CustomSection("Test") { Text("section body") }),
                    state = state,
                ) {
                    Text("app content")
                }
            }
        }
        waitForIdle()
        assertFalse(state.isOpen.value)
        state.toggle()
        waitForIdle()
        assertTrue(state.isOpen.value)
    }

    @Test
    fun openSection_resolvesByTitleAndSelectsTab() = runComposeUiTest {
        val state = DebugBarState()
        setContent {
            MaterialTheme {
                DebugBar(
                    sections = listOf(
                        CustomSection("First") { Text("first") },
                        CustomSection("Second") { Text("second") },
                        CustomSection("Third") { Text("third") },
                    ),
                    state = state,
                ) {
                    Text("app content")
                }
            }
        }
        waitForIdle()
        state.openSection("Third")
        // The drawer's LaunchedEffect collects from a StateFlow; give it a moment to resolve.
        waitUntil(timeoutMillis = 3_000) { state.activeSectionIndex.value == 2 }
        // openSection sets pendingSectionTitle which is consumed once the drawer resolves it.
        assertEquals(2, state.activeSectionIndex.value)
        assertEquals(null, state.pendingSectionTitle.value)
        assertTrue(state.isOpen.value)
    }

    @Test
    fun closeButton_fires() = runComposeUiTest {
        val state = DebugBarState(initialOpen = true)
        setContent {
            MaterialTheme {
                DebugBar(
                    sections = listOf(CustomSection("Test") { Text("body") }),
                    state = state,
                ) {
                    Text("app content")
                }
            }
        }
        waitForIdle()
        onNodeWithTag("debug_bar_close", useUnmergedTree = true).performClick()
        waitForIdle()
        assertFalse(state.isOpen.value)
    }
}
