## Purpose

Gives each Solar Array Settings field an in-app hint describing where to find its value in the official EMA app, so users don't have to guess or search externally.

## Requirements

### Requirement: Solar Array Settings fields display a contextual hint
Each Solar Array Settings field (EMA App ID, EMA App Secret, EMA System ID, EMA ECU ID, System Capacity) SHALL display an info icon button in read-only mode. When the user taps the info icon the app SHALL show an `AlertDialog` titled with the field label and containing a description of where to find that value in the EMA app. The dialog SHALL be dismissible via its OK button or the system Back gesture.

#### Scenario: Info icon is visible in read-only mode
- **WHEN** the user views the Settings screen and a Solar Array Settings field is in read-only mode
- **THEN** an info icon button SHALL be visible next to the field

#### Scenario: Info icon opens hint dialog
- **WHEN** the user taps the info icon on a Solar Array Settings field
- **THEN** an AlertDialog SHALL appear with the field label as its title and the corresponding hint text as its body

#### Scenario: Hint dialog is dismissible
- **WHEN** the hint dialog is shown
- **THEN** tapping OK or pressing Back SHALL dismiss the dialog

#### Scenario: Info icon is hidden during edit mode
- **WHEN** the user is editing a Solar Array Settings field
- **THEN** the info icon button SHALL NOT be visible

### Requirement: Hint text describes where to find each value in the EMA app
The hint text for each Solar Array Settings field SHALL describe the navigation path in the EMA app where the user can find that value, as follows:

- **EMA App ID**: States that the APP ID is generated in Settings → OpenAPI Service → Developer Authorization in the EMA app.
- **EMA App Secret**: States that the APP Secret is generated in Settings → OpenAPI Service → Developer Authorization in the EMA app.
- **EMA System ID**: States that the sid value is found in Settings → Account Details in the EMA app.
- **EMA ECU ID**: States that the ECU ID value is found in Settings → ECU in the EMA app.
- **System Capacity**: States that the Capacity value is shown on the Home screen in the EMA app.

#### Scenario: EMA App ID hint text is correct
- **WHEN** the user taps the info icon on the EMA App ID field
- **THEN** the dialog body SHALL mention Settings, OpenAPI Service, and Developer Authorization

#### Scenario: EMA System ID hint text is correct
- **WHEN** the user taps the info icon on the EMA System ID field
- **THEN** the dialog body SHALL mention sid and Settings, Account Details

#### Scenario: System Capacity hint text is correct
- **WHEN** the user taps the info icon on the System Capacity field
- **THEN** the dialog body SHALL mention Capacity and Home screen

### Requirement: Hint text is localised
Hint text for all Solar Array Settings fields SHALL be provided in English (default) and German.

#### Scenario: Hint text is shown in German when device language is German
- **WHEN** the device language is set to German and the user taps an info icon
- **THEN** the hint dialog body SHALL display the German-language hint text for that field
