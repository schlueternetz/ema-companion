## 1. Build Infrastructure

- [ ] 1.1 Add Markwon core and image-plugin dependencies to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [ ] 1.2 Add a Gradle `Copy` task in `app/build.gradle.kts` that copies the entire `docs/user-guide/` directory tree to `src/main/assets/user-guide/` and wire it as a `preBuild` dependency
- [ ] 1.3 Add `src/main/assets/user-guide/` to `.gitignore` (derived artifact)

## 2. User Guide Screen

- [ ] 2.1 Create `feature/userguide/UserGuideAssetImageLoader.kt` — a custom Markwon `AsyncDrawableLoader` that accepts the current file's parent folder path (derived from `assetPath`), resolves relative image paths against it, and loads them via `AssetManager`
- [ ] 2.2 Create `feature/userguide/UserGuideFragment.kt` that accepts an `assetPath` argument (full path relative to `assets/` root; defaults to `user-guide/user-guide.md`), reads the file via `AssetManager`, derives the current folder from `assetPath`'s parent for link and image resolution, builds a Markwon instance with the image plugin and a `LinkResolver`, and renders into a scrollable `TextView`
- [ ] 2.3 Implement the `LinkResolver` in `UserGuideFragment`: `.md` links call `navController.navigate()` to a new `UserGuideFragment` with the resolved asset path; `http(s)://` links open via `Intent.ACTION_VIEW`
- [ ] 2.4 Create layout `res/layout/fragment_user_guide.xml` with a `NestedScrollView` wrapping a `TextView` (use `NestedScrollView` not `ScrollView` to propagate nested-scroll events correctly inside the `NavHostFragment` hierarchy)
- [ ] 2.5 Add `userGuideFragment` destination with an `assetPath` string argument to `res/navigation/nav_graph.xml`
- [ ] 2.6 Add User Guide item to `res/menu/bottom_nav_menu.xml` between Home and Settings (order: Home, User Guide, Settings — Settings is always rightmost) with icon `@drawable/ic_nav_user_guide` and label string resource

## 3. Navigation & Unconfigured State

- [ ] 3.1 Add `nav_user_guide` string resource to `strings.xml` and `strings-de.xml` (nav label only; guide content is English-only)
- [ ] 3.2 Add a vector drawable `ic_nav_user_guide` for the bottom nav icon
- [ ] 3.3 Update both places that gate nav items when unconfigured: `MainActivity.disableNonSettingsNavItems` (initial setup) and `SettingsFragment.checkConfigurationAndUpdateNav()` (called on every save) — both must exempt `settingsFragment` and `userGuideFragment`. Rename `disableNonSettingsNavItems` to `applyUnconfiguredNavState` to reflect the allowlist intent.

## 4. Test Fixtures

- [ ] 4.1 Create `src/test/assets/feature/userguide/index.md` — minimal Markdown with a relative link to `linked-page.md` and an image reference `![alt](test-image.png)`
- [ ] 4.2 Create `src/test/assets/feature/userguide/linked-page.md` — minimal Markdown page for back-navigation testing
- [ ] 4.3 Create `src/test/assets/feature/userguide/test-image.png` — smallest valid PNG (e.g. 1×1 pixel)

## 5. Tests

- [ ] 5.1 Write a Robolectric smoke test that checks `assets/user-guide/user-guide.md` exists (requires copy task to have run) — fail with a clear message pointing to `preBuild` if missing
- [ ] 5.2 Write a Robolectric test loading `feature/userguide/index.md` and asserting the `TextView` renders non-empty text; include `AccessibilityValidator` ATF checks (ADR-003)
- [ ] 5.3 Write a Robolectric test loading `feature/userguide/index.md`, tapping the link to `linked-page.md`, and asserting `navController` navigated to `userGuideFragment` with `assetPath = "feature/userguide/linked-page.md"`; include ATF checks
- [ ] 5.4 Write a Robolectric test loading `feature/userguide/index.md` and asserting the image at `test-image.png` is resolved without throwing (drawable is non-null); include ATF checks
- [ ] 5.5 Write a Robolectric test verifying that when the app is unconfigured, the User Guide nav item is enabled while the Home nav item is disabled
- [ ] 5.6 Run `./gradlew ktlintCheck` and fix any lint errors

## 6. Verification

- [ ] 6.1 Manually verify the User Guide screen on the `Lenovo_Tab_11_Plus` AVD (portrait and landscape) — confirm text wraps correctly, images scale sensibly, and scrolling is smooth (ADR-003)
- [ ] 6.2 Update the Maestro navigation flow in `maestro/` to include the User Guide as a third bottom-nav destination (ADR-002)

## 7. Documentation

- [ ] 7.1 Run `/write-user-guide` to update `docs/user-guide/user-guide.md` with the new User Guide navigation item
