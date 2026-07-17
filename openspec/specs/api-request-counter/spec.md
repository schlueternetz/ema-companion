## Purpose

Tracks how many successful EMA API reads have been made in the current calendar month, since APsystems bills only on successful data access.

## Requirements

### Requirement: Count successful EMA API reads per calendar month
The app SHALL count EMA API requests and persist that count together with the calendar month (year + month) it applies to. Only a **successful** read (EMA `code` 0) SHALL be counted — APsystems bills on data access, and a failed request returned no data. Throttled/skipped calls and all failures (network-unreachable, authentication/authorization, parameter, server errors) SHALL NOT be counted.

#### Scenario: Successful read increments the count
- **WHEN** a request succeeds (returns data)
- **THEN** the persisted monthly request count SHALL increase by one

#### Scenario: Throttled call does not increment the count
- **WHEN** a fetch is skipped because of the 10-minute throttle
- **THEN** the persisted monthly request count SHALL NOT change

#### Scenario: A failed request does not increment the count
- **WHEN** a fetch fails for any reason — it never reached the EMA API (network), or EMA rejected it (authentication, parameter, or server error)
- **THEN** the persisted monthly request count SHALL NOT change

#### Scenario: Count persists across restarts
- **WHEN** the app is restarted within the same calendar month
- **THEN** the previously persisted count SHALL still be reflected

### Requirement: Count resets on month rollover
When a successful read occurs in a calendar month different from the stored month, the persisted count SHALL reset to start counting the new month (the new month's first counted read yields a count of 1).

#### Scenario: New month starts a fresh count
- **WHEN** the stored count belongs to a previous calendar month and a new successful read occurs
- **THEN** the count SHALL reset and reflect only the current month
