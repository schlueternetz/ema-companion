## 1. Chart Library Dependency

- [x] 1.1 Add MPAndroidChart to `gradle/libs.versions.toml` and `app/build.gradle.kts`; confirm `./gradlew assembleDebug` succeeds

## 2. EMA API — New Endpoints

- [x] 2.1 Add `getHourlyEnergy(date: String): HourlyEnergyFetch` to `EmaApiClient`; define `HourlyEnergyFetch` data class (result: `ApiResult<HourlySnapshot>`, endpoint, durationMs, requestText, responseText)
- [x] 2.2 Define `HourlySnapshot` data class: `val hours: Map<Int, Double>` (key = hour 0–23, nullable hours omitted)
- [x] 2.3 Implement `getHourlyEnergy` in `OkHttpEmaApiClient`: `energy_level=hourly`, `date_range=<date>_<date>`, HMAC signed; parse `data` array into `HourlySnapshot`
- [x] 2.4 Add `getDailyEnergy(startDate: String, endDate: String): DailyEnergyFetch` to `EmaApiClient`; define `DailyEnergyFetch` and `DailySnapshot` (`val days: Map<String, Double>` keyed by "YYYY-MM-DD")
- [x] 2.5 Implement `getDailyEnergy` in `OkHttpEmaApiClient`: `energy_level=daily`, `date_range=<start>_<end>`, HMAC signed; parse response into `DailySnapshot`
- [x] 2.6 Write unit tests for `getHourlyEnergy` and `getDailyEnergy` request signing and response parsing using MockWebServer

## 3. HourlyEnergyRepository

- [x] 3.1 Create `core/api/HourlyEnergyRepository` with SharedPreferences store `ema_hourly`; persist the 24 hourly values and the last-fetch timestamp
- [x] 3.2 Implement `currentState(): HourlyProductionState` reconstructing `HourlySnapshot` + `FetchError?` from prefs
- [x] 3.3 Implement `refresh(force: Boolean = false): HourlyProductionState` with 3,600 s throttle (skipped when `force = true`, success-only otherwise); log the call via `ApiCallLogRepository`; count via `ApiUsageRepository` on success
- [x] 3.4 Implement `ThrottleResettable.resetThrottle()` — clears last-fetch timestamp; add to `SettingsFragment.tileRepositories` and clear prefs in `showFactoryResetDialog()`
- [x] 3.5 Add `ema_hourly` prefs store to `SettingsFragmentTest.setUp()` to prevent cross-test leakage
- [x] 3.6 Write unit tests: throttle gate, force=true bypasses throttle, success path (counts + throttles), failure path (no count, no throttle), `currentState()` reconstruction from prefs

## 4. DailyEnergyRepository

- [x] 4.1 Create `core/api/DailyEnergyRepository` with SharedPreferences store `ema_daily`; persist per-day totals keyed by "YYYY-MM-DD" and the current-day last-fetch timestamp
- [x] 4.2 Implement `currentState(): DailyProductionState` reconstructing all cached day totals + `FetchError?` from prefs
- [x] 4.3 Implement `refresh(force: Boolean = false)`: on first fetch issue one full-window `getDailyEnergy` call; on subsequent fetches issue one today-only call (throttle skipped when `force = true`); past days already in cache are never re-fetched regardless of `force`
- [x] 4.4 Implement `ThrottleResettable.resetThrottle()` — clears today's throttle timestamp (not the per-day cache); add to `SettingsFragment.tileRepositories` and clear full prefs in `showFactoryResetDialog()`
- [x] 4.5 Add `ema_daily` to `SettingsFragmentTest.setUp()`
- [x] 4.6 Write unit tests: first-fetch full window, steady-state today-only, today throttle, force=true bypasses today throttle, past days not re-fetched (even with force=true), failure path, `currentState()` reconstruction

## 5. ADR-009 Budget Update

- [x] 5.1 Add two rows to the ADR-009 allocation table: hourly endpoint (~150 calls/month) and daily endpoint (~30 calls/month); update the Total and Headroom rows

## 6. Home Screen — Today Section

