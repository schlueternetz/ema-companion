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
