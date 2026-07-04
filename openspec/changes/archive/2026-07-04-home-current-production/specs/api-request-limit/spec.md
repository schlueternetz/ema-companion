## MODIFIED Requirements

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