- [x] 6.1 Add `HourlyProductionState` to `HomeFragment`; wire `HourlyEnergyRepository` alongside existing `ProductionRepository`; call `currentState()` in `onViewCreated` and `refresh(force = false)` in `onResume`
- [x] 6.2 Add today-section `MaterialCardView` to `fragment_home.xml` containing: `LineChart` (MPAndroidChart), morning table `RecyclerView`, afternoon table `RecyclerView`
- [x] 6.3 Implement `bindHourlyChart(state: HourlyProductionState)`: plot hours 06–currentHour, solid line for past hours, dashed `DashPathEffect` for current hour's segment; Y-axis max = system capacity if set
- [x] 6.4 Implement morning/afternoon table binding: two-column grid (hour label + kWh or "–"); use `GridLayoutManager(12)` or a fixed `TableLayout`
- [x] 6.5 Add today-total summary card to `fragment_home.xml`; bind sum of non-null hourly values formatted to 2 dp + "kWh"
- [x] 6.6 Show inline error status line below today section when `HourlyProductionState.error != null`; show neutral placeholder when no data
- [x] 6.7 Write Robolectric tests for today-section binding: chart populated, table renders "–" for null hours, error line visible on fetch error, placeholder shown on no data

## 7. Home Screen — Best-Day Cards

- [x] 7.1 Add best-day cards to `fragment_home.xml`: "Best day this month" and "Best day in last N days"; wire to `DailyProductionState`
- [x] 7.2 Implement `bindBestDayCards(state: DailyProductionState)`: filter by current month / history window, find max, format date as "EEE dd MMM" and kWh to 2 dp; show "–" when no data
- [x] 7.3 Write Robolectric tests: correct best day selected, multi-month data picks right month, "–" when empty

## 8. Home Screen — History Section

- [x] 8.1 Wire `DailyEnergyRepository` into `HomeFragment`; call `currentState()` in `onViewCreated` and `refresh()` in `onResume`
- [x] 8.2 Add history-section `MaterialCardView` to `fragment_home.xml` containing: `BarChart` (MPAndroidChart) and month-colour legend `FlexboxLayout` (or a simple `LinearLayout` of colour-chip + month label pairs)
- [x] 8.3 Implement `bindHistoryChart(state: DailyProductionState)`: one `BarEntry` per day in history window, assign colour per calendar month from a fixed palette, draw legend; Y-axis max = system capacity if set; X-axis label every other day number
- [x] 8.4 Add this-month total and last-30-days total summary cards to `fragment_home.xml`; bind from `DailyProductionState`
- [x] 8.5 Show inline error status line below history section when `DailyProductionState.error != null`; show neutral placeholder when no data
- [x] 8.6 Write Robolectric tests for history-section binding: bars rendered, month colours distinct, period totals correct, error line visible, placeholder when no data

## 9. System Capacity Wired to Charts

- [x] 9.1 Read `SettingsRepository.getSystemCapacity()` in `HomeFragment`; pass to both `bindHourlyChart` and `bindHistoryChart`; Y-axis max = capacity when > 0, else data max
- [x] 9.2 Write Robolectric test: Y-axis max equals capacity when set; auto-scales when not set

## 10. Pull-to-Refresh

- [x] 10.1 Add `force: Boolean = false` parameter to `ProductionRepository.refresh()`; bypass throttle check when `force = true`; add unit test: force=true bypasses throttle
- [x] 10.2 Wrap `fragment_home.xml` root in `SwipeRefreshLayout`; on swipe call `refresh(force = true)` on all three repositories in parallel; hide the spinner when all three complete
- [x] 10.3 Write Robolectric test: swipe triggers all three repositories with force=true and hides spinner on completion

## 11. Lint, QA, and User Guide

- [x] 11.1 Run `./gradlew ktlintCheck`; fix all violations
- [x] 11.2 Run `./gradlew testDebugUnitTest`; confirm all tests pass
- [x] 11.3 Run `/qa` (full pre-flight: unit tests + ktlint + debug build/install + Maestro flows); confirm all Maestro flows pass on the emulator
- [x] 11.4 Run `/write-user-guide` to update `docs/user-guide/home.md` with the new chart and summary card sections
