package io.github.nadeemiqbal.debugbar.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nadeemiqbal.debugbar.DebugBar
import io.github.nadeemiqbal.debugbar.DebugBarActivation
import io.github.nadeemiqbal.debugbar.rememberDebugBarState
import io.github.nadeemiqbal.debugbar.sections.CustomSection
import io.github.nadeemiqbal.debugbar.sections.DeviceInfoEntry
import io.github.nadeemiqbal.debugbar.sections.DeviceInfoSection
import io.github.nadeemiqbal.debugbar.sections.EnvEntry
import io.github.nadeemiqbal.debugbar.sections.EnvStore
import io.github.nadeemiqbal.debugbar.sections.EnvSwitcherSection
import io.github.nadeemiqbal.debugbar.sections.LogLevel
import io.github.nadeemiqbal.debugbar.sections.LogStore
import io.github.nadeemiqbal.debugbar.sections.LogViewerSection
import io.github.nadeemiqbal.debugbar.sections.NetworkLogStore
import io.github.nadeemiqbal.debugbar.sections.NetworkLogSection
import io.github.nadeemiqbal.debugbar.sections.ScreenshotBundleSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Demo app that wraps a fake e-commerce screen with [DebugBar]. Tap the **Open Debug Bar**
 * button (or long-press the top-right corner) to reveal the drawer with all 6 built-in sections
 * plus a custom "Test states" tab.
 */
@Composable
fun SampleApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DemoScreen()
        }
    }
}

@Composable
private fun DemoScreen() {
    // Stores are created once per app — owned by the host, shared between section + the code
    // that records into them.
    val networkStore = remember { NetworkLogStore() }
    val logStore = remember { LogStore() }
    val envStore = remember {
        EnvStore(
            envs = listOf(
                EnvEntry("dev", "https://dev-api.example.com"),
                EnvEntry("staging", "https://staging-api.example.com"),
                EnvEntry("prod", "https://api.example.com"),
            ),
            initialName = "staging",
        )
    }

    val debugState = rememberDebugBarState()
    val scope = rememberCoroutineScope()
    val orderHistory = remember { mutableStateListOf<String>() }
    var orderCount by remember { mutableStateOf(3) }

    // Seed some demo activity so the drawer has content the moment the user opens it.
    LaunchedEffect(Unit) {
        delay(300)
        logStore.record(LogLevel.Info, "App started", tag = "App")
        logStore.record(LogLevel.Debug, "User session loaded", tag = "Auth")
        networkStore.record("GET", "${envStore.selected.value.baseUrl}/me", 200, 124L)
        networkStore.record("GET", "${envStore.selected.value.baseUrl}/products", 200, 287L)
    }

    DebugBar(
        enabled = true,
        activation = DebugBarActivation.LongPressCorner() + DebugBarActivation.KeyboardShortcut(),
        state = debugState,
        sections = listOf(
            NetworkLogSection(networkStore),
            LogViewerSection(logStore),
            EnvSwitcherSection(envStore),
            ScreenshotBundleSection(
                networkStore = networkStore,
                logStore = logStore,
                envStore = envStore,
                deviceInfoSupplier = { listOf("DebugBar Sample · iOS Simulator · iPhone 17") },
            ),
            DeviceInfoSection(
                extraInfo = listOf(
                    DeviceInfoEntry("App version", "0.1.0"),
                    DeviceInfoEntry("Build", "debug"),
                    DeviceInfoEntry("User", "demo@example.com"),
                ),
            ),
            CustomSection("Test states") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Force the host app into specific states.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        orderCount = 0
                        orderHistory.clear()
                        logStore.record(LogLevel.Warn, "Forced empty state", tag = "Demo")
                    }) { Text("Force empty list") }
                    Button(onClick = {
                        logStore.record(LogLevel.Error, "Forced 500 from server", tag = "Net", throwable = "java.io.IOException: 500 Internal Server Error")
                        networkStore.record("POST", "${envStore.selected.value.baseUrl}/checkout", 500, 1342L)
                    }) { Text("Force network error") }
                    Button(
                        onClick = {
                            logStore.record(LogLevel.Error, "Crash forced from debug bar", tag = "Demo")
                        },
                    ) { Text("Log a fake crash") }
                }
            },
        ),
    ) {
        StoreScreen(
            orderCount = orderCount,
            orderHistory = orderHistory,
            onPlaceOrder = {
                val orderId = Random.nextInt(1000, 9999)
                orderHistory.add("Order #$orderId")
                orderCount++
                logStore.record(LogLevel.Info, "Order #$orderId placed", tag = "Order")
                networkStore.record("POST", "${envStore.selected.value.baseUrl}/orders", 201, Random.nextLong(60, 480))
            },
            onOpenDebugBar = { debugState.open() },
            onScrollToFlags = { debugState.openSection("Logs") },
            scope = scope,
        )
    }
}

@Composable
private fun StoreScreen(
    orderCount: Int,
    orderHistory: List<String>,
    onPlaceOrder: () -> Unit,
    onOpenDebugBar: () -> Unit,
    onScrollToFlags: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "DebugBar — Compose Multiplatform",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "A pretend e-commerce screen. Wrapped in DebugBar — long-press the top-right corner, " +
                "press the button below, or hit Cmd+Shift+D to open the drawer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onOpenDebugBar,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) {
                Icon(Icons.Outlined.BugReport, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Open Debug Bar")
            }
            OutlinedButton(
                onClick = onPlaceOrder,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) { Text("Place order") }
            OutlinedButton(
                onClick = onScrollToFlags,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) { Text("Open Logs tab") }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("$orderCount orders placed today", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                if (orderHistory.isEmpty()) {
                    Text(
                        "No orders yet — tap 'Place order'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(orderHistory) { id ->
                            Text("· $id", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            "All debug-drawer activity above (place order, force error) feeds into the drawer's " +
                "Network/Logs tabs so QA can see exactly what happened.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Suppress unused-scope warning — real apps would launch coroutines from buttons.
        Box(modifier = Modifier.height(0.dp)) {
            scope.launch {}
        }
    }
}

@Suppress("unused")
private fun ensureBgImport(m: Modifier) = m.background(androidx.compose.ui.graphics.Color.Unspecified)

@Suppress("unused")
private fun ensureClipImport(m: Modifier) = m.clip(RoundedCornerShape(0.dp))
