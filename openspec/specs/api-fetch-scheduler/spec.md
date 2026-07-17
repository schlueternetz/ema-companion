## Requirements

### Requirement: A single scheduler is the only path to a data-source fetch
No Fragment, Worker, or other frontend component SHALL call a tile repository's (`HourlyEnergyRepository`, `DailyEnergyRepository`, `ModuleHealthRepository`) `refresh()` method directly. All refresh requests SHALL be issued through `ApiSyncScheduler`.

#### Scenario: Home requests rather than fetches directly
- **WHEN** `HomeFragment` becomes visible or the user pulls to refresh
- **THEN** it SHALL call `ApiSyncScheduler`, not a repository's `refresh()`, to request the update

#### Scenario: Settings requests rather than fetches directly
- **WHEN** the user saves a connection-affecting setting (App ID, App Secret, System ID, ECU ID, Base URL)
- **THEN** `SettingsFragment` SHALL call `ApiSyncScheduler`, not a repository's `refresh()`, to request the resulting resync

### Requirement: Repeated requests of the same kind coalesce into one fetch
When multiple requests of the same kind (opportunistic, forced, or settings-changed) for the same data source arrive before the previous one has completed, only the most recent request SHALL result in a persisted fetch outcome. This SHALL hold regardless of the time gap between requests (sub-second or several seconds).

#### Scenario: Rapid-fire settings edits result in one fetch
- **WHEN** the user saves two or more connection-affecting settings within the time it takes the first resulting fetch to complete
- **THEN** only one fetch per affected data source SHALL actually run to completion and persist its result

#### Scenario: Slower sequential settings edits still coalesce
- **WHEN** the user saves two connection-affecting settings several seconds apart, such that the first save's fetch has already started (or already finished) before the second save occurs
- **THEN** the first fetch's in-flight execution SHALL be cancelled (if still running) and SHALL NOT persist its result once the second request has been made; the second (latest) request's fetch SHALL be the one whose result is persisted and rendered

#### Scenario: A later successful fetch is never overwritten by an earlier straggler
- **WHEN** two fetches for the same data source were requested close together and the earlier one is still in flight when the later one is requested
- **THEN** the earlier fetch's outcome (success or failure) SHALL NOT be persisted after the later request supersedes it

### Requirement: A requested fetch survives the requesting UI going away
Once a fetch request is accepted by the scheduler, it SHALL run to completion independent of the requesting Fragment's view lifecycle — navigating away from the screen that made the request SHALL NOT cancel an in-flight, still-current (not superseded) fetch.

#### Scenario: Fetch completes after leaving Home
- **WHEN** the user requests a sync from Home and then navigates to another screen before it completes
- **THEN** the fetch SHALL continue running and its result SHALL be persisted, visible the next time Home or an affected widget is viewed

