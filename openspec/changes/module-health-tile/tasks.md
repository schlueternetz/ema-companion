## 1. API & Data Models

- [x] 1.1 Add `ModuleHealthState` data class with status, offlineModules list, and computationTimestamp
- [x] 1.2 Add `Module` data class with `uid: String` and `offlineDays: Int` (consecutive days with 0 kWh, 1–3)
- [x] 1.3 Create `ModuleHealthRepository` with `computeStatus()` function and SharedPreferences backing
- [x] 1.4 Add batch inverter energy API call to `EmaApiClient`: `GET /user/api/v2/systems/{sid}/devices/inverter/batch/energy/{eid}?energy_level=energy&date_range={date}` — parse `"{uid}-{channel}-{kWh}"` entries, group by uid, sum kWh across channels
- [x] 1.5 Create `ema_module_health` SharedPreferences for computed status persistence
- [x] 1.6 Create `ema_module_health_daily` SharedPreferences for per-day batch energy cache, keyed by `daily_{yyyy-MM-dd}`, value = JSON string `{"uid":kWh,...}`

## 2. AppConfig

- [x] 2.1 Create `core/AppConfig.kt` with `PRODUCTION_FETCH_INTERVAL` (10 min) and `MODULE_HEALTH_CHECK_INTERVAL` (24h)
- [x] 2.2 Update production repository throttle to use `AppConfig.PRODUCTION_FETCH_INTERVAL` instead of hardcoded 10 min
- [x] 2.3 Write unit test: verify both constants are read correctly and match expected durations

## 3. API Stub Updates

- [x] 3.1 Create `module-health-all-healthy-EDU.json` scenario: batch energy response where all inverters have non-zero kWh today
- [x] 3.2 Create `module-health-one-offline-1day-EDU.json` scenario: one inverter has 0 kWh today but non-zero yesterday (YELLOW)
- [x] 3.3 Create `module-health-one-offline-3day-EDU.json` scenario: one inverter has 0 kWh for today, yesterday, and day-before (RED)
- [x] 3.4 Test each scenario with the mock API server (verify responses load correctly)

## 4. Module Health Status Logic

