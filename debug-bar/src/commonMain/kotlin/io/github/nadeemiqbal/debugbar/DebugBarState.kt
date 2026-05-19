package io.github.nadeemiqbal.debugbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State container for [DebugBar]. Holds drawer open/closed state and the currently-active section
 * tab. Use [rememberDebugBarState] from a composable, or construct directly for tests.
 */
class DebugBarState(
    initialOpen: Boolean = false,
    initialSectionIndex: Int = 0,
) {

    private val isOpenState = MutableStateFlow(initialOpen)

    /** Reactive flag — `true` while the drawer is visible. */
    val isOpen: StateFlow<Boolean> = isOpenState.asStateFlow()

    private val activeSectionIndexState = MutableStateFlow(initialSectionIndex)

    /** Index of the currently-selected section tab. */
    val activeSectionIndex: StateFlow<Int> = activeSectionIndexState.asStateFlow()

    private val pendingSectionTitleState = MutableStateFlow<String?>(null)

    /**
     * Reactive — set by [openSection], consumed by DebugBar's `LaunchedEffect` which resolves
     * the title to an index. A `StateFlow` (not a plain var) so the drawer's collector wakes up
     * when the host calls `openSection(...)` after composition has already settled.
     */
    val pendingSectionTitle: StateFlow<String?> = pendingSectionTitleState.asStateFlow()

    /** Clear the pending title once the drawer has resolved it. */
    internal fun consumePendingSectionTitle() {
        pendingSectionTitleState.value = null
    }

    /** Imperative — open the drawer. */
    fun open() {
        isOpenState.value = true
    }

    /** Imperative — close the drawer. */
    fun close() {
        isOpenState.value = false
    }

    /** Imperative — toggle visibility. */
    fun toggle() {
        isOpenState.update { !it }
    }

    /**
     * Select a section tab by index. Out-of-bounds is silently clamped — useful when the host's
     * `sections` list is shorter than expected.
     */
    fun selectSection(index: Int) {
        activeSectionIndexState.value = index.coerceAtLeast(0)
    }

    /**
     * Select a section by title (case-insensitive). Useful for jumping to a specific tab from a
     * remote trigger or test (`state.openSection("Feature Flags")`).
     */
    fun openSection(title: String) {
        // Index resolution happens inside DebugBar where the section list is known; we just stash
        // the intent here via a reactive flow that DebugBar's collector picks up.
        pendingSectionTitleState.value = title
        open()
    }
}

/**
 * Creates and remembers a [DebugBarState]. Calling [DebugBarState.toggle] from anywhere with
 * a reference to the same state will open/close the drawer.
 */
@Composable
fun rememberDebugBarState(
    initialOpen: Boolean = false,
    initialSectionIndex: Int = 0,
): DebugBarState = remember { DebugBarState(initialOpen, initialSectionIndex) }
