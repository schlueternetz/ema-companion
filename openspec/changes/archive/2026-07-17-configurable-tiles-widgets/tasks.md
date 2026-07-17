## 1. Remove the Current Production tile

- [x] 1.1 Move `FetchError` out of `ProductionRepository.kt` into its own file `core/api/FetchError.kt` (it's used by every remaining tile's error state — must survive the deletion below)
- [x] 1.2 Delete `ProductionRepository.kt` (`ProductionRepository`, `ProductionSource`, `ProductionState`), `ProductionSnapshot.kt`, and `ProductionRepositoryTest.kt`
- [x] 1.3 Remove `getCurrentProduction`/`ProductionFetch` from `EmaApiClient.kt` and its implementation from `OkHttpEmaApiClient.kt`; remove the now-unnecessary override from every fake `EmaApiClient` in tests (it had no default implementation, unlike `getHourlyEnergy`/`getDailyEnergy`/`getBatchInverterEnergy`)
- [x] 1.4 Remove `getCurrentProduction`-specific coverage from `OkHttpEmaApiClientTest.kt`
- [x] 1.5 Simplify `ApiUsageRepository`: remove `getLastProductionWatts`/`setLastProduction`/`getLastProductionEpochMs`/`getLastError`/`setLastError`/`getLastFetchEpochMs`/`setLastFetchEpochMs`/`resetThrottle`/`ThrottleResettable`; keep `recordRequest()`, `getRequestCount()`, `clear()`, and the per-calendar-month rollover logic (still used by every remaining repository and the API Request Limit progress bar)
- [x] 1.6 Write/update `ApiUsageRepositoryTest.kt` to cover only the surviving counter/clear behavior; remove obsolete production/throttle test cases
- [x] 1.7 Remove `usageRepository` from `SettingsFragment.tileRepositories` (no longer `ThrottleResettable`); keep the `usageRepository` field/instance for `getRequestCount()` (progress bar) and `clear()` (factory reset)
- [x] 1.8 Remove the Current Production card from `res/layout/fragment_home.xml` (`tile_current_production`, `text_current_production`, `production_updated`, `production_status`)
- [x] 1.9 Remove Current Production fields, `render()`, `source`/`sourceOverride`, and the production `refresh()` calls in `onResume`/`onPullToRefresh` from `HomeFragment.kt`
- [x] 1.10 Delete `HomeProductionIntegrationTest.kt`; remove Current Production coverage and `sourceOverride` usage from `HomeFragmentTest.kt`
- [x] 1.11 Remove now-unused strings (`home_current_production_title`, `home_production_value`, `home_production_updated`, `home_production_neutral`, `home_status_network_error`, `home_status_auth_error`, `home_status_api_error`) from `values/strings.xml` and `values-de/strings.xml`
- [x] 1.12 Update `docs/adr/009-ema-api-call-budget.md`: remove the "Production tile" allocation row and recompute the allocated total and headroom
- [x] 1.13 Rewrite `maestro/a-home-screen.yaml`: remove all `tile_current_production` references and the "8000 W" assertion; assert the remaining two tiles (`tile_today_production`, `tile_history_production`) in both the never-fetched (placeholder/dummy-credentials) and stub-populated phases, using a concrete populated-data signal (e.g. `today_total` text) in place of "8000 W" to prove the stub round trip actually returned data
- [x] 1.14 Run the full local Maestro suite against the emulator and confirm `a-home-screen.yaml` and the other flows still pass after the rewrite (deferred to the group 8 `/qa` pass)

## 2. Settings model: enabled flags and derived data-need methods