- [x] 4.1 Implement `ModuleHealthRepository.computeStatus()`: build expected UID set = union of all UIDs across the 3-day window; treat absent UIDs on any day as 0 kWh; GREEN if all expected UIDs have >0 kWh on every day; YELLOW if any have 0/absent for 1–2 consecutive days; RED if any have 0/absent for 3 consecutive days
- [x] 4.2 Write unit tests for status computation (all three states, boundary day counts, empty list, inverter absent from today's response treated as 0 kWh)
- [x] 4.3 Implement throttle check: do not fetch if `now - lastCheckEpochMs < AppConfig.MODULE_HEALTH_CHECK_INTERVAL`
- [x] 4.4 Implement incremental fetch: for each date in the 3-day window, skip if cached (past days only); always re-fetch today; write each successful result to `ema_module_health_daily` before the next call
- [x] 4.5 Implement cache pruning: after each check, remove `ema_module_health_daily` entries older than 3 days
- [x] 4.6 Implement state persistence: save/retrieve computed status and offline module list from `ema_module_health` SharedPreferences
- [x] 4.7 Write unit test: verify normal run makes exactly 1 API call (yesterday + day-before cached)
- [x] 4.8 Write unit test: verify catch-up case (2 days missing) makes exactly 2 calls
- [x] 4.9 Add integration test: mock EMA API, verify status computed and persisted correctly

## 5. Array Timezone Setting

- [x] 5.1 Add `arrayTimezone: String` to `SettingsRepository` (default: `TimeZone.getDefault().id`)
- [x] 5.2 Add timezone selector row to Settings UI (searchable list of all IANA timezone IDs)
- [x] 5.3 Write Robolectric test: verify default is system timezone, verify saved value is read back correctly

## 6. Home Screen Tile UI

- [x] 6.1 Create `ModuleHealthTile` view component with status icon, label, and "Checked [date] at [time]" subtitle
- [x] 6.2 Add layouts: green checkmark, yellow warning icon, red alert icon, gray error/unknown state
- [x] 6.3 Create tile detail modal dialog layout showing offline inverters with "no production for X days"
- [x] 6.4 Wire tile to `ModuleHealthRepository.getStatus()` (live data observer)
- [x] 6.5 Implement tile tap: show detail modal on YELLOW/RED; do nothing (no ripple) on GREEN
- [x] 6.6 Add tile to Home screen layout XML below current production tile
- [x] 6.7 Write Robolectric tests for tile display (green/yellow/red states, detail modal, tap handling, subtitle timestamp)
- [x] 6.8 Verify layout on tablet (landscape/portrait) and phone

## 7. WorkManager & Permissions

- [x] 7.1 Add WorkManager 2.x dependency to `app/build.gradle.kts`
- [x] 7.2 Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` to AndroidManifest.xml
- [x] 7.3 Request `POST_NOTIFICATIONS` at runtime on API 33+ (e.g. from MainActivity on first launch)
- [x] 7.4 Create `ModuleHealthWorker` using WorkManager for 24-hour periodic task
- [x] 7.5 On first schedule (or after timezone change): calculate `initialDelay` = milliseconds until next 8pm in array timezone; enqueue `PeriodicWorkRequest` with `AppConfig.MODULE_HEALTH_CHECK_INTERVAL` interval and `ExistingPeriodicWorkPolicy.KEEP`
- [x] 7.6 On timezone setting change: cancel existing work and re-enqueue with new `initialDelay` to realign to 8pm in the new timezone
- [x] 7.7 Verify WorkManager does NOT re-reset the schedule on every app start (KEEP policy confirmed)

## 8. Notifications

- [x] 8.1 Implement notification channel creation (importance HIGH) on API 26+
- [x] 8.2 Implement notification logic: post on every YELLOW or RED check; replace previous notification (do not stack); clear on GREEN
- [x] 8.3 Gate notification posting behind `POST_NOTIFICATIONS` permission check on API 33+
- [x] 8.4 Write test: verify YELLOW status posts notification
- [x] 8.5 Write test: verify YELLOW on two consecutive checks posts notification both times (no suppression)
- [x] 8.6 Write test: verify GREEN clears existing notification without posting a new one
- [x] 8.7 Add notification title/text strings to strings.xml (EN + DE)

## 9. Factory Reset

- [x] 9.1 Add `ema_module_health` and `ema_module_health_daily` SharedPreferences to the factory-reset clear list (alongside existing `ema_api_usage` and `ema_api_log`)
- [x] 9.2 Write test: verify factory reset clears both module health stores

## 10. Integration & Polish

- [x] 10.1 Verify `INTERNET` permission already present in AndroidManifest.xml
- [x] 10.2 Add tile title/label and timezone setting strings to strings.xml (EN + DE in values-de)
- [x] 10.3 Test full flow on emulator: open Home, tap tile, verify details, check background task scheduled
- [x] 10.4 Run `./gradlew ktlintCheck` and fix any lint errors
- [x] 10.5 Verify all Robolectric tests pass: `./gradlew testDebugUnitTest --rerun`
- [x] 10.6 Verify integration tests pass against API stub
- [x] 10.7 Add accessibility labels to tile icons and detail modal (48dp touch targets minimum)
- [x] 10.8 Verify AppConfig intervals drive both throttle checks and WorkManager job (grep for hardcoded values — none should remain outside AppConfig)
- [ ] 10.9 Test on real device: background notification triggers after 24h, POST_NOTIFICATIONS dialog appears on API 33+

## 11. Notification Methods Documentation

- [x] 11.1 Create `docs/notification-methods.md` documenting:
  - Local notifications (current MVP solution, zero cost, no backend)
  - Gmail API emails (user-authenticated, zero backend cost, phase 2)
  - Firebase Cloud Messaging (recommended for backend-push alternative, free tier, requires backend)
  - Email via SendGrid / AWS SES (free tier available, ~$0.10/1000 emails, backend costs)
  - Webhook to IFTTT / Applet / Zapier (free, event-driven, no backend code)
  - Telegram / Discord webhooks (free, instant, easy setup)
- [x] 11.2 Include setup cost, latency, and reliability tradeoffs for each method

## 12. Documentation & Handoff

- [x] 12.1 Update `docs/user-guide/user-guide.md` to include Module Health Tile and Array Timezone sections
- [x] 12.2 Add screenshots (green/yellow/red states and detail modal) to user guide
- [x] 12.3 Update API documentation or ADRs as needed
- [x] 12.4 Verify all tests pass: unit, Robolectric, integration (API stub)
- [ ] 12.5 Test on real device: background notification triggers after 24h, permissions work correctly
