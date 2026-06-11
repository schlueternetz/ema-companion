# Lessons Learned
This file is for the AI to keep track of lessons learned and avoid making the same mistakes again.
It is never authored by a human.

## 2026-06-11: implement-settings — full settings screen implementation

### What Went Well
* Grouping 47 tasks into 8 coherent implementation chunks (Repository → Crypto → Custom View → Layout → Fragment → MainActivity → Tests → Lint) made the AI-TDD cycle efficient and kept each Green phase small
* Adding `isConfigured()` to the repository before writing `SettingsFragment` and `MainActivity` prevented all compile failures — always implement dependencies before consumers
* Writing string resources before the layouts that reference them avoided "symbol not found" compile errors on the first test run
* Deferring `write-user-guide` until all UI changes were complete produced a single accurate guide instead of multiple partial ones
* Using `java.util.Base64` instead of `android.util.Base64` in `SettingsCrypto` keeps the class testable with plain JUnit4 — no Robolectric overhead needed for pure crypto logic

### What Didn't Work (Obstacles & Roadblocks)
* `android.util.Base64` is not mocked in plain JUnit4 (non-Robolectric) tests — using it in a utility class caused `RuntimeException: Method not mocked` failures
* `SecretKeySpec` lives in `javax.crypto.spec`, not `javax.crypto` — the wrong import compiled silently until test execution
* The existing `mainActivity_bottomNavStartsOnHome` test broke when unconfigured-state navigation was added, without an obvious error message

### ⚠️ Mistakes to Avoid Next Time
* **Never use `android.util.Base64` in non-UI utility classes** — use `java.util.Base64` (available from API 26, fine for minSdk 31). Reserve `android.util.Base64` only for code that already requires Robolectric for other reasons
* `SecretKeySpec` import is `javax.crypto.spec.SecretKeySpec`, not `javax.crypto.SecretKeySpec` — double-check the `spec` subpackage for all JCE parameter/key-spec classes
* When adding conditional navigation in `MainActivity.onCreate()`, immediately update any existing tests that assume the default start destination — they will fail without an obvious connection to the new behaviour
* The `write-user-guide` hook fires on **every** UX file write during a session. Acknowledge and defer; only invoke the skill once when all UI changes for the session are complete

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
