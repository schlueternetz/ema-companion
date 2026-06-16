## Why

The app is fully configurable but makes no real API calls — the Home screen still shows a "Hello world" placeholder and the Settings progress bar shows a hardcoded consumption figure. This change makes the first real EMA API call: fetching the current production value and showing it on Home. It also establishes the properly separated API-client foundation (request signing, request counting, throttling, logging) that every later API feature builds on.

## What Changes

- Replace the Home "Hello world" placeholder with **"Current Production: {value} {unit}"**, sourced from a real EMA API call.
- Introduce a properly separated **EMA API client** in `core/`: HMAC request signing (per manual §2.2), a typed result, and a single `getCurrentProduction` operation against the configured ECU energy (minutely) endpoint.
- **Count EMA API requests per calendar month** and persist the count (with the month it belongs to); reset automatically on month rollover.
- The Settings consumption **progress bar now reflects the real persisted request count** instead of the hardcoded `800`.
- **Throttle** this endpoint to **at most 1 call per 10 minutes**; a fresh value is fetched **when the app opens and when the Home screen becomes highlighted**, otherwise the cached value is shown.
- Show an **app banner** on the Home screen when the API is unreachable (network error).
- Add a **Logs section in Settings** listing each API call (timestamp, endpoint, duration, success); tapping a log shows the pretty-printed request and full response, with any value that is masked on the Settings screen (e.g. App Secret) never shown in plain text.
- Add an **integration test** that drives the real HTTP path against the API stub and verifies the production value is displayed on Home.

## Capabilities

### New Capabilities
- `ema-api-client`: A separated, signed EMA API client (HMAC signing, base-URL/path handling, typed success/error results) exposing the current-production read.
- `current-production-display`: Home screen shows the current production value and unit, fetched on app open and on Home highlight, throttled to 1 call / 10 min, with a network-error banner.
- `api-request-counter`: Per-calendar-month counting and persistence of EMA API requests, resetting on month change.
- `api-call-log`: A Settings "Logs" section recording each API call and showing request/response detail on tap, with masked fields kept masked.

### Modified Capabilities
- `api-request-limit`: The monthly-consumption progress bar SHALL be driven by the real persisted request count (from `api-request-counter`) instead of the hardcoded placeholder value.

## Impact

- **New code:** `core/api/` (client, signer, models, result, `ApiUsageRepository` for counter+throttle), `core/api/log/` (log model + `ApiCallLogRepository`), Home wiring, Settings Logs UI. The usage and log repositories each use their own plain `SharedPreferences` file, separate from `SettingsRepository`.
- **Modified code:** `HomeFragment` + `fragment_home.xml` (production display + banner), `SettingsFragment` (`updateApiRequestProgress` reads the real counter from `ApiUsageRepository`; factory reset also clears the usage + log stores; new Logs section + layout), version catalog + `app/build.gradle.kts` (HTTP client + coroutines). `SettingsRepository` is **not** used to store API usage or logs.
- **Dependencies:** add OkHttp (HTTP + signing interceptor) and kotlinx-coroutines; MockWebServer for the integration test.
- **Specs:** modifies `api-request-limit`; UI changes trigger the `write-user-guide` skill.
