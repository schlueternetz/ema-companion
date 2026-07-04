## Requirements

### Requirement: Home screen shows today's hourly production chart
The Home screen SHALL display a line chart of today's energy production, with one data point per hour from 06:00 up to and including the current local hour.

The Y-axis maximum SHALL be the configured system capacity in kW when set; otherwise it SHALL scale to the data maximum.

The current hour's segment SHALL be rendered with a dashed stroke to indicate it is in-progress. Past hours SHALL use a solid stroke.

Hours with a null value from the API SHALL be omitted from the chart (no point plotted).

#### Scenario: Chart renders with data
- **WHEN** hourly data has been fetched successfully for today
- **THEN** the Home screen SHALL display a line chart with one point per non-null hour from 06:00 to the current hour, X-axis labelled in two-hour increments (06, 08, 10, …), Y-axis in kWh

#### Scenario: Chart shows in-progress hour as dashed
- **WHEN** the current local hour is between 06 and 23
- **THEN** the line segment entering the current hour's point SHALL be rendered dashed/dotted, distinct from past hours

#### Scenario: Chart absent before first fetch
- **WHEN** no hourly data has been fetched yet (fresh install or factory reset)
- **THEN** the chart section SHALL display a neutral placeholder (e.g. "No data yet") and no error line

#### Scenario: Fetch error shown inline
- **WHEN** the hourly fetch fails with a network or API error
- **THEN** the chart section SHALL display the last known data (if any) and an inline error status line below it; the chart is not blanked

### Requirement: Today section shows last-updated timestamp
The today section SHALL display a "Updated HH:mm" line below the chart using the timestamp of the last successful hourly fetch. The line SHALL be hidden when no successful fetch has occurred yet.

#### Scenario: Timestamp shown after successful fetch
- **WHEN** at least one successful hourly fetch has completed
- **THEN** the today section SHALL show "Updated HH:mm" formatted in the device locale's short time format

#### Scenario: Timestamp hidden before first fetch
- **WHEN** no successful hourly fetch has occurred
- **THEN** the timestamp line SHALL not be visible

### Requirement: Home screen shows today's hourly production table
The Home screen SHALL display a two-part table below the line chart: morning hours (00:00–11:00) and afternoon hours (12:00–23:00). Each cell shows the hour label and the kWh value (or "–" for null/missing values).

#### Scenario: Table renders all 24 hours
- **WHEN** hourly data is available
- **THEN** the morning table SHALL show hours 0–11 and the afternoon table SHALL show hours 12–23, each with its kWh value formatted to two decimal places

#### Scenario: Missing hour shown as dash
- **WHEN** an hour value is null in the API response
- **THEN** the corresponding table cell SHALL display "–" instead of a number

### Requirement: Home screen shows today's total production card
The Home screen SHALL display a summary card with the total kWh produced today, calculated as the sum of all non-null hourly values.

#### Scenario: Today total shown
- **WHEN** at least one hourly value is non-null
- **THEN** a card SHALL display the today-total formatted to two decimal places with a "kWh" unit label

#### Scenario: Today total zero when no data
- **WHEN** all hourly values are null or no data has been fetched
- **THEN** the card SHALL display "0.00 kWh" or the neutral placeholder

### Requirement: Home screen shows best-day cards for this month and history window
The Home screen SHALL display two summary cards derived from the daily energy data:
- **Best day this month**: the calendar date and kWh total of the highest-producing day in the current month
- **Best day in last N days**: the calendar date and kWh total of the highest-producing day within the configured history window (from `data-history-preference`)

#### Scenario: Best day this month shown
- **WHEN** daily data exists for at least one day in the current month
- **THEN** the card SHALL show the date (formatted as day-of-week + date, e.g. "Mon 02 Jun") and the kWh total of the best day

#### Scenario: Best day in history shown
- **WHEN** daily data exists for at least one day in the history window
- **THEN** the card SHALL show the date and kWh total of the highest-producing day in that window

#### Scenario: No data placeholder
- **WHEN** no daily data is available
- **THEN** both best-day cards SHALL display a neutral placeholder (e.g. "–")

### Requirement: Hourly data fetched and cached with 1-hour throttle
The app SHALL fetch hourly energy data for today at most once per hour. A successful fetch SHALL store the 24 hourly values in persistent storage. The stored data SHALL be used to populate the chart, table, and today-total immediately on fragment start without waiting for a new fetch.

#### Scenario: Throttle prevents redundant fetch
- **WHEN** a successful hourly fetch occurred less than 3,600 seconds ago
- **THEN** the repository SHALL return the cached state without issuing an API call

#### Scenario: Fetch issued after throttle expires
- **WHEN** the last successful fetch was more than 3,600 seconds ago
- **THEN** the repository SHALL issue one `energy_level=hourly` API call for today's date

#### Scenario: Only successful fetch starts throttle
- **WHEN** a fetch fails (network or API error)
- **THEN** the throttle timestamp SHALL NOT be updated, so the next trigger retries immediately

#### Scenario: Fetch count and throttle reset on credential change
- **WHEN** the user saves new EMA credentials or base URL
- **THEN** the hourly repository throttle SHALL be reset so the next Home visit triggers a fresh fetch
