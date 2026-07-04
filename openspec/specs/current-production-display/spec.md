## Requirements

### Requirement: Home shows current production in a tile
The Home screen SHALL present the current production in a tile (card) titled "Current Production", replacing the previous placeholder text. The tile SHALL show the latest production power with the unit `W` as its value, and — once a value has been fetched — the time that value was last updated. The Home screen is a vertical, scrollable container of tiles so further data tiles can be added.

#### Scenario: Production value is displayed
- **WHEN** the client returns a current production of 8000 W
- **THEN** the Current Production tile SHALL show the value "8000 W"

#### Scenario: Last-updated time is shown
- **WHEN** a value has been successfully fetched
- **THEN** the tile SHALL show when that value was last updated

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

### Requirement: At most one successful call per ten minutes
The current-production endpoint SHALL be polled at most once per 10 minutes once it is returning data. Only a **successful** read SHALL start the throttle (its timestamp is persisted so the throttle is honored across screen changes and app restarts). Any failure — network-unreachable, authentication, parameter, or server error — SHALL NOT start the throttle, so the next trigger may retry without waiting. Changing a connection setting (App ID, App Secret, System ID, ECU ID, or Base URL) SHALL clear the throttle so the next fetch runs immediately with the new configuration.

#### Scenario: Throttle blocks a too-soon call after success
- **WHEN** a fetch is requested less than 10 minutes after the previous **successful** read
- **THEN** the client SHALL NOT issue a new request

#### Scenario: Throttle allows a call after the window
- **WHEN** a fetch is requested 10 minutes or more after the previous successful read
- **THEN** a new request SHALL be issued

#### Scenario: A failure does not start the throttle
- **WHEN** a fetch fails for any reason
- **THEN** the next trigger SHALL be allowed to retry without waiting for the 10-minute window

#### Scenario: Changing connection settings allows an immediate retry
- **WHEN** the user changes a connection setting after a failed (or any) fetch
- **THEN** the throttle SHALL be cleared so returning to Home issues a fresh request with the new configuration

### Requirement: Per-tile fetch-error status
When a current-production fetch fails — whether because the API is unreachable (network error) or because the API returns an error — the Current Production tile SHALL show an error status **on the tile itself** (not a screen-level banner), while keeping the last known value visible. The status SHALL be visible without any interaction (no tap required) and SHALL distinguish at least: a network issue, an authentication failure (EMA account/authorization codes), and any other API error. The status SHALL be cleared once a subsequent fetch succeeds. Because each tile owns its own status, future data tiles indicate their own errors independently.

#### Scenario: Status appears on unreachable API
- **WHEN** a fetch fails with a network/unreachable error
- **THEN** the tile SHALL show a network-issue status without requiring a tap

#### Scenario: Status distinguishes an authentication failure
- **WHEN** a fetch fails with an EMA authentication/authorization error (codes 2000–2004 / 3000–3004)
- **THEN** the tile SHALL show an authentication status prompting the user to check their API credentials

#### Scenario: Status appears on other API error
- **WHEN** a fetch fails with another API error (e.g. invalid parameters or a server error)
- **THEN** the tile SHALL show a status indicating production data could not be updated, while still showing the last known value

#### Scenario: Status persists across screen changes
- **WHEN** the user navigates away from Home and back while the failure still stands
- **THEN** the tile SHALL still show the error status and the last known value (the displayed state is reconstructed from persisted state)

#### Scenario: Status clears on recovery
- **WHEN** a later fetch succeeds after a failed fetch
- **THEN** the tile SHALL no longer show the error status
