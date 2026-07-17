## Context

Home (`HomeFragment`) currently renders four cards: Current Production, Today Production (hourly chart + best-day cards), History Production (daily chart), Module Health. Three Glance widgets (`TodayProductionWidget`, `ProductionSummaryWidget`, `ProductionHistoryWidget`) mirror subsets of that data on the launcher. This change removes Current Production entirely (see Decision 0) because it duplicates the instantaneous production reading already shown in the original APsystems EMA app — the README states EMA Companion is designed to *complement*, not replace, that app. After removal, Home has three tiles and three widgets, backed by three repositories, each with its own throttle (ADR-007 tile repository pattern):

| Data source | Repository | Endpoint | Consumers |
|---|---|---|---|
| Hourly energy | `HourlyEnergyRepository` | `getHourlyEnergy` | Today Production tile, `TodayProductionWidget`, `ProductionSummaryWidget` |
| Daily energy | `DailyEnergyRepository` | `getDailyEnergy` | Today Production tile (best-day cards, via `bindBestDayCards`), History Production tile, `ProductionSummaryWidget`, `ProductionHistoryWidget` |
| Module health | `ModuleHealthRepository` | `getBatchInverterEnergy` | Module Health tile |

(For context on what's being removed: `ProductionRepository` currently owns `getCurrentProduction`, with its own 10-minute throttle state and monthly-counter increment sharing `ApiUsageRepository` with nothing else meaningfully — see Decision 0.)

Every remaining consumer currently calls `refresh()` unconditionally on `onResume` (Home) or on its 2-hour schedule (`WidgetRefreshWorker`), regardless of whether the resulting data is ever displayed. `SettingsFragment.invalidateApiThrottle()` also force-refreshes hourly+daily unconditionally after any connection-setting change. ADR-006 (tile error display) and ADR-009 (API call budget) already govern how each of these repositories behaves; this change adds a visibility layer on top without altering their internal throttle/error/caching logic.

There is no existing mechanism to hide a tile, and Android provides no API for an app to remove an already-placed `AppWidget` instance — only the user can do that from the launcher (confirmed: `AppWidgetManager`/`AppWidgetHost` expose no "delete instance from provider side" call; `setComponentEnabledSetting` on the receiver only affects future widget-picker visibility and future `onUpdate` delivery, and would leave already-placed instances frozen/broken rather than hidden).

## Goals / Non-Goals

**Goals:**
- Remove the Current Production tile and its `getCurrentProduction` fetch entirely, since it duplicates the original EMA app.
- Let the user enable/disable each of the remaining 3 Home tiles and 3 widgets independently, defaulting to all enabled.
- Disabled tiles disappear from Home entirely (not just their data).
- Disabled widgets show a clear "disabled" message in place of data, since they cannot be removed by the app.
- Skip fetching a data source's EMA API endpoint entirely when it has zero enabled consumers (tile or widget).
- Persist the new flags as part of the existing settings store so import/export/factory-reset cover them automatically.

**Non-Goals:**
- Reordering or resizing tiles/widgets.
- Disabling a widget from the widget picker (new placements of a disabled widget remain possible; they just render the disabled message).
- Per-field granularity within a tile (e.g. hiding only the best-day cards inside Today Production) — the toggle is per whole tile/widget.
- Forcing an immediate re-fetch when a tile/widget is re-enabled — it follows the existing throttle/cache behavior like any other settings change that doesn't touch connection credentials.
- Any equivalent "current production" feature elsewhere in the app (e.g. a widget) — it is being removed, not relocated.

## Decisions

### 0. Remove the Current Production tile rather than make it toggleable

The user's own instantaneous-production reading is already available in the original APsystems EMA app, which this app is explicitly designed to complement (README: "does not replace it"). Keeping a fourth, always-fetchable tile whose sole content duplicates the other app adds Settings complexity (a fourth checkbox), Home screen clutter, and ~150 EMA API calls/month (ADR-009) for data the user can already see. Removing it outright — rather than adding it as an 8th toggle alongside the other 3 tiles and 3 widgets — is simpler and frees budget headroom for future features.

Removal touches: `ProductionRepository`, `ProductionSource`, `ProductionState`, `ProductionSnapshot` (deleted); `EmaApiClient.getCurrentProduction`/`ProductionFetch` (deleted — unlike `getHourlyEnergy`/`getDailyEnergy`, this method has no default implementation, so every fake `EmaApiClient` in tests currently overrides it and loses that override); `FetchError` (currently defined inside `ProductionRepository.kt` despite being used by every tile's error state — relocated to its own `core/api/FetchError.kt` so deleting the file doesn't strand the other tiles); `ApiUsageRepository` (its `getLastProductionWatts`/`setLastProduction`/`getLastProductionEpochMs`/`getLastError`/`setLastError`/`getLastFetchEpochMs`/`setLastFetchEpochMs`/`resetThrottle`/`ThrottleResettable` all existed solely to back the production tile's persisted state and throttle — removed, leaving only `recordRequest()`/`getRequestCount()`/`clear()`, which back the shared monthly budget counter and Factory Reset and are used by every remaining repository); `SettingsFragment.tileRepositories` (drops `usageRepository`, since there is no longer a throttle to reset); the Home layout/fragment Current Production views; and `maestro/a-home-screen.yaml`, which currently asserts on `tile_current_production` and the literal text "8000 W" as its main "did the stub round-trip work" signal (rewritten to assert on the two remaining tiles' populated state instead — e.g. `today_total` or `this_month_total` text, or the charts' non-empty rendering).

**Alternative considered**: keep Current Production as an 8th toggleable item (default-on) instead of deleting it. Rejected per the user's explicit direction and because it leaves permanently-dead-weight code (a whole repository + endpoint + tile) reachable only by unchecking a box nobody asked to keep, contrary to Simplicity First.

### 1. Store flags in `SettingsRepository`, not a new tile-repository

The ADR-007 tile-repository pattern (`currentState()`/`refresh()`/`ThrottleResettable`/`clear()`) exists for repositories that own an EMA API fetch and a throttle. Tile/widget enablement is neither — it's a plain user preference, structurally identical to `notificationsEnabled` or `displayMode`, which already live in `SettingsRepository` and already flow through `exportToJson()`/`importFromJson()`/`clearAll()` for free.

**Alternative considered**: a dedicated `TileVisibilityRepository`. Rejected — it would need its own manual wiring into export/import/factory-reset that `SettingsRepository` already provides, for no benefit (there's no throttle or fetched data to own).

### 2. Typed keys via `HomeTile` / `HomeWidget` enums

```kotlin
enum class HomeTile { TODAY_PRODUCTION, HISTORY_PRODUCTION, MODULE_HEALTH }
enum class HomeWidget { TODAY_PRODUCTION, PRODUCTION_SUMMARY, PRODUCTION_HISTORY }
```

`SettingsRepository` gains `isTileEnabled(tile: HomeTile): Boolean` / `setTileEnabled(tile, enabled)` and the widget equivalents, backed by per-enum-constant SharedPreferences keys (default `true`). Enums are shared by Settings UI, Home, widgets, and the worker, so a typo can't silently create a permanently-"enabled" (missing-key) flag in one place and a differently-spelled one in another.

**Alternative considered**: raw string keys per call site. Rejected — this is exactly the kind of scattered, easy-to-typo mapping ADR-007 was written to avoid for tile repos; the same discipline applies here.

### 3. Single source of truth for "is this data source needed"

`SettingsRepository` gains derived methods used by every fetch site:

```kotlin
fun isHourlyDataNeeded(): Boolean =
    isTileEnabled(TODAY_PRODUCTION) || isWidgetEnabled(TODAY_PRODUCTION) || isWidgetEnabled(PRODUCTION_SUMMARY)

fun isDailyDataNeeded(): Boolean =
    isTileEnabled(TODAY_PRODUCTION) || isTileEnabled(HISTORY_PRODUCTION) ||
        isWidgetEnabled(PRODUCTION_SUMMARY) || isWidgetEnabled(PRODUCTION_HISTORY)

fun isModuleHealthDataNeeded(): Boolean =
    isTileEnabled(MODULE_HEALTH)
```

`HomeFragment.onResume`, `WidgetRefreshWorker.doWork`, and `SettingsFragment.invalidateApiThrottle` all call these before invoking the corresponding repository's `refresh()`. This is the one place the Today-tile-needs-daily-data coupling is encoded, so it can't be missed when a fetch site is added or edited later.

**Alternative considered**: compute "needed" ad hoc at each call site by listing consumers inline. Rejected — the Today/daily coupling is non-obvious (it's a UI detail of `bindBestDayCards`, not something a future reader would guess from the tile's name); duplicating that list at four call sites risks drift the first time a widget or tile is added or removed.

### 4. Tile hiding applied in both `onViewCreated` and `onResume`

Bottom-nav fragments are not guaranteed to be recreated on every tab switch, so a flag changed in Settings must be picked up the next time Home becomes visible even without a fresh `onViewCreated`. `HomeFragment` gets a small `applyTileVisibility()` that sets each card's `View.GONE`/`View.VISIBLE` from `SettingsRepository`, called at the start of both lifecycle methods. Disabled tiles' data sources are simply not asked to `refresh()` (guarded by the Decision-3 methods) — the tile being invisible already means nothing renders even if `currentState()` were called, but skipping the call also avoids wasted work and keeps the "is this endpoint needed" logic in one place.

### 5. Widget disabled state checked first, inside `TestContent()`

Each widget's `TestContent()` starts with `if (!settings.isWidgetEnabled(WIDGET_ID)) { Text(disabled message); return@TestContent }` before touching any data source. This means a disabled widget never calls `currentState()` or contributes a "needed" consumer for its data source (consistent with Decision 3, since the derived methods read the same enabled flag, not runtime fetch activity). `WidgetRefreshWorker` also filters its widget list to enabled widgets before calling `updateAllAction`, so disabled widget instances aren't force-redrawn on the 2-hour schedule (they'd redraw to the same disabled message regardless, but skipping avoids the pointless `provideGlance` work).

### 6. Settings UI: `MaterialCheckBox` rows + one Select All / Deselect All toggle

The user asked for checkboxes specifically (not switches, which the rest of Settings uses for booleans like Notifications/Email Alerts) — checkboxes read as "select from a list" which matches selecting which tiles/widgets to show, so `com.google.android.material.checkbox.MaterialCheckBox` is used for the 6 rows in the new "Tiles & Widgets" section. One header row provides a single toggle whose label flips between "Select All" and "Deselect All" depending on whether all 6 are currently checked, rather than two separate buttons — fewer controls for the same capability (Simplicity First).

### 7. No forced re-fetch on toggle — but the persisted timestamp still governs, so an overdue fetch runs immediately

Unlike a connection-setting change (`invalidateApiThrottle()`), toggling a tile/widget on never resets or bypasses its throttle timestamp — there is no `force = true` and no "cooldown starts now" logic tied to the toggle itself. Re-enabling a tile shows its last-cached `currentState()` immediately (same as any Home visit), then the next `refresh(force = false)` call (the same call every enabled tile already gets on `onResume`) checks the real persisted `lastFetchEpochMs` against the throttle window, exactly as if the tile had been enabled the whole time:

- If the data source was fetched recently enough that the throttle window hasn't elapsed, the cached value is kept and no request is issued — identical to any other Home visit within the window.
- If the throttle window has already elapsed — e.g. the tile was disabled for longer than its throttle interval, or was already overdue at the moment it was disabled — that same `refresh(force = false)` call issues a fresh request **immediately**, with no extra wait imposed by having been disabled. Being disabled never gates a fetch that would otherwise be due; only `isXDataNeeded()` gates whether `refresh()` is called at all (Decision 3), not what `refresh()` decides once called.

This is a direct consequence of gating on `isXDataNeeded()` around the *existing* `refresh(force = false)` call rather than inventing new toggle-specific fetch logic — the throttle timestamp is the single source of truth for "is a fetch due," unaffected by whether the tile happened to be visible in between. This matches existing tile behavior everywhere else (e.g. simply not opening the app for a while) and avoids adding a new "why did this one wait an extra window" special case.

### 8. Receiver components stay enabled; new placements of a disabled widget still show the disabled message

Disabling the `<receiver>` in the manifest via `setComponentEnabledSetting` would remove the widget from the picker for *new* placements, but would also stop `onUpdate` broadcasts to *already-placed* instances — the opposite of "always show a clear disabled message." So receivers are never toggled; `TestContent()`'s enabled check (Decision 5) is the only gate, and it applies uniformly to old and new placements.

## Risks / Trade-offs

- **[Risk]** A user disables History Production expecting to also stop Today Production's best-day cards from updating, since both are "history-ish" → they don't stop, because Today Production still needs daily data. **Mitigation**: this is documented behavior (Decision 3), and the Today Production checkbox's own state is what controls its best-day cards; verify this is intuitive during implementation review, no UI copy change planned beyond the tile being named "Today Production."
- **[Risk]** Re-enabling a tile can show stale cached data until the next throttle window elapses (up to 1h for hourly/daily, 24h for module health) → **Mitigation**: identical to existing behavior when the app is simply not opened for a while; `currentState()` always shows the last-known value with its real timestamp, never a false "live" indicator.
- **[Risk]** `settings-import-export` and `factory-reset` specs currently hardcode "11 settings" / "9 settings" counts → **Mitigation**: delta specs update the counts and enumerations explicitly as part of this change.
- **[Risk]** `maestro/a-home-screen.yaml` is the critical, alphabetically-first Home-reachability flow (deliberately named to run before other flows stress Maestro's driver — see `ai/lessons-learned.md`); removing its `tile_current_production`/"8000 W" assertions without a solid replacement signal could silently weaken the one check that proves the stub round-trip actually populated real data. → **Mitigation**: replace with an `extendedWaitUntil` on a concrete populated-data string from one of the remaining two tiles (e.g. the rendered `today_total` kWh text), keeping the same "seeded-then-populated" two-phase assertion shape the flow already uses for `tile_today_production`/`tile_history_production`.
- **[Risk]** Deleting `ApiUsageRepository`'s production-specific fields leaves orphaned keys (`lastProductionWatts`, `lastProductionEpochMs`, `lastFetchError`, `lastFetchEpochMs`) in existing installs' `ema_api_usage` SharedPreferences file. → **Mitigation**: harmless — nothing reads them once the corresponding getters are deleted, and `usageRepository.clear()` (still called by Factory Reset) wipes the whole file including the orphaned keys on the next reset. No migration code needed.

## Migration Plan

No data migration. Absent keys default to `true` (enabled) via `getBoolean(key, true)`, so existing installs behave exactly as before on first launch post-update — every remaining tile/widget reads as enabled until a user unchecks one. The Current Production tile simply disappears on update; no user data is lost (it held no history, only a live reading).

## Open Questions

None blocking implementation.
