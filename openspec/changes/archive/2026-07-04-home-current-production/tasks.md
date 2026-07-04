## 1. Dependencies & scaffolding

- [x] 1.1 Add `okhttp`, `kotlinx-coroutines-android`, and test-only `mockwebserver` to `gradle/libs.versions.toml` and wire them in `app/build.gradle.kts`
- [x] 1.2 Create the `core/api/` package under `feature`-sibling `core/` per ADR-004 and confirm `assembleDebug` still builds

## 2. Request signing (pure, AI-TDD)

- [x] 2.1 Write a failing JUnit test for `EmaRequestSigner` asserting the exact `stringToSign` (`{ts}/{nonce}/{appId}/{lastSegment}/{method}/{method}`) and Base64 HMAC-SHA256 output for a fixed timestamp/nonce/secret
- [x] 2.2 Implement `EmaRequestSigner` producing the five `X-CA-*` headers (timestamp in ms, 32-char dashless nonce, `HmacSHA256`); make the test pass
- [x] 2.3 Add a test for last-path-segment extraction (e.g. `.../ecu/energy/203000001234` → `203000001234`)

## 3. API client (AI-TDD against MockWebServer)

- [x] 3.1 Define `ApiResult` (`Success`/`NetworkError`/`ApiError`) and `ProductionSnapshot(powerWatts)`
- [x] 3.2 Write a failing client test (MockWebServer) for the success path: minutely response `data.power=[...,8000]` → `Success(8000)`; assert the signed headers are present on the received request
- [x] 3.3 Write failing tests for: empty `power` → no-data/`ApiError`; non-zero `code`/HTTP error → `ApiError`; unreachable server → `NetworkError`; not-configured → config error without issuing a request
- [x] 3.4 Implement `EmaApiClient`/`OkHttpEmaApiClient` (signing inline, `Dispatchers.IO`, parse `data.power` last element); make tests pass

## 4. Request counter + throttle store (AI-TDD, Robolectric/SharedPreferences)

- [x] 4.1 Create `ApiUsageRepository` in `core/api/` backed by its **own** plain `SharedPreferences` file (`ema_api_usage`) holding `apiRequestCountMonth`, `apiRequestCount`, `lastFetchEpochMs`, with a `clear()`; do NOT add these to `SettingsRepository`
- [x] 4.2 Write failing tests for: increment within month; persistence across reads; reset on month rollover (count → 1)
- [x] 4.3 Implement the increment/rollover logic (keyed on `YearMonth.now()`); make tests pass

## 5. Logging (AI-TDD)

- [x] 5.1 Define `ApiCallLog(timestampMs, endpoint, durationMs, success, requestText, responseText)` and a bounded (newest-first, cap 100) JSON-backed `ApiCallLogRepository` in `core/api/log/`, backed by its **own** plain `SharedPreferences` file (`ema_api_log`) with a `clear()` — separate from `SettingsRepository`
- [x] 5.2 Write failing tests for: append a record; bound/cap behavior; masking so the App Secret never appears in plain text in stored `requestText`
- [x] 5.3 Implement persistence + masking; make tests pass

## 6. Throttle + orchestration (AI-TDD)

- [x] 6.1 Write failing `ProductionRepository` tests with a fake clock + fake client: fetch when window elapsed, no-op (cached) within 10 min (using `ApiUsageRepository.lastFetchEpochMs`), counter+log incremented only on an issued request
- [x] 6.2 Implement `ProductionRepository.refresh()` (throttle check → client call → increment `ApiUsageRepository` counter + append to `ApiCallLogRepository` → cache); make tests pass

## 7. Home screen (UI, AI-TDD + Robolectric/ATF)

- [x] 7.1 Add string resources (en + de) for "Current Production: %1$s %2$s", neutral placeholder, and the network-issue banner; update `fragment_home.xml` (production TextView + banner view) with content descriptions and 48dp targets
- [x] 7.2 Write failing Robolectric tests: Home renders "Current Production: 8000 W" from a fake repository; banner shows on `NetworkError` and clears on later success; include `AccessibilityValidator` (ATF) checks
- [x] 7.3 Implement `HomeFragment` (`onResume` → `repository.refresh()` via `viewLifecycleOwner.lifecycleScope`, render snapshot, toggle banner); make tests pass

## 8. Settings progress bar (Modify, AI-TDD)

- [x] 8.1 Write a failing test that `updateApiRequestProgress()` uses the persisted monthly count from `ApiUsageRepository` (not 800)
- [x] 8.2 Replace the hardcoded `consumed = 800` with the real counter read from `ApiUsageRepository`; make the test pass
- [x] 8.3 Write a failing test that factory reset clears API usage + logs; update `showFactoryResetDialog()` to also call `ApiUsageRepository.clear()` and `ApiCallLogRepository.clear()`; make it pass

## 9. Settings Logs section (UI, AI-TDD + ATF)

- [x] 9.1 Add a "Logs" section to `fragment_settings.xml` (list + empty-state) with strings (en + de) and accessibility attributes
- [x] 9.2 Write failing Robolectric tests: list shows recorded calls newest-first with timestamp/endpoint/duration/success; empty state when none; tap opens pretty-printed detail; masked field stays masked in detail; ATF checks
- [x] 9.3 Implement the Logs list + detail dialog (`JSONObject.toString(2)` pretty-print, reuse `SettingRowView` masking rendering); make tests pass

## 10. Integration test (stub, real HTTP)

- [x] 10.1 Add an integration test that starts MockWebServer seeded with the canonical `203000001234.json` minutely body, points the client base URL at it, and asserts Home displays "Current Production: 8000 W" over real HTTP

## 11. Verification & docs

- [x] 11.1 Run `./gradlew testDebugUnitTest` and `./gradlew ktlintCheck` — all green
- [x] 11.2 Invoke the `write-user-guide` skill to update the user guide for the Home production display and Settings Logs section
- [x] 11.3 Build + install (`./gradlew installDebug`) and confirm Home shows the value against the running stub (deployed to Lenovo_Tab_11_Plus emulator; visual confirmation done manually by the user)
