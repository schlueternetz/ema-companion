## Requirements

### Requirement: String resources for all user-visible text
All user-visible text in the app SHALL be defined as named string resources using Android's standard locale-qualifier resource directories (e.g. `res/values/strings.xml` for the English default, `res/values-de/strings.xml` for German). No hardcoded string literals SHALL appear in layout XML files or Kotlin source files.

#### Scenario: Supported locale displayed in matching language
- **WHEN** the device system language is set to a supported locale (e.g. German)
- **THEN** the app SHALL display all UI text in that language

#### Scenario: Unsupported locale falls back to English
- **WHEN** the device system language is set to a locale with no matching resource directory
- **THEN** the app SHALL display all UI text in English

#### Scenario: No hardcoded text in layouts
- **WHEN** a lint check (`./gradlew lint`) is run
- **THEN** no `HardcodedText` warnings SHALL be reported for user-visible strings

### Requirement: Translation completeness
Every string defined in the default `res/values/strings.xml` SHALL have a corresponding entry in each supported locale's resource directory.

Currently supported locales: **English** (default, `res/values/`) and **German** (`res/values-de/`).

#### Scenario: Lint reports missing translations
- **WHEN** a string is added to `res/values/strings.xml` but omitted from `res/values-de/strings.xml`
- **THEN** the Android lint rule `MissingTranslation` SHALL flag it as a warning or error

#### Scenario: All strings translated at build time
- **WHEN** the app is built
- **THEN** the build SHALL produce no `MissingTranslation` lint errors for any supported locale
