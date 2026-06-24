## Context

The app already fetches production data from the EMA API on Home screen load. Module health is a derived metric—each module has a last-production timestamp that can be queried. The Home screen uses a tile-based layout with production and other metrics. Background work uses SharedPreferences for state persistence (API throttle, last-fetch timestamp). Notifications are not yet implemented in the app.

## Goals / Non-Goals

**Goals:**
- Display module health status on Home with single API call (efficient, no per-module queries)
- Show green/yellow/red status synchronized with real production data
- Enable background 24-hour check with local notifications
- Minimize API overhead: use throttle + persisted state (same 24-hour throttle pattern as production tile)
- Provide debug scenarios in API stub for testing all three states

**Non-Goals:**
- Email/SMS notifications (document free/cheap options; leave for future)
- Per-module settings or alerts
- Detailed module telemetry beyond "hasn't produced since"
- Real-time notifications (use 24-hour background check)

## Decisions

### 1. Batch Inverter Energy per Day, with Per-Day Persistence
**Decision**: Use `GET /user/api/v2/systems/{sid}/devices/inverter/batch/energy/{eid}?energy_level=energy&date_range=yyyy-MM-dd` (section 3.5.3) to retrieve all inverters' energy for a single day. Past days' results are persisted in `ema_module_health_daily` SharedPreferences keyed by date. On each 24-hour check, only fetch dates that are missing from the cache — past days are immutable once fetched, so they are never re-fetched. Today is always re-fetched (data accumulates during the day). Status is computed from the persisted 3-day window.

**Normal case (device ran yesterday)**: Only today's batch energy call is made — yesterday and day-before are already cached. 1 API call.

**Catch-up case (device was off N days)**: Fetch the N missing days plus today. Maximum 3 calls regardless, since only the last 3 days matter for classification; older days are not needed.

**Rationale**: No single "module summary" endpoint exists in the EMA API — there is no per-module last-production timestamp field. Persisting per-day results avoids redundant calls for immutable historical data and handles device downtime without over-fetching.

**Alternatives Considered**:
- Re-fetch all 3 days every run: wastes quota on immutable data
- Per-module queries (3.5.1): N+1 problem, excessive API usage
- System `light` field (3.1.1): gives green/yellow/red at system level but no per-module identity for the detail modal

### 2. Schedule Job at 8pm Array-Local Time
**Decision**: The `ModuleHealthWorker` is scheduled to run at 8pm in the configured array timezone. A `PeriodicWorkRequest` with a calculated `initialDelay` (milliseconds until next 8pm) aligns the first run; subsequent runs repeat every 24h. `ExistingPeriodicWorkPolicy.KEEP` prevents app restarts from resetting the timer. The array timezone is a user setting defaulting to the device timezone.

**Rationale**: Running at 8pm means solar production hours are always over when the check executes. Today's 0 kWh readings are real, not artifacts of a midnight run. This eliminates any need for a time-of-day cutoff in the status computation logic.

**Alternatives Considered**:
- 8pm evaluation cutoff in computation: runs job anytime, skips today if before 8pm — adds conditional logic and edge cases to computeStatus(); scheduling is simpler
- Sunrise/sunset API: adds dependency; fixed 8pm is safe and simpler
- AlarmManager exact alarm: requires `SCHEDULE_EXACT_ALARM` permission dialog; WorkManager inexact is sufficient for ±30min tolerance

### 3. Absent UID = 0 kWh via Union UID Set
**Decision**: The expected inverter set is the union of all UIDs seen across the evaluation window. Any UID absent from a given day's response is treated as 0 kWh for that day — identical to an explicit 0.0 entry. No separate "count changed" alert type is needed; the drop is caught by the existing YELLOW/RED threshold logic.

**Rationale**: It is unknown whether a broken inverter reports 0.0 or disappears from the response entirely. The union approach handles both cases identically. If a broken inverter stops reporting, the count drops, the missing UIDs accumulate 0-kWh days, and the YELLOW/RED path fires normally.

**Alternatives Considered**:
- Separate count-change alert: adds a new alert type and state; union approach achieves the same effect with no extra logic
- Treat absent as unknown (skip): risks silently missing broken inverters that stop reporting

### 4. Status Logic in Repository, Not UI
**Decision**: `ModuleHealthRepository.computeStatus()` takes timestamps and returns immutable `ModuleHealthState(status, offlineModules: List<Module>)`.

**Rationale**: Deterministic, testable pure function. UI observes the state. Same pattern as `ProductionSource`.

**Alternatives Considered**:
- Logic in Fragment: fragile, hard to test
- Live calculation: no; state is persisted, displayed immediately on app open

