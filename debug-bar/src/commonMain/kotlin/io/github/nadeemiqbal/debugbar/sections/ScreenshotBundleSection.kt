package io.github.nadeemiqbal.debugbar.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.debugbar.DebugBarSection
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Snapshot of debug state at a moment — what a tester would send to engineering. Holds the data
 * collected from the other configured sections.
 *
 * v0.1 collects + previews the bundle inline. v0.2 will add ZIP packaging + native share-sheet
 * export via `expect`/`actual` per platform.
 */
@OptIn(ExperimentalTime::class)
data class DebugBundle(
    val capturedAtMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val networkSummary: List<String> = emptyList(),
    val logSummary: List<String> = emptyList(),
    val envSummary: String? = null,
    val deviceSummary: List<String> = emptyList(),
    val flagSummary: List<String> = emptyList(),
)

/**
 * The "send this to engineering" button. Tap → collect bundle of recent network calls + logs +
 * env + device + flag state → preview inline.
 *
 * Wire the sources you want included via the constructor; pass `null` for sections you don't use.
 */
class ScreenshotBundleSection(
    private val networkStore: NetworkLogStore? = null,
    private val logStore: LogStore? = null,
    private val envStore: EnvStore? = null,
    private val deviceInfoSupplier: () -> List<String> = { emptyList() },
    private val flagStateSupplier: () -> List<String> = { emptyList() },
    override val title: String = "Bundle",
) : DebugBarSection {

    override val icon: ImageVector get() = Icons.Outlined.PhotoCamera

    @Composable
    override fun Content() {
        var bundle by remember { mutableStateOf<DebugBundle?>(null) }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Capture a snapshot of recent network calls, logs, env, device info, and flag state. " +
                    "Useful for QA: tap → see the bundle → share contents to your engineer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    bundle = DebugBundle(
                        networkSummary = networkStore?.entries?.value?.takeLast(30)?.map {
                            "${it.method} ${it.statusCode} ${it.durationMs}ms ${it.url}"
                        } ?: emptyList(),
                        logSummary = logStore?.entries?.value?.takeLast(50)?.map {
                            "${it.level} ${it.tag ?: "-"} ${it.message}"
                        } ?: emptyList(),
                        envSummary = envStore?.selected?.value?.let { "${it.name} → ${it.baseUrl}" },
                        deviceSummary = deviceInfoSupplier(),
                        flagSummary = flagStateSupplier(),
                    )
                }) { Text("Capture bundle") }
                if (bundle != null) {
                    Button(onClick = { bundle = null }) { Text("Clear") }
                }
            }
            val captured = bundle
            if (captured == null) {
                EmptyState("Tap 'Capture bundle' to snapshot current debug state.")
            } else {
                BundlePreview(captured)
            }
        }
    }
}

@Composable
private fun BundlePreview(bundle: DebugBundle) {
    val sections = remember(bundle) {
        buildList {
            add("Captured at" to bundle.capturedAtMillis.toString())
            bundle.envSummary?.let { add("Environment" to it) }
            if (bundle.deviceSummary.isNotEmpty()) add("Device" to bundle.deviceSummary.joinToString("\n"))
            if (bundle.flagSummary.isNotEmpty()) add("Flags" to bundle.flagSummary.joinToString("\n"))
            if (bundle.networkSummary.isNotEmpty()) add("Network (${bundle.networkSummary.size})" to bundle.networkSummary.joinToString("\n"))
            if (bundle.logSummary.isNotEmpty()) add("Logs (${bundle.logSummary.size})" to bundle.logSummary.joinToString("\n"))
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(sections) { (title, body) ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(title, style = MaterialTheme.typography.labelMedium)
                    Text(
                        body,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Suppress("unused")
private fun ensureBgImport(m: Modifier) = m.background(androidx.compose.ui.graphics.Color.Unspecified)
