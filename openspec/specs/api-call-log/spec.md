## Requirements

### Requirement: API calls are recorded
The app SHALL record each EMA API call that is issued, capturing at least: the timestamp, the endpoint, the call duration, and whether it succeeded. Records SHALL be persisted and SHALL survive app restarts. The number of retained records MAY be bounded (oldest dropped first).

#### Scenario: A call is logged
- **WHEN** the client issues an EMA API call (success or failure)
- **THEN** a log record SHALL be stored with its timestamp, endpoint, duration, and success flag

#### Scenario: Throttled call is not logged
- **WHEN** a fetch is skipped because of the throttle
- **THEN** no new log record SHALL be created

### Requirement: Logs section in Settings
The Settings screen SHALL include a "Logs" section listing the recorded API calls, most recent first, each showing its timestamp, endpoint, duration, and success/failure indicator.

#### Scenario: Logs are listed
- **WHEN** the user opens the Settings screen and at least one call has been recorded
- **THEN** the Logs section SHALL list those calls newest-first with timestamp, endpoint, duration, and success state

#### Scenario: Empty logs
- **WHEN** no API calls have been recorded yet
- **THEN** the Logs section SHALL indicate that there are no records

### Requirement: Log detail with masked sensitive values
Tapping a log entry SHALL show the full request and full response, both pretty-printed (formatted JSON where applicable). Any value that is masked on the Settings screen (such as the App Secret) SHALL NOT appear in plain text in the detail view; it SHALL be masked there as well.

#### Scenario: Detail is pretty-printed
- **WHEN** the user taps a log entry
- **THEN** the request and the full response SHALL be shown pretty-printed

#### Scenario: Masked field stays masked
- **WHEN** a log detail would otherwise reveal a value that is masked on the Settings screen (e.g. the App Secret)
- **THEN** that value SHALL be masked in the detail view rather than shown in plain text
