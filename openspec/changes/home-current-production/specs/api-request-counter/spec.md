## ADDED Requirements

### Requirement: Count EMA API requests per calendar month
The app SHALL count the number of EMA API requests made and persist that count together with the calendar month (year + month) it applies to. The count SHALL be incremented once per request actually issued to the API (throttled/skipped calls SHALL NOT be counted).

#### Scenario: Issued request increments the count
- **WHEN** the client issues an EMA API request
- **THEN** the persisted monthly request count SHALL increase by one

#### Scenario: Throttled call does not increment the count
- **WHEN** a fetch is skipped because of the 10-minute throttle
- **THEN** the persisted monthly request count SHALL NOT change

#### Scenario: Count persists across restarts
- **WHEN** the app is restarted within the same calendar month
- **THEN** the previously persisted count SHALL still be reflected

### Requirement: Count resets on month rollover
When a request is made in a calendar month different from the stored month, the persisted count SHALL reset to start counting the new month (the new month's first counted request yields a count of 1).

#### Scenario: New month starts a fresh count
- **WHEN** the stored count belongs to a previous calendar month and a new request is issued
- **THEN** the count SHALL reset and reflect only requests in the current month
