package io.github.nadeemiqbal.debugbar

/**
 * How the user opens the debug drawer. Multiple activation methods can be combined with [Combine]
 * so the drawer opens on ANY of them (e.g. shake on mobile + Cmd+Shift+D on desktop).
 *
 * The host can also open the drawer programmatically by calling
 * [DebugBarState.toggle] / [DebugBarState.open] / [DebugBarState.close] regardless of which
 * activation is configured.
 */
sealed class DebugBarActivation {

    /**
     * Long-press the top-right corner of the screen (default 1 second). Works on every CMP target
     * because it uses Compose pointer input rather than platform-specific gesture APIs.
     */
    data class LongPressCorner(
        val corner: Corner = Corner.TopRight,
        val holdMillis: Long = 1000L,
    ) : DebugBarActivation()

    /**
     * A keyboard shortcut — primarily for Desktop, but also works on Android tablets / iPads with
     * a hardware keyboard. Default `Cmd+Shift+D` on Apple platforms, `Ctrl+Shift+D` elsewhere.
     *
     * [shortcut] is a free-form display label; the actual key matching is governed by
     * [matchesShortcut].
     */
    data class KeyboardShortcut(
        val shortcut: String = "Cmd+Shift+D",
        val matchesShortcut: (key: String, isMeta: Boolean, isCtrl: Boolean, isShift: Boolean) -> Boolean = { key, isMeta, isCtrl, isShift ->
            key.equals("D", ignoreCase = true) && (isMeta || isCtrl) && isShift
        },
    ) : DebugBarActivation()

    /**
     * No activation gesture — the drawer is opened programmatically via [DebugBarState.toggle].
     * Use this when you want to wire a hidden in-app button (e.g. tap the app version 7 times in
     * Settings) or a remote trigger.
     */
    data object Programmatic : DebugBarActivation()

    /**
     * Activate on ANY of the listed activations.
     */
    data class Combine(val activations: List<DebugBarActivation>) : DebugBarActivation() {
        companion object {
            operator fun invoke(vararg activations: DebugBarActivation): Combine = Combine(activations.toList())
        }
    }

    /** Operator overload: `LongPressCorner() + KeyboardShortcut()`. */
    operator fun plus(other: DebugBarActivation): Combine = when {
        this is Combine && other is Combine -> Combine(activations + other.activations)
        this is Combine -> Combine(activations + other)
        other is Combine -> Combine(listOf(this) + other.activations)
        else -> Combine(listOf(this, other))
    }

    enum class Corner { TopLeft, TopRight, BottomLeft, BottomRight }
}
