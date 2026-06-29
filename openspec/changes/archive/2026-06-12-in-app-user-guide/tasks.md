## 1. Build Infrastructure

- [x] 1.1 Add Markwon core and image-plugin dependencies to `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] 1.2 Add a Gradle `Copy` task in `app/build.gradle.kts` that copies the entire `docs/user-guide/` directory tree to `src/main/assets/user-guide/` and wire it as a `preBuild` dependency
- [x] 1.3 Add `src/main/assets/user-guide/` to `.gitignore` (derived artifact)

## 2. User Guide Screen

- [x] 2.1 ~~Create `feature/userguide/UserGuideAssetImageLoader.kt`~~ — **superseded:** no custom `AsyncDrawableLoader` needed. Images load via Markwon's built-in `FileSchemeHandler.createWithAssets(assets)` plus a relative→`file:///android_asset/` path rewrite in `UserGuideFragment` (see design.md "Image loading" correction)
- [x] 2.2 Create `feature/userguide/UserGuideFragment.kt` that accepts an `assetPath` argument (full path relative to `assets/` root; defaults to `user-guide/user-guide.md`), reads the file via `AssetManager`, derives the current folder from `assetPath`'s parent for link and image resolution, builds a Markwon instance (with `CorePlugin`, `ImagesPlugin`, and a `LinkResolver`), and renders into a scrollable `TextView`
- [x] 2.3 Implement the `LinkResolver` in `UserGuideFragment`: `.md` links call `navController.navigate()` to a new `UserGuideFragment` with the resolved asset path; `http(s)://` links open via `Intent.ACTION_VIEW`
- [x] 2.4 Create layout `res/layout/fragment_user_guide.xml` with a `NestedScrollView` wrapping a `TextView` (use `NestedScrollView` not `ScrollView` to propagate nested-scroll events correctly inside the `NavHostFragment` hierarchy)
- [x] 2.5 Add `userGuideFragment` destination with an `assetPath` string argument to `res/navigation/nav_graph.xml`
- [x] 2.6 Add User Guide item to `res/menu/bottom_nav_menu.xml` between Home and Settings (order: Home, User Guide, Settings — Settings is always rightmost) with icon `@drawable/ic_nav_user_guide` and label string resource

## 3. Navigation & Unconfigured State

- [x] 3.1 Add `nav_user_guide` string resource to `strings.xml` and `strings-de.xml` (nav label only; guide content is English-only)
- [x] 3.2 Add a vector drawable `ic_nav_user_guide` for the bottom nav icon
- [x] 3.3 Update both places that gate nav items when unconfigured: `MainActivity.disableNonSettingsNavItems` (initial setup) and `SettingsFragment.checkConfigurationAndUpdateNav()` (called on every save) — both must exempt `settingsFragment` and `userGuideFragment`. Rename `disableNonSettingsNavItems` to `applyUnconfiguredNavState` to reflect the allowlist intent.

## 4. Test Fixtures

- [x] 4.1 Create `src/debug/assets/feature/userguide/index.md` — minimal Markdown with a relative link to `linked-page.md` and an image reference `![alt](test-image.png)` (moved from `src/test/assets/`; see design.md "Test fixtures" correction — `src/test/assets` is not on the Robolectric asset path)
- [x] 4.2 Create `src/debug/assets/feature/userguide/linked-page.md` — minimal Markdown page for back-navigation testing
- [x] 4.3 Create `src/debug/assets/feature/userguide/test-image.png` — smallest valid PNG (e.g. 1×1 pixel)

## 5. Tests

- [x] 5.1 Write a Robolectric smoke test that checks `assets/user-guide/user-guide.md` exists (requires copy task to have run) — fail with a clear message pointing to `preBuild` if missing
- [x] 5.2 Write a Robolectric test loading `feature/userguide/index.md` and asserting the `TextView` renders fixture content (asserts the rendered text contains `index`, not just non-empty — the error fallback is also non-empty); include `AccessibilityValidator` ATF checks (ADR-003)
- [x] 5.3 Write a Robolectric test loading `feature/userguide/index.md`, tapping the link to `linked-page.md`, and asserting `navController` navigated to `userGuideFragment` with `assetPath = "feature/userguide/linked-page.md"`; include ATF checks
- [x] 5.4 Write a Robolectric test loading `feature/userguide/index.md` and asserting the image at `test-image.png` is resolved without throwing; include ATF checks
- [x] 5.5 Write a Robolectric test verifying that when the app is unconfigured, the User Guide nav item is enabled while the Home nav item is disabled
- [x] 5.6 Run `./gradlew ktlintCheck` — **passes**, but see note below: under AGP 9.2.1 the ktlint 12.1.1 plugin only lints `.kts` scripts, not the Android `.kt` source sets (verified the whole existing codebase, not just this change, is unchecked). New `.kt` files were verified to match the project's de-facto style by running the ktlint 1.0.1 engine directly. **Repo-wide infrastructure gap — flagged for a separate decision (not fixed here).**

## 6. Verification

- [x] 6.1 Manually verify the User Guide screen on the `Lenovo_Tab_11_Plus` AVD (portrait and landscape) — confirm text wraps correctly, images scale sensibly, and scrolling is smooth (ADR-003). Verified manually on the emulator (portrait + landscape) — looks good.
- [x] 6.2 Create the Maestro navigation flow `code/ema-companion/maestro/bottom-nav.yaml` covering all three bottom-nav destinations, including User Guide reachability while unconfigured (ADR-002). Folder created from scratch (none existed). Requires a live emulator to execute (`maestro test code/ema-companion/maestro/bottom-nav.yaml`).

## 7. Documentation

- [x] 7.1 Run `/write-user-guide` to update `docs/user-guide/user-guide.md` with the new User Guide navigation item (and the `system-context.png` C4 diagram in the Overview)
