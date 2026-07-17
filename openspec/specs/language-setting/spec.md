## Purpose

Lets the user override the app's display language independently of the device's system locale.

## Requirements

### Requirement: In-app language selector
The Settings screen SHALL include a language selector with three options: **System** (follow device locale), **English**, and **German**. The selected language SHALL be applied immediately without requiring an app restart.

#### Scenario: Default language is System
- **WHEN** the app is launched for the first time with no language preference stored
- **THEN** the language selector SHALL show **System** as the selected option and the app SHALL follow the device locale

#### Scenario: User selects English
- **WHEN** the user selects **English** in the language selector
- **THEN** all app UI text SHALL be displayed in English regardless of the device locale

#### Scenario: User selects German
- **WHEN** the user selects **German** in the language selector
- **THEN** all app UI text SHALL be displayed in German regardless of the device locale

#### Scenario: User selects System
- **WHEN** the user selects **System** in the language selector
- **THEN** the app locale override SHALL be removed and the app SHALL follow the device locale

#### Scenario: Language selection persisted across restarts
- **WHEN** the user selects a language and the app is closed and reopened
- **THEN** the previously selected language SHALL still be active
