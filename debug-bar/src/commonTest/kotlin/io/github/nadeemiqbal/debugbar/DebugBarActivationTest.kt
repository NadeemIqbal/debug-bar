package io.github.nadeemiqbal.debugbar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugBarActivationTest {

    @Test
    fun longPressCorner_hasSensibleDefaults() {
        val activation = DebugBarActivation.LongPressCorner()
        assertEquals(DebugBarActivation.Corner.TopRight, activation.corner)
        assertEquals(1000L, activation.holdMillis)
    }

    @Test
    fun keyboardShortcut_hasDefaultLabel() {
        val activation = DebugBarActivation.KeyboardShortcut()
        assertEquals("Cmd+Shift+D", activation.shortcut)
    }

    @Test
    fun keyboardShortcut_defaultMatcher_acceptsCmdShiftD() {
        val activation = DebugBarActivation.KeyboardShortcut()
        // Args are (key, isMeta, isCtrl, isShift) — function-type params can't be named.
        assertTrue(activation.matchesShortcut("D", true, false, true))
        assertTrue(activation.matchesShortcut("d", true, false, true))
        assertTrue(activation.matchesShortcut("D", false, true, true))
    }

    @Test
    fun keyboardShortcut_defaultMatcher_rejectsOtherCombos() {
        val activation = DebugBarActivation.KeyboardShortcut()
        // missing shift
        assertEquals(false, activation.matchesShortcut("D", true, false, false))
        // wrong key
        assertEquals(false, activation.matchesShortcut("X", true, false, true))
        // no modifier
        assertEquals(false, activation.matchesShortcut("D", false, false, true))
    }

    @Test
    fun plus_combinesTwoActivations() {
        val combined = DebugBarActivation.LongPressCorner() + DebugBarActivation.KeyboardShortcut()
        assertEquals(2, combined.activations.size)
    }

    @Test
    fun plus_flattensNestedCombines() {
        val a = DebugBarActivation.LongPressCorner()
        val b = DebugBarActivation.KeyboardShortcut()
        val c = DebugBarActivation.Programmatic
        val combined = (a + b) + c
        assertEquals(3, combined.activations.size)
    }

    @Test
    fun plus_flattensWhenSecondIsCombine() {
        val a = DebugBarActivation.LongPressCorner()
        val b = DebugBarActivation.KeyboardShortcut()
        val c = DebugBarActivation.Programmatic
        val combined = a + (b + c)
        assertEquals(3, combined.activations.size)
    }

    @Test
    fun combineCompanionInvoke_buildsCombine() {
        val combined = DebugBarActivation.Combine(
            DebugBarActivation.LongPressCorner(),
            DebugBarActivation.KeyboardShortcut(),
        )
        assertEquals(2, combined.activations.size)
    }
}
