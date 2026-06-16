## Context

The app is configured but does no real networking. `HomeFragment` shows a static placeholder; `SettingsFragment.updateApiRequestProgress()` uses a hardcoded `consumed = 800`. The `ema-api-stub` (separate Gradle project) already implements exactly one endpoint — ECU energy in period, minutely — returning a `data.power` array of watt samples, with the bundled "Good Data" scenario ending at `8000`. The app has no HTTP client, no coroutines, and no ViewModel layer; Fragments talk to `SettingsRepository` (Encrypted)SharedPreferences directly.

Constraints: minSdk 31; English + German only (all strings in resources); WCAG 2.1 AA + ATF checks on UI; AI-TDD (failing test first); ktlint must pass. Per ADR-002, pure logic → JUnit, Android-context logic → Robolectric, integration tests hit the local mock API over real HTTP with a configurable base URL, Maestro reserved for critical flows.

## Goals / Non-Goals

**Goals:**
- One real, signed API call surfacing current production on Home.
- A cleanly separated `core/api/` client reusable by future features.
- Real monthly request counting driving the existing progress bar.
- 10-minute throttle; fetch on app open + Home highlight.
- Network-error banner on Home.
- Settings Logs section with masked-safe detail.
- Integration test over real HTTP against the stub's response.

**Non-Goals:**
- Other endpoints (system summary, meters, storage, inverters).
- Historical graphs, widgets, notifications.
- Token/refresh auth (the API uses per-request HMAC, not JWT).
- A full DI framework or app-wide ViewModel/Flow architecture.
- Configurable throttle/limit logic beyond the existing limit setting.

## Decisions

### D1: `core/api/` package, UI-independent client
Introduce `core/api/` (first use of `core/` per ADR-004). Shape:
- `EmaApiClient` (interface) — `suspend fun getCurrentProduction(): ApiResult<ProductionSnapshot>`.
- `OkHttpEmaApiClient` (impl) — builds the request, signs it, executes, parses, records the log + counter.
- `EmaRequestSigner` — pure function producing the five `X-CA-*` headers from method, last path segment, appId, secret, timestamp, nonce. Pure → JUnit-testable with a fixed clock/nonce.
- `ApiResult` — `sealed`: `Success(data)`, `NetworkError`, `ApiError(code/status)`. No exceptions cross the boundary.
- `ProductionSnapshot(powerWatts: Int)` plus a `unit` of `W`.

Rationale: the manual's signing (last-segment, ms timestamp) is error-prone; isolating it in a pure signer makes it the cheapest test layer. Fragments never see HTTP.

**Alternative considered:** HttpURLConnection (no new dep). Rejected — manual header/threading/error plumbing is more code and harder to test than an OkHttp `Interceptor`; "best practices" was explicitly requested.

### D2: OkHttp + signing interceptor; kotlinx-coroutines
Add OkHttp and kotlinx-coroutines-android to the version catalog. Signing is an OkHttp `Interceptor` that derives the last path segment from the outgoing request and adds the `X-CA-*` headers — so every call is signed uniformly. Fetches run on `Dispatchers.IO` via `viewLifecycleOwner.lifecycleScope`.

**Alternative considered:** Retrofit. Rejected for one endpoint — Retrofit's value is many typed endpoints; OkHttp + manual parse is leaner here and still extensible.

### D3: Throttle + counter + logging live in a repository, not the Fragment
Add `ProductionRepository` (in `core/api/`) orchestrating: check throttle (persisted `lastFetchEpochMs`) → if due, call client → on issue, increment counter + append log → cache + return snapshot. The 10-min window, monthly counter, and last-fetch timestamp are persisted in the API context's own stores (`ApiUsageRepository` / `ApiCallLogRepository`, see D5–D6), **not** in `SettingsRepository`, so throttle and count survive restarts and Home recreation. Home only renders state and calls `repository.refresh()`.

Rationale: keeps the no-ViewModel convention while making throttle/count robust to lifecycle churn and unit-testable with a fake clock + fake client.

**Alternative considered:** A Home `ViewModel` with `StateFlow`. Rejected to match the codebase's lightweight Fragment+Repository style and keep the diff surgical; persisted throttle already covers rotation.

### D4: Trigger points = `HomeFragment.onResume`
App-open lands on Home (when configured) and Home-highlight both surface as `onResume`. So `onResume` calls `repository.refresh()`, which internally no-ops within the throttle window. This single trigger satisfies both "app opens" and "Home highlighted" without nav-listener wiring. (Per lessons-learned, drive the real path in tests.)

