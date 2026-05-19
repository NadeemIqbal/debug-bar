# DebugBar

**An in-app debug drawer for Compose Multiplatform apps.** A Square Cascade-style developer
menu with a pluggable section system — drop it into any CMP app and get a hidden drawer with
network log, log viewer, env switcher, screenshot+log+state bundle export, device info, and
custom test-state buttons. Tree-shaken in release builds. One drawer for Android, iOS, Desktop,
and Web.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.nadeemiqbal/debug-bar)](https://central.sonatype.com/artifact/io.github.nadeemiqbal/debug-bar)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Build](https://github.com/NadeemIqbal/debug-bar/actions/workflows/build.yml/badge.svg)](https://github.com/NadeemIqbal/debug-bar/actions/workflows/build.yml)

> **Pairs naturally with [`flag-bar`](https://github.com/NadeemIqbal/flag-bar)** — drop the
> `FlagBarSection` into the `sections` list and your feature flags get their own tab in the
> same drawer.

## Why

Every CMP team eventually rebuilds the same in-app dev menu: a hidden drawer for QA + devs to
toggle flags, inspect network calls, switch environments, force crash, share logs. Square's
Cascade, Pinterest's internal tools, Reddit's "secret menu" — same pattern, hand-rolled every
time. Nobody has shipped this as a polished library for any cross-platform framework. DebugBar
is that library.

## Install

```kotlin
dependencies {
    implementation("io.github.nadeemiqbal:debug-bar:0.1.0")
}
```

## Usage

```kotlin
@Composable
fun App() {
    DebugBar(
        enabled = BuildConfig.DEBUG,
        activation = DebugBarActivation.LongPressCorner() + DebugBarActivation.KeyboardShortcut(),
        sections = listOf(
            FlagBarSection(flags),                  // from flag-bar (optional)
            NetworkLogSection(networkStore),
            LogViewerSection(logStore),
            EnvSwitcherSection(envStore),
            ScreenshotBundleSection(networkStore, logStore, envStore),
            DeviceInfoSection(extraInfo = listOf(
                DeviceInfoEntry("App version", BuildConfig.VERSION_NAME),
                DeviceInfoEntry("Build SHA", BuildConfig.GIT_SHA),
            )),
            CustomSection("Test states") {
                Button({ vm.forceEmpty() })       { Text("Force empty list") }
                Button({ vm.forceError() })       { Text("Force network error") }
                Button({ throw RuntimeException("debug crash") }) { Text("Force crash") }
            },
        ),
    ) {
        MainAppContent()   // your real app — debug bar is an overlay
    }
}
```

## Activation

| Activation | What | Default |
|---|---|---|
| `LongPressCorner()` | Long-press a screen corner (default top-right, 1 sec) | ✅ |
| `KeyboardShortcut()` | Default `Cmd+Shift+D` (or `Ctrl+Shift+D` elsewhere) | ✅ |
| `Programmatic` | Call `state.toggle()` from your code (e.g. a hidden settings-tap) | opt-in |
| `Combine(a, b)` | Activate on **any** of the listed activations | use `a + b` |

The default (`DebugBarDefaults.defaultActivation`) is `LongPressCorner() + KeyboardShortcut()`
— covers both mobile and desktop with no setup.

## Built-in sections

| Section | What it does |
|---|---|
| `NetworkLogSection` | Lists HTTP requests recorded via `NetworkLogStore.record(...)`. Tap a row → headers + request + response. Wire from your Ktor/OkHttp interceptor. |
| `LogViewerSection` | Filterable log viewer. Records via `LogStore.record(level, message, tag, throwable)`. Wire from Kermit/Napier/`println`. |
| `EnvSwitcherSection` | Switch between named environments. Observe `envStore.selected` from your networking layer. |
| `ScreenshotBundleSection` | One tap → captures network history + recent logs + env + flag state + device info into a single previewable bundle. The "send to engineering" workflow that usually takes 10 manual steps. |
| `DeviceInfoSection` | Compose density + font scale by default. Pass `extraInfo` for app-specific rows (build SHA, app version, user ID). |
| `CustomSection` | Wrap any `@Composable` as a tab — the escape hatch for app-specific debug actions. |
| `FlagBarSection` | (Comes from `flag-bar`) Feature flag toggles + variant assignment + override drawer. |

## Wiring the stores into your app

The library doesn't intercept your HTTP client or logger directly (that would force heavy
dependencies). You wire them:

```kotlin
// One-time setup
val networkStore = NetworkLogStore(maxEntries = 200)
val logStore = LogStore(maxEntries = 500)
val envStore = EnvStore(envs = listOf(/* ... */))

// Ktor interceptor
client.plugin(HttpSend).intercept { request ->
    val started = Clock.System.now()
    val response = execute(request)
    networkStore.record(
        method = request.method.value,
        url = request.url.toString(),
        statusCode = response.response.status.value,
        durationMs = (Clock.System.now() - started).inWholeMilliseconds,
    )
    response
}

// Kermit log writer
Logger.addLogWriter(object : LogWriter() {
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        logStore.record(
            level = severity.toLogLevel(),
            tag = tag,
            message = message,
            throwable = throwable?.stackTraceToString(),
        )
    }
})

// Env-switcher → your network layer
envStore.selected.onEach { env -> httpClient.config { defaultBaseUrl = env.baseUrl } }.launchIn(scope)
```

## State API (headless)

```kotlin
val state = rememberDebugBarState()

state.open()                           // imperatively open
state.close()                          // imperatively close
state.toggle()                         // flip
state.selectSection(3)                 // jump to tab N
state.openSection("Feature Flags")     // jump to tab by title (case-insensitive)

val isOpen by state.isOpen.collectAsState()    // observe
```

## Production safety

Default behavior: `enabled = BuildConfig.DEBUG` — drawer is **completely tree-shaken in release
builds**. Even the activation listener isn't installed. Zero production overhead.

For QA on internal builds, gate on a flag instead: `enabled = remoteFlag("debugMenu")`.

## Platforms

| Target | Status |
|---|---|
| Android (minSdk 24) | ✅ |
| iOS (x64, arm64, simulatorArm64) | ✅ |
| Desktop (JVM 11) | ✅ |
| Web (wasmJs) | ✅ |

## Sample app

```bash
./gradlew :sample:desktopApp:run                 # Desktop
./gradlew :sample:androidApp:assembleDebug       # Android
./gradlew :sample:webApp:wasmJsBrowserDevelopmentRun   # Web
```

## License

Apache 2.0 — see [LICENSE](LICENSE).
