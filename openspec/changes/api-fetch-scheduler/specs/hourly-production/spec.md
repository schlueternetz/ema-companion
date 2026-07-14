## MODIFIED Requirements

### Requirement: Hourly data fetched and cached with a 45-minute throttle
The app SHALL fetch hourly energy data for today at most once per 45 minutes. A successful fetch SHALL store the 24 hourly values in persistent storage. The stored data SHALL be used to populate the chart, table, and today-total immediately on fragment start without waiting for a new fetch. All fetches SHALL be issued through `ApiSyncScheduler` (see `api-fetch-scheduler`), never called directly by a Fragment or Worker.

#### Scenario: Hourly fetch also serves Daily's derived today-total
- **WHEN** `DailyEnergyRepository` has an enabled consumer but `HourlyEnergyRepository` has none of its own
- **THEN** the scheduler SHALL still trigger hourly fetches, since Daily's "today" value is derived from hourly data (see `production-history`'s "Today's total is derived from hourly data" requirement)

#### Scenario: Throttle prevents redundant fetch
- **WHEN** a successful hourly fetch occurred less than 2,700 seconds ago
- **THEN** the repository SHALL return the cached state without issuing an API call

#### Scenario: Fetch issued after throttle expires
- **WHEN** the last successful fetch was more than 2,700 seconds ago
- **THEN** the repository SHALL issue one `energy_level=hourly` API call for today's date

#### Scenario: Only successful fetch starts throttle
- **WHEN** a fetch fails (network or API error)
- **THEN** the throttle timestamp SHALL NOT be updated, so the next trigger retries immediately

#### Scenario: Fetch count and throttle reset on credential change
- **WHEN** the user saves new EMA credentials or base URL
- **THEN** the hourly repository throttle SHALL be reset so the next resync triggers a fresh fetch

#### Scenario: Multiple credential-field edits in a row coalesce into one resulting fetch
- **WHEN** the user saves two or more connection-affecting settings (credentials or base URL) in quick succession, before the resulting resync has finished
- **THEN** only one hourly fetch SHALL actually run to completion and persist its result — an earlier, now-superseded fetch attempt SHALL NOT overwrite the outcome of the latest one, whether that earlier attempt failed or is still in flight when superseded
