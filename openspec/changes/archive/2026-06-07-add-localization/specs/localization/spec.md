## ADDED Requirements

### Requirement: String resources for all user-visible text
All user-visible text in the app SHALL be defined as named string resources in `res/values/strings.xml` (English) and `res/values-de/strings.xml` (German). No hardcoded string literals SHALL appear in layout XML files or Kotlin source files.

#### Scenario: English displayed on unsupported locale
- **WHEN** the device system language is set to a locale other than German
- **THEN** the app SHALL display all UI text in English

#### Scenario: German displayed on German locale
- **WHEN** the device system language is set to German (`de`)
- **THEN** the app SHALL display all UI text in German

#### Scenario: No hardcoded text in layouts
- **WHEN** a lint check (`./gradlew lint`) is run
- **THEN** no `HardcodedText` warnings SHALL be reported for user-visible strings

### Requirement: Translation completeness
Every string defined in the default `res/values/strings.xml` SHALL have a corresponding entry in `res/values-de/strings.xml`.

#### Scenario: Lint reports missing translations
- **WHEN** a string is added to `res/values/strings.xml` but omitted from `res/values-de/strings.xml`
- **THEN** the Android lint rule `MissingTranslation` SHALL flag it as a warning or error

#### Scenario: All strings translated at build time
- **WHEN** the app is built
- **THEN** the build SHALL produce no `MissingTranslation` lint errors for any supported locale
