## Why

The app currently has no localization support, meaning all UI strings are hardcoded in English. Adding Android's standard string resource system enables German-speaking users to use the app in their native language, with the device's system locale determining which language is shown.

## What Changes

- Introduce Android string resources (`res/values/strings.xml` for English, `res/values-de/strings.xml` for German) to replace all hardcoded UI strings
- System locale is used automatically; English serves as the fallback for any unsupported locale

## Capabilities

### New Capabilities

- `localization`: Android string resource support for English (default) and German, covering all user-visible strings in the app

### Modified Capabilities

<!-- No existing spec-level behavior changes -->

## Impact

- All existing layout XML files and Kotlin source files that reference hardcoded strings will be updated to use `@string/` resource references
- New resource files: `res/values/strings.xml`, `res/values-de/strings.xml`
- No new dependencies; uses Android's built-in localization mechanism
