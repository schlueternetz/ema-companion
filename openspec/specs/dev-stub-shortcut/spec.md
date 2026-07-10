## Requirements

### Requirement: Debug-only "Use local stub" Settings action
In debug builds only, the API Settings section SHALL include a "Use local stub" action alongside the Base URL field. Activating it SHALL set the Base URL field to `http://10.0.2.2:{port}/user/api/v2/`, where `{port}` is the build-time configured stub port (a `STUB_PORT` build property, defaulting to `8080` when unset), and save it through the same validation and persistence path as manually entering a Base URL. In release builds the action SHALL NOT be present.

#### Scenario: Activating the action in a debug build points the app at the local stub
- **WHEN** the app is a debug build and the user activates "Use local stub" in API Settings
- **THEN** the Base URL field SHALL display `http://10.0.2.2:{port}/user/api/v2/` using the configured `STUB_PORT` (or `8080` if not configured)
- **AND** that value SHALL be persisted as the current Base URL

#### Scenario: Action is absent in release builds
- **WHEN** the app is a release build
- **THEN** the API Settings section SHALL NOT show a "Use local stub" action

#### Scenario: Persisted default and reset action are unaffected
- **WHEN** the app has never had "Use local stub" activated, or is freshly installed, or the user activates "Reset to default" on the Base URL field
- **THEN** the Base URL SHALL be (or SHALL be restored to) the production default `https://api.apsystemsema.com:9282/user/api/v2/`, in every build type
