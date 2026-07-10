## 1. Stub Scenario Fixtures

- [x] 1.1 Append `hourly` (`energy_level=hourly`) and `daily` (`energy_level=daily`) interactions to `code/ema-api-stub/src/main/resources/scenarios/203000001234.json`, directly after the existing `minutely` interaction, with response bodies consistent with the Good Data persona (10 kW array); update the file's `description` to note hourly/daily-focused tests should use their own isolated fixtures rather than this shared file
- [x] 1.2 Write a dedicated no-data fixture (`code:1001`) for `energy_level=hourly`/`daily`, and a dedicated two-calendar-month `energy_level=daily` fixture, as small standalone JSON scenario files (not part of the bundled `203000001234.json`)
- [x] 1.3 Write/extend `ema-api-stub` unit tests (`MatchingEngineTest` or a new test class) asserting these new interactions match and respond correctly, loaded via `ScenarioLoader.loadFromDirectory` against a scoped `TemporaryFolder` — not `loadDefault()`
- [x] 1.4 Fix the `loadDefault()` duplicate-ECU-id collision: reassign each module-health scenario file's internal `ecuId` (and the ECU id used inside its own interactions) to a distinct value — `module-health-all-healthy-203000001234.json` → `203000005678`, `module-health-one-offline-1day-203000001234.json` → `203000009012`, `module-health-one-offline-3day-203000001234.json` → `203000003456`; leave filenames unchanged. Confirm `ModuleHealthIntegrationTest` (which mocks HTTP directly, not via these files) still passes unaffected
- [x] 1.5 Run `cd code/ema-api-stub && ./gradlew test`; confirm `ApplicationTest`/`GoodDataScenarioTest` (which call `loadDefault()`) now pass with no duplicate-ECU-id error

## 2. Embedded Integration Tests — Hourly/Daily Repositories

- [x] 2.1 Add an embedded-stub integration test for `HourlyEnergyRepository`: construct a `MatchingEngine`/`testApplication` from a scoped hourly fixture, drive `OkHttpEmaApiClient.getHourlyEnergy` through it, assert the parsed `HourlySnapshot`
- [x] 2.2 Add an embedded-stub integration test for `DailyEnergyRepository` using the multi-month fixture (task 1.2): assert both months are fetched and merged into one `DailySnapshot`
- [x] 2.3 Add an embedded-stub integration test exercising the `code:1001` no-data fixture for both `getHourlyEnergy` and `getDailyEnergy`, asserting an empty-map `Success` (not an error), matching `parseHourlyEnergy`/`parseDailyEnergy`'s existing `1001` handling

## 3. Rewrite HomeProductionIntegrationTest

- [x] 3.1 Replace the hand-typed `canonicalBody` string in `HomeProductionIntegrationTest` with the real `203000001234.json` resource's `minutely` interaction response body (load and extract via `ScenarioLoader`/`kotlinx.serialization`, not a duplicated literal)
- [x] 3.2 Confirm the test still passes unchanged otherwise (same assertions on `HomeFragment` rendering `8000 W`)

## 4. Debug-Only "Use Local Stub" Settings Action

- [x] 4.1 Enable `buildFeatures { buildConfig = true }` in `code/ema-companion/app/build.gradle.kts`; add `buildConfigField("String", "STUB_PORT", "\"${'$'}{project.findProperty("STUB_PORT") ?: System.getenv("STUB_PORT") ?: "8080"}\"")` so the port is read from the same `STUB_PORT` property/env-var name the stub server itself reads, defaulting to `8080`
- [x] 4.2 Add a `setting_use_local_stub` action view next to the Base URL field in `fragment_settings.xml`, gated to debug builds only (removed/`GONE` when `!BuildConfig.DEBUG`)
- [x] 4.3 Wire its click listener in `SettingsFragment.wireBaseUrl` (alongside the existing reset button): call `repository.setBaseUrl("http://10.0.2.2:${BuildConfig.STUB_PORT}/user/api/v2/")`, refresh the displayed value, and `invalidateApiThrottle()` — reusing the same path the reset button already uses
- [x] 4.4 Write a Robolectric test asserting the action is visible under a debug-configured build and `GONE` under a release-configured build (or equivalent `BuildConfig.DEBUG` seam), and a second test asserting activation sets and persists the local stub URL without touching `BASE_URL_DEFAULT` or the Reset-to-default behavior

## 5. Maestro Flow Against the Stub

- [x] 5.1 Extend `code/ema-companion/maestro/a-home-screen.yaml`: after the existing placeholder-path assertions, tap `setting_use_local_stub`, edit ECU-ID (index 3) to `203000001234` (the stub's canonical Good Data id; leave App-ID/App-Secret/System-ID as their existing dummy values since the stub wildcards `sid` and never verifies HMAC), then re-open Home
- [x] 5.2 Assert real data renders after the reload: `tile_current_production` shows `8000 W`, and the hourly/history chart sections show populated (non-empty-state) charts — reuse the existing `scrollUntilVisible` pattern for each section
- [x] 5.3 Update `.github/workflows/ci.yml`'s `e2e` job: before `maestro test maestro/`, start `./gradlew run` from `code/ema-api-stub` in the background and poll (bounded `curl` retry loop) until the port responds; fail the job loudly if it never comes up rather than letting Maestro race an unready server
- [x] 5.4 Run the extended flow locally against a manually-started stub (`cd code/ema-api-stub && ./gradlew run`) and confirm it passes before relying on CI

## 6. Documentation

- [x] 6.1 Update `docs/ema-api-stub/README.md`'s "ECU-id scenarios" table to include the module-health scenarios (with their new distinct ECU ids) and the new hourly/daily interactions (it currently only lists `minutely`)
- [x] 6.2 Add a short section to `docs/getting-started.md` framing the stub as the dev/test environment and the real EMA API as prod, documenting the debug-only "Use local stub" action, the `STUB_PORT` property, starting the stub before running `/qa`'s Maestro step locally, and linking to `docs/ema-api-stub/README.md` for manual setup on physical devices

## 7. Lint, QA

- [x] 7.1 Run `cd code/ema-api-stub && ./gradlew test ktlintCheck`; confirm all tests pass, including `ApplicationTest`/`GoodDataScenarioTest` (`loadDefault()` is fixed by task 1.4 — no more known-failing tests here)
- [x] 7.2 Run `./gradlew ktlintCheck` in `code/ema-companion`; fix all violations
- [x] 7.3 Run `./gradlew testDebugUnitTest` in `code/ema-companion`; confirm all tests pass
- [x] 7.4 Run `/qa` (full pre-flight: unit tests + ktlint + debug build/install + Maestro flows), with the stub running locally first; confirm all Maestro flows pass on the emulator, including the extended stub-backed assertions in `a-home-screen.yaml`
