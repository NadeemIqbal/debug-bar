package io.github.nadeemiqbal.debugbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A plugin section in the debug drawer. The drawer renders the union of every section the host
 * passes — each section gets a tab in the drawer's tab bar, with its own [Content] composable.
 *
 * Built-in sections (`NetworkLogSection`, `LogViewerSection`, `EnvSwitcherSection`,
 * `ScreenshotBundleSection`, `DeviceInfoSection`, `CustomSection`) cover ~80% of needs. Third-party
 * libraries can plug in too — e.g. `flag-bar` ships a `FlagBarSection` you drop into the
 * `sections` list to get a feature-flag tab inside the same drawer.
 *
 * Implementations:
 * - Should be lightweight to instantiate; the drawer creates one per render of the host.
 * - May hold their own internal state (use `remember`/`mutableStateOf` inside [Content]).
 * - Should keep [title] short — it's the tab label.
 * - [badgeCount] surfaces a red dot with a number in the tab bar (e.g. "5 errors logged").
 */
interface DebugBarSection {

    /** Tab label shown in the drawer's tab strip. Keep it short (≤ 12 chars renders best). */
    val title: String

    /** Optional icon next to the title in the tab strip. */
    val icon: ImageVector?
        get() = null

    /** Optional badge count — surfaces as a red dot with the number in the tab strip. */
    val badgeCount: Int?
        get() = null

    /** The section's body. Rendered when the section's tab is active. */
    @Composable
    fun Content()
}
