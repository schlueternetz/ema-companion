## ADDED Requirements

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

### Requirement: Daily data fetched with immutable-past caching and daily throttle for current day
The app SHALL fetch daily energy data using the following caching rules:

1. Past days' totals are immutable — once fetched and stored they SHALL NOT be re-fetched.
2. The current calendar day's total SHALL be re-fetched on each trigger (it changes as the day progresses), subject to a 1-hour throttle.
3. On the first fetch (no cache), the app SHALL fetch the full history window in a single `energy_level=daily` range call.
4. On subsequent fetches (partial cache), the app SHALL issue at most one call for the current day only.

#### Scenario: First fetch retrieves full history window
- **WHEN** no daily data has been cached yet
- **THEN** the repository SHALL issue one `energy_level=daily` API call covering today minus the history-window days through today

#### Scenario: Steady-state fetch retrieves only today
- **WHEN** daily data for all past days in the window is already cached
- **THEN** the repository SHALL issue one `energy_level=daily` API call for today's date only

#### Scenario: Today throttle prevents redundant same-day fetch
- **WHEN** the current day's data was fetched less than 3,600 seconds ago
- **THEN** the repository SHALL return the cached state without issuing an API call

#### Scenario: Past days never re-fetched
- **WHEN** a daily record for a past calendar day exists in the cache
- **THEN** the repository SHALL NOT issue any API call for that date on subsequent fetches

#### Scenario: Only successful fetch starts throttle
- **WHEN** a fetch fails (network or API error)
- **THEN** the throttle timestamp for the current day SHALL NOT be updated, so the next trigger retries

#### Scenario: Cache cleared on factory reset
- **WHEN** the user performs a factory reset
- **THEN** all cached daily data SHALL be cleared and the next Home visit SHALL trigger a fresh full-window fetch

#### Scenario: Throttle reset on credential change
- **WHEN** the user saves new EMA credentials or base URL
- **THEN** the daily repository throttle SHALL be reset so the next Home visit triggers a fresh fetch
