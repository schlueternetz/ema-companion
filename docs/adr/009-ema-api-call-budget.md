# ADR-009: EMA API Call Budget

**Status:** Accepted  
**Date:** 2026-06-30

## Context

The EMA API enforces a hard monthly call quota. Calls cannot be refunded; every successful request that retrieves data is billed against the quota regardless of whether the data was needed. As the app grows to cover more features, uncoordinated API usage will silently exhaust the budget mid-month and disable all data retrieval until the quota resets.

A budget allocation table and counting rules are needed so:
- the total can be verified before a new feature ships
- agents designing features have a concrete number to work within
- the current balance is visible without reading every repository

## Decision

### Monthly budget

**1,000 successful API calls per month.**

This is the planning target. Features are designed to stay within it collectively.

### What counts

Only **successful** API calls count toward the budget — EMA API responses where `code == 0` (data was returned). This matches how the EMA API bills: data access is billed, not rejected requests.

Failures (network errors, auth errors, bad parameters, server errors) are free and retry on the next trigger. `ConfigurationError` never issues a request and never counts.

This rule is already enforced in `ProductionRepository` and `ModuleHealthRepository`: only `ApiResult.Success` triggers `usage.recordRequest()` and sets the throttle timestamp.

### Allocation table

Update this table whenever a new feature is added or an existing one changes its call pattern. Estimates use realistic daily usage (not theoretical maximums).

| Feature | Endpoint | Throttle | Calls/trigger | Triggers/day | Calls/month |
|---------|----------|----------|---------------|--------------|-------------|
| Module Health tile | `getBatchInverterEnergy` | 24 hr | 1 (steady state†) | 1 | ~30 |
| Today hourly chart + widgets | `getHourlyEnergy` | 1 hr | 1 | ~12 (widget background worker, every 2h, no time-of-day gating; app opens absorbed by the same throttle) | ~360 |
| History daily chart + widgets | `getDailyEnergy` | 1 hr | 1 (steady state‡) | 1 (widget background worker replaces app-open trigger 1:1) | ~30 |
| **Total allocated** | | | | | **~420** |
| **Headroom** | | | | | **~580** |

† First run ever: up to 3 calls (3-day window, no cache). Steady state: 1 call/day (today always re-fetched; past days cached permanently).

‡ First run ever: 1–2 calls (one per calendar month in the history window). Steady state: 1 call/day (today only; past days cached permanently).

The Current Production tile (`getCurrentProduction`, ~150 calls/month) was removed from the app — it duplicated the instantaneous production reading already shown in the original APsystems EMA app — freeing the headroom above.

### Design constraints for new features

Before implementing any feature that calls the EMA API:

1. Add a row to the allocation table above with endpoint, throttle, calls/trigger, triggers/day, and calls/month.
2. Confirm the new total stays at or below 1,000/month.
3. If the feature cannot fit, reduce its trigger frequency or increase its throttle before implementing.

Prefer in order:
1. **Reuse already-fetched data** — if another feature fetched the same endpoint in the same session, share the result.
2. **Cache immutable past data** — data for completed days cannot change; cache it on first fetch and never re-fetch.
3. **Throttle guards** — persist the last-fetch timestamp and skip the request if the throttle window has not elapsed.
4. **Fresh fetch** — only when the above are genuinely insufficient.

### Tile/widget visibility gates fetches below their worst-case allocation

Users can disable individual Home tiles and widgets in Settings ("Tiles & Widgets" section). Each data source above (hourly, daily, module health) is only fetched when at least one enabled tile or widget still consumes it — `SettingsRepository.isHourlyDataNeeded()` / `isDailyDataNeeded()` / `isModuleHealthDataNeeded()` gate every fetch site (`HomeFragment`, `WidgetRefreshWorker`, `SettingsFragment.invalidateApiThrottle()`). This means the allocation table above is a ceiling, not a guarantee: a user who disables, say, the History Production tile and both daily-consuming widgets stops all daily-energy calls entirely. New features that add a tile or widget must add their own `isXDataNeeded()` consumer check alongside the allocation table row, not just the row itself.

## Alternatives Considered

### Runtime enforcement (disable features when budget is exhausted)

Would require persisting a monthly call counter with a calendar-month reset. The EMA API does not expose remaining quota, so the counter could drift from reality (e.g. after a reinstall). A false positive would disable the app for the rest of the month. Rejected: the risk of false positives outweighs the protection, and staying within budget by design is more reliable than a runtime guard.

### Tracking in code comments near each repository

Scattered across files, invisible at design time, and not updated when adjacent features change. Rejected: a single table at design time (this ADR) is the only place a new feature author can see the running total.

### A separate budget spreadsheet

Not version-controlled alongside the code, drifts immediately. Rejected in favour of this ADR, which is committed with the implementation.

## Consequences

- Every new EMA API feature must update the allocation table in this ADR before the implementation is merged.
- Agents designing features that call the EMA API must consult this table, verify headroom, and propose the minimum-call approach.
- The existing AGENTS.md "EMA API call budget" section remains the concise agent-facing reminder; this ADR is the authoritative source for the numbers.
