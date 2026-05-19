package io.github.nadeemiqbal.debugbar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugBarStateTest {

    @Test
    fun freshState_isClosedAtIndexZero() {
        val state = DebugBarState()
        assertFalse(state.isOpen.value)
        assertEquals(0, state.activeSectionIndex.value)
    }

    @Test
    fun open_setsIsOpenTrue() {
        val state = DebugBarState()
        state.open()
        assertTrue(state.isOpen.value)
    }

    @Test
    fun close_setsIsOpenFalse() {
        val state = DebugBarState(initialOpen = true)
        state.close()
        assertFalse(state.isOpen.value)
    }

    @Test
    fun toggle_flipsState() {
        val state = DebugBarState()
        state.toggle()
        assertTrue(state.isOpen.value)
        state.toggle()
        assertFalse(state.isOpen.value)
    }

    @Test
    fun selectSection_storesIndex() {
        val state = DebugBarState()
        state.selectSection(3)
        assertEquals(3, state.activeSectionIndex.value)
    }

    @Test
    fun selectSection_clampsNegativeToZero() {
        val state = DebugBarState()
        state.selectSection(-5)
        assertEquals(0, state.activeSectionIndex.value)
    }

    @Test
    fun openSection_setsPendingTitleAndOpens() {
        val state = DebugBarState()
        state.openSection("Feature Flags")
        assertTrue(state.isOpen.value)
        assertEquals("Feature Flags", state.pendingSectionTitle.value)
    }
}
