## Context

The app currently has a single `MainActivity` with a placeholder XML layout and no navigation structure. It already pulls in `appcompat` 1.6.1, Material 1.14, and `constraintlayout`. There are no Compose dependencies. The existing code is View-based (XML layouts + Fragments implied by AppCompatActivity).

minSdk is 31; targetSdk is 36.

## Goals / Non-Goals

**Goals:**
- Establish a bottom nav shell (Home, Settings) that future screens slot into
- Persist settings securely via `EncryptedSharedPreferences`
- Language selector that overrides the system locale within the app

**Non-Goals:**
- Home screen content (placeholder only for now)
- Any settings beyond the language selector
- Migrating to Jetpack Compose

## Decisions

### Navigation: Jetpack Navigation Component with Fragments

Use `androidx.navigation:navigation-fragment-ktx` + `navigation-ui-ktx` wired to a `BottomNavigationView`. This is the standard View-based approach and integrates cleanly with `NavController.setupWithNavController()`.

Alternatives considered:
- **Manual fragment transactions** — more boilerplate, back-stack management is error-prone
- **Jetpack Compose NavHost** — would require migrating the entire UI stack; out of scope

### Settings persistence: EncryptedSharedPreferences

Use `androidx.security:security-crypto` (`EncryptedSharedPreferences`) for all settings, including the language preference. Single encrypted store; no need to split sensitive vs. non-sensitive data for the volume of settings expected.

Alternatives considered:
- **Preferences DataStore** — modern API, but no built-in encryption; would need a custom `EncryptedDataStore` wrapper
- **Plain SharedPreferences** — rejected; credentials may be stored here in future settings

### Language switching: AppCompatDelegate.setApplicationLocales()

`AppCompatDelegate.setApplicationLocales(LocaleListCompat)` (available in `appcompat` ≥ 1.6.0, already on 1.6.1) applies a per-app locale override compatible down to API 21. For "System" the app calls `setApplicationLocales(LocaleListCompat.getEmptyLocaleList())` to remove the override.

This approach handles activity recreation automatically and persists across process restarts via the OS (Android 13+) or via `AppLocalesStorageHelper` shim on older APIs — no manual persistence needed for the locale itself.

Alternatives considered:
- **`Configuration.setLocale()` manually** — deprecated, requires manual activity recreation and persistence
- **`LocaleManager` (API 33+)** — not backported; would exclude minSdk 31

### Settings UI: Custom Fragment layout (no Preferences library)

A simple Fragment with a static XML layout (one item per setting) is sufficient for 1–3 settings. The Preferences library (`androidx.preference`) adds significant complexity and its default UI does not match Material 3 styling without heavy customization.

## Risks / Trade-offs

- **EncryptedSharedPreferences key rotation** — if the Keystore key is lost (factory reset, some OEM bugs), settings are unreadable. The app should handle `SecurityException` on read and fall back to defaults gracefully.
- **Activity recreation on language change** — `setApplicationLocales()` triggers a configuration change and recreates the activity. The back stack is cleared back to the start destination; this is acceptable UX for a language change.
- **Navigation back behavior** — `BottomNavigationView` with `NavigationUI` uses `saveState`/`restoreState` for each tab's back stack. Behavior should be verified with back-press handling.

## Migration Plan

1. Add new dependencies to version catalog and `app/build.gradle.kts`
2. Create nav graph XML with `homeFragment` as start destination
3. Update `activity_main.xml` to host `FragmentContainerView` + `BottomNavigationView`
4. Implement `HomeFragment` (empty placeholder)
5. Implement `SettingsFragment` with language selector UI
6. Implement `SettingsRepository` wrapping `EncryptedSharedPreferences`
7. Wire language change to `AppCompatDelegate.setApplicationLocales()`
8. Add string resources for all new UI text (EN + DE per localization spec)
