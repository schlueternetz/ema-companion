## Purpose

Defines what the Home screen shows for production history — chart, totals, and summary cards — sourced from cached daily EMA data.

## Requirements

### Requirement: Home screen shows history bar chart
The Home screen SHALL display a bar chart of daily energy production over the history window (configured via `data-history-preference`; defaults to 45 days if not set).

Each bar represents one calendar day's total kWh. Bars SHALL be colour-coded by calendar month, with a legend below the chart mapping month names to their colours.

The Y-axis maximum SHALL be the configured system capacity in kW when set; otherwise it SHALL scale to the data maximum.

The X-axis SHALL label every other day number to avoid crowding.

#### Scenario: Chart renders with multi-day data
- **WHEN** daily data has been fetched for at least two days
- **THEN** the Home screen SHALL display a bar chart with one bar per day in the history window, each bar coloured by its month, with a month-colour legend

#### Scenario: Single-day data renders as single bar
- **WHEN** daily data is available for exactly one day
- **THEN** the chart SHALL display a single bar

#### Scenario: No data placeholder
- **WHEN** no daily data has been fetched yet
- **THEN** the chart section SHALL show a neutral placeholder (e.g. "No data yet") and no error line

#### Scenario: Fetch error shown inline
- **WHEN** the daily data fetch fails
- **THEN** the chart section SHALL display the last cached data (if any) and an inline error status line; the chart is not blanked

### Requirement: History section shows last-updated timestamp
The history section SHALL display a "Updated HH:mm" line below the bar chart using the timestamp of the last successful daily fetch. The line SHALL be hidden when no successful fetch has occurred yet.

#### Scenario: Timestamp shown after successful fetch
- **WHEN** at least one successful daily fetch has completed
- **THEN** the history section SHALL show "Updated HH:mm" formatted in the device locale's short time format

#### Scenario: Timestamp hidden before first fetch
- **WHEN** no successful daily fetch has occurred
- **THEN** the timestamp line SHALL not be visible

### Requirement: Home screen shows period-total summary cards
The Home screen SHALL display two summary cards in the history section:
- **This month**: total kWh produced in the current calendar month, summed from cached daily data
- **Last 30 days**: total kWh produced in the 30-day window ending today, summed from cached daily data

#### Scenario: This month total displayed
- **WHEN** at least one daily record exists for the current month
- **THEN** the card SHALL show the summed kWh formatted to two decimal places with a "kWh" unit label

#### Scenario: Last 30 days total displayed
- **WHEN** at least one daily record exists within the last 30 days
- **THEN** the card SHALL show the summed kWh formatted to two decimal places

#### Scenario: Zero or missing data placeholder
- **WHEN** no daily records are available for the relevant period
- **THEN** the card SHALL show "0.00 kWh" or a neutral placeholder

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
