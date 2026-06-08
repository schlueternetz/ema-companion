## 1. Dependencies

- [x] 1.1 Add `androidx.navigation:navigation-fragment-ktx` and `navigation-ui-ktx` to `libs.versions.toml` and `app/build.gradle.kts`
- [x] 1.2 Add `androidx.security:security-crypto` to `libs.versions.toml` and `app/build.gradle.kts`

## 2. Navigation Structure

- [x] 2.1 Create `res/navigation/nav_graph.xml` with `homeFragment` as start destination and `settingsFragment` as second destination
- [x] 2.2 Create `res/menu/bottom_nav_menu.xml` with Home and Settings menu items
- [x] 2.3 Update `res/layout/activity_main.xml` to contain a `FragmentContainerView` (NavHost) and a `BottomNavigationView`
- [x] 2.4 Wire `NavController` to `BottomNavigationView` in `MainActivity` using `NavigationUI.setupWithNavController()`

## 3. Home Screen

- [x] 3.1 Create `HomeFragment` with a placeholder layout (`res/layout/fragment_home.xml`)

## 4. Settings Storage

- [x] 4.1 Create `SettingsRepository` class that wraps `EncryptedSharedPreferences` with read/write for the language preference key
- [x] 4.2 Handle `SecurityException` in `SettingsRepository` reads to fall back to defaults gracefully

## 5. Settings Screen

- [x] 5.1 Create `res/layout/fragment_settings.xml` with a language selector row (label + current value or dropdown)
- [x] 5.2 Create `SettingsFragment` that reads the current language from `SettingsRepository` and displays it
- [x] 5.3 Implement language selection dialog/menu showing System, English, German options
- [x] 5.4 On language selection, save to `SettingsRepository` and call `AppCompatDelegate.setApplicationLocales()` with the appropriate `LocaleListCompat`

## 6. String Resources

- [x] 6.1 Add all new UI strings to `res/values/strings.xml` (English): nav labels (Home, Settings), language selector label, language option names (System, English, German)
- [x] 6.2 Add German translations for all new strings to `res/values-de/strings.xml`
- [x] 6.3 Add icons for Home and Settings nav items (`res/drawable/`)

## 7. Verification

- [x] 7.1 Run `./gradlew lint` and confirm no `HardcodedText` or `MissingTranslation` errors
- [x] 7.2 Run `./gradlew assembleDebug` and confirm the build succeeds
