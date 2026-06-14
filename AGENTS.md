# AGENTS.md

This file provides guidance to AI coding agents when working in this repository.

## Project
Load @README.md for general project information.
Load @ai/lessons-learned.md and apply past lessons before starting any task.

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

**Debugging** (cost discipline):
- For any bug or unexpected-behavior report, reproduce it at the lowest test layer first (unit, then Robolectric) before launching the emulator — a deterministic failing test is cheaper and pinpoints the cause
- Reserve the emulator and screenshots for final confirmation or genuinely visual/layout bugs, not for exploratory "did it move?" loops (each screenshot read costs thousands of tokens)
- For navigation/UI-state bugs, drive the real code path in the test (e.g. `NavigationUI.onNavDestinationSelected`), not just a bare `navController.navigate(id)` — the menu/tap path adds NavOptions that a plain navigate hides

**Platform, localization, and accessibility** (ADR-003):
- minSdk 31 (Android 12) — no APIs above 31 without a runtime check or AndroidX backport
- Reference device: Lenovo Tab P11 Plus (tablet); verify layouts on that form factor
- Supported locales: English (default) and German — all text in string resources, no hardcoded strings
- Accessibility target: WCAG 2.1 AA; all interactive elements need content descriptions, 48dp touch targets minimum
- UI tasks are only complete when lint passes and Robolectric tests include ATF (`AccessibilityValidator`) checks

**Package and code organization** (ADR-004):
- Feature-first: all code lives in `feature/<name>/` (e.g. `feature/home/`, `feature/settings/`)
- Only `MainActivity` stays at the root package
- Shared code goes in `core/` only when used by ≥2 features
- Test packages mirror source packages (`feature/home/HomeFragmentTest` alongside `HomeFragment`)

**ADRs**:
- When writing a new ADR or significantly updating an existing one, invoke the `write-adr` skill — it writes the file and updates all cross-references (`getting-started.md` ADR table, `AGENTS.md` Key Conventions)

**User guide**:
- After completing any UI (frontend) change — layouts, activities, fragments, menus, or navigation — invoke the `write-user-guide` skill to update `docs/user-guide/user-guide.md`
- The English files in `docs/user-guide/` are the single source of truth (and the only version on GitHub). German is **in-app only**: `write-user-guide` generates `user-guide-de.md` and `system-context-de.png` into `code/ema-companion/app/src/main/assets/user-guide/` (committed there via a `.gitignore` negation, never in `docs/`). Edit English only and re-run the skill; never hand-edit a `*-de.*` file. The `*-de.mmd` intermediate is transient (gitignored).

**Hooks** (`.claude/settings.json` — fire automatically, but know they exist):
- **UX file written** (`PostToolUse` on Edit/Write): fires when layout, Activity, Fragment, strings, menu, or navigation files change — injects a reminder to invoke `write-user-guide`
- **SKILL.md written** (`PostToolUse` on Edit/Write): fires when `.claude/skills/*/SKILL.md` is written — injects a reminder to invoke `skill-check`
- **`git commit` staged** (`PreToolUse` on Bash): scans staged diff for secrets (API keys, tokens, private keys) and blocks the commit if found

## Build & Run

```bash
cd code/ema-companion
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit + Robolectric tests
./gradlew ktlintCheck          # lint
./gradlew installDebug         # install on running emulator
```

See [docs/getting-started.md](docs/getting-started.md) for full setup instructions.
