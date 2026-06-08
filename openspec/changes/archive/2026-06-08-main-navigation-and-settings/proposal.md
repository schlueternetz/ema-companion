## Why

The app currently has no navigation structure or user-configurable settings. A bottom navigation bar and a settings screen are needed to establish the app's shell before feature screens are added.

## What Changes

- Add a bottom navigation bar with two top-level destinations: **Home** and **Settings**
- Home screen loads on app start (initial destination)
- Settings screen with encrypted persistent storage (`EncryptedSharedPreferences`)
- First setting: in-app language selector with options System (follow device), English, German

## Capabilities

### New Capabilities

- `main-navigation`: Bottom navigation bar with Home and Settings destinations; Home is the default start destination
- `settings`: Settings screen with encrypted persistence; hosts user-configurable app preferences
- `language-setting`: In-app language selector (System / English / German) that overrides the device locale within the app

### Modified Capabilities

- `localization`: Add requirement for in-app language override — the app must respect a user-selected language in addition to the existing system-locale fallback behavior

## Impact

- New dependency: `androidx.security:security-crypto` (EncryptedSharedPreferences)
- New dependency: `androidx.appcompat:appcompat` (AppCompatDelegate for per-app locale)
- Navigation component wiring (`NavHostFragment` or Compose `NavHost`)
- Existing Activity/Fragment structure will be extended with nav graph and bottom nav view
