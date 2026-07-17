## 1. Build spike — validate Glance/Compose on this toolchain

- [x] 1.1 Add `androidx.glance:glance-appwidget` (+ required Compose compiler plugin/runtime) to `libs.versions.toml` and `app/build.gradle.kts`; determine whether it's compatible with AGP 9.2.1's built-in Kotlin support (no standalone `org.jetbrains.kotlin.android` plugin currently applied)
- [x] 1.2 Build a single trivial `GlanceAppWidget` (static text only) and confirm `assembleDebug` + `ktlintCheck` succeed
- [x] 1.3 In the same spike, confirm how Glance resolves day/night for "System" mode (`GlanceTheme.colors` vs. an explicit `Configuration.uiMode` check) and how to force a fixed Light/Dark `ColorProviders` scheme regardless of system theme
- [x] 1.4 If incompatible, stop and re-open design.md's "Open Questions" to switch to classic RemoteViews before continuing; otherwise proceed

## 2. ADR-009 budget update

- [x] 2.1 Update the allocation table in `docs/adr/009-ema-api-call-budget.md` per design.md's Decision 6 (Hourly line revised to ~360/month, Daily line unchanged, new total ~570/month, headroom ~430/month)

## 3. Shared widget infrastructure (`feature/widgets/`)

- [x] 3.1 Write a failing unit test for a `WidgetUpdater` helper that wraps `GlanceAppWidgetManager`/`updateAll()` for the three widget classes, then implement it
- [x] 3.2 Write a failing test for a bitmap chart renderer that lays out a detached MPAndroidChart `LineChart`/`BarChart` off-screen at a given pixel size and returns a `Bitmap`, reusing the same styling constants (colors, month palette) as `HomeFragment`; then implement it
- [x] 3.3 Write a failing test for a helper that derives "today's total" from `HourlySnapshot` (sum of hours), for reuse by the Summary widget; then implement it (or confirm `HomeFragment`'s existing computation can be extracted/shared)
- [x] 3.4 Write a failing test for a shared `WidgetTheme` provider that reads `SettingsRepository.getDisplayMode()` and returns the corresponding `ColorProviders` (system-following, forced light, or forced dark); then implement it
- [x] 3.5 Write a failing test for a shared helper mapping `FetchError` (NETWORK/AUTH/API) to the widget error message text used by all three widgets (reusing existing string resources where applicable); then implement it
- [x] 3.6 Write a failing test for a shared tap-target helper that, given "not configured" / a fetch error / normal state, returns `"settings"` or `"home"`; then implement it and a `PendingIntent` builder that sets it as the `EXTRA_WIDGET_TARGET` extra on the `MainActivity` intent

## 4. Today's Production widget

- [x] 4.1 Write a failing Robolectric/Glance-content test asserting the widget renders the hourly chart + today's total from `HourlyEnergyRepository.currentState()`
- [x] 4.2 Write a failing test for the "not configured" neutral placeholder state
- [x] 4.3 Write failing tests that a network/auth/other fetch error replaces the chart and total with the corresponding error message, and that the chart reappears after the next successful fetch
- [x] 4.4 Write a failing test that the chart's Y-axis maximum equals `SettingsRepository.getSystemCapacity()` when set, and auto-scales to the data max otherwise
- [x] 4.5 Write a failing test that the widget renders using the `WidgetTheme` provider's colours (System/Light/Dark per Display Mode)
- [x] 4.6 Implement `TodayProductionWidget` (`GlanceAppWidget`) + `TodayProductionWidgetReceiver` to pass 4.1–4.5
- [x] 4.7 Add widget layout/info resources (`res/xml/today_production_widget_info.xml`), preview image, and picker label/description strings (English + German, per ADR-003)
- [x] 4.8 Wire tap target via the shared tap-target helper (Home normally; Settings when not configured or showing an error)
- [x] 4.9 Register `<receiver>` in `AndroidManifest.xml`

## 5. Production Summary widget

- [x] 5.1 Write failing tests for today/month/last-30-days totals rendering, per-figure neutral placeholder when a period has no data, and the "not configured" state
- [x] 5.2 Write failing tests that an hourly fetch error replaces only "Today" with an error message (Month/Last-30-Days unaffected), a daily fetch error replaces only "This Month"/"Last 30 Days" (Today unaffected), and each clears on its next successful fetch
- [x] 5.3 Write a failing test that the widget renders using the `WidgetTheme` provider's colours (System/Light/Dark per Display Mode)
- [x] 5.4 Implement `ProductionSummaryWidget` + receiver to pass 5.1–5.3
- [x] 5.5 Add widget layout/info resources, preview image, and picker label/description strings (English + German)
- [x] 5.6 Wire tap target via the shared tap-target helper (Home normally; Settings when not configured or any figure is showing an error)
- [x] 5.7 Register `<receiver>` in `AndroidManifest.xml`

## 6. Production History widget

- [x] 6.1 Write failing tests for the bar chart (multi-day, single-day, no-data placeholder) using `DailyEnergyRepository.currentState()` and the configured history window, and the "not configured" state
- [x] 6.2 Write a failing test that the chart window/title follows `SettingsRepository.getHistoricDataDays()` (not the 45-day default) when the user has configured a different value
- [x] 6.3 Write a failing test that the chart's Y-axis maximum equals `SettingsRepository.getSystemCapacity()` when set, and auto-scales to the data max otherwise
- [x] 6.4 Write failing tests that a network/auth/other fetch error replaces the chart with the corresponding error message, and that the chart reappears after the next successful fetch
- [x] 6.5 Write a failing test that the widget renders using the `WidgetTheme` provider's colours (System/Light/Dark per Display Mode)
- [x] 6.6 Implement `ProductionHistoryWidget` + receiver to pass 6.1–6.5
- [x] 6.7 Add widget layout/info resources, preview image, and picker label/description strings (English + German)
- [x] 6.8 Wire tap target via the shared tap-target helper (Home normally; Settings when not configured or showing an error)
- [x] 6.9 Register `<receiver>` in `AndroidManifest.xml`

## 7. Background refresh worker

- [x] 7.1 Write a failing test (modeled on existing `ModuleHealthWorker` tests) for `WidgetRefreshWorker`: hourly branch calls `HourlyEnergyRepository.refresh(force = false)` at most every 2 hours, with no time-of-day restriction (a night-time gate is a separate, later change — see design.md)
- [x] 7.2 Write a failing test for the daily branch: calls `DailyEnergyRepository.refresh(force = false)` once per day at a fixed time
- [x] 7.3 Write a failing test confirming `WidgetUpdater.updateAll()` is called after each successful refresh branch, and NOT called when a refresh fails
- [x] 7.4 Implement `WidgetRefreshWorker` to pass 7.1–7.3
- [x] 7.5 Register the periodic work request (`WorkManager.enqueueUniquePeriodicWork`, `KEEP` policy) from `MainActivity.onCreate`, mirroring `ModuleHealthWorker.schedule()`'s call site
- [x] 7.6 Write a test confirming that after `WidgetRefreshWorker` successfully refreshes a repository, `HomeFragment`'s `currentState()`-based initial render reflects that data on the next app open, and its own `onResume` refresh is throttled into a no-op (same repository, same persisted cache — no separate fetch path for widgets vs. tiles)

## 8. Foreground refresh hookup

- [x] 8.1 Write a failing `HomeFragmentTest` assertion that `WidgetUpdater.updateAll()` is invoked after a successful hourly or daily refresh in `onResume`/pull-to-refresh
- [x] 8.2 Wire `HomeFragment` to call `WidgetUpdater.updateAll(context)` after those refreshes to pass 8.1

## 9. Settings integration

- [x] 9.1 Confirm (with a test) that factory reset and credential changes still correctly clear `HourlyEnergyRepository`/`DailyEnergyRepository` state per ADR-007 — no new SharedPreferences store is introduced by widgets, so no new registration should be needed in `SettingsFragment.tileRepositories` or `SettingsFragmentTest.setUp()`; document this conclusion in a code comment only if non-obvious
- [x] 9.2 Write a failing `SettingsFragmentTest` (using a `MockWebServer`, matching `HourlyEnergyRepositoryTest`/`DailyEnergyRepositoryTest`'s style) asserting that saving a changed connection setting triggers an immediate hourly + daily refresh and then calls a test-seamed `WidgetUpdater.updateAll()`
- [x] 9.3 Write a failing test for the same behavior via settings import
- [x] 9.4 Write a failing test that factory reset (which clears credentials) results in `WidgetUpdater.updateAll()` being called with widgets showing the "not configured" placeholder, without attempting a network call
- [x] 9.5 Wire `SettingsFragment.invalidateApiThrottle()` to launch the forced hourly/daily refresh + `WidgetUpdater.updateAll(context)` to pass 9.2–9.4
- [x] 9.6 Write a failing Robolectric test that launching `MainActivity` with the `EXTRA_WIDGET_TARGET = "settings"` extra (while configured) selects the Settings bottom-nav destination, and that recreating the Activity afterward (e.g. rotation) does not re-trigger the navigation
- [x] 9.7 Write a failing test that `EXTRA_WIDGET_TARGET = "home"` selects the Home destination
- [x] 9.8 Implement `MainActivity`'s extra handling (read after existing `configured`-driven setup, select the bottom-nav item, then clear the extra) to pass 9.6–9.7

## 10. QA and documentation

- [x] 10.1 Run `./gradlew ktlintCheck` and `./gradlew testDebugUnitTest`
- [x] 10.2 Manually place all three widgets on the emulator home screen (Lenovo Tab P11 Plus reference device, per ADR-003) and visually confirm chart rendering, tap targets, the "not configured" placeholder, the error-message replacement (simulate a fetch failure), and correct colours under System/Light/Dark Display Mode; save any debug artifacts to `D:\ema-debug\`. Confirmed by user.
- [x] 10.3 Run `/qa` and confirm existing Maestro flows still pass (no new flow added for widgets, per design.md)
- [x] 10.4 Invoke `write-user-guide` to document the three widgets (new page or section, since this is a user-visible surface outside the app's own fragments)
- [x] 10.5 Invoke `lessons-learned` to record what worked/didn't for the Glance build spike and off-screen chart rendering