### D5: Request counter + throttle store (separate from settings)
`ApiUsageRepository` (in `core/api/`) owns `apiRequestCountMonth` (e.g. `"2026-06"`), `apiRequestCount` (int), and `lastFetchEpochMs`, persisted in **its own plain `SharedPreferences` file** (`ema_api_usage`) — not in `SettingsRepository`. On a counted request: if current `YYYY-MM` ≠ stored month, set month and count to 1; else increment. Month derived from `java.time.YearMonth.now()` (API 26+, fine at minSdk 31). `SettingsFragment.updateApiRequestProgress()` reads the count from this repository instead of `800`. Rationale: counting and throttling are API-domain operational state, not user settings; keeping them out of `SettingsRepository` is the DDD-correct split. The file is tiny, so the progress-bar read on Settings open stays cheap, and it carries no secret so it needs no encryption.

### D6: Log store + masking (separate from settings)
`ApiCallLogRepository` (in `core/api/log/`) owns `ApiCallLog(timestampMs, endpoint, durationMs, success, requestText, responseText)` as a bounded JSON array (newest-first, cap 100) in **its own plain `SharedPreferences` file** (`ema_api_log`) — deliberately not in `SettingsRepository`. Rationale: `SettingsRepository` uses `EncryptedSharedPreferences` and is loaded on every app/fragment start (`MainActivity.onCreate`); SharedPreferences loads the whole backing file into memory on first access, so co-locating up to 100 records of full request/response text would force decrypting that blob just to read a preference. The log file is read lazily — only when the client appends or the Logs screen opens — so it never touches the startup or settings-load path. The Logs UI reuses the existing Settings section styling; the list shows summary rows, tap opens an `AlertDialog` with pretty-printed (`JSONObject.toString(2)`) request/response. **Masking:** the stored `requestText` already redacts the App Secret (the signer's secret is never serialized; the `X-CA-Signature` is shown but the secret is not), and any echoed masked field is replaced with the same `••••`+last-4 rendering used by `SettingRowView`. Responses from this endpoint contain no secrets. Persisting pre-masked text guarantees the secret can never be reconstructed from logs, which is also why this file does not need encryption.

### D7: Integration test via real-HTTP loopback seeded with the stub scenario
Per ADR-002 (real HTTP, configurable base URL), the integration test starts OkHttp's `MockWebServer` on loopback, seeds it with the **exact** body from the stub's `203000001234.json` scenario, points the client's base URL at it, and asserts `HomeFragment` renders "Current Production: 8000 W". This exercises real socket I/O + signing + parsing + rendering while staying inside the app module's Robolectric suite. Running the standalone `ema-api-stub` is the higher-fidelity option but crosses project boundaries; the seeded MockWebServer uses the same canonical response so behavior matches.

## Risks / Trade-offs

- **Signing correctness (last segment, ms timestamp, nonce format)** → isolate in pure `EmaRequestSigner` with fixed-clock/fixed-nonce unit tests asserting the exact `stringToSign` and Base64 output.
- **MockWebServer fixture drifting from the real stub scenario** → load the assertion fixture from the same canonical JSON shape; document that the stub scenario is the source of truth so a drift is caught when the scenario changes.
- **Throttle hiding a real outage / stale value** → banner reflects the last *attempt's* error; successful cache still shown but the 10-min window bounds staleness, which is acceptable for production power.
- **Counting vs. logging consistency** → increment counter and append log at the same single point in `ProductionRepository` (only when a request is actually issued) so progress bar and Logs never disagree.
- **New deps (OkHttp, coroutines, MockWebServer)** → all mainstream, minSdk-31 compatible; MockWebServer is test-only.
- **Robolectric per-app-locale / async pitfalls (per lessons-learned)** → drive fetch through a `suspend` repository with an injected dispatcher so tests can run it synchronously.

## Migration Plan

Additive only — no data migration. The new stores default to empty/zero (counter starts at 0, throttle allows the first fetch). Because the counter/throttle and logs now live in their own files (not `SettingsRepository`), **factory reset must clear them explicitly**: `showFactoryResetDialog()` SHALL also clear `ApiUsageRepository` and `ApiCallLogRepository` alongside `SettingsRepository.clearAll()`. Rollback = revert the change; no persisted schema breakage. UI changes require invoking `write-user-guide` after implementation.

## Open Questions

_(resolved)_

- Display before first successful fetch: show a neutral "Current Production: — W" until the first success. **Confirmed.**
- Log retention cap: **100** records (newest-first, oldest dropped). **Confirmed.**
