# Contributing to DebugBar

Thanks for your interest! Contributions of all kinds are welcome — bug reports, feature
requests, docs, code.

## Project layout

```
debug-bar/                       The published Kotlin Multiplatform library.
  src/commonMain/
    DebugBar.kt                  Main wrapper composable + drawer surface.
    DebugBarState.kt             Open/close + active section index.
    DebugBarSection.kt           Plugin interface.
    DebugBarActivation.kt        LongPressCorner / KeyboardShortcut / Combine / Programmatic.
    DebugBarDefaults.kt          Defaults — shape, padding, colors.
    sections/                    Built-in sections.
  src/commonTest/                Pure-logic tests (state, activation, stores).
  src/skikoTest/                 Compose UI tests (drawer open/close, sections).
sample/composeApp/               Shared sample with fake e-commerce screen + all sections wired.
sample/androidApp/               Android launcher.
sample/desktopApp/               Desktop (JVM) launcher.
sample/webApp/                   Web (wasmJs) launcher.
sample/iosApp/                   iOS launcher (Xcode project).
```

## Build & test

```bash
./gradlew build                          # build + test everything
./gradlew :debug-bar:desktopTest         # fastest feedback
./gradlew :sample:desktopApp:run         # run the sample
```

## Design invariants — please preserve

- **Tree-shake when disabled.** When `enabled = false`, the wrapper is a pure pass-through —
  no listeners installed, no allocations. Don't add work that runs unconditionally.
- **Section plugins are independent.** Built-in sections only depend on `DebugBarSection` and
  their own store. They never reach into the drawer's state. Third-party sections (like
  `FlagBarSection` from flag-bar) work the same way.
- **No platform reach-arounds in commonMain.** No `expect`/`actual` for v0.1. Shake detection,
  native ZIP+share, FPS HUD all defer to v0.2 where they'll live in platform source sets.
- **Stores are headless and shareable.** `NetworkLogStore` / `LogStore` / `EnvStore` work
  without composition — instantiable in `Application.onCreate`, observed from any layer.

## Conventions

- Public API gets KDoc.
- Add/update tests for every behavior change.
- Update the sample when you change a public API.
- Add a `CHANGELOG.md` entry under `## Unreleased`.

## Releasing

Releases are tag-driven via `publish.yml` (configure secrets in your repo settings first), or
local via `./gradlew :debug-bar:publishAndReleaseToMavenCentral`.
