## Why

Every Home tile and home-screen widget currently fetches its data unconditionally, and there is no way to hide a tile or widget a user does not care about. As more tiles/widgets are added, this wastes screen space, wastes EMA API call budget (ADR-009) on data nobody looks at, and gives users no control over their Home screen or widget error surface.

## What Changes

- **Remove the Current Production tile from Home altogether.** It duplicates the instantaneous production reading already shown in the original APsystems EMA app (which this app is designed to complement, not replace — see README), so it is dropped rather than made toggleable. This removes `ProductionRepository`, its `getCurrentProduction` EMA API call, its Home UI, and ~150 calls/month from the ADR-009 budget allocation.
- Add a "Tiles & Widgets" section to Settings with one checkbox per remaining Home tile (Today Production, History Production, Module Health) and per widget (Today Production, Production Summary, Production History), plus a "Select All / Deselect All" control.
- All tiles and widgets default to **enabled** (checked).
- A disabled tile is removed from the Home screen (its card view is hidden entirely, not just its data).
- A disabled widget cannot be removed from the home screen by the app (Android provides no API to force-remove an already-placed widget instance), so a disabled widget instead renders a "disabled in Settings" message in place of its normal content, both for existing placements and any new placement.
- Each tile/widget's underlying EMA API data source (current production, hourly energy, daily energy, module health) is only fetched when at least one enabled tile or widget consumes it — a data source with zero enabled consumers is skipped entirely, freeing that budget. Note the Today Production tile also consumes daily energy data (for its best-day cards), so daily energy stays gated on the History tile, the Today tile, and the two widgets that use it.
- Tile/widget enabled flags are persisted settings and are included in Settings import/export and reset to defaults (all enabled) on Factory Reset, alongside the existing settings.
- **BREAKING**: none (existing behavior when all tiles/widgets stay enabled is unchanged).

## Capabilities

### New Capabilities
- `tile-widget-visibility`: Settings UI to enable/disable each remaining Home tile and widget (3 tiles + 3 widgets); Home tile hide/show behavior; widget disabled-placeholder behavior; gating of EMA API data-source fetches to only sources with at least one enabled consumer.

### Modified Capabilities
- `settings`: Settings screen gains a fourth section, "Tiles & Widgets", listing the new checkboxes.
- `settings-import-export`: the exported/imported settings set grows to include the new tile/widget enabled flags (key count and enumeration change).
- `factory-reset`: the set of settings reset to defaults grows to include the new tile/widget enabled flags, all resetting to enabled (key count changes).
- `current-production-display`: **removed entirely** — all requirements in this capability are dropped along with the tile.

## Impact

- `feature/settings/SettingsRepository.kt` — new persisted keys (tile/widget enabled flags), new data-source-needed derived getters, export/import/clearAll coverage.
- `feature/settings/SettingsFragment.kt` + `res/layout/fragment_settings.xml` — new "Tiles & Widgets" section with checkboxes and select-all/deselect-all.
- `feature/home/HomeFragment.kt` + `res/layout/fragment_home.xml` — remove the Current Production tile entirely; hide the remaining disabled tile cards; gate `refresh()` calls per data source.
- `feature/widgets/TodayProductionWidget.kt`, `ProductionSummaryWidget.kt`, `ProductionHistoryWidget.kt` — render a disabled-placeholder state when the widget is disabled.
- `feature/widgets/WidgetRefreshWorker.kt`, `WidgetUpdater.kt` — gate hourly/daily refresh and skip updating disabled widgets.
- `core/api/ProductionRepository.kt`, `ProductionSnapshot.kt` — deleted. `FetchError` (currently defined inside `ProductionRepository.kt` but used by every other tile) moves to its own file.
- `core/api/EmaApiClient.kt`, `OkHttpEmaApiClient.kt` — remove `getCurrentProduction`/`ProductionFetch`.
- `core/api/ApiUsageRepository.kt` — drop the production-specific/throttle fields and `ThrottleResettable` implementation; keep only the monthly successful-call counter (still used by the API Request Limit progress bar) and `clear()` (still used by Factory Reset).
- `feature/settings/SettingsFragment.kt` — remove `usageRepository` from `tileRepositories` (no longer a throttle to reset).
- `maestro/a-home-screen.yaml` — the critical Home-reachability flow currently asserts on `tile_current_production` and the literal value "8000 W"; rewritten to assert on the two remaining tiles and a populated-data signal from the stub round trip.
- `docs/adr/009-ema-api-call-budget.md` — remove the Production tile row and update the allocation total/headroom; note the new conditional-fetch behavior for the remaining sources.
- Deleted tests: `ProductionRepositoryTest`, `HomeProductionIntegrationTest`. Updated tests: every fake `EmaApiClient` implementation currently forced to override `getCurrentProduction` (it has no default, unlike the other three methods) loses that override; `HomeFragmentTest` loses production-tile coverage.
- New strings for the Settings section and widget disabled message; removed strings for the deleted tile (English + German).
