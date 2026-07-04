## Context

EMA Companion currently fetches only live current power (watts) via `getCurrentProduction`. The EMA API exposes two additional endpoints under `GET /systems/energy/{sid}`:

- `energy_level=hourly` — 24 hourly kWh buckets for a single calendar day
- `energy_level=daily` — daily kWh totals over a date range

The existing ema-solar-widget app uses these endpoints to display a today-hourly line chart, a history bar chart, and period-based summary cards. We are bringing the same capabilities into EMA Companion using its View-based XML / Material Design 3 style, expanded directly on the Home screen.

## Goals / Non-Goals

**Goals:**
- Add two new EmaApiClient methods (`getHourlyEnergy`, `getDailyEnergy`) with HMAC signing
- Introduce two new repositories (`HourlyEnergyRepository`, `DailyEnergyRepository`) with correct caching and throttle semantics
- Expand HomeFragment with a today section (line chart + tables + today-total + best-day cards) and a history section (bar chart + period-total cards)
- Keep the API call budget well under ADR-009's 1,000 call/month cap
- Reuse the existing `solar-array-capacity` setting as the chart Y-axis maximum

**Non-Goals:**
- Home screen widgets (separate project; the ema-solar-widget already handles this)
- Real-time push updates (polling on fragment resume is sufficient)
- Offline-first sync / background WorkManager fetches for these endpoints (the Module Health worker is already a WorkManager use case; production data is fetched on resume)
- Configurable "historic data days" setting beyond what `data-history-preference` already provides

## Decisions

### D1: Chart library — MPAndroidChart

**Decision:** Use [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for both the line chart and bar chart.

**Rationale:** MPAndroidChart is the de-facto standard for View-based Android charts, is available as a Gradle dependency, renders natively without WebView, and supports customised styling (colours, grids, legends) with no third-party server calls.

**Widget compatibility:** A future widget OpenSpec will need bitmap rendering (RemoteViews cannot embed a View directly). MPAndroidChart supports this: create the chart off-screen, call `measure()/layout()/draw()` onto a Bitmap-backed Canvas. This is the same approach the ema-solar-widget takes with its custom renderer, just using the library instead of raw Canvas. The chart library choice does not constrain the widget implementation.

**Alternatives considered:**
- *Custom Canvas drawing (like the widget)*: Higher implementation cost, requires manual hit-testing, no axis scaling helpers. Rejected.
- *Vico*: Compose-first; forcing it into View-based layouts requires interop overhead. Rejected.

### D2: Caching strategy

**Decision:**
- **Hourly data** (today only): Persist in SharedPreferences (`ema_hourly`). Re-fetch at most once per hour (3,600 s throttle). Today's data can change minute-to-minute so it is never cached permanently.
- **Daily data** (history): Persist per-day totals in SharedPreferences (`ema_daily`). Past days' totals are immutable — cache on first successful fetch and never re-fetch. Only the current calendar day is re-fetched on each trigger (one call per session that reaches the threshold).

**Rationale:** Matches ADR-009 preference order (reuse → cache immutable past data → throttle → fresh fetch). Limits history endpoint to ~1 call/day in steady state.

**Budget impact:**
- Hourly: throttle 1 hr → max 24 triggers/day but user typically opens app ~5×/day → ~150 calls/month
- Daily (steady state): 1 call/day (current day only) → ~30 calls/month
- Combined new spend: ≈ 180 calls/month; new total ≈ 360 calls/month (well under 1,000)

### D3: HomeFragment layout structure

**Decision:** Expand HomeFragment with a `NestedScrollView` wrapping all sections. From top to bottom:
1. Existing live-production tile (unchanged)
2. Today section: line chart → morning table → afternoon table → today-total card → best-day cards
3. History section: bar chart → period-summary cards (this month, last 30 days)

Each section is a `MaterialCardView` to maintain the existing card-based visual language.

**Rationale:** User confirmed "expand Home screen" over a new tab. Card-per-section keeps the layout visually bounded and easy to test in isolation.

### D4: Data flow — two independent repositories

**Decision:** `HourlyEnergyRepository` and `DailyEnergyRepository` are independent of each other and of `ProductionRepository`. HomeFragment drives all three on `onResume`. No shared state.

**Rationale:** Simpler; each repository owns its cache, throttle, and error state. A failure in one does not block the others.

### D6: Pull-to-refresh bypasses throttle; `refresh(force: Boolean)` API

**Decision:** All three repositories (`ProductionRepository`, `HourlyEnergyRepository`, `DailyEnergyRepository`) expose `refresh(force: Boolean = false)`. When `force = true` the throttle check is skipped and a fresh API call is issued unconditionally. HomeFragment wraps the screen in `SwipeRefreshLayout`; a pull calls `refresh(force = true)` on all three.

After a successful forced fetch the throttle timestamp is updated to now, so the next `onResume` correctly holds off. A forced fetch that fails does not update the throttle (same rule as normal failures).

**Cost of a pull:** at most 3 calls (current production + today hourly + today daily). Past daily data is immutable and cached; a pull never re-fetches history days.

**Rationale:** Pull-to-refresh is an explicit user gesture; silently returning cached data on pull is confusing. The bounded cost (≤3 calls) is acceptable within ADR-009 headroom.

### D5: In-progress hour handling

**Decision:** The current hour's bar in the line chart is rendered as a dashed stroke. The chart does not extrapolate or predict the full-hour value — it shows the actual partial value as-is.

**Rationale:** The ema-solar-widget predicts the full-hour value from elapsed minutes (a multiplication by 60/elapsed). This adds complexity and is misleading when cloud cover changes mid-hour. Show what the API returned; annotate the current hour bar as partial.

## Risks / Trade-offs

- **MPAndroidChart maintenance**: The library is community-maintained and not under active development. Risk: future AGP/Kotlin updates may require patches. Mitigation: version-pin the dependency; the chart requirements are simple (line + bar) and easy to migrate if needed.
- **Hourly SharedPreferences size**: 24 doubles × 30 days ≈ trivial. No risk.
- **HomeFragment length**: Scrollable Home with three sections may feel long on phones (the reference device is a tablet). Mitigation: section headers act as visual anchors; if needed a future change can collapse sections.
- **Clock skew / timezone**: Day boundaries use the device local date. If the user's device clock is wrong, today's chart may show yesterday's data. Mitigation: document this as a known limitation; no special handling.

## Open Questions

- None blocking implementation. MPAndroidChart version to pin will be resolved during the task that adds the dependency.
