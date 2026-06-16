## ADDED Requirements

### Requirement: Home shows current production
The Home screen SHALL display the current production as the text "Current Production: {value} {unit}", replacing the previous placeholder text. The value SHALL be the latest production power from the EMA API client and the unit SHALL be `W`.

#### Scenario: Production value is displayed
- **WHEN** the client returns a current production of 8000 W
- **THEN** the Home screen SHALL display "Current Production: 8000 W"

#### Scenario: Placeholder is removed
- **WHEN** the Home screen is shown after this change
- **THEN** it SHALL NOT display the previous "Hello world" placeholder text

### Requirement: Fetch on app open and on Home highlight
A fresh current-production fetch SHALL be triggered when the app opens and when the Home screen becomes highlighted (selected/resumed), subject to the throttle below. While a cached value exists and the throttle window has not elapsed, the cached value SHALL be shown without a new API call.

#### Scenario: Fetch when Home becomes highlighted
- **WHEN** the user navigates to (highlights) the Home screen and the throttle window has elapsed
- **THEN** a new current-production request SHALL be issued and the displayed value updated

#### Scenario: Cached value shown within throttle window
- **WHEN** the Home screen is highlighted again less than 10 minutes after the last successful fetch
- **THEN** no new request SHALL be issued and the last value SHALL remain displayed

### Requirement: At most one call per ten minutes
The current-production endpoint SHALL be called at most once per 10 minutes. The timestamp of the last attempt SHALL be persisted so the throttle is honored across screen changes and app restarts.

#### Scenario: Throttle blocks a too-soon second call
- **WHEN** a fetch is requested less than 10 minutes after the previous one
- **THEN** the client SHALL NOT issue a new request

#### Scenario: Throttle allows a call after the window
- **WHEN** a fetch is requested 10 minutes or more after the previous one
- **THEN** a new request SHALL be issued

### Requirement: Network-error banner
When a current-production fetch fails because the API is unreachable, the Home screen SHALL show a banner informing the user of network issues. The banner SHALL be dismissed (or hidden) once a subsequent fetch succeeds.

#### Scenario: Banner appears on unreachable API
- **WHEN** a fetch fails with a network/unreachable error
- **THEN** Home SHALL display a network-issue banner

#### Scenario: Banner clears on recovery
- **WHEN** a later fetch succeeds after a network error
- **THEN** the network-issue banner SHALL no longer be shown
