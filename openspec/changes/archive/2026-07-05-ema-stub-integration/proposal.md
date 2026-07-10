## Why

ADR-002 defines integration tests as requiring a real running local mock API service, but nothing today actually launches `ema-api-stub`: `HomeProductionIntegrationTest` hand-copies JSON out of the stub's scenario file into an ad-hoc `MockWebServer` body, and the hourly/daily endpoints added by `production-data-and-graphs` have zero stub scenario coverage at all. Meanwhile every call against the real EMA API counts toward ADR-009's 1,000-call/month budget, so manual local testing has no cost-free option even though a stub built for exactly this purpose already exists and sits unused.

## What Changes

- Add `energy_level=hourly` and `energy_level=daily` interactions to the stub's bundled Good Data scenario (`203000001234.json`), including a `code:1001` no-data case and a case spanning two calendar months
- Add integration tests that embed the real stub engine (Ktor `testApplication`, the same in-process pattern the stub's own tests use — no real socket) and drive `OkHttpEmaApiClient` plus `HourlyEnergyRepository`/`DailyEnergyRepository` through it end-to-end
- Rewrite `HomeProductionIntegrationTest` to hit the embedded stub instead of a hand-copied JSON string, so the fixture cannot silently drift from the real scenario file
- Add a debug-build-only "Use local stub" action in Settings (`BuildConfig.DEBUG` gated) that one-taps the Base URL to the local stub's address (`http://10.0.2.2:{STUB_PORT}/user/api/v2/`, port sourced from a `STUB_PORT` Gradle property defaulting to `8080`); the persisted default and the existing "Reset to default" action remain unconditionally the production URL in every build type — release builds never see this action and can never default to localhost
- Fix `ema-api-stub`'s pre-existing `loadDefault()` duplicate-ECU-id collision by reassigning distinct ECU ids to the three `module-health-*-203000001234.json` scenario files, so the bundled default scenario set actually loads
- Extend the existing `a-home-screen.yaml` Maestro flow to also drive the app against a live local stub instance (via the new debug shortcut) and assert real, populated data renders (`Current Production: 8000 W`, populated hourly/history charts) — not just the placeholder/error path dummy credentials exercise today; update `.github/workflows/ci.yml`'s `e2e` job to launch the stub as a background process before running Maestro
- Update `docs/ema-api-stub/README.md`'s endpoint table (currently stale — omits module-health, and will omit hourly/daily) and `docs/getting-started.md` with the debug-only shortcut, the stub-is-dev/test vs real-API-is-prod split, and how to run the stub locally before Maestro flows

## Capabilities

### New Capabilities
- `dev-stub-shortcut`: a debug-build-only Settings action that points the app's Base URL at the local `ema-api-stub` in one tap, without changing the persisted production default used by fresh installs, factory reset, or "Reset to default"

### Modified Capabilities
- `ema-api-stub`: adds hourly/daily scenario interactions to the bundled Good Data ECU (currently only `minutely` and inverter-batch `energy` are scripted), and fixes the pre-existing `loadDefault()` duplicate-ECU-id collision (module-health scenario files reassigned distinct ECU ids). Note: this capability's spec still lives under `openspec/changes/ema-api-stub/specs/` — that change is task-complete but not yet archived, so `openspec/specs/ema-api-stub/` does not exist yet. This change's delta spec targets the capability as defined there; consider archiving `ema-api-stub` before or alongside this change so the merge lands cleanly.

## Impact

- `code/ema-api-stub/src/main/resources/scenarios/203000001234.json` — new interactions
- `code/ema-api-stub/src/main/resources/scenarios/module-health-*-203000001234.json` (3 files) — `ecuId` reassigned to eliminate the `loadDefault()` collision
- `code/ema-companion/app/src/test/.../feature/home/HomeProductionIntegrationTest.kt` — rewritten to use the embedded stub
- New integration tests for `HourlyEnergyRepository` / `DailyEnergyRepository` under `code/ema-companion/app/src/test/`
- `code/ema-companion/app` — `SettingsFragment`, `fragment_settings.xml` (new debug-only action), `build.gradle.kts` (`buildFeatures.buildConfig` + `STUB_PORT` build field), a `BuildConfig`-gated code path
- `code/ema-companion/maestro/a-home-screen.yaml` — extended with the stub-shortcut + real-data assertions
- `.github/workflows/ci.yml` — `e2e` job launches the stub in the background before Maestro
- `docs/ema-api-stub/README.md`, `docs/getting-started.md`
- No change to ADR-009 budget accounting — stub calls are not real API calls and are not counted
