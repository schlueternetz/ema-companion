## Why

`HomeFragment`, `SettingsFragment`, and `WidgetRefreshWorker` each independently decide when to call `refresh()` on the hourly/daily/module-health repositories, with no coordination between them. This scattering caused a real bug, confirmed via a CI Maestro failure: `SettingsFragment.invalidateApiThrottle()` fires an **independent** forced hourly+daily refresh on *every single* credential-affecting field save (App ID, App Secret, System ID, ECU ID, Base URL, "Use local stub" — 7 call sites). Editing two or more of these fields in a row while already configured fires multiple redundant fetches for the same data; if a later one fails (network blip, or — as observed in CI — the local `ema-api-stub`'s single-use canonical interaction already being consumed by an earlier redundant fetch) its failure **overwrites and masks** the good data + success state a moments-earlier fetch had just persisted. This is user-visible (a real user editing multiple settings in a row can see a spurious error banner despite a just-succeeded fetch) and wastes EMA API budget (ADR-009 says "reuse already-fetched data" first; today's code fires unconditionally per field, not once per "credentials changed" event).

The fix requires more than patching one call site: as long as three independent places (Fragment lifecycle, Settings field saves, a periodic Worker) can each decide to fetch the same data with no visibility into what the others are doing, this class of bug can recur. This change centralizes that decision into one scheduler.

## What Changes

- Introduce `ApiSyncScheduler`, the single entry point every frontend uses to request a data refresh — `HomeFragment`, `SettingsFragment`, and the widget background worker stop calling `repository.refresh()` directly.
- All actual fetching moves into a WorkManager `CoroutineWorker` (process/Fragment-lifecycle-independent — a fetch in flight is no longer cancelled by the user quickly navigating away from Home), enqueued via `WorkManager.enqueueUniqueWork(..., ExistingWorkPolicy.REPLACE, ...)`. Repeated requests (e.g. a burst of Settings field saves) collapse into a single execution: a pending, not-yet-started request is replaced outright; an in-flight one is cancelled (`CoroutineWorker` honors coroutine cancellation) before it can persist a stale result.
- Three request kinds, all routed through the scheduler: opportunistic (Home becoming visible — respects each source's own throttle), forced (pull-to-refresh — bypasses throttle), and settings-changed (credential/Base URL edits — resets throttle, coalesces a burst of edits into one resulting fetch instead of one per field).
- The scheduler continues to gate every fetch on `SettingsRepository.isHourlyDataNeeded()`/`isDailyDataNeeded()` (unchanged semantics from `configurable-tiles-widgets`) — centralizing *who* decides to fetch does not change *whether* a data source with zero enabled consumers gets fetched. The **unattended background poll** additionally gates on whether a consuming widget is *actually placed* (`GlanceAppWidgetManager.getGlanceIds()`), not just enabled in Settings — Home's own on-demand triggers are unaffected by this extra check.
- **Module Health (alerting) is never gated** by app-open state, widget placement, or even its own tile's enabled checkbox — it must keep checking and sending email/push alerts in the background unconditionally, since alerting is a distinct feature from the Home tile's visibility. This matches `ModuleHealthWorker`'s existing behavior; this change makes the invariant explicit and tested.
- The background poll cadence is retuned for freshness within budget: hourly polls only during the array's daylight production window (06:00–22:00 array-local) at a 45-minute cadence, rather than a flat every-2-hours-including-overnight schedule.
- **Daily's "today" total is derived from the hourly snapshot** (`sum(hourly.hours.values())`) instead of its own independent API call — `DailyEnergyRepository` no longer fetches "today" from the EMA API at all; it only calls `getDailyEnergy` for the one-time historical backfill and once per day to lock in each day's authoritative final total after it rolls over to "past". Since Daily's freshness now depends on Hourly's cadence, the scheduler's hourly-needed check widens to `isHourlyDataNeeded() || isDailyDataNeeded()`. See `design.md`'s proposed schedule table (~713 calls/month worst case on a 31-day basis, ~287 headroom).
- `HomeFragment` observes sync completion (via `WorkManager`'s `WorkInfo` flow) to re-render `currentState()` once a requested sync finishes, instead of awaiting its own coroutine.
- **BREAKING**: none for end users. `ModuleHealthWorker`'s own daily-8pm-array-local periodic schedule is unchanged; only its Settings-triggered throttle-reset routes through the new scheduler for consistency.

## Capabilities

### New Capabilities
- `api-fetch-scheduler`: the centralized scheduler — request kinds, coalescing/cancellation semantics, and the invariant that no frontend calls a tile repository's `refresh()` directly.

### Modified Capabilities
- `hourly-production`: "throttle reset on credential change" requirement updated to specify coalesced (not per-field) resync behavior.
- `production-history`: same coalescing update as `hourly-production`, for the daily repository.

## Impact

- New: `core/api/ApiSyncScheduler.kt`, `core/api/ApiSyncWorker.kt` (or repurposed from `feature/widgets/WidgetRefreshWorker.kt`).
- `feature/home/HomeFragment.kt` — `onResume()`/`onPullToRefresh()` call the scheduler instead of `repository.refresh()`; add a `WorkInfo` observer for re-render-on-completion.
- `feature/settings/SettingsFragment.kt` — all 7 `invalidateApiThrottle()` call sites route through `ApiSyncScheduler.requestResyncAfterSettingsChange()` instead of resetting throttle + force-refreshing inline.
- `feature/widgets/WidgetRefreshWorker.kt` — becomes (or is replaced by) the scheduler's `CoroutineWorker`; `WidgetUpdater.enabledWidgets()` gating is unchanged; gains a widget-placement check (`GlanceAppWidgetManager`) and the daylight-window/retuned cadence.
- `core/api/HourlyEnergyRepository.kt` — `THROTTLE_MS` lowered from 3,600,000ms (1h) to 2,700,000ms (45min).
- `core/api/DailyEnergyRepository.kt` — gains a `todayTotalProvider: () -> Double?` constructor lambda; steady-state `refresh()` derives today's value from it instead of calling `client.getDailyEnergy`; the existing `hasMissingPastDays` backfill branch (unchanged) now also serves as the once-per-day-rollover lock-in call.
- `feature/widgets/*WidgetReceiver.kt` (3 files) — `onEnabled()`/`onDisabled()` become the hooks that start/stop the periodic background schedule based on placement.
- `feature/home/ModuleHealthWorker.kt` — unchanged periodic logic and unchanged never-gated scheduling; its throttle-reset trigger from Settings routes through the new scheduler.
- Test files exercising the old direct-call pattern (`SettingsWidgetRefreshTest`, `HomeWidgetUpdateTest`, `HomeTodaySectionTest`'s pull-to-refresh test, `WidgetRefreshWorkerTest`) need rework around the new request/coalesce model.
- `HourlyEnergyRepositoryTest`/`DailyEnergyRepositoryTest` need new coverage for the 45-min throttle and the today-derived-from-hourly / day-rollover-backfill behavior respectively.
- `docs/adr/009-ema-api-call-budget.md` — allocation table updated to replace the flat "every 2h" hourly/daily rows with the new daylight-windowed cadence and the always-on module-health row's unchanged status made explicit.
- `docs/adr/` — a new ADR documenting the centralized-scheduler pattern (mirrors ADR-007's tile-repository pattern, ADR-009's call-budget discipline) and the always-on-alerting / placement-gated-background-poll invariants.
