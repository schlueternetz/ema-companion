# Getting Started

## Prerequisites

- JDK 11+
- Android SDK 31+ with an AVD configured (the [Android Studio SDK Manager](https://developer.android.com/studio) is the easiest way to install both)

## Building

```bash
cd code/ema-companion
./gradlew assembleDebug
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
