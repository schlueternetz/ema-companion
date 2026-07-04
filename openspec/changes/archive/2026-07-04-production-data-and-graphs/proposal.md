## Why

The Home screen currently shows only live current power (watts). The EMA API already provides two additional endpoints — hourly energy and daily energy — that unlock today's production chart, a history bar chart, and period-based summary cards that users of the existing EMA solar widget rely on daily.

## What Changes

- **Expanded Home screen**: the existing production tile gains a scrollable lower section with charts and summary cards, replacing the currently sparse single-value tile layout.
- **New API calls in EmaApiClient**: `getHourlyEnergy(date)` and `getDailyEnergy(dateRange)` added alongside the existing `getCurrentProduction`.
- **Two new repositories**: `HourlyEnergyRepository` (caches today's hourly breakdown; re-fetches once per hour) and `DailyEnergyRepository` (caches per-day totals permanently; only re-fetches the current day and any uncached days).
- **New HomeFragment sections**:
  - Today's hourly line chart (6 AM → current hour; past hours solid, current hour dashed)
  - Morning (hours 0–11) and afternoon (hours 12–23) production tables
  - Summary cards: today's total, best day this month, best day in last N days, this month total, last 30 days total
  - History bar chart (daily totals over last N days, bars coloured by month)
- **System capacity setting** wired to chart Y-axis maximum (reuses existing `solar-array-capacity` spec).
- **ADR-009 allocation table** updated with the two new endpoints.

## Capabilities

### New Capabilities

- `hourly-production`: Today's hourly energy data — line chart, morning/afternoon table, today-total card, best-day cards
- `production-history`: Multi-day daily energy data — history bar chart, this-month total, last-30-days total

### Modified Capabilities

- `solar-array-capacity`: System capacity (kW) now also used as the Y-axis maximum for charts, not just stored as a preference. Adds a behavioural requirement on top of the existing storage spec.

## Impact

- **EmaApiClient / OkHttpEmaApiClient**: two new suspend methods; new request-signing paths for the `energy_level=hourly` and `energy_level=daily` endpoints
- **HomeFragment**: layout grows significantly; fragment drives two new repositories in addition to the existing ProductionRepository
- **ADR-009**: two new rows in the API budget allocation table; combined new spend ≈ 60 calls/month (well within the 820-call headroom)
- **No breaking changes** to Settings, navigation, or existing tile data
