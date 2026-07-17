## Purpose

Lets the user configure the monthly EMA API call budget the app tracks against, so consumption can be monitored relative to a known ceiling.

## Requirements

### Requirement: API Request Limit is a configurable setting
The app SHALL provide an **API Request Limit** setting representing the maximum number of EMA API calls permitted per month. The value SHALL be a positive integer between 1 and 2,678,400 inclusive (1 request/second for 31 days), stored under the key `apiRequestLimit`. The setting SHALL have a default value of 1000, so it always holds a usable value and is NOT a required field — it is not part of the `isConfigured()` check and never blocks app configuration. The setting SHALL appear in the "API Settings" section of the Settings screen. Both in display mode and during editing, the unit "req/month" SHALL appear as a suffix outside the text input field (not inside it).

#### Scenario: API Request Limit shows its default when never set
- **WHEN** the user views the Settings screen and no API Request Limit has been saved
- **THEN** the API Request Limit field SHALL display the default value 1000 (it does not show a "Required" hint)

#### Scenario: API Request Limit accepts and saves a valid positive integer
- **WHEN** the user enters an integer between 1 and 2,678,400 and taps Save
- **THEN** the value SHALL be persisted and displayed with "req/month" shown outside the input field

#### Scenario: API Request Limit rejects out-of-range values
- **WHEN** the user enters 0, a negative number, or a value greater than 2,678,400 and taps Save
- **THEN** an error message SHALL be shown and the value SHALL NOT be persisted

#### Scenario: API Request Limit can be reset to its default
- **WHEN** the user activates the reset action on the API Request Limit field
- **THEN** the stored value SHALL be restored to 1000 without the user typing it
- **AND** the reset action SHALL be disabled while the field is in edit mode

### Requirement: Monthly API request consumption progress bar
The Settings screen SHALL display a horizontal progress bar immediately below the API Request Limit field, showing the proportion of the monthly API request limit that has been consumed. The consumed count used to compute progress SHALL be sourced from the real persisted per-calendar-month request count (see the `api-request-counter` capability); the limit is the stored API Request Limit value. The displayed consumed/limit figures and the bar fill SHALL stay in sync with that persisted count.

#### Scenario: Progress bar reflects consumed vs. limit ratio
- **WHEN** the user views the Settings screen and an API Request Limit is set
- **THEN** the progress bar SHALL be filled proportionally to consumed / limit, clamped between 0 and 1

#### Scenario: Progress bar uses the real monthly request count
- **WHEN** the Settings screen is shown
- **THEN** the consumed count used for the progress bar SHALL be the persisted number of EMA API requests made in the current calendar month (not a hardcoded value)

#### Scenario: Progress updates after a new request
- **WHEN** a new EMA API request is counted and the Settings screen is next shown
- **THEN** the progress bar and its consumed figure SHALL reflect the increased count
