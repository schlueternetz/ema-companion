## ADDED Requirements

### Requirement: Home-screen widget shows daily production history bar chart
The app SHALL offer a home-screen widget, "Production History", showing a bar chart of daily energy totals over the configured history window (`data-history-preference`; defaults to 45 days if unset). Each bar SHALL represent one calendar day's total kWh, colour-coded by calendar month, matching the equivalent Home screen chart's colour scheme. The widget SHALL show a title reflecting the window size, e.g. "Last 45 Days".

#### Scenario: Chart renders with multi-day data
- **WHEN** daily data has been cached for at least two days within the history window
- **THEN** the widget SHALL display a bar chart with one bar per cached day, each bar coloured by its month

#### Scenario: Single-day data renders as a single bar
- **WHEN** daily data is cached for exactly one day
- **THEN** the widget SHALL display a single bar

#### Scenario: No data placeholder
- **WHEN** no daily data has been cached yet
- **THEN** the widget SHALL show a neutral placeholder instead of an empty or broken chart

#### Scenario: Chart window follows the configured history length
- **WHEN** the user has set the history-data-days preference to a value other than the default
- **THEN** the widget's chart window and title (e.g. "Last N Days") SHALL reflect that configured value, not the default

### Requirement: Chart scales to the configured system capacity
The Y-axis maximum SHALL be the configured system capacity in kW when set; otherwise it SHALL scale to the data maximum, matching the equivalent Home screen chart's behavior.

#### Scenario: Chart scales to configured system capacity
- **WHEN** the user has configured a system capacity in Settings
- **THEN** the widget's Y-axis maximum SHALL equal that configured capacity, the same value used by the Home screen's equivalent chart

#### Scenario: Chart auto-scales when capacity is not configured
- **WHEN** no system capacity is configured in Settings
- **THEN** the widget's Y-axis maximum SHALL scale to the largest cached daily value in the history window

### Requirement: Widget shows a neutral placeholder when the app is not configured
When EMA credentials are not configured, the widget SHALL show a neutral "not configured" placeholder instead of a chart.

#### Scenario: Unconfigured app shows placeholder
- **WHEN** the app has no EMA credentials configured
- **THEN** the widget SHALL show a neutral "not configured" placeholder and no chart

### Requirement: A fetch error replaces the chart with an error message
When the latest daily-energy fetch (foreground or background) fails, the widget SHALL replace the bar chart with a clear error message instead of showing the last cached chart — the message SHALL distinguish at least a network issue, an authentication failure, and any other API error. The widget SHALL NOT go blank. The error message SHALL clear and normal chart display SHALL resume as soon as a subsequent fetch succeeds.

#### Scenario: Network error replaces the chart
- **WHEN** the latest fetch fails with a network/unreachable error
- **THEN** the widget SHALL show a network-issue error message in place of the chart

#### Scenario: Authentication error replaces the chart
- **WHEN** the latest fetch fails with an EMA authentication/authorization error
- **THEN** the widget SHALL show an authentication error message in place of the chart

#### Scenario: Other API error replaces the chart
- **WHEN** the latest fetch fails with another API error
- **THEN** the widget SHALL show a generic "couldn't update" error message in place of the chart

#### Scenario: Error clears on the next successful fetch
- **WHEN** a fetch succeeds after a previous failure
- **THEN** the widget SHALL show the chart again, with no error message

### Requirement: Tapping the widget opens Home, or Settings when unconfigured or in error
Tapping anywhere on the widget SHALL open the app. If the app is not configured, or the widget is currently showing a fetch-error message, the app SHALL open directly to the Settings screen (where credentials are edited and the API call log is available) instead of Home. Otherwise the app SHALL open directly to the Home screen.

#### Scenario: Tap opens Home when data is displayed normally
- **WHEN** the user taps the widget while it is showing chart data (no error, app configured)
- **THEN** the app SHALL open with the Home screen shown

#### Scenario: Tap opens Settings when not configured
- **WHEN** the user taps the widget while the app has no EMA credentials configured
- **THEN** the app SHALL open with the Settings screen shown

#### Scenario: Tap opens Settings when showing a fetch error
- **WHEN** the user taps the widget while it is showing a fetch-error message
- **THEN** the app SHALL open with the Settings screen shown