### 3. 12-Hour Throttle + Persisted State
**Decision**: Store `lastCheckEpochMs` and `lastState` in `SharedPreferences` (`ema_module_health`). Check only if `now - lastCheck > 24h`. Only trigger notifications on state change (status stepped up: green→yellow, yellow→red, or reverse).

**Rationale**: Matches existing `ema_api_usage` pattern. No duplicate notifications. Works offline (shows cached state immediately).

**Alternatives Considered**:
- No throttle: spam API, waste quota
- Notify on every check: confusing; user sees yellow daily if nothing changes

### 4. Background Work: WorkManager Periodic Task
**Decision**: Use `PeriodicWorkRequest` (24-hour interval, flexInterval=1h) to poll in background. Runs even if app closed. On state change, post notification via `NotificationManager`.

**Rationale**: WorkManager is the modern Android background pattern (replaces AlarmManager for periodic tasks). Survives device reboot. Respects doze/battery-optimization (flexInterval allows system to batch).

**Alternatives Considered**:
- AlarmManager: older, requires boilerplate for API levels
- Service: foreground service requires persistent notification (ugly UX)
- Scheduled task library: add dependency; WorkManager is built-in

### 5. Daily Re-Alert While Non-Green
**Decision**: Post a notification on every check where status is YELLOW or RED. Replace (not stack) the previous notification. Clear on GREEN. No suppression for repeated non-green status.

**Rationale**: A module that stays offline for a week should alert daily — the user needs a persistent reminder to act. State-change-only notification means one alert then silence until the status steps up or recovers, which misses the ongoing problem.

**Alternatives Considered**:
- State-change-only: quieter but lets persistent faults go unnoticed after the first alert
- FCM push from server: requires backend infrastructure; out of scope for MVP

**Notification permission**: `POST_NOTIFICATIONS` required at runtime on API 33+. Check is performed before posting; silent no-op if denied (background check and status persistence still run).

### 6. Tile Detail Modal: Simple List View
**Decision**: Tap tile → modal dialog shows list of `Module` objects with "hasn't produced for X hours". Scrollable if >10 modules.

**Rationale**: Minimal UI, matches app's existing modal pattern. No new fragment.

**Alternatives Considered**:
- Full Fragment: more complex, navigation headaches
- Inline expand: eats space, clutters Home

### 7. API Stub Scenarios
**Decision**: Add three `.json` scenario files under `code/ema-api-stub/scenarios/`:
- `module-health-all-healthy-EDU.json`: all modules, all timestamps ≤24h ago
- `module-health-one-offline-24h-EDU.json`: one module, last timestamp 24h ago
- `module-health-one-offline-90h-EDU.json`: one module, last timestamp 90h ago

**Rationale**: Covers green/yellow/red states. EDU-keyed so tests can pick them by ID. Matches existing stub design.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Batch energy returns 0 kWh on a cloudy day (not just offline) | Acceptable: a module producing 0 kWh for 3 consecutive days is almost certainly offline, not just cloudy |
| Daily cache grows unbounded | Prune entries older than 3 days on each successful check |
| Device off >3 days: missing data for the full classification window | Fetch all 3 days on catch-up (max 3 calls); accept that the oldest day may have been a low-sun day if device was off exactly 3 days |
| 24-hour throttle misses real changes in between | Acceptable: module offline >24h is a slow-changing signal. User can tap "Refresh" to force (future) |
| WorkManager not scheduled on first app install | Trigger schedule in `MainActivity.onCreate` or `Application.onCreate` |
| Battery-heavy polling | 24h interval is reasonable. Use flexInterval so system can batch with other work |
| State change logic is fragile | Test: `assertEquals(status, computeStatus(timestamps))` for all three cases + edge (boundary times) |
| Notification channel not created before API 26 | Conditional: `if (Build.VERSION.SDK_INT >= 26) { createChannel() }` |
| Exact alarm permission missing → background check delayed | Graceful: app still works, just less precise scheduling. Document permission requirement |
| WorkManager not available (removed in future Android) | Unlikely: it's the official background task library, will have long support |

## Migration Plan

1. **Phase 1 (MVP)**: Home tile + repository + local notifications
   - Deploy, verify tile shows correctly
   - Manual test: fake timestamps in SharedPreferences, observe status
2. **Phase 2**: Background WorkManager task
   - Deploy, trigger manually via WorkManager testing tools
3. **Phase 3 (Future)**: FCM integration (separate change)

Rollback: Disable tile visibility in feature flag or remove tile from Home layout XML.

## Open Questions

1. **Notification titles/text**: Finalize copy for yellow/red alerts. (Resolve before coding notification builder.)
2. **Background task frequency**: 24h is defined in `AppConfig.MODULE_HEALTH_CHECK_INTERVAL`. Adjust if needed.
