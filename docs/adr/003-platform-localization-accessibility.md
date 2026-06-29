# ADR-003: Platform Support, Localization, and Accessibility

**Status:** Accepted  
**Date:** 2026-06-07

## Context

The app targets a specific minimum Android version and a known reference device. Localization and accessibility are first-class requirements, not afterthoughts. Decisions here constrain UI implementation and testing across all features.

## Decisions

### Android version support

| Property | Value |
|----------|-------|
| `minSdk` | 31 (Android 12) |
| `targetSdk` | 36 |
| Reference device | Lenovo Tab P11 Plus (11-inch tablet, 2000×1200, 213 DPI) |

Android 12 is the floor. No features or APIs above API 31 may be used without a runtime version check or an AndroidX backport.

The Lenovo Tab P11 Plus is the primary test target. All layouts must be verified on its form factor (large tablet, portrait and landscape). The AVD `Lenovo_Tab_11_Plus` (android-33) is used for local development.

### Localization

Supported locales: **English** (default, `res/values/`) and **German** (`res/values-de/`).

- All user-visible text must be defined as string resources — no hardcoded strings in layouts or Kotlin source
- Translation completeness is enforced by the `MissingTranslation` lint rule at build time
- The app supports an in-app language selector (System / English / German) that overrides the device locale using `AppCompatDelegate.setApplicationLocales()`

Adding a new locale requires entries in both `strings.xml` and a matching `res/values-<locale>/strings.xml`, plus a new option in the language selector.

### Accessibility

Target: **WCAG 2.1 AA**.

Material Design 3 satisfies most baseline requirements when followed correctly. Additional requirements:

- **Touch targets**: minimum 48×48dp for all interactive elements
- **Content descriptions**: all non-text interactive elements (icons, image buttons) must have a `contentDescription`
- **Color contrast**: 4.5:1 minimum for normal text, 3:1 for large text and UI components
- **Focus order**: logical traversal order for TalkBack; do not rely on visual position alone
- **No color-only information**: never use color as the sole means of conveying state

#### Testing accessibility

Three layers, in order of speed:

1. **Android Lint** (`./gradlew lint`) — catches `ContentDescription` violations and basic issues automatically; already part of the CI gate
2. **ATF (Accessibility Test Framework) + Robolectric** — add `AccessibilityValidator` checks to Robolectric tests for any Fragment or View under test; runs on the JVM, no emulator needed
3. **Accessibility Scanner** (manual, Google app) — run on the emulator against the `Lenovo_Tab_11_Plus` AVD before merging changes that touch UI

ATF Robolectric checks are required for all UI component tests (see [ADR-002](002-testing-strategy.md)). This is enforced via the AI-TDD skill.

## Consequences

- UI tasks have a larger definition of done: implementation + tests + ATF accessibility check + lint clean
- New locales require coordinated changes across string resources and the language selector UI
- Layouts must be tested on a tablet form factor; phone-only testing is insufficient
- Android 12 sets the capability floor; backports (AppCompat, etc.) are preferred over version checks where available
