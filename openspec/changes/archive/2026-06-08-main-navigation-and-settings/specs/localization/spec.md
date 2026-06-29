## MODIFIED Requirements

### Requirement: String resources for all user-visible text
All user-visible text in the app SHALL be defined as named string resources using Android's standard locale-qualifier resource directories (e.g. `res/values/strings.xml` for the English default, `res/values-de/strings.xml` for German). No hardcoded string literals SHALL appear in layout XML files or Kotlin source files.

#### Scenario: Supported locale displayed in matching language
- **WHEN** the active locale is set to a supported locale (either via device system language or in-app language selector)
- **THEN** the app SHALL display all UI text in that language

#### Scenario: Unsupported locale falls back to English
- **WHEN** the device system language is set to a locale with no matching resource directory and no in-app language override is set
- **THEN** the app SHALL display all UI text in English

#### Scenario: No hardcoded text in layouts
- **WHEN** a lint check (`./gradlew lint`) is run
- **THEN** no `HardcodedText` warnings SHALL be reported for user-visible strings

## ADDED Requirements

### Requirement: In-app language override takes precedence over system locale
When the user has selected an explicit language in the in-app language selector, the app SHALL display UI text in that language regardless of the device system locale.

#### Scenario: In-app language overrides system locale
- **WHEN** the device system language is German and the user has selected English in the in-app language selector
- **THEN** the app SHALL display all UI text in English

#### Scenario: System option removes the override
- **WHEN** the user selects System in the in-app language selector
- **THEN** the app SHALL revert to following the device system locale
