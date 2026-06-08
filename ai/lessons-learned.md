# Lessons Learned
This file is for the AI to keep track of lessons learned and avoid making the same mistakes again.
It is never authored by a human.

## 2026-06-08: main-navigation-and-settings implementation

### What Went Well
* Implementing string resources and icons before fragment/activity code prevented circular dependency failures — layouts reference strings/drawables at compile time
* Using an injectable `SharedPreferences` constructor (`SettingsRepository(prefs)`) alongside a `create(context)` factory made unit tests trivial without mocking frameworks — plain `SharedPreferences` in tests, encrypted in production
* Adding `debugImplementation(fragment-testing)` (not `testImplementation`) is the correct way to make `launchFragmentInContainer` available to Robolectric — it puts `EmptyFragmentActivity` in the debug merged manifest that Robolectric reads
* AI-TDD order: write resource files first (strings, icons, nav graph, menus) without tests, then write failing tests for behavior classes — avoids the problem of tests failing to compile because referenced resources don't exist yet

### What Didn't Work (Obstacles & Roadblocks)
* `EncryptedSharedPreferences.create()` throws `KeyStoreException → NoSuchAlgorithmException` in Robolectric because the Android Keystore provider is not emulated — any fragment that calls `SettingsRepository.create(context)` in `onViewCreated` will crash all its Robolectric tests
* `launchFragmentInContainer<F>(Bundle(), R.style.X)` produces a confusing "type mismatch: actual Int but Int expected" compile error when `F` is unresolved — the real error is the unresolved fragment class, not the argument types
* The ktlint Gradle plugin 12.x with AGP 9.2.x only discovers `.kts` scripts, not app `.kt` source files — `./gradlew ktlintCheck` passes vacuously for Kotlin source

### ⚠️ Mistakes to Avoid Next Time
* **Never call `EncryptedSharedPreferences.create()` directly inside a Fragment lifecycle method** without a Keystore fallback — Robolectric cannot run any test on that fragment. Always wrap the factory with a try-catch that falls back to plain `SharedPreferences`, or use constructor injection so tests bypass the encrypted path entirely
* When `launchFragmentInContainer<MyFragment>(...)` gives confusing type mismatch errors, check that `MyFragment` is resolvable first — the cascade of type errors is caused by the unresolved generic type, not the argument types
* When adding `fragment-testing` for Robolectric use: use `debugImplementation`, not `testImplementation` — only the debug manifest is merged into what Robolectric uses
* Use named parameters (`themeResId = R.style.X`) in `launchFragmentInContainer` calls to avoid nullable-vs-non-nullable `Bundle?` ambiguity

## 2026-06-08: ATF accessibility testing setup

### What Went Well
* Inspecting the actual `.aar` jar contents with `javap` resolved all API uncertainty — decompiling `AccessibilityValidator.class` revealed the exact method signatures (`setThrowExceptionFor`, `check(View)`) after guessing failed 4 times
* Robolectric + `espresso-accessibility` as `testImplementation` works cleanly on JVM — no emulator needed, `isRobolectric()` is detected internally by `AccessibilityValidator`
* `@Config(qualifiers = "de")` on a Robolectric test correctly loads German string resources without any additional setup

### What Didn't Work (Obstacles & Roadblocks)
* Guessed the ATF API 4 times from training data before giving up and inspecting the jar — each wrong attempt cost a full Gradle build cycle (~2 min each)
* `AccessibilityValidator` is in `integrations.espresso` subpackage, not the top-level ATF package — the class name alone is not enough to find the right import
* `check(View, null)` does not exist — the two-arg overload takes `Parameters`, not `Locale` or `null`

### ⚠️ Mistakes to Avoid Next Time
* When the ATF or any unfamiliar library API is needed: **inspect the jar first** using `javap` before writing any code. The jar is always in `~/.gradle/caches` after the first Gradle sync. Extract the `.aar` as a zip to get `classes.jar`, then use `javap -p <classname>` to see real method signatures
* `AccessibilityValidator` correct usage: `AccessibilityValidator().setThrowExceptionFor(AccessibilityCheckResult.AccessibilityCheckResultType.ERROR).check(view)` — no second arg, no `setCheckLevel`, no `runChecks`
* Import path: `com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator`
* Always add an `android:id` to views that need to be found in tests — a `TextView` without an ID cannot be retrieved via `findViewById`

## 2026-06-07: Project documentation and workflow setup

### What Went Well
* Reading existing code (build.gradle.kts, libs.versions.toml, existing specs) before writing design docs produced accurate, grounded decisions — no invented dependencies or wrong API levels
* Checking `openspec/specs/` for existing capabilities before writing the proposal prevented duplicate spec creation and correctly identified `localization` as a modified capability
* Exploring the skill and command file structure before creating new ones kept the format consistent
* Asking for clarification on "Blue/Green TDD" before writing the ADR avoided encoding the wrong concept in a permanent document

### What Didn't Work (Obstacles & Roadblocks)
* Assumed Android Studio was the primary development tool without asking — had to revise the Getting Started section after the user corrected it
* The `lessons-learned` skill was never invoked during the OpenSpec proposal session — it existed but wasn't wired into any workflow, so it silently did nothing

### ⚠️ Mistakes to Avoid Next Time
* Do not assume the user's editor or toolchain — ask or check `local-android-dev` skill and settings for environment context before writing setup instructions
* Do not wait for the user to notice a skill isn't firing — when wiring up new skills, verify they have an explicit trigger (hook or workflow step), not just a description that says "use when X"
* Always invoke `lessons-learned` after documentation or code changes; it must be explicitly called — it has no automatic trigger
