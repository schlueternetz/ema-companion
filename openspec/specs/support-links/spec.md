## Requirements

### Requirement: Support screen with outbound links
The app SHALL provide a Support screen reachable from the bottom navigation bar. The screen SHALL display two actions: "Buy Me a Coffee" and "Visit Website". Each action SHALL open its target URL in the device's default browser via an external intent; the app SHALL NOT render either page in an in-app WebView.

#### Scenario: Tapping Buy Me a Coffee opens the browser
- **WHEN** the user taps the "Buy Me a Coffee" action on the Support screen
- **THEN** the device's default browser SHALL open `https://buymeacoffee.com/schlueternetz`

#### Scenario: Tapping Visit Website opens the browser
- **WHEN** the user taps the "Visit Website" action on the Support screen
- **THEN** the device's default browser SHALL open `https://www.schlueternetz.com`

#### Scenario: Support screen is reachable at all times
- **WHEN** the user taps the Support item in the bottom navigation bar from any other screen
- **THEN** the Support screen SHALL be displayed and the Support item SHALL be selected in the bottom navigation bar