### Requirement: Hourly/daily gating on enabled consumers is unchanged by centralization, except hourly's gate widens to cover Daily's dependency
The scheduler SHALL only fetch daily data (the once-per-day backfill call — see `production-history`) when `SettingsRepository.isDailyDataNeeded()` returns true. Because `DailyEnergyRepository` derives its live "today" value from hourly data (see `production-history`'s "Today's total is derived from hourly data" requirement), the scheduler SHALL fetch hourly data when `isHourlyDataNeeded() OR isDailyDataNeeded()` returns true, not `isHourlyDataNeeded()` alone.

#### Scenario: Scheduler skips a daily backfill with no enabled consumer
- **WHEN** daily data currently has no enabled tile or widget consumer
- **THEN** the scheduler SHALL NOT issue a daily backfill fetch, regardless of which request kind triggered the scheduler

#### Scenario: Scheduler skips an hourly fetch with no enabled consumer of either kind
- **WHEN** neither hourly nor daily data currently has an enabled tile or widget consumer
- **THEN** the scheduler SHALL NOT issue an hourly fetch, regardless of which request kind triggered the scheduler

#### Scenario: A daily-only consumer still keeps hourly fetching alive
- **WHEN** a daily tile or widget is enabled but no hourly-specific tile or widget is enabled
- **THEN** the scheduler SHALL still issue hourly fetches, so Daily's derived today-total has fresh data to sum

### Requirement: The alerting data source is never gated
Module Health fetches (the data source backing email/push alerting) SHALL run on their existing unconditional daily schedule regardless of: whether the Module Health tile is enabled in Settings, whether the app is currently open, or whether any widget is placed. `isModuleHealthDataNeeded()` (or any tile/widget-enabled check) SHALL NOT be consulted before a scheduled Module Health check runs.

#### Scenario: Module Health tile disabled does not stop alerting
- **WHEN** the user disables the Module Health tile in Settings
- **THEN** the daily Module Health background check SHALL continue to run and SHALL continue to send email/push alerts on status change

#### Scenario: App never opened does not stop alerting
- **WHEN** the app has not been opened since the last Module Health check
- **THEN** the next scheduled Module Health check SHALL still run at its usual time

#### Scenario: No widgets placed does not stop alerting
- **WHEN** no home-screen widgets of any kind are placed
- **THEN** the daily Module Health background check SHALL still run

### Requirement: The unattended periodic background poll for hourly data requires an actual placed widget
Independent of `isHourlyDataNeeded()`/`isDailyDataNeeded()` (which only reflect Settings-enabled state), the periodic background poll for hourly data SHALL additionally require at least one placed instance (`GlanceAppWidgetManager.getGlanceIds()` non-empty) of a widget type that consumes hourly or daily data, unless the app is currently in the foreground. This additional check applies only to the unattended background poll — Home's own on-demand (opportunistic or forced) requests are never subject to it. Daily's once-per-day backfill check (see `production-history`) is gated only on `isDailyDataNeeded()`, not placement, since its cost is fixed regardless of trigger frequency.

#### Scenario: Enabled but unplaced widget does not keep the background poll alive
- **WHEN** a widget type that consumes hourly data is enabled in Settings but no instance of it is placed on any home screen, and the app is not in the foreground
- **THEN** the periodic background poll SHALL NOT fetch hourly data on that basis alone

#### Scenario: Placing a widget starts the background poll
- **WHEN** the user places the first instance of a widget that consumes hourly or daily data
- **THEN** the periodic background poll for hourly data SHALL become active

#### Scenario: Removing the last instance of a widget type stops contributing to the background poll
- **WHEN** the user removes the last placed instance of a widget type, and no other placed widget or app-foreground condition still needs hourly data
- **THEN** the periodic background poll for hourly data SHALL stop

#### Scenario: Home's on-demand request is unaffected by placement
- **WHEN** the user opens Home while no widgets are placed
- **THEN** Home's own opportunistic sync request SHALL still be issued, subject only to `isHourlyDataNeeded()`/`isDailyDataNeeded()` and the repository's own throttle

### Requirement: Background hourly polling is limited to the array's daylight window
The unattended background poll for hourly data SHALL only run between 06:00 and 22:00 array-local time. Outside that window, the periodic poll SHALL take no action for hourly data, regardless of widget placement or app-foreground state. The once-per-day daily backfill check is not subject to this window — a past day's total does not change with time of day, so it may run at the first opportunity after local midnight.

#### Scenario: No background hourly poll before 06:00 or after 22:00 array-local
- **WHEN** the periodic background poll's scheduled run time falls outside 06:00–22:00 array-local time
- **THEN** it SHALL NOT issue an hourly fetch

#### Scenario: Daily backfill is not time-of-day gated
- **WHEN** the daily backfill check's scheduled run time falls outside 06:00–22:00 array-local time
- **THEN** it SHALL still be attempted, subject only to `isDailyDataNeeded()`

#### Scenario: User-initiated requests are not time-of-day gated
- **WHEN** the user opens Home or pulls to refresh outside the 06:00–22:00 array-local window
- **THEN** the resulting on-demand request SHALL still be attempted, subject only to the repository's own throttle

### Requirement: A completed sync is observable by the requesting screen
A Fragment that requested a sync and remains visible SHALL be able to observe when that sync completes, so it can re-render from the repository's updated `currentState()`.

#### Scenario: Home re-renders after an opportunistic sync completes
- **WHEN** Home requests an opportunistic sync on becoming visible and remains on screen until the sync completes
- **THEN** Home SHALL re-render the affected tile(s) from the updated `currentState()` without requiring the user to leave and return
