package io.github.nadeemiqbal.debugbar.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.debugbar.DebugBarSection

/**
 * One key/value row in the device-info dump. Hosts can add extra rows via [extraInfo] — useful
 * for app-specific data like build SHA, server version, user ID, install date.
 */
data class DeviceInfoEntry(val key: String, val value: String)

/**
 * Read-only system-info dump. Always shows screen size (derived from Compose `LocalDensity` so
 * it works on every CMP target). Hosts can pass [extraInfo] with platform-specific values they
 * already know (build SHA, app version, etc.) — the library doesn't reach into platform APIs
 * itself to avoid `expect`/`actual` for v0.1.
 */
class DeviceInfoSection(
    private val extraInfo: List<DeviceInfoEntry> = emptyList(),
    override val title: String = "Device",
) : DebugBarSection {

    override val icon: ImageVector get() = Icons.Outlined.PhoneAndroid

    @Composable
    override fun Content() {
        val density = LocalDensity.current

        val rows = remember(extraInfo, density) {
            extraInfo + listOf(
                DeviceInfoEntry("Density", density.density.toString()),
                DeviceInfoEntry("Font scale", density.fontScale.toString()),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Read-only system info. Pass `extraInfo` to add app-specific rows (build SHA, app version, user ID, etc.).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            rows.forEach { entry ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            entry.key,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.4f),
                        )
                        Text(
                            entry.value,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private fun ensureBgImport(m: Modifier) = m.background(androidx.compose.ui.graphics.Color.Unspecified)
