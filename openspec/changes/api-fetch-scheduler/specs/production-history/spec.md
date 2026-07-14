## MODIFIED Requirements

### Requirement: Today's total is derived from hourly data; daily API calls are backfill-only
The app SHALL derive the current calendar day's total from `HourlyEnergyRepository`'s cached hourly values (`sum(hours.values())`), not from an independent `getDailyEnergy` call. The app SHALL use the following caching rules for the `getDailyEnergy` endpoint itself:

1. Past days' totals are immutable — once fetched and stored they SHALL NOT be re-fetched.
2. On the first fetch (no cache), the app SHALL fetch the full history window in a single `energy_level=daily` range call.
3. Once the current calendar day rolls over (the local date changes), the app SHALL issue exactly one `energy_level=daily` call covering the newly-completed day, to lock in its authoritative total — it SHALL NOT rely on the last-cached hourly-derived sum for a day that is no longer "today".
4. Outside of cases 2 and 3, the app SHALL NOT issue a `getDailyEnergy` call for the current day.
5. All fetches SHALL be issued through `ApiSyncScheduler` (see `api-fetch-scheduler`), never called directly by a Fragment or Worker.

#### Scenario: First fetch retrieves full history window
- **WHEN** no daily data has been cached yet
- **THEN** the repository SHALL issue one `energy_level=daily` API call covering today minus the history-window days through today

#### Scenario: Steady-state today has no daily API call
- **WHEN** daily data for all past days in the window is already cached and hourly data for today is available
- **THEN** the repository SHALL derive today's total from the hourly snapshot's sum and SHALL NOT issue a `getDailyEnergy` API call

#### Scenario: Day rollover triggers exactly one backfill call
- **WHEN** the local calendar date advances past a day whose total was previously only available via the hourly-derived sum
- **THEN** the repository SHALL issue exactly one `energy_level=daily` API call for that now-past date and persist its result as immutable

#### Scenario: Past days never re-fetched
- **WHEN** a daily record for a past calendar day exists in the cache
- **THEN** the repository SHALL NOT issue any API call for that date on subsequent fetches

#### Scenario: Only a successful backfill call updates the immutable cache
- **WHEN** a day-rollover backfill call fails (network or API error)
- **THEN** the affected date SHALL NOT be marked as cached, so the next trigger retries it

#### Scenario: Cache cleared on factory reset
- **WHEN** the user performs a factory reset
- **THEN** all cached daily data SHALL be cleared and the next resync SHALL trigger a fresh full-window fetch

#### Scenario: Throttle reset on credential change
- **WHEN** the user saves new EMA credentials or base URL
- **THEN** any pending daily backfill retry delay SHALL be reset so the next resync retries immediately

#### Scenario: Multiple credential-field edits in a row coalesce into one resulting resync
- **WHEN** the user saves two or more connection-affecting settings (credentials or base URL) in quick succession, before the resulting resync has finished
- **THEN** only one daily backfill attempt (if one is due) SHALL actually run to completion and persist its result — an earlier, now-superseded attempt SHALL NOT overwrite the outcome of the latest one, whether that earlier attempt failed or is still in flight when superseded
