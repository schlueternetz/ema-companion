## 1. Dependencies

- [ ] 1.1 Add `androidx.navigation:navigation-fragment-ktx` and `navigation-ui-ktx` to `libs.versions.toml` and `app/build.gradle.kts`
- [ ] 1.2 Add `androidx.security:security-crypto` to `libs.versions.toml` and `app/build.gradle.kts`

## 2. Navigation Structure

- [ ] 2.1 Create `res/navigation/nav_graph.xml` with `homeFragment` as start destination and `settingsFragment` as second destination
- [ ] 2.2 Create `res/menu/bottom_nav_menu.xml` with Home and Settings menu items
- [ ] 2.3 Update `res/layout/activity_main.xml` to contain a `FragmentContainerView` (NavHost) and a `BottomNavigationView`
- [ ] 2.4 Wire `NavController` to `BottomNavigationView` in `MainActivity` using `NavigationUI.setupWithNavController()`

## 3. Home Screen

- [ ] 3.1 Create `HomeFragment` with a placeholder layout (`res/layout/fragment_home.xml`)

## 4. Settings Storage

- [ ] 4.1 Create `SettingsRepository` class that wraps `EncryptedSharedPreferences` with read/write for the language preference key
- [ ] 4.2 Handle `SecurityException` in `SettingsRepository` reads to fall back to defaults gracefully

## 5. Settings Screen

- [ ] 5.1 Create `res/layout/fragment_settings.xml` with a language selector row (label + current value or dropdown)
- [ ] 5.2 Create `SettingsFragment` that reads the current language from `SettingsRepository` and displays it
- [ ] 5.3 Implement language selection dialog/menu showing System, English, German options
- [ ] 5.4 On language selection, save to `SettingsRepository` and call `AppCompatDelegate.setApplicationLocales()` with the appropriate `LocaleListCompat`

## 6. String Resources

- [ ] 6.1 Add all new UI strings to `res/values/strings.xml` (English): nav labels (Home, Settings), language selector label, language option names (System, English, German)
- [ ] 6.2 Add German translations for all new strings to `res/values-de/strings.xml`
- [ ] 6.3 Add icons for Home and Settings nav items (`res/drawable/`)

## 7. Verification

- [ ] 7.1 Run `./gradlew lint` and confirm no `HardcodedText` or `MissingTranslation` errors
- [ ] 7.2 Run `./gradlew assembleDebug` and confirm the build succeeds
