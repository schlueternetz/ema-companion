## ADDED Requirements

### Requirement: User Guide entry page renders in the app's locale with English fallback
The User Guide SHALL open its entry page in the app's current locale when a localized version of the entry file is bundled. When no localized version is available for the current locale, the User Guide SHALL fall back to the English entry page.

#### Scenario: German entry page shown when German is set and available
- **WHEN** the app locale is German and a German entry guide asset is bundled
- **THEN** the User Guide screen SHALL render the German entry page

#### Scenario: English fallback when no localized entry page exists
- **WHEN** the app locale is German but no German entry guide asset is bundled
- **THEN** the User Guide screen SHALL render the English entry page

#### Scenario: English shown for the default locale
- **WHEN** the app locale is English
- **THEN** the User Guide screen SHALL render the English entry page

### Requirement: Localized guide content is generated from the English source
Localized guide files (markdown and diagram images) SHALL be generated from the English sources rather than authored independently. The English files SHALL be the single source of truth. Localized markdown files SHALL carry a marker indicating they are generated and SHALL NOT be hand-edited as the source of record.

#### Scenario: Localized guide is produced from English sources
- **WHEN** the guide is localized
- **THEN** each localized markdown file SHALL be produced by translating its English counterpart
- **AND** each localized diagram image SHALL be rendered from a translation of the English diagram source

#### Scenario: Localized diagram preserves diagram structure
- **WHEN** a guide diagram is localized
- **THEN** only the diagram's textual labels SHALL be translated
- **AND** the diagram SHALL render with the same structure as the English diagram
