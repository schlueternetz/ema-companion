## Why

Users can only see their solar production data by opening the app. EMA Companion already fetches and caches hourly and daily production data through its tile-repository pattern, but none of it is visible without opening the app. Home-screen widgets give users that same data at a glance, reusing Companion's existing repositories, throttles, and single EMA credential set — no new data source, no new account, no new API budget.

## What Changes

- Add three home-screen widgets, built with **Jetpack Glance** (introduces Compose to the project, scoped to the widget surface):
  - **Today's Production widget**: today's hourly production as a line/area chart (solid = completed hours, dashed = in-progress current hour projected to a full hour), plus today's running total in kWh.
  - **Production Summary widget**: today / this-month / last-30-days totals as three bold numeric values.
  - **Production History widget**: bar chart of daily totals over the user's configured history window (`data-history-preference`), bars colour-coded by calendar month with a legend.
- All three widgets read from the **same cached repository state** already maintained by `HourlyEnergyRepository` and `DailyEnergyRepository` — no new network client, no new local database, no duplicate fetch path.
- Tapping any widget normally opens the app directly to the Home screen (Companion already merges "today" and "history" into one scrollable screen, so there is only one destination to open) — but opens directly to **Settings** instead when the app isn't configured yet, or when the tapped widget is currently showing a fetch-error message, since that's where credentials are edited and the API call log lives.
- No per-widget configuration screen: widgets reflect the single system already configured in Settings; an unconfigured app shows a neutral placeholder on every widget, consistent with ADR-006.
- Add a background refresh path so widget content updates even when the app is not opened, without exceeding the existing shared EMA API monthly budget (ADR-009). This reuses each repository's own existing throttle — the only new behavior is *triggering* `refresh()` periodically in the background; the call-counting and caching rules are unchanged. Because the background trigger writes into the **same** persisted cache `HomeFragment` reads from, its results are also what the Home screen's tiles show the next time the app is opened (no separate/duplicate cache, no stale tile data waiting for its own re-fetch). **The exact trigger cadence and its budget impact are worked out in design.md and added as a new row to the ADR-009 allocation table before implementation.**
- Widgets also react immediately to a settings change — a credential edit, an import, or a factory reset — rather than only picking it up on the next scheduled trigger: the existing settings-save path already resets each repository's throttle, and now also triggers an immediate refresh (or, for factory reset, an immediate "not configured" placeholder) so a widget never keeps showing a different or no-longer-valid system's numbers for hours after the user changes settings.
- Widget charts are rendered as a bitmap (via the app's existing MPAndroidChart-based rendering, adapted for off-screen drawing) and displayed inside the Glance layout — Glance/RemoteViews cannot host a live custom `View`.
- Widgets follow the app's own Display Mode preference (System/Light/Dark, `dark-mode` spec) rather than only the device's system theme — the same choice the user already made inside the app applies to the widgets.
- When a fetch fails, the affected widget replaces its chart or figures with a clear error message (network / authentication / other) instead of silently showing a possibly-stale chart — a deliberate widget-specific departure from the Home tile pattern (ADR-006 keeps the last value plus a small inline error line), since a glance widget has no room for both and a stale-looking chart is worse than a clear "couldn't update" message.

## Capabilities

### New Capabilities
- `today-production-widget`: home-screen widget showing today's hourly production chart and running total, refreshed from cached/throttled data.
- `production-summary-widget`: home-screen widget showing today / this-month / last-30-days production totals.
- `production-history-widget`: home-screen widget showing a bar chart of daily production over the configured history window.
- `widget-background-refresh`: the shared periodic background trigger that keeps all three widgets' underlying repository caches current without requiring the app to be opened, within the existing EMA API budget.
- `widget-display-mode`: widgets render using the app's own Display Mode preference (System/Light/Dark) rather than only the device's ambient theme.

### Modified Capabilities
- None. Existing specs (`hourly-production`, `production-history`, `current-production-display`, `data-history-preference`, `dark-mode`) are read by the new widgets but their requirements do not change — widgets consume the same `currentState()`/persisted preference each already exposes.

## Impact

- **New code**: three `GlanceAppWidget` implementations + `AppWidgetReceiver`s, widget layout/theme composables, a bitmap chart renderer for the widget surface, and a `WorkManager` periodic worker that triggers existing repositories' `refresh()`.
- **New dependency**: `androidx.glance:glance-appwidget` (and its Compose runtime transitive deps) — the first Compose usage in the project; confined to `feature/widgets/`.
- **AndroidManifest.xml**: new `<receiver>` entries for each widget provider; no new permissions (INTERNET already present).
- **ADR-009 (EMA API budget)**: must be updated with a new row for the background refresh trigger once design.md fixes its cadence; must stay within current ~640 headroom.
- **ADR-004 (package organization)**: new `feature/widgets/` package, following the existing feature-first convention.
- **Existing repositories** (`HourlyEnergyRepository`, `DailyEnergyRepository`): consumed read-only via their existing `currentState()`/`refresh()`; no interface changes anticipated, but confirm in design.md.
- **User guide**: new widget pages/section needed (`write-user-guide` skill) since this is a user-visible UI surface, even though it lives outside the app's own fragments.
