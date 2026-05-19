package io.github.nadeemiqbal.debugbar.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.debugbar.DebugBarSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * One HTTP request as observed by the host's networking layer. Hosts populate the store via
 * [NetworkLogStore.record]; this section just renders what's in the store.
 *
 * The library doesn't intercept Ktor / OkHttp automatically — that would force a heavy dependency.
 * Wire it from your existing networking layer (e.g. a Ktor `HttpClient` plugin or an OkHttp
 * `Interceptor`) by calling `networkStore.record(...)` once per round-trip.
 */
@OptIn(ExperimentalTime::class)
data class NetworkLogEntry(
    val id: Long,
    val method: String,
    val url: String,
    val statusCode: Int,
    val durationMs: Long,
    val requestBody: String? = null,
    val responseBody: String? = null,
    val timestamp: Instant = Clock.System.now(),
)

/**
 * Shared store for [NetworkLogSection]. Construct once at app startup and pass to both the
 * section and your HTTP client interceptor.
 */
class NetworkLogStore(private val maxEntries: Int = 200) {

    private val entriesState = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
    val entries: StateFlow<List<NetworkLogEntry>> = entriesState.asStateFlow()

    private var nextId = 0L

    /** Record a completed request. Drops the oldest when the buffer hits [maxEntries]. */
    fun record(
        method: String,
        url: String,
        statusCode: Int,
        durationMs: Long,
        requestBody: String? = null,
        responseBody: String? = null,
    ) {
        val entry = NetworkLogEntry(
            id = nextId++,
            method = method,
            url = url,
            statusCode = statusCode,
            durationMs = durationMs,
            requestBody = requestBody,
            responseBody = responseBody,
        )
        entriesState.update { existing ->
            val updated = existing + entry
            if (updated.size <= maxEntries) updated else updated.takeLast(maxEntries)
        }
    }

    /** Clear every recorded entry. */
    fun clear() {
        entriesState.value = emptyList()
    }
}

/** A debug-bar tab showing the last N HTTP requests observed by [store]. */
class NetworkLogSection(
    private val store: NetworkLogStore,
    override val title: String = "Network",
) : DebugBarSection {

    override val icon: ImageVector get() = Icons.Outlined.NetworkCheck

    @Composable
    override fun Content() {
        val entries by store.entries.collectAsState()
        var selectedId by remember { mutableStateOf<Long?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${entries.size} request(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { store.clear() }) { Text("Clear") }
            }
            if (entries.isEmpty()) {
                EmptyState("No network activity recorded yet.\nCall `networkStore.record(...)` from your HTTP client.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(entries.reversed(), key = { it.id }) { entry ->
                        NetworkRow(
                            entry = entry,
                            expanded = selectedId == entry.id,
                            onToggle = { selectedId = if (selectedId == entry.id) null else entry.id },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkRow(entry: NetworkLogEntry, expanded: Boolean, onToggle: () -> Unit) {
    val statusColor = when (entry.statusCode) {
        in 200..299 -> MaterialTheme.colorScheme.primary
        in 300..399 -> MaterialTheme.colorScheme.tertiary
        in 400..599 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                StatusBadge(entry.method, MaterialTheme.colorScheme.secondary)
                Box(modifier = Modifier.padding(start = 6.dp)) {
                    StatusBadge(entry.statusCode.toString(), statusColor)
                }
                Text(
                    "${entry.durationMs}ms",
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                entry.url,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            if (expanded) {
                if (entry.requestBody != null) {
                    Text(
                        "Request:",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(entry.requestBody, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
                if (entry.responseBody != null) {
                    Text(
                        "Response:",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(entry.responseBody, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("unused")
private fun ensureDurationImported(d: Duration) = d
