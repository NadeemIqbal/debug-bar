# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-18

### Added
- Initial release of `DebugBar` for Compose Multiplatform.
- `DebugBar` composable — wraps your app content with a debug drawer overlay. Disabled-mode is a
  pass-through (zero runtime overhead in release builds).
- `DebugBarState` + `rememberDebugBarState` — open/close/toggle/selectSection/openSection.
- `DebugBarActivation` — `LongPressCorner`, `KeyboardShortcut`, `Programmatic`, `Combine`. Default
  is `LongPressCorner() + KeyboardShortcut()` which works on mobile and desktop.
- `DebugBarSection` — the plugin interface (`title`, `icon`, `badgeCount`, `@Composable Content`).
- Built-in sections:
  - `NetworkLogSection` + `NetworkLogStore` — bounded list of HTTP entries with expandable
    request/response bodies. Color-coded by status (2xx/3xx/4xx-5xx). Wire from your Ktor/OkHttp
    interceptor via `networkStore.record(...)`.
  - `LogViewerSection` + `LogStore` — bounded, filterable log buffer with min-level chip + free-text
    filter. Wire from Kermit/Napier via `logStore.record(...)`.
  - `EnvSwitcherSection` + `EnvStore` — radio-list of named environments; reactive `selected`
    flow for your networking layer to observe.
  - `ScreenshotBundleSection` — collects network + logs + env + flags + device info into a
    `DebugBundle` previewable inline (ZIP packaging + native share coming in v0.2).
  - `DeviceInfoSection` — Compose density + font scale + caller-supplied `extraInfo` rows.
  - `CustomSection` — generic escape hatch wrapping any `@Composable` as a tab.
- Tab strip with active highlighting + optional badge count per tab.
- Tree-shaken in release builds when `enabled = false`.
- 23 pure-logic tests (state, activation, network store, log store, env store) + 5 Compose UI
  tests (drawer open/close, section selection, close button).
- Targets: Android (minSdk 24), iOS (x64, arm64, simulatorArm64), Desktop (JVM 11), Web (wasmJs).

[Unreleased]: https://github.com/NadeemIqbal/debug-bar/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/NadeemIqbal/debug-bar/releases/tag/v0.1.0
