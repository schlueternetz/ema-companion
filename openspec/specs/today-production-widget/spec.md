## Requirements

### Requirement: Home-screen widget shows today's hourly production chart
The app SHALL offer a home-screen widget, "Today's Production", showing today's hourly energy production as a line/area chart from 06:00 to the current hour. Completed hours SHALL render as a solid line; the current in-progress hour SHALL render as a dashed segment whose value is the partial hour's energy linearly projected to a full hour. The Y-axis maximum SHALL be the configured system capacity in kW when set; otherwise it SHALL scale to the data maximum, matching the equivalent Home screen chart's behavior.

#### Scenario: Chart renders with today's data
- **WHEN** hourly data has been cached for at least one hour today
- **THEN** the widget SHALL display a line chart from 06:00 through the current hour, with completed hours solid and the current hour dashed

#### Scenario: No data placeholder
- **WHEN** no hourly data has been cached yet for today
- **THEN** the widget SHALL show a neutral placeholder instead of an empty or broken chart

#### Scenario: Chart scales to configured system capacity
- **WHEN** the user has configured a system capacity in Settings
- **THEN** the widget's Y-axis maximum SHALL equal that configured capacity, the same value used by the Home screen's equivalent chart

#### Scenario: Chart auto-scales when capacity is not configured
- **WHEN** no system capacity is configured in Settings
- **THEN** the widget's Y-axis maximum SHALL scale to the largest cached hourly value for today

### Requirement: Widget shows today's running total
The widget SHALL display today's total energy produced so far, computed as the sum of cached hourly values, formatted to two decimal places with a "kWh" unit label.

#### Scenario: Total displayed with data
- **WHEN** at least one hourly value is cached for today
- **THEN** the widget SHALL show the summed total formatted as e.g. "12.34 kWh"

#### Scenario: Total placeholder with no data
- **WHEN** no hourly data is cached for today
- **THEN** the widget SHALL show a neutral placeholder in place of the total

### Requirement: A fetch error replaces the chart with an error message
When the latest hourly-energy fetch (foreground or background) fails, the widget SHALL replace the chart and today's total with a clear error message instead of showing the last cached chart — the message SHALL distinguish at least a network issue, an authentication failure, and any other API error. The widget SHALL NOT go blank. The error message SHALL clear and normal chart display SHALL resume as soon as a subsequent fetch succeeds.

#### Scenario: Network error replaces the chart
- **WHEN** the latest fetch fails with a network/unreachable error
- **THEN** the widget SHALL show a network-issue error message in place of the chart and total

#### Scenario: Authentication error replaces the chart
- **WHEN** the latest fetch fails with an EMA authentication/authorization error
- **THEN** the widget SHALL show an authentication error message in place of the chart and total

#### Scenario: Other API error replaces the chart
- **WHEN** the latest fetch fails with another API error
- **THEN** the widget SHALL show a generic "couldn't update" error message in place of the chart and total

#### Scenario: Error clears on the next successful fetch
- **WHEN** a fetch succeeds after a previous failure
- **THEN** the widget SHALL show the chart and total again, with no error message

### Requirement: Widget shows a neutral placeholder when the app is not configured
When EMA credentials are not configured, the widget SHALL show a neutral "not configured" placeholder instead of a chart, with no error indication (matches `ConfigurationError` being silent elsewhere in the app).

#### Scenario: Unconfigured app shows placeholder
- **WHEN** the app has no EMA credentials configured
- **THEN** the widget SHALL show a neutral "not configured" placeholder and no chart

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
