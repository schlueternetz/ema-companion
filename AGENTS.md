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
- **Definition of done**: before marking any code change or OpenSpec task/implementation complete, run `/qa` (or at minimum `maestro test maestro/` against a running emulator) and confirm all Maestro flows pass locally — do not mark work done or push with failing or unrun Maestro flows

**EMA API call budget** (ADR-009):
- Monthly budget: **1,000 successful calls** — only `ApiResult.Success` counts; failures are free and retry
- Before designing any feature that calls the EMA API, check the allocation table in [ADR-009](docs/adr/009-ema-api-call-budget.md), verify headroom, and add a row for the new feature before implementing
- Prefer in order: reuse already-fetched data → cache immutable past data → throttle guards → fresh fetch
- Flag any design that makes redundant or unbounded API calls and propose an alternative before implementing

**Centralized API sync scheduler** (ADR-010):
- All EMA API fetch requests go through `ApiSyncScheduler` — never call a tile repository's `refresh()` directly from a Fragment, Activity, or ad hoc Worker
- New alerting-class data (must run unconditionally in the background, like Module Health) gets its own dedicated `PeriodicWorkRequest`, never gated by tile/widget-enabled state, app-open state, or widget placement; new display-class data (a Home tile or widget) routes through `ApiSyncScheduler`/`ApiSyncWorker`'s existing gating instead
- Before adding a new endpoint call, check whether the value is derivable from an already-fetched sibling data source (see `DailyEnergyRepository`'s `todayTotalProvider` pattern) instead of issuing a redundant fetch

**Debugging** (cost discipline):
- For any bug or unexpected-behavior report, reproduce it at the lowest test layer first (unit, then Robolectric) before launching the emulator — a deterministic failing test is cheaper and pinpoints the cause
- Reserve the emulator and screenshots for final confirmation or genuinely visual/layout bugs, not for exploratory "did it move?" loops (each screenshot read costs thousands of tokens)
- For navigation/UI-state bugs, drive the real code path in the test (e.g. `NavigationUI.onNavDestinationSelected`), not just a bare `navController.navigate(id)` — the menu/tap path adds NavOptions that a plain navigate hides
- **All emulator debug artifacts (screenshots, `adb pull` files, uiautomator XML dumps, SharedPreferences dumps) must be saved to `D:\ema-debug\` — never inside the project directory.** Create the folder if it doesn't exist (`New-Item -ItemType Directory -Force D:\ema-debug`). This keeps throwaway files out of git entirely.

**Platform, localization, and accessibility** (ADR-003):
- minSdk 31 (Android 12) — no APIs above 31 without a runtime check or AndroidX backport
- Reference device: Lenovo Tab P11 Plus (tablet); verify layouts on that form factor
- Supported locales: English (default) and German — all text in string resources, no hardcoded strings
- Accessibility target: WCAG 2.1 AA; all interactive elements need content descriptions, 48dp touch targets minimum
- UI tasks are only complete when lint passes and Robolectric tests include ATF (`AccessibilityValidator`) checks

**Tile repository pattern** (ADR-007):
- Each tile repo implements a tile-specific source interface (`currentState()` + `refresh()`) and `ThrottleResettable`
- Add every new tile repo to `SettingsFragment.tileRepositories` (for throttle reset) and call its `clear()` in `showFactoryResetDialog()` (for factory reset)
- Add the tile's SharedPreferences store name(s) to `SettingsFragmentTest.setUp()` so state doesn't leak across tests
- Known gap: `ModuleHealthRepository` lacks a public `clear()` — factory reset clears its prefs directly; fix this when adding a third tile

**Tile error display** (ADR-006):
- Every Home tile must show the last known data (or a neutral placeholder) at all times — never blank on error
- Fetch errors are shown as an inline status line below the data text; no dialog, toast, or popup for transient fetch errors
- Each tile's state class carries a `FetchError?` field (reuse `core/api/FetchError`); persist it alongside the tile data so `currentState()` reconstructs the full rendered state
- `ConfigurationError` is silent — show neutral placeholder only, no error line

**Email alerts** (ADR-008):
- Push and email are each gated by their own `AlertLevel` (`OFF`/`ALERTS_ONLY`/`ALL`, `core/AlertLevel.kt`) via the shared pure `shouldAlert(level, previousStatus, newStatus)` in `ModuleHealthWorker.kt` — Off never fires, Alerts Only fires only on status change (covers both degradation and recovery), All fires on every check regardless of change
- `lastEmailedStatus` and `lastNotifiedStatus` are separate persisted fields in `ema_module_health`; do not merge them; both are updated on every dispatched alert, including under All
- RED latch: if persisted status is RED and computed status is YELLOW, final status stays RED — only GREEN clears RED
- `lastEmailedStatus` is updated only on `EmailResult.Success`; leave it unchanged on `AuthFailure` or `NetworkError` so the next eligible check retries
- App Password must never appear in logs, crash reports, or `ApiCallLogRepository`; `resetThrottle()` on `ModuleHealthRepository` must clear `KEY_LAST_EMAILED_STATUS`

**Package and code organization** (ADR-004):
- Feature-first: all code lives in `feature/<name>/` (e.g. `feature/home/`, `feature/settings/`)
- Only `MainActivity` stays at the root package
- Shared code goes in `core/` only when used by ≥2 features
- Test packages mirror source packages (`feature/home/HomeFragmentTest` alongside `HomeFragment`)

**ADRs**:
- When writing a new ADR or significantly updating an existing one, invoke the `write-adr` skill — it writes the file and updates all cross-references (`getting-started.md` ADR table, `AGENTS.md` Key Conventions)

**User guide**:
- After completing any UI (frontend) change — layouts, activities, fragments, menus, or navigation — invoke the `write-user-guide` skill to update the relevant pages in `docs/user-guide/`
- "Completing" means the code has actually been implemented and the behavior/numbers it describes are real — not while an OpenSpec change is still in the proposal/design stage. If a proposal's design implies a user-guide update, add it as a task in that change's `tasks.md` (Documentation section) instead of writing the guide immediately; the guide gets updated when `/opsx:apply` implements that task, not before
- The guide is split into one file per screen plus an index: `user-guide.md` (index), `home.md`, `settings.md`, `import-export.md`. Each page must stay under ~600 words (3-minute read). Only add a new page when a section is logically self-contained and too long for an existing page.

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
