# AGENTS.md

This file provides guidance to AI coding agents when working in this repository.

## Project
Load @README.md for general project information.

## Stack

- **Language:** Kotlin
- **Platform:** Android (minSdk 31, targetSdk 36)
- **UI:** View-based XML layouts, Material Design 3
- **Build:** Gradle with version catalog (`gradle/libs.versions.toml`)

## Key Conventions

Architecture decisions are documented in [`docs/adr/`](docs/adr/). Read the relevant ADRs before making decisions in their domains. Key decisions currently in effect:

**Coding standards** (ADR-001):
- All Kotlin code must pass `./gradlew ktlintCheck` — run it before considering any task complete
- All implementations follow AI-TDD: write a failing test first, then implement, then refactor

**Testing** (ADR-002):
- Default to unit tests (JUnit4, `src/test/`) for all pure logic
- Use Robolectric for code that needs Android `Context` but not a real device
- Integration tests hit the local mock API service (real HTTP, configurable base URL)
- Maestro (`maestro/`) for a small set of critical UI flows only — not for broad UI coverage
- No Espresso

**Platform, localization, and accessibility** (ADR-003):
- minSdk 31 (Android 12) — no APIs above 31 without a runtime check or AndroidX backport
- Reference device: Lenovo Tab P11 Plus (tablet); verify layouts on that form factor
- Supported locales: English (default) and German — all text in string resources, no hardcoded strings
- Accessibility target: WCAG 2.1 AA; all interactive elements need content descriptions, 48dp touch targets minimum
- UI tasks are only complete when lint passes and Robolectric tests include ATF (`AccessibilityValidator`) checks

## Build & Run

```bash
cd code/ema-companion
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit + Robolectric tests
./gradlew ktlintCheck          # lint
./gradlew installDebug         # install on running emulator
```

See [docs/getting-started.md](docs/getting-started.md) for full setup instructions.
