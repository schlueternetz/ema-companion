# AI Lessons Learned

## 2026-06-15: home-current-production (first real EMA API call)

### Went Well
* Pure `EmaRequestSigner` (injected `clock`/`nonce`) + reference HMAC computed via `openssl dgst -sha256 -hmac` → assert exact Base64 output, not impl-vs-impl
* `java.util.Base64` (NOT `android.util.Base64`) keeps signer a plain-JUnit test; `android.util.Base64` needs Robolectric
* `ProductionSource` interface + `HomeFragment.sourceOverride` companion seam = fake the fetch with no HTTP
* Real-HTTP MockWebServer test THROUGH `HomeFragment.onResume`: inject client `ioDispatcher = Dispatchers.Unconfined` so socket call runs inline → coroutine done before assert (no flaky timing)
* Throttle keyed on persisted `lastFetchEpochMs`; count+log+lastFetch updated at ONE point, only when a request is actually issued (`ConfigurationError`→none) → progress bar & Logs never disagree
* Multi-line `"""..."""` JSON fixtures: shorten lines AND ktlint max-line-length ignores inside multiline strings (JSON whitespace-insignificant, `JSONObject`/`JSONArray` tolerate it)
* Robolectric dialog button: `ShadowDialog.getLatestDialog()` as androidx `AlertDialog` + `Looper` idle BEFORE+AFTER `performClick()` (factory-reset positive button didn't fire without idle)
* Per-domain own plain `SharedPreferences` (`ema_api_usage`, `ema_api_log`) not in encrypted `SettingsRepository`; factory reset must `clear()` each explicitly

### Didn't Work
* First real network call crashed on device: `AndroidManifest.xml` lacked `<uses-permission android:name="android.permission.INTERNET"/>` → `SecurityException: missing INTERNET permission`. Robolectric/MockWebServer does NOT enforce INTERNET, so the integration test passed green — only a real emulator caught it
* Client caught only `IOException`; the missing-permission `SecurityException` (a RuntimeException) escaped the IO coroutine and killed the whole app — a background fetch must catch broad `Exception` → degrade to NetworkError
* Banner was wired to `NetworkError` only → bad credentials return an EMA `ApiError` (code 4000), so NO banner showed and stale value stayed; broadened banner to any failed fetch (NETWORK vs API), keep last value. Confirmed cause via on-device log: `run-as <pkg> cat /data/data/<pkg>/shared_prefs/ema_api_log.xml`
* Count + throttle semantics settled on: ONLY a successful read (EMA code 0) counts toward the monthly limit and starts the 10-min throttle (billed on data access). EVERY failure (network/auth/param/server) is logged + shown but free + retried next trigger. Plus: changing a connection setting (creds/baseUrl) resets the throttle (`lastFetchEpochMs=0`) + clears persisted error → immediate retry on return to Home
* EMA "bad credentials" often returns code 4000 (Request parameter exception), NOT a 2xxx auth code — invalid System/ECU IDs are params. Auth codes = 2000-2004 / 3000-3004. Don't assume a credential failure is an "auth" code; check the actual `code`
* Flash-free tile: give the data source a synchronous `currentState()` (reconstructed from persisted store) for the initial `onViewCreated` render, then `refresh()` in `onResume` updates — don't render an empty `ProductionState()` first
* `gradlew ktlintCheck` in app module wires ONLY `runKtlintCheckOverKotlinScripts` (.kts) — lints ZERO `.kt`; green proves nothing about Kotlin source
* Hand-rebuilding standalone ktlint classpath from Gradle cache = deep rabbit hole: ktlint 1.0.1 needs kotlin-compiler-embeddable 1.9.x (2.2.x → `NoSuchFieldError HEADER_KEYWORD`), exactly one slf4j-api 2.0.x + logback 1.3.5 (extra/sources jars → NOPLogger cast crash)
* `adb` not on PATH in the Bash tool

### Avoid
* "Propose" means present design for review/approval BEFORE coding — got called out for editing files when the user asked for a proposal; stop at the proposal, implement only after approval
* Transient in-memory UI state (banner/error) vanishes on fragment recreation (bottom-nav) → make displayed state a pure function of PERSISTED last-result (value + timestamp + error); seed it in the repo ctor so a recreated tile looks identical
* Per-endpoint error belongs IN its tile (local status line, visible without tap), not a screen-level banner — scales as Home grows to multiple data tiles; build one tile well in a repeatable shape, don't build a generic dashboard framework before the 2nd endpoint exists
* First networking feature → add `INTERNET` permission to the manifest AND deploy to a real emulator; Robolectric won't catch its absence
* Don't reconstruct a parallel lint harness when the project gate is broken — trust `gradlew ktlintCheck`, match existing style manually, move on
* Fragment that fetches in `onResume`: inject data source via companion seam + run client on `Dispatchers.Unconfined` in tests so it completes inline before assertions
* Count/log an API call at exactly one point (the repository), only for issued requests

## 2026-06-14: ema-api-stub (new standalone Ktor mock app)

### Went Well
* Standalone Gradle project under `code/ema-api-stub/` (NOT a module of the Android build) — reused app's `gradlew`+wrapper jar, own `settings.gradle.kts`; isolates JVM/server toolchain from AGP
* Record/replay design: per-ECU JSON scenario file, ordered `interactions[]` (request matcher + response), per-ECU cursor, strict sequential match → loud HTTP 409 diagnostic on mismatch
* `RequestMatcher` partial match (only listed `pathParams`/`query` asserted, rest wildcard) → pin `eid`+`energy_level`, leave `sid` open
* `JsonElement` response body served verbatim via `body.toString()` (valid compact JSON) — avoids `encodeToString` serializer-inference errors
* Ktor `testApplication {}` + fresh `MatchingEngine(loadDefault())` per test = cursor isolation without a reset call; integration coverage with no real socket
* Verified real CIO server with `./gradlew run` + curl (testApplication uses a test engine, not CIO — boot bugs hide otherwise)
* ktlint log showed `runKtlintCheckOverMainSourceSet`/`TestSourceSet` → confirmed it actually lints `.kt` (past pitfall: linting zero files)

### Didn't Work
* C: drive 100% full (system-wide, not just Gradle's 3.8G home) → builds AND shell pipes died ("No space left on device" even from `tail`, since git-bash `/tmp` is on C:)
* KGP 2.1.0 does NOT support Gradle 9.x (wrapper is 9.4.1) → used Kotlin 2.2.0
* `.properties` file eats single backslash → `-Djava.io.tmpdir=D:\gradle-tmp` parsed as `D:gradle-tmp`; use forward slashes `D:/gradle-tmp`
* Ktor 3 routing: `io.ktor.server.routing.get(path){}` as FQN call → "Unresolved reference 'get'"; must `import ...routing.get` and call unqualified (extension on Route)
* `pkill`/curl-shutdown didn't stop the bg `./gradlew run` server on Windows → killed via PowerShell `Get-NetTCPConnection -LocalPort N | Stop-Process`

### Avoid
* On C:-full Windows box: redirect Gradle off C: → `GRADLE_USER_HOME=/d/gradle-home` + `TMPDIR/TMP/TEMP=/d/gradle-tmp` + `org.gradle.jvmargs ... -Djava.io.tmpdir=D:/gradle-tmp` in the D: home's `gradle.properties`; keep machine-specific tmpdir OUT of the committed project `gradle.properties`
* Don't pipe big Gradle output through `| tail` when C:/tmp is full — redirect to a file on D: then read it
* Pick Kotlin version by Gradle-version support matrix, not latest-by-habit (KGP↔Gradle compat is strict)
* Bundled-scenario default load uses classpath `getResource("scenarios")`→`File` (works under `gradlew run`/tests, exploded resources) — a fat-jar would break dir listing (out of scope, documented)

## 2026-06-14: maestro CI flake — `homeFragment is visible` (cold emulator)

### Went Well
* Read `.github/workflows/ci.yml` gave the decisive facts: CI = API 33 `pixel_tablet`, `maestro test maestro/`, latest Maestro pulled per-run (turned out same 2.6.1 — version ruled out)
* The stash-the-fix A/B test was the turning point: reverting my change and seeing the ORIGINAL code PASS proved my "fix" was the regression, not the cure
* Dumping rendered `text=` (not just resource-ids) surfaced the real blocker: a system `Application Not Responding: com.google.android.apps.nexuslauncher` dialog ("Pixel Launcher reagiert nicht") covering the app — the German text is the *emulator* locale, not the app
* `extendedWaitUntil { visible: id: settingsFragment; timeout: 60000 }` after `launchApp` makes the first assertion wait for the app to reach foreground instead of racing startup

### Didn't Work
* Edge-to-edge theory (API 35+ enforces it, bottom nav under system bar) was a RED HERRING. Adding `ViewCompat.setOnApplyWindowInsetsListener` on the root made it WORSE — blank screen via Maestro launch (content laid out off-screen, inverted bounds `Rect(84,2074-468,2063)`). Original layout renders the bottom nav fine on API 36
* Trusting a `grep -c bottom_nav` presence count — the node can be PRESENT in the tree but laid out off-screen (Maestro correctly reports it not visible). Must check actual `bounds=`, not just presence
* `adb root` on the `google_apis_playstore` image → "cannot run as root in production builds"; can't change system locale via setprop without a rootable (`google_apis`, not playstore) image
* Running two emulators + Gradle builds concurrently ANR'd the launcher → false "failures". Single emulator, let it settle (launcher focus = `NexusLauncherActivity`) before trusting a Maestro result

### Avoid
* Don't theorize a layout/code cause for a Maestro failure before reproducing on a COLD emulator — warm runs passed 3×, cold first-launch failed; the bug was startup timing, not code (`git log` confirmed no layout change)
* Don't add window-inset handling speculatively to "fix" edge-to-edge unless you've SEEN the nav clipped — verify with a real screenshot first
* When a Maestro `assertVisible` flakes right after `launchApp` on CI, suspect app/emulator readiness; gate with `extendedWaitUntil` on a stable element rather than bumping per-assert timeouts
* `MSYS_NO_PATHCONV=1` for `adb shell ... /sdcard/...`; pull dumps to a path under the project (`./x.xml`), not `/tmp` (not visible to win adb)

## 2026-06-13: localize-user-guide (German in-app only)

### Went Well
* gitignore negation to commit generated files in a gitignored dir: `/app/src/main/assets/user-guide/*` then `!*-de.md` / `!*-de.png` — `git add -n` confirms only `-de` files stage
* `npx -p @mermaid-js/mermaid-cli@11.15.0 mmdc -i x.mmd -o x.png` works with no global install; pin version → byte-stable PNG (EN re-render produced identical 38737 bytes, zero churn)
* locale detection via `ConfigurationCompat.getLocales(resources.configuration)[0].language` — reliable at runtime, test with `@Config(qualifiers = "de")` (no `getApplicationLocales()` pitfall)
* fragment swaps to `-de.md` sibling only at read time → link-resolution/navigation tests stay green under default locale
* translated guide using real `values-de/strings.xml` UI labels (Einstellungen, Werksreset…), not ad-hoc German

### Didn't Work
* first put German in `docs/user-guide/` → wrong: `docs/` renders on GitHub; user wanted German in-app only → moved `-de` files to `app/src/main/assets/user-guide/` mid-task
* `.gitignore` can't use `../` parent paths — `docs/` ignores must go in the repo-root `.gitignore`, not `code/ema-companion/.gitignore`
* Edit tool needs a fresh `Read` in the current session even for files shown in the system prompt (SKILL.md, AGENTS.md)

### Avoid
* Don't commit generated translations in `docs/` if they shouldn't hit GitHub — commit them where consumed (app assets) via gitignore negation
* Never translate mermaid ids — only quoted label strings (`Person(owner, "...")`: translate the `"..."`, keep `owner`); leaked id breaks render
* `mmdc` renders raster PNG — never "translate the image"; translate the `.mmd` source and re-render
* `*-de.mmd` is a throwaway intermediate — render then discard; never commit (regenerate from EN each time)

## 2026-06-12: settings import didn't apply theme/language

### Went Well
* Display-mode test asserted `AppCompatDelegate.getDefaultNightMode()` directly — synchronous and reliable in Robolectric

### Didn't Work
* `refreshAllDisplayedValues()` (import + factory-reset path) only updated labels (`updateLanguageDisplay`/`updateDisplayModeDisplay`); it never called `applyDisplayMode`/`applyLanguage`, so the effect was deferred until the next edit triggered a recreate
* Asserting `AppCompatDelegate.getApplicationLocales()` at `@Config(sdk=[33])` → always empty: on Tiramisu+ it reads the framework `LocaleManager` (unbacked in Robolectric); below 33 AppCompat's backport storage reflects `setApplicationLocales`

### Avoid
* After import/factory-reset, apply persisted theme AND locale, not just their labels
* Robolectric per-app-locale assertions: pin the test to `@Config(sdk = [32])` (or lower) so `getApplicationLocales()` reads AppCompat backport storage, not the framework service
* `setDefaultNightMode` is synchronous in Robolectric; `setApplicationLocales` is not at API 33+

## 2026-06-12: Maestro flow + CI (locale & wrapper)

### Went Well
* Reproduced the Maestro failure locally via `adb` (clear state → launch → `uiautomator dump` + screencap) without Maestro installed — screenshot revealed the emulator was German
* Installed Maestro locally (`curl get.maestro.mobile.dev | bash`, set `ANDROID_HOME`) and ran the flow to confirm GREEN before spending a CI cycle

### Didn't Work
* Maestro flow asserted English labels (`"Settings"`, `"Home"`) — fails on the German emulator (`Einstellungen`/`Startseite`). Nav labels are localized; ids are not
* `assertVisible: "EMA Companion User Guide"` — Markwon renders the whole guide into ONE TextView; Maestro regex-matches the element's entire text blob, so a title substring never matches
* `adb exec-out cat /sdcard/...` returned empty in git-bash — MSYS rewrote `/sdcard/...` to `C:/Program Files/Git/sdcard/...`
* CI failed at first step: `gradle-wrapper.jar` was gitignored (root `.gitignore`) so the wrapper couldn't run on a clean checkout

### Avoid
* Maestro selectors → match `id:` (resource-id, locale-independent), not visible text. Confirmed ids: `homeFragment`/`userGuideFragment`/`settingsFragment`, content view `user_guide_content`
* For a single-TextView Markdown screen, assert the **view id**, not rendered text
* Prefix adb device-path commands with `MSYS_NO_PATHCONV=1` (or use `//sdcard/...`) in git-bash
* `gradle-wrapper.jar` MUST be committed (build tool, not bundled in the APK) — never gitignore it

## 2026-06-12: in-app-user-guide (Markwon + Robolectric assets)

### Went Well
* Diagnostic test that throws an `AssertionError` dumping `text.javaClass`, length, and all span class names — pinpointed the real cause (error-fallback text) in one run
* Linting the engine directly from the Gradle cache (resolved the exact classpath via `gradlew app:dependencies --configuration ktlint`) when the plugin wouldn't lint `.kt`

### Didn't Work
* `src/test/assets/` is NOT on the Robolectric asset path — `android_merged_assets` points to `build/intermediates/assets/debug/mergeDebugAssets` (the **debug variant** merge = `src/main` + `src/debug`). `isIncludeAndroidResources` does not add `src/test/assets`. Fragment silently fell back to error text; weak `isNotEmpty()` assertions passed falsely
* `Markwon.builder()` does NOT register `CorePlugin` (unlike `Markwon.create()`) — without it, text still renders but `.md` links never become `LinkSpan`s
* ktlint 12.1.1 plugin under AGP 9.2.1 only wires `.kts` script tasks — `ktlintCheck` passes while linting **zero** `.kt` files (whole repo, not just this change)

### Avoid
* Robolectric test-only assets → put in `src/debug/assets/` (read by Robolectric, excluded from release APK), never `src/test/assets/`
* Assert real fixture content (`contains("index")`), never bare `isNotEmpty()` — error fallbacks are non-empty and mask load failures
* Markwon image-from-assets: use built-in `FileSchemeHandler.createWithAssets()` + rewrite relative paths to `file:///android_asset/…`; don't hand-roll an `AsyncDrawableLoader`
* When `gradlew ktlintCheck` looks suspiciously cheap, confirm it actually lints `.kt` (grep the report for a source file) before trusting it

## 2026-06-11: Robolectric appcompat AlertDialog

### Went Well
* `hintText: String?` on `SettingRowView` — clean, no new classes, 4/5 tests green first try

### Didn't Work
* `ShadowAlertDialog.getLatestAlertDialog()` → null for appcompat dialogs
* `is android.app.AlertDialog` → fails for appcompat instances in Robolectric
* `AlertDialog.Builder(ApplicationContext)` → dialog never shown

### Avoid
* Use `ShadowDialog.getLatestDialog()`, cast to `androidx.appcompat.app.AlertDialog` (not platform class)
* Dialog tests need `Robolectric.buildActivity(AppCompatActivity).setup().get()`, not Application context
* Read dialog message via `dialog.window?.decorView?.findViewById<TextView>(android.R.id.message)`
* Set `label` before `hintText` in SettingsFragment — listener captures label at set time

---

## 2026-06-11: nav orphaned back stack

### Went Well
* `NavigationUI.onNavDestinationSelected` in Robolectric — matches real bottom-nav tap path

### Didn't Work
* `bottomNav.selectedItemId = id` → doesn't fire listener in Robolectric
* `adb logcat -d` → empty on this emulator session

### Avoid
* Don't push gated screen over start dest — breaks `popUpTo`; set it AS start dest instead
* `navController.navigate(id)` passing ≠ bottom-nav works — test via `NavigationUI.onNavDestinationSelected`

---

## 2026-06-11: api-settings-improvements

### Went Well
* Read all source files before writing — prevented mid-task surprises

### Didn't Work
* PostToolUse hook fires every Edit to a UX file — 5 interruptions for one task
* Edit requires fresh `Read` in current tool sequence even if file is visible in context

### Avoid
* Always `Read` before `Edit` in current sequence — tool enforces this independently
* After adding field to `isConfigured()`, find all test helpers that build settings config
* Batch UX edits; invoke `write-user-guide` once at end, not per edit

---

## 2026-06-10: required-settings-indicators

### Went Well
* `TextView.hint` for "Required" indicator — auto-shows/hides with text, zero extra logic

### Didn't Work
* PostToolUse hook fires per-edit, not per-task — batching avoids interruption
* Gradle caches tests → `--rerun` needed for targeted runs after new property

### Avoid
* Batch UX edits; invoke `write-user-guide` once at end
* Add `--rerun` for targeted Robolectric tests when verifying red/green cycle
* Set `isRequired` before `value` in SettingsFragment — hint reads value at set time
