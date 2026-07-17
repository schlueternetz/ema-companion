## ADDED Requirements

### Requirement: Home-screen widget shows today/month/last-30-days totals
The app SHALL offer a home-screen widget, "Production Summary", showing three energy totals as bold numeric values: today's total, the current calendar month's total, and the trailing 30-day total. Today's total SHALL be computed from cached hourly data (sum of hours); the month and last-30-days totals SHALL be computed from cached daily data. Each value SHALL be formatted to two decimal places with a "kWh" unit label.

#### Scenario: All three totals displayed
- **WHEN** hourly data exists for today and daily data exists for the current month and the last 30 days
- **THEN** the widget SHALL show all three totals, each formatted as e.g. "12.34 kWh"

#### Scenario: Missing data shows a neutral value per figure
- **WHEN** no cached data exists for a given period (today, this month, or last 30 days)
- **THEN** that figure SHALL show "0.00 kWh" or a neutral placeholder, independent of the other two figures

### Requirement: Widget shows a neutral placeholder when the app is not configured
When EMA credentials are not configured, the widget SHALL show a neutral "not configured" placeholder instead of any totals.

#### Scenario: Unconfigured app shows placeholder
- **WHEN** the app has no EMA credentials configured
- **THEN** the widget SHALL show a neutral "not configured" placeholder and no totals

### Requirement: A fetch error replaces the affected figure(s) with an error message
Because the "Today" figure is sourced from the hourly cache and the "This Month"/"Last 30 Days" figures are sourced from the daily cache, an error replaces only the figure(s) backed by the failing source, independent of the other. The message SHALL distinguish at least a network issue, an authentication failure, and any other API error. The widget SHALL NOT go blank. Each affected figure SHALL clear its error and resume showing its total as soon as its source's next fetch succeeds.

#### Scenario: Hourly fetch error replaces only "Today"
- **WHEN** the latest hourly-energy fetch fails but the daily cache is unaffected
- **THEN** the "Today" figure SHALL show an error message while "This Month" and "Last 30 Days" continue showing their cached totals

#### Scenario: Daily fetch error replaces "This Month" and "Last 30 Days"
- **WHEN** the latest daily-energy fetch fails but the hourly cache is unaffected
- **THEN** "This Month" and "Last 30 Days" SHALL each show an error message while "Today" continues showing its cached total

#### Scenario: Error clears on the next successful fetch
- **WHEN** a fetch succeeds for a source whose figure(s) were showing an error
- **THEN** those figure(s) SHALL show their total again, with no error message

### Requirement: Tapping the widget opens Home, or Settings when unconfigured or any figure is in error
Tapping anywhere on the widget SHALL open the app. If the app is not configured, or any one of the three figures is currently showing a fetch-error message, the app SHALL open directly to the Settings screen (where credentials are edited and the API call log is available) instead of Home. Otherwise the app SHALL open directly to the Home screen.

#### Scenario: Tap opens Home when all totals are displayed normally
- **WHEN** the user taps the widget while all three figures show cached totals with no error (app configured)
- **THEN** the app SHALL open with the Home screen shown

#### Scenario: Tap opens Settings when not configured
- **WHEN** the user taps the widget while the app has no EMA credentials configured
- **THEN** the app SHALL open with the Settings screen shown

#### Scenario: Tap opens Settings when any figure is showing a fetch error
- **WHEN** the user taps the widget while at least one of the three figures is showing a fetch-error message
- **THEN** the app SHALL open with the Settings screen shown
