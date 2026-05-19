package io.github.nadeemiqbal.debugbar.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PublishedWithChanges
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.debugbar.DebugBarSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One named environment in the env-switcher (e.g. `"dev" -> "https://dev-api.example.com"`).
 */
data class EnvEntry(val name: String, val baseUrl: String)

/**
 * Shared store for [EnvSwitcherSection]. Construct with your list of environments and the
 * currently-active one (typically loaded from KV storage). Observe [selected] from your
 * networking layer to pick up changes.
 */
class EnvStore(
    val envs: List<EnvEntry>,
    // Default is a sentinel; the actual fallback ("first env") happens in the init block
    // so we can `require(envs.isNotEmpty())` BEFORE touching `envs.first()`.
    initialName: String = "",
    private val onSelected: (EnvEntry) -> Unit = {},
) {
    init {
        require(envs.isNotEmpty()) { "EnvStore requires at least one environment" }
    }

    private val selectedState = MutableStateFlow(
        envs.firstOrNull { it.name == initialName } ?: envs.first(),
    )
    val selected: StateFlow<EnvEntry> = selectedState.asStateFlow()

    fun select(name: String) {
        envs.firstOrNull { it.name == name }?.let {
            selectedState.value = it
            onSelected(it)
        }
    }
}

class EnvSwitcherSection(
    private val store: EnvStore,
    override val title: String = "Env",
) : DebugBarSection {

    override val icon: ImageVector get() = Icons.Outlined.PublishedWithChanges

    @Composable
    override fun Content() {
        val current by store.selected.collectAsState()
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Switch backend environment. Your networking layer must observe `envStore.selected` for changes to take effect.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            store.envs.forEach { env ->
                Surface(
                    color = if (env.name == current.name) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { store.select(env.name) },
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = env.name == current.name, onClick = { store.select(env.name) })
                        Column(modifier = Modifier.padding(start = 4.dp)) {
                            Text(env.name, fontWeight = FontWeight.SemiBold)
                            Text(env.baseUrl, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private fun ensureBgImport(m: Modifier) = m.background(androidx.compose.ui.graphics.Color.Unspecified)
