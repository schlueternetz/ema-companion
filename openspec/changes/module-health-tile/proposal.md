## Why

Solar array owners need visibility into individual module performance to detect and address hardware degradation early. Current production stats only show system-wide data, missing module-level issues. Adding a module health status tile with background notifications enables proactive monitoring and faster troubleshooting.

## What Changes

- **New tile on Home screen** displaying module health status: green checkmark (all modules producing in last 24h), yellow warning (any module not producing in last 72h), red alert (any module not producing >72h)
- **Tap-to-reveal details** showing which modules are offline and for how long (hours)
- **Background check every 12 hours** when app is in background, with local notifications on yellow/red status changes
- **Efficient API usage** leveraging a single module summary endpoint to retrieve all module production timestamps
- **Developer-tunable config** (`AppConfig.kt`): per-endpoint poll intervals — `PRODUCTION_FETCH_INTERVAL` (10 min) and `MODULE_HEALTH_CHECK_INTERVAL` (24h) — each drives both the repository throttle and (for module health) the WorkManager job interval from one place
- **WorkManager background task** with required permissions (`SCHEDULE_EXACT_ALARM` for precise 24h intervals)
- **Updated EMA API stub** with test scenarios for: (1) all healthy, (2) one module offline 24h, (3) one module offline 90h
- **Free notification options** documented (local notifications as MVP baseline; Gmail and Firebase alternatives documented for future phases)

## Capabilities

### New Capabilities
- `module-health-status`: Determine health state (green/yellow/red) based on module production timestamps in last 24/72+ hours
- `module-health-tile`: UI tile on Home screen showing module health with color-coded status and tap-to-expand detail view
- `module-health-notifications`: Local notifications on status change (yellow info, red alert) and background periodic check with 24-hour throttle
- `module-summary-api`: Single API endpoint to fetch module production timestamps efficiently
- `api-client`: WorkManager dependency; timeout application to OkHttpClient via `AppConfig`
- `android-permissions`: SCHEDULE_EXACT_ALARM permission for precise background task scheduling

### Modified Capabilities
- `home-screen-layout`: Adds new tile to Home screen layout below current production tile
- `api-client`: Adds support for timeouts via `AppConfig` constants, includes WorkManager dependency for background tasks

## Impact

- **Frontend**: New `ModuleHealthTile` fragment/view component, detail modal, Home layout update
- **Data**: New `ModuleHealthRepository` for status calculation and persistence; new `ModuleHealthState` model
- **Background work**: New WorkManager periodic task for 24-hour check; `SCHEDULE_EXACT_ALARM` permission added
- **Notifications**: New notification channel for module alerts; integration with Android NotificationManager (local notifications only, MVP)
- **API client**: New module summary endpoint call in `EmaApiClient`
- **Config**: New `core/AppConfig.kt` with `PRODUCTION_FETCH_INTERVAL` (10 min) and `MODULE_HEALTH_CHECK_INTERVAL` (24h)
- **Dependencies**: Add WorkManager 2.x to Gradle
- **Manifest**: Add `SCHEDULE_EXACT_ALARM` permission for background task scheduling
- **API stub**: New test scenarios for module production states
- **Testing**: New unit tests for status logic, Robolectric tests for UI, API stub scenarios
