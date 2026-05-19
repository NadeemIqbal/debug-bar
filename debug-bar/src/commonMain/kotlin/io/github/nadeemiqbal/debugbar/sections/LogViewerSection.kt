package io.github.nadeemiqbal.debugbar.sections

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Severity levels — the section colours rows by level. */
enum class LogLevel { Verbose, Debug, Info, Warn, Error }

@OptIn(ExperimentalTime::class)
data class LogEntry(
    val id: Long,
    val level: LogLevel,
    val tag: String?,
    val message: String,
    val throwable: String? = null,
    val timestamp: Instant = Clock.System.now(),
)

/**
 * Shared store for [LogViewerSection]. Wire your logging library (Kermit, Napier, `println`) to
 * call [record] on each log. The buffer is bounded to [maxEntries] to avoid memory pressure.
 *
 * Example wiring into Kermit:
 * ```
 * Logger.addLogWriter(object : LogWriter() {
 *     override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
 *         logStore.record(
 *             level = when (severity) {
 *                 Severity.Verbose -> LogLevel.Verbose
 *                 Severity.Debug -> LogLevel.Debug
 *                 Severity.Info -> LogLevel.Info
 *                 Severity.Warn -> LogLevel.Warn
 *                 Severity.Error, Severity.Assert -> LogLevel.Error
 *             },
 *             tag = tag,
 *             message = message,
 *             throwable = throwable?.stackTraceToString(),
 *         )
 *     }
 * })
 * ```
 */
class LogStore(private val maxEntries: Int = 500) {

    private val entriesState = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = entriesState.asStateFlow()

    private var nextId = 0L

    fun record(level: LogLevel, message: String, tag: String? = null, throwable: String? = null) {
        val entry = LogEntry(id = nextId++, level = level, tag = tag, message = message, throwable = throwable)
        entriesState.update { existing ->
            val updated = existing + entry
            if (updated.size <= maxEntries) updated else updated.takeLast(maxEntries)
        }
    }

    fun clear() {
        entriesState.value = emptyList()
    }
}

/** Filterable log viewer tab. Search box matches against tag + message + throwable. */
class LogViewerSection(
    private val store: LogStore,
    override val title: String = "Logs",
) : DebugBarSection {

    override val icon: ImageVector get() = Icons.Outlined.Article

    @Composable
    override fun Content() {
        val entries by store.entries.collectAsState()
        var query by remember { mutableStateOf("") }
        var minLevel by remember { mutableStateOf(LogLevel.Verbose) }

        val filtered = remember(entries, query, minLevel) {
            entries.filter { entry ->
                entry.level.ordinal >= minLevel.ordinal &&
                    (query.isBlank() ||
                        entry.message.contains(query, ignoreCase = true) ||
                        (entry.tag?.contains(query, ignoreCase = true) == true) ||
                        (entry.throwable?.contains(query, ignoreCase = true) == true))
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterBox(query = query, onQueryChange = { query = it }, modifier = Modifier.weight(1f))
                LevelChip(level = minLevel, onCycle = { minLevel = LogLevel.entries[(minLevel.ordinal + 1) % LogLevel.entries.size] })
                TextButton(onClick = { store.clear() }) { Text("Clear") }
            }
            if (filtered.isEmpty()) {
                EmptyState(
                    if (entries.isEmpty()) "No logs recorded yet.\nWire `logStore.record(...)` into your logger."
                    else "No matches for filters.",
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered.reversed(), key = { it.id }) { entry ->
                        LogRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBox(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("Filter…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = LocalTextStyle.current)
                }
                inner()
            },
        )
    }
}

@Composable
private fun LevelChip(level: LogLevel, onCycle: () -> Unit) {
    Surface(onClick = onCycle, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
        Text(
            "≥ $level",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.Verbose -> MaterialTheme.colorScheme.onSurfaceVariant
        LogLevel.Debug -> MaterialTheme.colorScheme.primary
        LogLevel.Info -> MaterialTheme.colorScheme.tertiary
        LogLevel.Warn -> Color(0xFFFFA000)
        LogLevel.Error -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp),
    ) {
        Row {
            Text(
                entry.level.name.first().toString(),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
            if (entry.tag != null) {
                Text(
                    " · ${entry.tag}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Text(entry.message, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        if (entry.throwable != null) {
            Text(
                entry.throwable.take(400),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Suppress("unused")
private fun ensureInstantImported(): Instant = @OptIn(ExperimentalTime::class) Clock.System.now()