- [x] 2.1 Add `HomeTile` enum (`TODAY_PRODUCTION`, `HISTORY_PRODUCTION`, `MODULE_HEALTH`) and `HomeWidget` enum (`TODAY_PRODUCTION`, `PRODUCTION_SUMMARY`, `PRODUCTION_HISTORY`) — placed in `core/` (not `feature/settings/`) per ADR-004: they're referenced by feature/home, feature/widgets, AND feature/settings, so the ≥2-features rule puts them in `core/`, alongside `core/AppConfig.kt`
- [x] 2.2 Write failing unit tests for `SettingsRepository.isTileEnabled`/`setTileEnabled` and `isWidgetEnabled`/`setWidgetEnabled`: default `true` when unset, persists `false` after `setTileEnabled(x, false)`, independent per enum constant
- [x] 2.3 Implement `isTileEnabled`/`setTileEnabled`/`isWidgetEnabled`/`setWidgetEnabled` on `SettingsRepository` backed by per-enum-constant SharedPreferences keys, defaulting to `true`
- [x] 2.4 Write failing unit tests for the derived data-need methods (`isHourlyDataNeeded`, `isDailyDataNeeded`, `isModuleHealthDataNeeded`) covering: single consumer disabled with others enabled keeps fetching, all consumers disabled stops fetching, Today Production tile alone keeps daily energy needed (best-day cards coupling)
- [x] 2.5 Implement the three derived data-need methods per design.md Decision 3
- [x] 2.6 Write failing unit test: `exportToJson()` includes all 6 new keys; `importFromJson()` merges them (present keys overwrite, absent keys leave existing value unchanged)
- [x] 2.7 Add the 6 keys to `exportToJson()` and `importFromJson()`
- [x] 2.8 Confirm `clearAll()` (used by Factory Reset) resets the 6 new keys to default-enabled by construction (no code change expected — verify with a Robolectric test that factory reset leaves `isTileEnabled`/`isWidgetEnabled` returning `true` for all constants)

## 3. Settings UI: "Tiles & Widgets" section

- [x] 3.1 Add English strings: section title, tiles/widgets subheadings, Select All / Deselect All labels (tile/widget row labels reuse existing `home_today_title`/`home_history_title`/`home_module_health_title` and `*_widget_label` strings so Settings text always matches the tile/widget's own displayed name)
- [x] 3.2 Add matching German strings in `values-de/strings.xml`
- [x] 3.3 Add "Tiles & Widgets" section to `res/layout/fragment_settings.xml`: 6 `MaterialCheckBox` rows (grouped Tiles / Widgets) + one Select All/Deselect All row, positioned between "App Settings" and "API Settings" per the `settings` spec delta
- [x] 3.4 Write failing Robolectric test: `SettingsFragmentTest` — all 6 checkboxes reflect `SettingsRepository` state on `onViewCreated`; toggling one checkbox persists only that flag
- [x] 3.5 Wire each checkbox's `onCheckedChangeListener` to the corresponding `setTileEnabled`/`setWidgetEnabled` call in `SettingsFragment`
- [x] 3.6 Write failing Robolectric test: activating the all-toggle control when all 6 are checked unchecks all 6 and persists all as disabled; when at least one is unchecked, checks all 6 and persists all as enabled; label reflects current state
- [x] 3.7 Implement the Select All / Deselect All control and its label-flip logic
- [x] 3.8 Add `refreshAllDisplayedValues()` coverage: after import/factory-reset, all 6 checkboxes reflect the new persisted state

## 4. Home tile visibility and gated fetches

- [x] 4.1 Add `android:id` values needed to reference each remaining Home tile's root `MaterialCardView` (`tile_today_production`, `tile_history_production`, `tile_module_health` — confirm/add in `fragment_home.xml`) — all three already existed; no XML change needed
- [x] 4.2 Write failing `HomeFragmentTest`: a disabled tile's card view is `GONE` after `onViewCreated`; re-enabling and reaching `onResume` makes it `VISIBLE` again with its last cached value rendered
- [x] 4.3 Implement `applyTileVisibility()` in `HomeFragment` reading `SettingsRepository.isTileEnabled` for each of the 3 tiles, called from both `onViewCreated` and `onResume`
- [x] 4.4 Write failing `HomeFragmentTest`/integration test cases: `onResume` does not call `refresh()` on a data source when its corresponding `isXDataNeeded()` is false; does call it when true
- [x] 4.5 Gate the three `refresh()` calls in `HomeFragment.onResume` (and the equivalent in `onPullToRefresh`) behind `isHourlyDataNeeded()` / `isDailyDataNeeded()` / `isModuleHealthDataNeeded()`
- [x] 4.6 Write failing test: `SettingsFragment.invalidateApiThrottle()` only force-refreshes hourly/daily when the respective `isXDataNeeded()` is true
- [x] 4.7 Gate the two forced refreshes in `SettingsFragment.invalidateApiThrottle()` accordingly
- [x] 4.8 Write a regression test locking in design.md Decision 7: gate the existing `refresh(force = false)` call with `if (isXDataNeeded())` — do NOT add any toggle-specific cooldown or "just re-enabled" flag. Seed a data source with a `lastFetchEpochMs` older than its throttle window while disabled, re-enable it, call the gated `onResume` refresh, and assert a new request IS issued immediately (not deferred an extra window). Also assert the inverse: a `lastFetchEpochMs` within the throttle window at re-enable time does NOT trigger a new request

## 5. Widget disabled state

- [x] 5.1 Add an English string for the widget "disabled in Settings" message; add the German translation
- [x] 5.2 Write failing `TodayProductionWidgetTest`: when `HomeWidget.TODAY_PRODUCTION` is disabled, `TestContent()` renders the disabled message and does not read `hourlySourceOverride`/`HourlyEnergyRepository.currentState()`
- [x] 5.3 Implement the disabled-check-first branch in `TodayProductionWidget.TestContent()`
- [x] 5.4 Repeat 5.2–5.3 for `ProductionSummaryWidgetTest` / `ProductionSummaryWidget` (checks `HomeWidget.PRODUCTION_SUMMARY`)
- [x] 5.5 Repeat 5.2–5.3 for `ProductionHistoryWidgetTest` / `ProductionHistoryWidget` (checks `HomeWidget.PRODUCTION_HISTORY`)
- [x] 5.6 Write failing test confirming a re-enabled widget renders normal content again (no stale disabled message)

## 6. Widget refresh worker and updater gating

- [x] 6.1 Write failing `WidgetRefreshWorkerTest`: hourly `refresh()` is skipped when `isHourlyDataNeeded()` is false; daily `refresh()` is skipped when `isDailyDataNeeded()` is false
- [x] 6.2 Gate `WidgetRefreshWorker.doWork()`'s hourly and daily branches behind the corresponding data-need checks
- [x] 6.3 Write failing `WidgetUpdaterTest`: `WidgetUpdater.updateAll(context)` (no-args-widgets overload) only updates widgets whose `HomeWidget` flag is enabled — tested via the new internal `WidgetUpdater.enabledWidgets(settings)` pure function rather than observing Glance's real `updateAll()` side effects (per `ai/lessons-learned.md`: don't spy on Glance internals in Robolectric)
- [x] 6.4 Filter the widget list in `WidgetUpdater.updateAll(context)` and in `WidgetRefreshWorker`'s widget list by `isWidgetEnabled` before calling `updateAllAction`

