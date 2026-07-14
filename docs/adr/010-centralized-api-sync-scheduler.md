# ADR-010: Centralized API Sync Scheduler

**Status:** Accepted
**Date:** 2026-07-13

## Context

Three independent places decided when to fetch hourly, daily, and module-health data from the EMA API, each unaware of the others:

1. `HomeFragment.onResume()`/`onPullToRefresh()` — launched `viewLifecycleOwner.lifecycleScope` coroutines calling each tile repository's `refresh()` directly.
2. `SettingsFragment.invalidateApiThrottle()` — called from 7 separate call sites (one per credential-affecting field's `onSave`, the Base URL reset button, and the "Use local stub" button), each independently resetting throttles and force-refreshing hourly and daily.
3. `WidgetRefreshWorker` (periodic, every 2 hours) and `ModuleHealthWorker` (periodic, daily at 8pm array-local) — each did its own gating and called `refresh()` independently of the other two.

This scattering caused a confirmed bug: editing two or more connection-setting fields in a row fired multiple redundant fetches for the same data, and whichever fetch finished last won — if that one failed, it overwrote a moments-earlier success with an error, masking good data behind a spurious error banner. This was found via a CI Maestro E2E failure (the local `ema-api-stub`'s single-use canonical interaction was consumed by an earlier redundant fetch) but is equally reproducible against the real EMA API on a bad network moment. Fetches scoped to `viewLifecycleOwner.lifecycleScope` also silently dropped when the user navigated away from Home mid-fetch, even though the fetch was legitimately due and throttle-respecting.

As the app adds more tiles and widgets (ADR-007's tile-repository pattern), each new data source risks repeating this class of bug unless fetch-triggering decisions live in one place instead of being re-implemented per frontend.

## Decision

**`ApiSyncScheduler` (in `core/api/`) is the single entry point every frontend uses to request a data refresh.** No Fragment, Activity, or Worker calls a tile repository's `refresh()` method directly — ever. `HomeFragment`, `SettingsFragment`, and the widget background worker all route through it.

### Request kinds and coalescing

Three request kinds are supported, each mapped to its own `WorkManager` unique work name so different kinds don't cancel each other:

- **Opportunistic** — a screen became visible; respects each source's own throttle.
- **Forced** — pull-to-refresh; bypasses throttle.
- **Settings-changed** — a credential or Base URL edit; resets throttle, so a burst of edits coalesces into one resulting fetch instead of one per field.

Every request is enqueued via `WorkManager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)`. `REPLACE` correctly coalesces both a *fast* burst (a not-yet-started duplicate is cancelled outright) and a *slow* burst (an in-flight `CoroutineWorker` is cancelled via ordinary coroutine cancellation, so its result is never persisted) — at any timing gap between requests, only the last-enqueued request's fetch ever completes and persists.

### Execution moves into a `CoroutineWorker`

All actual fetching happens in `ApiSyncWorker`, not in a Fragment's lifecycle scope. A Fragment calls a `request*Sync()` method (fire-and-forget) and separately observes completion via `ApiSyncScheduler.observeCompletion(context): Flow<Unit>` (backed by `WorkManager.getWorkInfosForUniqueWorkFlow`) to re-render from `currentState()` once a sync finishes while still visible. If the Fragment is destroyed mid-sync, only the observer's collection stops — the underlying `WorkManager` request keeps running and persists its result regardless, so the next visit to Home or a widget update reflects it.

### Alerting data is never gated

Module Health (the data source backing email/push alerting) is the one data source that ignores every gate: it does not check whether its own tile is enabled, whether the app is open, or whether any widget is placed. `ModuleHealthWorker` keeps its own separate daily-8pm-array-local `PeriodicWorkRequest`, entirely outside `ApiSyncScheduler`'s cadence, because alerting is a distinct feature from a Home tile's visibility — a user who hides the Module Health tile must still receive alerts. Only its Settings-triggered throttle-reset routes through `ApiSyncScheduler` for consistency; its schedule and gating logic are otherwise untouched.

### Display-class data is gated on enabled consumers, actual placement, and a daylight window

For hourly and daily data (and any future tile/widget's data), the unattended background poll requires:

1. **An enabled consumer** — `SettingsRepository.isHourlyDataNeeded()`/`isDailyDataNeeded()` (unchanged from the tile/widget-visibility feature).
2. **An actually placed widget consuming that data, or the app in the foreground** — `GlanceAppWidgetManager.getGlanceIds()` non-empty for a relevant widget type, or a recent `ProcessLifecycleOwner`-backed foreground flag. A widget type enabled in Settings but never dragged onto a home screen does not keep a background job alive. Placement changes are detected via each widget's `GlanceAppWidgetReceiver.onEnabled()`/`onDisabled()`, which (re-)arm or cancel `ApiSyncWorker`'s periodic schedule; a safety-net re-check also runs on every opportunistic sync request in case a callback is ever missed.
3. **The array's daylight window (06:00–22:00 array-local) for hourly** — production cannot change overnight, so polling then is pure budget waste. Home's own on-demand requests (opportunistic or forced) are exempt from both the placement check and the daylight window — a user action always gets a real attempt, subject only to the repository's own throttle.

Hourly polls at a 45-minute cadence within the daylight window (`HourlyEnergyRepository.THROTTLE_MS` = 2,700,000ms).

### A data source computable from an already-fetched sibling should not issue its own redundant call

`DailyEnergyRepository`'s "today" total is exactly `sum(hourly.hours.values())` — data already obtained by `HourlyEnergyRepository`'s own fetch. Instead of independently calling `getDailyEnergy` for today on every trigger, `DailyEnergyRepository` derives it from a `todayTotalProvider: () -> Double?` lambda wired to `HourlyEnergyRepository`'s cached snapshot, at zero additional API cost. Real `getDailyEnergy` calls remain only for the one-time historical backfill and one authoritative call per day to lock in each day's final total once it rolls over to "past" (a real API call, not a possibly-incomplete hourly-derived sum, to keep "past days are immutable and authoritative" intact).

Because Daily's freshness now depends on Hourly's cadence, the scheduler's "is hourly needed" check widens to `isHourlyDataNeeded() || isDailyDataNeeded()` — hourly keeps polling even with zero hourly-specific consumers, as long as a daily consumer is enabled. This widening happens only at the scheduler/worker gating call site; `SettingsRepository.isHourlyDataNeeded()` itself keeps its narrower original meaning for other callers (e.g. Home's own tile-visibility check).

### Worst-case schedule (all widgets placed, app foreground continuously)

| Data source | Poll interval | Triggers/day | Calls/month (×31d) |
|---|---|---|---|
| Module Health | Fixed, once @ 8pm array-local, never gated | 1 | ~31 |
| Hourly energy | Every 45min, 06:00–22:00 array-local only | ~21 | ~651 |
| Daily energy (backfill only) | Once, first trigger after local midnight | 1 | ~31 |
| **Total** | | | **~713** |
| **Headroom** (of 1,000/month, ADR-009) | | | **~287** |

See [ADR-009](009-ema-api-call-budget.md) for the full budget accounting.

### Pattern for future tiles and widgets

A new EMA-API-backed tile or widget must decide, at design time:

- **Is it alerting-class** (must run unconditionally in the background, like Module Health)? Give it its own dedicated `PeriodicWorkRequest`, never gated by tile/widget-enabled state, app-open state, or widget placement.
- **Is it display-class** (a Home tile or widget showing data)? Route every fetch through `ApiSyncScheduler`; gate its background poll on an enabled consumer, actual widget placement or app foreground, and an appropriate time-of-day window.
- **Is its value computable from an already-fetched sibling data source?** If so, derive it (a lambda-injected provider, matching `DailyEnergyRepository`'s `todayTotalProvider` pattern) instead of issuing a redundant independent fetch.
- **Never add a new direct `repository.refresh()` call site** in a Fragment, Activity, or ad hoc Worker.
- Add a row to ADR-009's allocation table using the same trigger-window methodology (triggers/day × 31), and confirm the new total still fits under the 1,000/month ceiling.

## Alternatives Considered

**A `Mutex`/`Job`-cancellation pattern inside a long-lived singleton, without `WorkManager`.** Rejected — this reintroduces the exact problem being fixed: a coroutine scoped to something that can die (a Fragment's `lifecycleScope`, or a process killed by the OS) loses in-flight work. `WorkManager`'s request survives process death and re-runs on next opportunity, which the app's existing periodic workers already rely on.

**A fixed-delay debounce (e.g. "wait 500ms after the last request before firing") instead of `WorkManager` unique-work replacement.** Rejected — the failure sequence that motivated this change (tap "Use local stub" → scroll → edit ECU-ID → save → navigate to Home) spans several real seconds of UI interaction, far longer than any reasonable debounce window, so a timer-based debounce would not have coalesced it. `enqueueUniqueWork(..., REPLACE, ...)` coalesces correctly at any timing gap, not just short ones.

**Merging `ModuleHealthWorker`'s daily-8pm-array-local schedule into `ApiSyncWorker`'s rolling-interval cadence.** Rejected — a specific wall-clock time in a specific timezone is a fundamentally different scheduling shape than a rolling interval; collapsing them would require either running `ApiSyncWorker` every few minutes (wasteful) or teaching it two scheduling models for no benefit, since the two data sources already have unrelated freshness requirements.

**A more surgical fix — only route Settings' force-refresh through a Worker, leaving Home's fetch in its Fragment scope.** Rejected — this closes only the specific bug found, not the general problem (fetch-triggering decisions scattered across every frontend); Home's own fetch would remain vulnerable to the same silent-cancellation-on-navigate-away class of issue, and any new tile added later would face the same choice again with no established pattern to follow.

**Keeping `DailyEnergyRepository`'s independent "today" call but loosening its throttle instead of deriving from hourly.** Rejected — any independent call duplicates data already being fetched; loosening the throttle only shrinks the waste, it does not eliminate it, and buys no freshness `DailyEnergyRepository` does not already get for free by reading hourly's cache.

## Consequences

- No Fragment, Activity, or ad hoc Worker may call a tile repository's `refresh()` directly; all fetch requests go through `ApiSyncScheduler`.
- `WidgetRefreshWorker` is replaced by `ApiSyncWorker`, which also handles the three on-demand request kinds; `ModuleHealthWorker` is unchanged except its Settings-triggered throttle-reset now routes through `ApiSyncScheduler`.
- Adding a new tile or widget backed by the EMA API means deciding alerting-class vs. display-class, wiring it through `ApiSyncScheduler`/`ApiSyncWorker`'s existing gating rather than inventing a new trigger point, and checking whether its value is derivable from an already-fetched sibling before adding a new endpoint call.
- `HourlyEnergyRepository`'s throttle tightens from 1 hour to 45 minutes; `DailyEnergyRepository` no longer independently fetches "today" and instead depends on `HourlyEnergyRepository`'s cache, which widens hourly's own trigger condition to also fire for daily-only consumers.
- ADR-009's allocation table must be kept in sync with this scheduler's actual gating and cadence whenever either changes.
- Existing tests asserting synchronous call counts immediately after triggering an action need rework around `WorkManager`'s asynchronous execution model (`WorkManagerTestInitHelper` + `TestDriver` in Robolectric).
