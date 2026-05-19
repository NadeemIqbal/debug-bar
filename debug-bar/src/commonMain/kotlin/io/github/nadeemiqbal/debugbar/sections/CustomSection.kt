package io.github.nadeemiqbal.debugbar.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.nadeemiqbal.debugbar.DebugBarSection

/**
 * Generic escape hatch — wrap any composable as a debug-bar tab. Useful for "test states" panels
 * where the host wants to expose buttons that force the app into specific UI states (e.g. force
 * an empty list, force a network error, force a crash) without writing a whole section class.
 *
 * Example:
 * ```
 * CustomSection("Test states") {
 *     Button({ vm.forceEmpty() }) { Text("Force empty list") }
 *     Button({ vm.forceError(NetworkError) }) { Text("Force network error") }
 *     Button({ throw RuntimeException("debug crash") }) { Text("Force crash") }
 * }
 * ```
 */
class CustomSection(
    override val title: String,
    override val icon: ImageVector? = Icons.Outlined.Extension,
    private val content: @Composable () -> Unit,
) : DebugBarSection {
    @Composable
    override fun Content() = content()
}