## 7. Documentation

- [x] 7.1 Update `docs/adr/009-ema-api-call-budget.md`: add a short note under "Design constraints for new features" (or a new subsection) describing that tile/widget visibility now conditionally skips fetches per data source, referencing this change (reduces worst-case usage further, on top of the Production-tile row removed in Task 1.12)
- [x] 7.2 Invoke `write-user-guide` to update `docs/user-guide/settings.md` (new "Tiles & Widgets" section) and `docs/user-guide/home.md` (Current Production tile removed; remaining tiles can be hidden) once the UI changes above are complete — also updated `widgets.md` (disabled-widget message) since it was directly affected; regenerated German for all three

## 8. Verification

- [x] 8.1 Run `./gradlew ktlintCheck`
- [x] 8.2 Run `./gradlew testDebugUnitTest` and confirm all new and existing tests pass
- [x] 8.3 Run `/qa` (full pre-flight: unit/Robolectric tests, ktlint, debug build+install, Maestro flows) and confirm all Maestro flows pass locally before considering the change done
- [x] 8.4 (discovered during 8.3) Fix `ema-api-stub`'s shared "Good Data" fixture (`203000001234.json`): its `minutely` interaction was still first in the per-ECU strict-sequential-match order, but the app no longer calls `getCurrentProduction` — every real hourly request permanently mismatched at cursor 0. Removed the `minutely` interaction (hourly is now first, daily second); updated `ema-api-stub`'s own `GoodDataScenarioTest` and `ApplicationTest` (renamed `minutelyUrl`→`hourlyUrl`, repointed the "unexpected energy level" test to request `daily` instead of `hourly` so it still exercises a genuine cursor-0 mismatch); confirmed via `a-home-screen.yaml`'s "0.42" assertion against a freshly-reset local stub
