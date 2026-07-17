## Purpose

Lets the user override the EMA API's base URL, so the app can be pointed at a different environment (e.g. a local stub) without a rebuild.

## Requirements

### Requirement: Base URL is configurable
The app SHALL provide an editable field for the API Base URL. The field SHALL accept any non-empty string that is a syntactically valid URL (scheme + host at minimum) and at most 2,048 characters long. The default value SHALL be `https://api.apsystemsema.com:9282/user/api/v2/`. The field SHALL be grouped under the "API Settings" section.

#### Scenario: Valid URL is accepted and saved
- **WHEN** the user enters a valid URL string and taps Save
- **THEN** the value SHALL be stored and displayed in the field

#### Scenario: Invalid URL is rejected
- **WHEN** the user enters an empty string, a string that is not a valid URL, or a string longer than 2,048 characters
- **THEN** the Save action SHALL be disabled and an inline error message SHALL be displayed

#### Scenario: Field shows the production URL when never configured
- **WHEN** the Base URL setting has never been set
- **THEN** the field SHALL display `https://api.apsystemsema.com:9282/user/api/v2/`

### Requirement: Base URL can be reset to the production default
The Base URL field SHALL include a "Reset to default" action that restores the value to `https://api.apsystemsema.com:9282/user/api/v2/` without requiring the user to type it.

#### Scenario: Reset to default restores the production URL
- **WHEN** the user activates the "Reset to default" action on the Base URL field
- **THEN** the stored Base URL SHALL be set to `https://api.apsystemsema.com:9282/user/api/v2/`
- **AND** the field SHALL display the restored default value
