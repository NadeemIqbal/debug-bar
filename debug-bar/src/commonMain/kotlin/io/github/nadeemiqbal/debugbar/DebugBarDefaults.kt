package io.github.nadeemiqbal.debugbar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Visual defaults for [DebugBar] and built-in sections. */
object DebugBarDefaults {

    val drawerShape: Shape get() = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    val maxDrawerHeightFraction: Float = 0.85f

    val sectionTabHeight: Dp = 48.dp
    val sectionContentPadding: Dp = 12.dp

    /** Default activation: long-press top-right corner OR keyboard shortcut (Desktop). */
    val defaultActivation: DebugBarActivation get() =
        DebugBarActivation.LongPressCorner() + DebugBarActivation.KeyboardShortcut()

    @Composable
    @ReadOnlyComposable
    fun container(): Color = MaterialTheme.colorScheme.surface

    @Composable
    @ReadOnlyComposable
    fun handle(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    @Composable
    @ReadOnlyComposable
    fun activeTabColor(): Color = MaterialTheme.colorScheme.primary
}
