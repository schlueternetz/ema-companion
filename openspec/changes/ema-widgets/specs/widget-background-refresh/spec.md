## ADDED Requirements

### Requirement: Widgets refresh in the background without requiring the app to be opened
The app SHALL periodically refresh the cached data backing the home-screen widgets so their content stays reasonably current even if the user does not open the app. This refresh SHALL reuse the existing `HourlyEnergyRepository` and `DailyEnergyRepository` throttled `refresh()` methods — it SHALL NOT introduce a separate network client, a separate cache, or bypass either repository's existing throttle.

#### Scenario: Widgets update without the app being opened
- **WHEN** the background refresh trigger fires and a repository's throttle has elapsed
- **THEN** the repository SHALL fetch fresh data and the on-screen widgets SHALL be updated to reflect it, without the user having opened the app

### Requirement: Hourly refresh runs periodically
The hourly background refresh trigger SHALL run at most once every 2 hours. There is no time-of-day restriction — the trigger runs around the clock (a night-time or sunrise/sunset-based gate is out of scope for this capability; see a future change).

#### Scenario: Trigger fires periodically
- **WHEN** the periodic trigger fires
- **THEN** the app SHALL attempt an hourly-energy refresh (subject to the existing 1-hour throttle)

### Requirement: Daily refresh runs once per day
The daily background refresh trigger SHALL run once per day at a fixed local time, since month-to-date and last-30-days totals do not need intraday freshness.

#### Scenario: Daily trigger runs once per day
- **WHEN** the fixed daily trigger time is reached
- **THEN** the app SHALL attempt a daily-energy refresh (subject to the existing 1-hour throttle)

### Requirement: Only successful fetches count toward the API budget
Background-triggered refreshes SHALL follow the same call-counting rules as all other EMA API usage (ADR-009): only a successful response counts toward the monthly budget and advances the throttle; failures are free and do not block the next trigger.

#### Scenario: Failed background fetch does not consume budget
- **WHEN** a background-triggered refresh fails (network, auth, or API error)
- **THEN** it SHALL NOT count toward the monthly call budget and SHALL NOT advance that repository's throttle

### Requirement: Opening the app also refreshes widget content
When the Home screen performs its own foreground refresh (on resume or pull-to-refresh), the app SHALL also update any placed widgets to reflect the resulting cached state, without waiting for the next background trigger.

#### Scenario: Foreground refresh updates widgets immediately
- **WHEN** the Home screen completes a successful hourly or daily refresh
- **THEN** any placed widgets depending on that data SHALL be updated to reflect the new cached state

### Requirement: Background-refreshed data is available to in-app tiles on reopen
A successful background refresh SHALL update the same persisted cache the Home screen's tiles read from, so the data fetched in the background is available to those tiles immediately when the app is reopened — not just to widgets. The app SHALL NOT perform a redundant fetch on reopen for data the background refresh already retrieved within the current throttle window.

#### Scenario: Home screen shows background-fetched data on reopen
- **WHEN** a background refresh successfully updates the hourly or daily cache while the app is closed, and the user then opens the app
- **THEN** the corresponding Home screen tile SHALL show that data immediately on render, without waiting for its own fetch to complete

#### Scenario: Reopening the app does not re-fetch data already fetched in the background
- **WHEN** the app is reopened within the throttle window of a successful background refresh
- **THEN** the Home screen's own refresh trigger SHALL NOT issue a new API call for that data

### Requirement: Settings changes trigger an immediate widget refresh
When the user saves a connection setting (App ID, App Secret, System ID, ECU ID, or Base URL), imports settings, or performs a factory reset, the app SHALL — in addition to resetting the throttle — immediately attempt a fresh hourly and daily energy fetch and update any placed widgets with the result, rather than leaving widgets to show stale data until the next scheduled trigger.

#### Scenario: Credential edit refreshes widgets immediately
- **WHEN** the user saves a changed connection setting
- **THEN** the app SHALL attempt an hourly and daily energy fetch immediately and update any placed widgets with the outcome (fresh data or an error message)

#### Scenario: Settings import refreshes widgets immediately
- **WHEN** the user successfully imports settings that change connection details
- **THEN** the app SHALL attempt an hourly and daily energy fetch immediately and update any placed widgets with the outcome

#### Scenario: Factory reset shows the not-configured placeholder immediately
- **WHEN** the user performs a factory reset (which also clears EMA credentials)
- **THEN** any placed widgets SHALL immediately show the "not configured" placeholder, without waiting for a network attempt to fail first
