# Getting Started

## Prerequisites

- JDK 11+
- Android SDK 31+ with an AVD configured (the [Android Studio SDK Manager](https://developer.android.com/studio) is the easiest way to install both)

Reference AVD: **`Lenovo_Tab_11_Plus`** (android-33, 11-inch tablet, 2000×1200) — see [ADR-003](adr/003-platform-localization-accessibility.md) for details.

## Building

```bash
cd code/ema-companion
./gradlew assembleDebug
```

## Testing and Lint

```bash
cd code/ema-companion
./gradlew testDebugUnitTest    # unit + Robolectric tests (primary fast-feedback loop)
./gradlew ktlintCheck          # Kotlin style check — must pass before merging
./gradlew lint                 # Android lint — must pass before merging
```

## Running

Start your emulator, then install and launch:

```bash
cd code/ema-companion
./gradlew installDebug
adb shell monkey -p com.schlueternetz.emacompanion -c android.intent.category.LAUNCHER 1
```

## Development vs Production API

By default the app talks to the real EMA API (production) — every successful call counts
against [ADR-009](adr/009-ema-api-call-budget.md)'s 1,000-call/month budget. For local
development and testing, use `ema-api-stub` — a free, deterministic local mock — instead:

```bash
cd code/ema-api-stub
./gradlew run    # starts on port 8080 by default
```

In a **debug build**, Settings → API Settings has a **Use local stub** action (not present
in release builds) that one-taps the Base URL to `http://10.0.2.2:{STUB_PORT}/user/api/v2/`.
`STUB_PORT` defaults to `8080` on both the app and the stub; if you run the stub on a
different port, set the same `STUB_PORT` Gradle property (`-PSTUB_PORT=<port>` or
`local.properties`) so both sides agree.

Start the stub before running `/qa`'s Maestro step (or `maestro test maestro/`) locally —
`a-home-screen.yaml` drives the app against it to verify real, populated data renders. See
[`docs/ema-api-stub/README.md`](ema-api-stub/README.md) for the stub's full scenario format
and for manual setup on a physical device (which can't reach `10.0.2.2`).

## Architecture Decision Records

Key decisions are documented in [`docs/adr/`](adr/):

| ADR | Title |
|-----|-------|
| [ADR-001](adr/001-coding-standards.md) | Coding Standards (ktlint, Dependabot, AI-TDD) |
| [ADR-002](adr/002-testing-strategy.md) | Testing Strategy |
| [ADR-003](adr/003-platform-localization-accessibility.md) | Platform Support, Localization, and Accessibility |
| [ADR-004](adr/004-package-and-code-organization.md) | Package and Code Organization |
| [ADR-005](adr/005-in-app-markdown-rendering.md) | In-App Markdown Rendering |
| [ADR-006](adr/006-tile-error-display.md) | Tile Error Display Pattern |
| [ADR-007](adr/007-tile-repository-pattern.md) | Tile Repository Pattern |
| [ADR-008](adr/008-email-alerts.md) | Email Alerts for Module Health |
| [ADR-009](adr/009-ema-api-call-budget.md) | EMA API Call Budget |
| [ADR-010](adr/010-centralized-api-sync-scheduler.md) | Centralized API Sync Scheduler |
