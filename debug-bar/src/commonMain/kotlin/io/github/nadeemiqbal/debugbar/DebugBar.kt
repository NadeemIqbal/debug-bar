package io.github.nadeemiqbal.debugbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Wraps your app content in a debug-drawer overlay. When [enabled] is `false` the wrapper is a
 * pass-through — the activation listener isn't even installed, so there is zero runtime overhead
 * in release builds.
 *
 * Typical usage:
 * ```
 * @Composable
 * fun App() {
 *     DebugBar(
 *         enabled = BuildConfig.DEBUG,
 *         sections = listOf(
 *             FlagBarSection(flags),       // from flag-bar
 *             NetworkLogSection(networkStore),
 *             LogViewerSection(logStore),
 *             EnvSwitcherSection(envs),
 *             ScreenshotBundleSection(),
 *             DeviceInfoSection(),
 *             CustomSection("Test states") { ... },
 *         ),
 *     ) {
 *         MainAppContent()
 *     }
 * }
 * ```
 *
 * Test tags:
 *  - root: `debug_bar_root`
 *  - drawer: `debug_bar_drawer`
 *  - tab strip: `debug_bar_tabs`
 *  - close button: `debug_bar_close`
 *  - corner activator (when LongPressCorner is configured): `debug_bar_corner_activator`
 */
@Composable
fun DebugBar(
    sections: List<DebugBarSection>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activation: DebugBarActivation = DebugBarDefaults.defaultActivation,
    state: DebugBarState = rememberDebugBarState(),
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    // Resolve `state.openSection("title")` calls — reactive: keeps observing the flow so
    // openSection invocations after first composition still trigger the resolution.
    LaunchedEffect(state, sections) {
        state.pendingSectionTitle.collect { target ->
            if (target != null) {
                val idx = sections.indexOfFirst { it.title.equals(target, ignoreCase = true) }
                if (idx >= 0) state.selectSection(idx)
                state.consumePendingSectionTitle()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("debug_bar_root")
            .keyboardActivation(activation, state),
    ) {
        // Host app content.
        content()

        // Corner long-press activator(s).
        cornerActivations(activation).forEach { longPress ->
            CornerActivator(longPress = longPress, state = state)
        }

        // The drawer overlay.
        val isOpen by state.isOpen.collectAsState()
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        ) {
            DrawerSurface(state = state, sections = sections)
        }
    }
}

@Composable
private fun DrawerSurface(state: DebugBarState, sections: List<DebugBarSection>) {
    val activeIndex by state.activeSectionIndex.collectAsState()
    val safeIndex = activeIndex.coerceIn(0, (sections.size - 1).coerceAtLeast(0))

    // Scrim — tap closes the drawer.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { state.close() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(DebugBarDefaults.maxDrawerHeightFraction)
                .clip(DebugBarDefaults.drawerShape)
                .pointerInput(Unit) {
                    // Eat taps so the scrim's clickable doesn't fire when interacting with the
                    // drawer itself.
                    detectTapGestures { /* no-op */ }
                }
                .testTag("debug_bar_drawer"),
            color = DebugBarDefaults.container(),
            shape = DebugBarDefaults.drawerShape,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle bar.
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).background(DebugBarDefaults.handle(), CircleShape))
                }
                // Header.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Outlined.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Debug Bar",
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { state.close() }, modifier = Modifier.testTag("debug_bar_close")) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close debug bar")
                    }
                }

                // Tab strip.
                if (sections.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DebugBarDefaults.sectionTabHeight)
                            .testTag("debug_bar_tabs"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(sections.withIndex().toList()) { (index, section) ->
                            TabChip(
                                section = section,
                                active = index == safeIndex,
                                onClick = { state.selectSection(index) },
                            )
                        }
                    }
                }

                // Active section body.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DebugBarDefaults.sectionContentPadding),
                ) {
                    sections.getOrNull(safeIndex)?.Content()
                        ?: Text(
                            "No sections configured.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
        }
    }
}

@Composable
private fun TabChip(section: DebugBarSection, active: Boolean, onClick: () -> Unit) {
    val container = if (active) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (active) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = container,
        modifier = Modifier.testTag("debug_bar_tab_${section.title}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            section.icon?.let {
                Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(16.dp), tint = onContainer)
            }
            val badge = section.badgeCount
            if (badge != null && badge > 0) {
                BadgedBox(badge = { Badge { Text(badge.toString()) } }) {
                    Text(section.title, color = onContainer, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Text(section.title, color = onContainer, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun cornerActivations(activation: DebugBarActivation): List<DebugBarActivation.LongPressCorner> = when (activation) {
    is DebugBarActivation.LongPressCorner -> listOf(activation)
    is DebugBarActivation.Combine -> activation.activations.flatMap { cornerActivations(it) }
    else -> emptyList()
}

@Composable
private fun BoxScope.CornerActivator(longPress: DebugBarActivation.LongPressCorner, state: DebugBarState) {
    val alignment = when (longPress.corner) {
        DebugBarActivation.Corner.TopLeft -> Alignment.TopStart
        DebugBarActivation.Corner.TopRight -> Alignment.TopEnd
        DebugBarActivation.Corner.BottomLeft -> Alignment.BottomStart
        DebugBarActivation.Corner.BottomRight -> Alignment.BottomEnd
    }
    Box(
        modifier = Modifier
            .size(64.dp)
            .align(alignment)
            .testTag("debug_bar_corner_activator")
            .pointerInput(longPress.holdMillis) {
                detectTapGestures(
                    onLongPress = { state.toggle() },
                )
            },
    )
}

/** Apply a key-event listener for the [activation]'s `KeyboardShortcut`, if any. */
private fun Modifier.keyboardActivation(activation: DebugBarActivation, state: DebugBarState): Modifier {
    val shortcuts = when (activation) {
        is DebugBarActivation.KeyboardShortcut -> listOf(activation)
        is DebugBarActivation.Combine -> activation.activations.mapNotNull { it as? DebugBarActivation.KeyboardShortcut }
        else -> emptyList()
    }
    if (shortcuts.isEmpty()) return this
    return this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val keyLabel = event.key.keyLabel()
        for (sc in shortcuts) {
            if (sc.matchesShortcut(keyLabel, event.isMetaPressed, event.isCtrlPressed, event.isShiftPressed)) {
                state.toggle()
                return@onPreviewKeyEvent true
            }
        }
        false
    }
}

private fun Key.keyLabel(): String = toString().removePrefix("Key: ")
