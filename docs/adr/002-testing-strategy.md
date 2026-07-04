# ADR-002: Testing Strategy

**Status:** Accepted  
**Date:** 2026-06-07

## Context

The app needs a testing strategy that is fast to run, maintainable, and provides confidence at each layer of the stack. The EMA API is an external dependency that must be testable without live network access. UI testing should cover critical flows without the brittleness and overhead of full instrumented test suites.

## Decisions

### Test pyramid: unit-heavy

The pyramid is weighted toward fast, isolated tests:

```
        [ Maestro E2E ]       ← few, critical flows only
      [ Integration tests ]   ← API client + data layer
    [ Robolectric tests ]      ← Android framework code
  [ Unit tests (JUnit4) ]      ← most tests live here
```

### Layer 1 — Unit tests (JUnit4, JVM)

Location: `src/test/`

Use for all pure logic that has no Android framework dependency:
- ViewModels (with mocked repositories)
- Business logic and data transformations
- Utility functions
- Repository classes with mocked storage/network

These are the default test type. If a class can be tested here, it should be.

### Layer 2 — Robolectric (Android framework on JVM)

Location: `src/test/` (annotated with `@RunWith(RobolectricTestRunner::class)`)

Use when code requires an Android `Context` or framework class but does not need a real device:
- `EncryptedSharedPreferences` / `SettingsRepository`
- `AppCompatDelegate` locale behavior
- `Fragment` and `Activity` lifecycle

Robolectric runs on the JVM — no emulator required, fast feedback. Prefer it over instrumented tests wherever it provides adequate fidelity.

### Layer 3 — Integration tests (mock API service)

Location: `src/test/` or a dedicated `integration/` source set

The app supports a configurable API base URL (a debug-only setting). A lightweight local HTTP service returns canned responses, simulating the real EMA API.

Use for:
- API client code (request construction, response parsing, error handling)
- End-to-end data flow from network call to ViewModel
- Simulating API edge cases: errors, timeouts, unexpected payloads, empty responses

The mock API service also serves as the target for local manual testing and exploratory API behavior simulation — not just automated tests.

Integration tests require the mock service to be running and are slower than unit tests. Keep the count low; cover the API contract and key error paths, not every permutation.

### Layer 4 — Maestro E2E flows

Location: `code/ema-companion/maestro/` (alongside the Gradle project, YAML flows — all app code and resources live under `code/`)

[Maestro](https://maestro.mobile.dev/) is a free, open-source mobile UI testing CLI. Flows run against a live emulator as a black box — no app code changes or compilation needed.

Use for a small set of critical user journeys:
- App launches and displays the Home screen
- Navigation between Home, User Guide, and Settings (`maestro/bottom-nav.yaml`)
- Language change takes effect immediately

Do not use Maestro for exhaustive UI coverage. ViewModel unit tests + manual testing cover the remainder. Espresso is not used.

**Execution:**
- **Locally**, on demand, via the `/qa` skill (build → install → `maestro test`) at commit-time checkpoints. This is the primary E2E trigger for solo, straight-to-`main` work.
- **In CI** (`.github/workflows/ci.yml`) on every `push` to `main`: a fast `unit` job (`testDebugUnitTest` + `ktlintCheck`) always runs; an `e2e` job runs `needs: unit` (only if unit is green) on an API-33 emulator via `reactivecircus/android-emulator-runner`. The Android emulator runs on `ubuntu-latest` (Linux, 1× minute rate — no macOS 10× cost). Per-push E2E is viable here because commit volume is low; if it grows, path-filter the `e2e` job to `app/**` + `maestro/**` or move it to a nightly `schedule:`.

## Consequences

- The emulator is only required for Maestro flows — all other automated tests run on the JVM
- `./gradlew testDebugUnitTest` covers layers 1 and 2 and is the primary fast-feedback loop
- Integration tests require the mock API service running locally; document startup in the service's README
- Maestro flows run locally via `/qa` and in CI on every push to `main` (see Layer 4 "Execution"); the self-hosted emulator path avoids Maestro Cloud's paid tier
- Maestro flows are a required local gate, not just commit-time discretion: a code change or OpenSpec implementation is not done until `/qa`'s E2E step (or a direct `maestro test`) passes locally
- All implementation follows AI-TDD (see [ADR-001](001-coding-standards.md)); the test layer for each task is chosen according to this strategy
