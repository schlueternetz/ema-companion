# AI Lessons Learned

## 2026-07-14: widget-preview-crash (RemoteViews rejects View/Space in previewLayout)

### Went Well
* Force-stop launcher (`com.google.android.apps.nexuslauncher`) + `adb shell input` long-press-home → Widgets → tap app row + screencap = full repro of widget-picker bug with zero manual interaction, no Maestro needed
* `adb logcat -c` right before repro, `-d` right after → exact `InflateException` stack per widget, one shot, no noise
* `InflateException: Class not allowed to be inflated android.view.View` in logcat = decisive; static analysis (resource compile, string refs, theme attrs) all looked fine and would never have caught this
* Fix verified twice: `:app:processDebugResources` (compiles) AND re-repro on-device after `installDebug` + launcher force-stop (actual preview renders) — resource compile passing does NOT mean RemoteViews will accept the layout

### Didn't Work
* Assuming `previewLayout` XML is inflated by a normal `LayoutInflater` because it's "just a View XML layout" (per the widget-preview skill's own framing) — the widget picker actually inflates it via `RemoteViews`, which enforces a hidden view-class allowlist
* `<View>` (used for chart-bar mockups) and `<Space>` (used for vertical gaps) are NOT in that allowlist — both throw `Class not allowed to be inflated`, silently breaking ALL THREE widgets identically since they shared the same pattern

### Avoid
* Never use plain `<View>` or `<Space>` in an `android:previewLayout` XML — RemoteViews only allows a fixed set of widget classes (TextView, ImageView, Button, layouts like LinearLayout/FrameLayout, etc.); use `<ImageView android:background="...">` for solid-color bars, and `layout_marginBottom`/`layout_marginTop` instead of `<Space>` for gaps
* `:app:processDebugResources` passing is necessary but NOT sufficient to prove a previewLayout works — it only checks resource references resolve, not that RemoteViews will accept every view class used; always do one live on-device check (force-stop launcher, reopen widget picker) after writing/editing a previewLayout
* The existing `widget-preview` skill doc doesn't mention the RemoteViews allowlist restriction — anyone hand-writing new previewLayout XML from its instructions alone will hit this same crash

## 2026-07-14: notification-alert-levels (Off/Alerts Only/All for push + email)

### Went Well
* Shared `AlertLevel` enum + one pure `shouldAlert(level, previous, new)` fn reused for both push and email gating in `ModuleHealthWorker` — one truth source, plain-JUnit testable, no Robolectric needed
* Lazy read-time migration in `SettingsRepository` getter (`readLevelWithLegacyMigration`) — old boolean key read once, translated, written under new key; zero explicit "migration step", transparent to every caller including tests
* Reused Language/Display Mode's tap-to-open-`AlertDialog.setItems()` row pattern for both new level pickers — zero new UI framework, same interaction the user already knows
* `postOnGreen: Boolean = false` default param on `ModuleHealthNotifier.notify()` kept every pre-existing test/call site compiling unchanged — only new "All"-tier callers pass `true`
* `android:id/text1` (Android's own internal `select_dialog_item` row ID) + Maestro `index:` — locale-independent way to tap `AlertDialog.setItems()` rows; confirmed via live `uiautomator dump`, no assignable custom ID exists on ArrayAdapter-backed list items

### Didn't Work
* First Maestro fix used `tapOn: text: "Alerts Only"` for the new dialog — emulator's system locale was German, dialog showed "Nur Warnungen", flow failed; the file's own header comment already warned against text selectors for exactly this reason
* Own debugging (`adb shell am start` + `uiautomator dump` to inspect the dialog) triggered a real Home fetch against the local `ema-api-stub` between two Maestro runs — desynced its per-ECU cursor, next `a-home-screen.yaml` run failed with "no chart data" (looked like a real regression, was self-inflicted)
* Removing a boolean setting's persisted key outright (no back-compat shim, per project convention) meant `SettingsRepository`, `ModuleHealthWorker`, and `SettingsFragment` all had to move together in one pass before ANY layer would compile again — no isolated per-file green until the whole call-site chain was done

### Avoid
* Never use Maestro `text:` selectors for content whose locale isn't pinned — dump the real UI hierarchy (`uiautomator dump`) and use a stable resource ID instead; `android:id/text1` works for any `AlertDialog.setItems()` list, disambiguated by `index`
* Don't run ad hoc `adb shell am start`/UI-inspection commands against the local stub between Maestro runs without `POST /__stub__/reset` right after — same rule as manual curl, already learned once, bit again via a different tool
* Before assuming a setting already gates behavior because it's persisted + tested + UI-wired, check whether the actual consumer (a Worker's `doWork()`) reads it at all — `notificationsEnabled` was fully wired end-to-end except the one place that mattered

## 2026-07-12: api-fetch-scheduler (centralize EMA API fetch triggering)

### Went Well
* `ApiSyncScheduler` as single entry point + `WorkManager.enqueueUniqueWork(name, REPLACE, request)` — coalesces bursty settings saves into one fetch, fixed real bug where redundant per-field-save fetches could overwrite good data with a later failure
* Module Health kept on its own separate always-on `ModuleHealthWorker` schedule (ADR-010) instead of folding into `ApiSyncWorker` — alerting-class data must never be gated by tile/widget/foreground state
* `WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())` in `@Before setUp()` — needed in every Robolectric test whose Fragment now touches real WorkManager (6 files)
* Temporary `Log.d()` in both `ApiSyncWorker.doWork()` and `HomeFragment.observeSyncCompletion()` + `adb logcat -c` before an isolated single-flow rerun + `adb logcat -d` after — pinpointed worker completion AND Flow emission were BOTH correct, narrowing the bug to the state-READ step; removed logs once root cause confirmed
* 3 real bugs found ONLY by `/qa`'s actual emulator run, zero caught by unit/Robolectric — validates AGENTS.md's Definition of Done requiring a real `/qa` pass before marking work done

### Didn't Work
* Moving fetch execution from Fragment-scoped coroutines into a Worker broke `HomeFragment`'s `private lateinit var hourlySource`/`dailySource`/`moduleHealthSource` fields silently — Worker constructs its OWN separate repo instance via the same `Xxx.create(context)`; both read/write the same SharedPreferences file but each instance's in-memory cache is loaded once at construction and never re-synced — Fragment's `currentState()` returned permanently stale (construction-time) data no matter how completion was signaled
* Plain `viewLifecycleOwner.lifecycleScope.launch { flow.collect {} }` for `observeSyncCompletion()` crashed the app: `requireContext()` called after view detached (`IllegalStateException`) — cooperative cancellation only checks at suspension points, so a Flow can still emit into a collector whose lifecycle is already tearing down
* Bare `assertVisible` on `hourly_chart`/`history_chart` in Maestro started flaking once fetch dispatch centralized through WorkManager — extra WorkManager DB-write + Flow-propagation hop pushed latency past the 7s default timeout even though the underlying network wait was unchanged

### Avoid
* Never cache a repository as a Fragment instance field once ANY OTHER component (a Worker, a different Fragment) can also construct and write through its own separate instance of the same repo — always resolve fresh via a function (`hourlySource()`, not `hourlySource`) on every read; `currentState()` is a cheap prefs read, so there's no cost. Same class of bug already avoided once this session for `DailyEnergyRepository.create()`'s `todayTotalProvider` — pattern wasn't applied consistently to `HomeFragment` itself
* Any Flow collected in a Fragment via `lifecycleScope.launch { flow.collect {} }` that touches `requireContext()`/`requireView()` needs `repeatOnLifecycle(Lifecycle.State.STARTED)` wrapping, not a bare launch — plus a defensive `if (view == null) return@collect` guard
* When centralizing a fetch path through WorkManager, widen any Maestro assertion on the fetch's resulting UI state to `extendedWaitUntil` — the dispatch hop adds real latency on top of the network wait a bare `assertVisible` was already borderline against

## 2026-07-13: widget-preview skill (fix robot-head icon in widget picker)

### Went Well
* minSdk=31 means `android:previewLayout` (API 31+) always applies — no previewImage bitmap fallback needed, skip emulator/screenshot pipeline entirely
* Root cause of robot head: `previewImage` pointed at `ic_launcher_foreground`, an adaptive-icon foreground layer, not a real static bitmap — system couldn't render it, fell back to placeholder
* Static plain-View XML layout (not Glance/Compose) mirroring the widget's real look, themed via `android:theme="@style/Theme.EMACompanion"` on root so `?attr/colorSurface`/`colorOnSurface`/`colorPrimary` resolve to the app's own Material3 DayNight palette — matches `WidgetTheme.kt`'s `GlanceTheme` defaults without needing Glance at all
* New `widget_preview_*` sample-value strings (both `values/` and `values-de/`) instead of hardcoding preview text — keeps ADR-003 (all text in string resources) intact even for OS-chrome preview content
* `./gradlew.bat :app:processDebugResources` is enough to verify a previewLayout/string reference compiles — no need for a full assembleDebug or emulator for this kind of change
* AskUserQuestion to pick previewLayout (static XML, no emulator) vs previewImage (screenshot pipeline) before building — real architectural fork, not just an implementation detail

### Didn't Work
* First pipe-test of the new hook printed nothing for "matching" paths — turned out the synthetic test JSON itself was invalid (`\p`, `\e` etc. are not legal JSON escapes in a Windows path inside single-quoted bash); rewrote test payloads with forward slashes / proper `\\\\` escaping before concluding the hook logic was broken

### Avoid
* Don't hand-write a Windows path into a bash single-quoted JSON test string with single backslashes — `\\p`, `\\e` aren't valid JSON escapes; JSON.parse throws before the hook logic ever runs, and the failure looks identical to "hook didn't match"
* UX-file hook (write-user-guide) and widget-file hook both fire on `res/values/strings.xml` / `res/xml/*_widget_info.xml` edits — for OS-chrome-only content (widget-picker preview, not in-app screens), judge and skip write-user-guide rather than invoking it reflexively

## 2026-07-13: support-buy-me-a-coffee (4th bottom-nav tab + email footer links)

### Went Well
* Reused `MainActivity.applyUnconfiguredNavState()` pattern (add supportFragment to always-enabled set) instead of new logic — static screen needs no EMA config, same as User Guide
* Reused single `support_bmac_url`/`support_website_url` string resources in both `SupportFragment` and `EmailContentBuilder` — one URL literal per link, not duplicated across two features
* Skipped adding a Maestro flow that taps BMAC/website buttons — would fire real `ACTION_VIEW` intent and send emulator to external browser mid-flow; covered button behavior at Robolectric layer instead, only extended `bottom-nav.yaml` for reachability
* `ktlintCheck` actually linted `.kt` this run (past lessons said it didn't) — caught a real indentation violation in `MainActivity.kt`; `ktlintFormat` auto-fixed it in one pass

### Didn't Work
* `a-home-screen.yaml` failed on `"0.42" is visible` — root cause was `ema-api-stub` server simply not running (nothing on port 8080), unrelated to this change; starting it (`./gradlew run` in `code/ema-api-stub`) + `POST /__stub__/reset` before retry fixed it

### Avoid
* Don't assume `ktlintCheck` is still only linting `.kts` — the repo-wide gap noted in older lessons may already be fixed; verify per-run instead of trusting a stale note
* Before blaming a Maestro flow failure on your own change, check whether it depends on infra you didn't touch (e.g. `ema-api-stub` on port 8080) — a 000/connection-refused check is faster than re-reading unrelated code

## 2026-07-12: configurable-tiles-widgets (remove Current Production + tile/widget visibility settings)

### Went Well
* `core/HomeTile.kt`/`core/HomeWidget.kt` enums in `core/` not `feature/settings/` — used by 3 features (home, widgets, settings), ADR-004 rule applies even though `SettingsRepository` itself is a pre-existing exception
* `SettingsRepository.isHourlyDataNeeded()`/`isDailyDataNeeded()`/`isModuleHealthDataNeeded()` as single source of truth for gating — Today Production tile counts as consumer of BOTH hourly and daily (best-day cards use daily) even though its own data is hourly; missing this coupling would've silently broken best-day cards when History tile disabled
* `WidgetUpdater.enabledWidgets(settings): List<GlanceAppWidget>` exposed as pure testable fn instead of trying to spy on Glance's real `updateAll()` — direct unit test, no Robolectric Glance placement hacks
* Widget disabled-check placed BEFORE touching `hourlySourceOverride`/`currentState()` in `TestContent()` — test asserts `source.currentStateCalls == 0` to prove disabled widget never touches data layer
* Gating `refresh(force=false)` calls with `if (isXDataNeeded())` around the EXISTING call (not new toggle-specific logic) → re-enabling a tile with an already-stale throttle timestamp fetches immediately next visit, free correctness from reusing the throttle check as-is

### Didn't Work
* Deleting `ProductionRepository`/`ProductionSnapshot`/`ProductionSource` broke 6+ unrelated test files (`HomeTodaySectionTest`, `HomeHistorySectionTest`, `HomeWidgetUpdateTest`, `ModuleHealthTileTest`) that only referenced them to satisfy `HomeFragment.sourceOverride` — a required test seam, not because they tested production behavior. Removing a companion-object seam ripples to every test file that ever stubbed it just to launch the fragment
* Manual `curl` against the local `ema-api-stub` to "check connectivity" during Maestro debugging silently consumed a scenario interaction (per-ECU cursor is a single server-side counter) — next Maestro run got a stale-cursor 409 mismatch, looked like a real regression
* Removing the `getCurrentProduction`/minutely fetch from the app broke the shared `ema-api-stub` "Good Data" fixture (`203000001234.json`): its `minutely` interaction was still `interactions[0]` in the strict-sequential per-ECU matcher, but nothing calls minutely anymore — the app's first real request (hourly) permanently mismatched cursor 0, and `POST /__stub__/reset` doesn't help since cursor 0 is STILL minutely after reset
* `./gradlew run` in `ema-api-stub` loads resources from disk at JVM start — editing the scenario JSON after the server is already running does nothing; must kill and restart the process (`netstat -ano | grep 8080` → `taskkill //F //PID`) to pick up fixture changes

### Avoid
* Before running Maestro flows against the local stub, always `POST /__stub__/reset` immediately before the run — not just once at setup — any manual curl/debug request in between silently advances the per-ECU cursor
* When removing a feature that was the FIRST call in a shared record/replay fixture's interaction order, the fixture's interaction list must be reordered/trimmed to match the new real call sequence — a strict-sequential matcher has no tolerance for a skipped leading interaction, even after reset
* When deleting a data class/interface that backs a Fragment's test-injection companion seam, grep for the seam name (`sourceOverride`, etc.) across ALL test files, not just the ones that test the removed feature directly — other tests use it purely to satisfy a constructor requirement to launch the fragment at all

## 2026-07-06: ema-widgets (Glance home-screen widgets, full change)

### Went Well
* Compose-compiler plugin version pinned EXACTLY to resolved Kotlin stdlib version (2.2.10) — Glance/Compose built clean first try on AGP 9.2.1 built-in-Kotlin, no separate Kotlin plugin needed
* `.editorconfig` `ktlint_function_naming_ignore_when_annotated_with = Composable` — fixes ktlint flagging PascalCase `@Composable` fns; don't rename functions to satisfy lint
* `androidx.glance.material3.ColorProviders(lightColorScheme())` / `(darkColorScheme())` single-scheme overload forces fixed light/dark regardless of system theme; default `GlanceTheme{}` (no args) already day/night-aware — needs `glance-material3` + `compose.material3:material3` deps
* Official `androidx.glance:glance-appwidget-testing` + `glance-testing` libs: `runGlanceAppWidgetUnitTest { setContext(); setAppWidgetSize(DpSize); provideComposable{ widget.TestContent() }; onNode(hasText(...)).assertExists() }` — real Robolectric widget-content testing, no manual Compose test harness needed
* `onAllNodes(matcher).assertCountEquals(n)` when >1 node matches same text (e.g. two "0.00 kWh" figures) — `onNode` throws on ambiguous match
* Function-reference test seams (`var updateAllAction: suspend (Context, List<GlanceAppWidget>) -> Unit`) on `WidgetRefreshWorker`/`HomeFragment`/`SettingsFragment` — needed because Glance's real `updateAll()` is a no-op with zero placed widget instances in Robolectric, so can't spy on it directly
* `SettingsFragment.hourlyRepoOverride`/`dailyRepoOverride` companion seams (mirrors `HomeFragment`'s existing pattern) — let settings-integration tests inject a `FakeClient`-backed repo instead of fighting real MockWebServer + IO-dispatcher timing in a Fragment test
* `NavigationUI.onNavDestinationSelected(item, navController)` + also setting `bottomNav.selectedItemId` for widget tap-target routing in `MainActivity` — deterministic in Robolectric AND keeps the tab visually highlighted on a real device

### Didn't Work
* Design doc said "MockWebServer matching HourlyEnergyRepositoryTest's style" — that referenced test actually uses a plain `FakeClient` (no sockets); took the actual style over the literal wording
* First `runGlanceAppWidgetUnitTest(DpSize(...))` positional arg → "actual type DpSize, but Duration expected" (first param is a timeout, not size); size is set via `setAppWidgetSize()` inside the test block, not a constructor arg
* Reused `activity` var after `Robolectric ActivityController.recreate()` → stale reference, `findFragmentById` returns null/NPE; must re-fetch via `controller.get()`
* `WidgetUpdater.updateAll(context, widgets)` with real `GlanceAppWidget` instances as a "spy" (counting inside `provideGlance`) → count stays 0 in Robolectric since no glance ids are placed, so `provideGlance` never runs

### Avoid
* Don't try to observe Glance's real `updateAll()`/`provideGlance()` side effects as a test double in Robolectric with zero placed widgets — inject a function-reference seam at the call site instead
* Don't trust a design doc's literal tool name ("MockWebServer") over what the referenced example test file actually does — read the cited test first
* Don't reuse a pre-recreate Activity/Fragment reference after `ActivityController.recreate()` — always re-fetch from the controller

## 2026-07-04: a-home-screen CI flake — "8000 W" race after stub switch

### Went Well
* `ColorBuffer` emulator error + abnormal 1m42s flow duration (vs 23-29s others) pointed at CI resource contention, not app logic — cross-checked against known `ReactiveCircus/android-emulator-runner` ColorBuffer reports
* Reading `HomeFragment.onViewCreated`/`onResume` confirmed the exact race: `render(source.currentState())` shows stale persisted value synchronously, "8000 W" only lands after async `refresh()` completes a real HTTP round-trip

### Didn't Work
* Bare `assertVisible: text: "8000 W"` right after switching to the local stub — Maestro's default `assertVisible` timeout is 7s, too tight for a real network fetch under CI load (flow's own other checks already use 10-15s `extendedWaitUntil`)

### Avoid
* Any Maestro assertion on data that arrives via async `refresh()` (not the synchronous `currentState()` seed) needs `extendedWaitUntil` with a real timeout, not bare `assertVisible` — the tile being visible does NOT mean its data has finished refreshing
* Don't chase a `ColorBuffer`/GPU emulator error as the root cause before checking for an ordinary async-timing race in the flow itself — the two often co-occur because both stem from CI load, but only one is fixable in the repo

## 2026-07-04: ema-stub-integration (composite build + cleartext + embedded stub)

### Went Well
* `includeBuild("../ema-api-stub")` in ema-companion's `settings.gradle.kts` + `testImplementation("com.schlueternetz.emaapistub:ema-api-stub:1.0")` in `app/build.gradle.kts` — Gradle dependency substitution resolves the sibling standalone project cleanly, no version conflicts
* Promoting ema-api-stub's ktor/kotlinx-serialization deps `implementation`→`api` (needed `` `java-library` `` plugin added alongside `kotlin("jvm")`, which alone has no `api` config) — exposes `MatchingEngine`/`ScenarioLoader`/`stubModule`'s own dependency types to the Android app's compile classpath
* Real `embeddedServer(CIO, port = 0)` bound to an ephemeral port (same pattern as GreenMail `ServerSetup(0, ...)`) for Android-side embedded-stub tests — `OkHttpEmaApiClient` needs an actual socket; Ktor's `testApplication` in-memory test client only works for Ktor's own `HttpClient`, not raw OkHttp
* Reading the bundled scenario file via `getResourceAsStream` + `kotlinx.serialization.decodeFromString<Scenario>` instead of `ScenarioLoader.loadDefault()` in Android tests — works whether the resource is exploded or inside a jar
* Screenshot from Maestro debug artifacts caught two distinct real bugs in one look each: element needing scroll, then a real network error banner (not a flaky assertion)

### Didn't Work
* `ScenarioLoader.loadDefault()` throws `IllegalArgumentException: URI is not hierarchical` when the stub's resources arrive as a packaged jar (composite-build project dependency, not `gradlew test`'s exploded `build/resources/main`) — `File(url.toURI())` can't handle a `jar:` URI
* Maestro flow taps `setting_use_local_stub` without scrolling first — it's below the Base URL row, off-screen; `assertVisible`/`tapOn` on an off-screen-but-present element fails
* After tapping the local-stub action, the flow tried to edit ECU-ID (Solar Array card, near top) without scrolling back up — the local-stub tap left the API Settings card (further down) in view
* App had zero `network_security_config` / `usesCleartextTraffic` anywhere — `http://10.0.2.2:8080` request from the emulator silently failed as a generic NetworkError (Android blocks cleartext HTTP by default at targetSdk 28+); looked exactly like a real fetch failure, not a config gap, until checked

### Avoid
* Don't assume "embed the real engine, no real socket" from a design doc applies uniformly — a client built on raw OkHttp always needs a real (even if in-process/ephemeral) socket; only a pure-Ktor-to-Ktor test can truly avoid one
* Don't use `ScenarioLoader.loadDefault()` from any consumer that isn't guaranteed an exploded resources directory — packaged-jar consumers must read the resource as a stream instead
* Don't add a debug-only "point at localhost/10.0.2.2" shortcut without also adding a debug-only network security config permitting cleartext to that specific host — targetSdk 28+ blocks it silently and the failure looks identical to an unrelated NetworkError
* When a Maestro step taps an action that scrolls the page (or is itself reached by scrolling), always re-`scrollUntilVisible` before the next tap that assumes a different part of the page — one card's action can leave a different, distant card in view

## 2026-07-03: home-screen Maestro flow (SettingRowView + driver ordering)

### Went Well
* `hideSoftInputFromWindow(windowToken, 0)` in `exitEditMode()` — dismisses keyboard explicitly after each save; without it keyboard stays open and shifts Maestro index-based selectors
* `requestFocus()` in `enterEditMode()` — connects IME to the correct `TextInputEditText`; without it `hideSoftInputFromWindow()` has the wrong window token and can't close the keyboard
* Screenshot from Maestro debug artifacts (`commands-(flow).json` + `.png`) — revealed exact failure step and state in one read; always check `C:\Users\micro\.maestro\tests\<timestamp>\` first
* `commands-(flow).json` `duration` field — `tapOn` taking 11700ms confirmed driver was under load before `inputText` timed out; much faster diagnosis than re-running
* Naming flow file `a-home-screen.yaml` (alphabetically first) — runs before other flows stress the Maestro driver; all 3 flows pass consistently

### Didn't Work
* `requestFocus()` in `enterEditMode()` causes gRPC `DEADLINE_EXCEEDED` on `inputText` when flow runs AFTER other flows — keyboard animation triggered by `requestFocus()` + stressed driver = `ACTION_SET_TEXT` deadlocks for 120s
* `hideKeyboard` in Maestro after each save — Maestro presses Back when keyboard is already gone, navigating app to the launcher; screenshot showed home screen of emulator, not the app
* `waitForAnimationToEnd` before `inputText` to let keyboard settle — didn't fix the gRPC deadlock; driver was stuck, not slow
* `waitForAnimationToEnd` at end of `email-alerts.yaml` to drain driver — didn't fix it either; driver stays in degraded state regardless
* `adb reboot` when emulator is frozen — timed out (2m) with no response; need `Get-Process qemu-system-x86_64 | Stop-Process -Force` then re-launch emulator
* Running many suite iterations with 120s gRPC timeouts — accumulates emulator degradation; cold-boot all flows eventually fail

### Avoid
* `hideKeyboard` in a Maestro flow after saving a `SettingRowView` — `exitEditMode()` already calls `hideSoftInputFromWindow()`; if keyboard is already gone, Maestro's `hideKeyboard` presses Back and sends app to launcher
* Asserting `hourly_placeholder` / `history_placeholder` in Maestro — these are `gone` by default; once a fetch is attempted (even failed) the chart renders with MPAndroidChart's empty-state message; assert `hourly_chart` / `history_chart` instead
* Running `home-screen` (Settings form-filling) after any other Maestro flow — Maestro accessibility driver becomes unresponsive to `ACTION_SET_TEXT` after clearState+relaunch cycles; flow must be named to sort first alphabetically
* Removing `requestFocus()` from `SettingRowView.enterEditMode()` to fix Maestro timing — without it `hideSoftInputFromWindow(windowToken, 0)` fails silently (IME connected to different window), keyboard stays open, index-based selectors break for all fields after the first
* Trusting emulator after many failed Maestro runs (each with 120s timeout) — kill emulator process and cold-boot; `adb reboot` may not work if frozen

## 2026-07-01: production-data-and-graphs (charts + new repos)

### Went Well
* Default interface methods (`= HourlyEnergyFetch(ApiResult.ConfigurationError)`) on new `EmaApiClient` methods — zero existing FakeClients broke
* Per-repo prefs for throttle timestamps instead of shared `ApiUsageRepository` — repos stay independent, no throttle cross-contamination
* `currentHourOverride: Int?` companion seam in `HomeFragment` — chart tests deterministic at any wall-clock hour
* `HourlyEnergySource` / `DailyEnergySource` interfaces for Fragment seams — clean Robolectric injection without heavyweight repo constructors
* `ktlintFormat` auto-fixed all violations in one pass after writing new files
* `getDailyEnergy` making one HTTP call per unique calendar month internally — keeps repository interface clean while matching real API (`yyyy-MM` date range format)

### Didn't Work
* `android:flexWrap="wrap"` on `LinearLayout` — resource linker error; attribute only valid on `FlexboxLayout`
* Test clock `now = 1_000_000L` (1s) is below `THROTTLE_MS = 3_600_000L` — all 7 hourly throttle tests failed until raised to `3_700_000L`
* Seeding `SharedPreferences` AFTER constructing `DailyEnergyRepository` — `loadDays()` runs in constructor so in-memory cache was empty; all "past days cached" tests failed
* Comparing dash characters with string literals in tests (`"–"` en-dash vs `"—"` em-dash in strings.xml) — use `fragment.getString(R.string.xxx)` instead
* First `HourlyEnergyRepository` implementation used `ApiUsageRepository.getLastFetchEpochMs()` for hourly throttle — production throttle and hourly throttle shared state

### Avoid
* `android:flexWrap` on `LinearLayout` — use plain `LinearLayout` for month legend; 2 months max so one row is fine
* Test `now` below the throttle window — must be `>= THROTTLE_MS` for first-call tests to pass through the gate
* Seed SharedPreferences BEFORE constructing repos — `loadDays()` / `loadSnapshot()` run in the constructor
* Literal dash characters in test assertions — always `getString(R.string.x)` for any string defined in resources
* Storing per-repo throttle in shared `ApiUsageRepository` — each repo owns its own `KEY_LAST_FETCH` in its own prefs file
* Binding `LineChart` to `LocalTime.now().hour` without an injectable seam — makes tests flaky (fail at night when `currentHour < 6`)

## 2026-06-27: Maestro permission dialog + GreenMail port

### Went Well
* Screenshot (adb screencap → pull) confirmed root cause immediately: POST_NOTIFICATIONS dialog visible, bottom-nav present but Maestro couldn't see it
* `runFlow when: visible: id: "com.android.permissioncontroller:id/permission_allow_button"` — locale-independent, skips gracefully when permission already granted
* GreenMail `ServerSetup(0, "127.0.0.1", PROTOCOL_SMTP)` → `greenMail.smtp.port` gives actual bound port after `start()`; one-line fix

### Didn't Work
* `extendedWaitUntil 60s` masked not ONE failure mode but TWO: ANR dialog (previous bug) AND permission dialog (this bug) — both have the same symptom (`settingsFragment not visible`) but different fixes

### Avoid
* `ActivityResultContracts.RequestPermission().launch()` starts a `com.android.permissioncontroller` activity in the FOREGROUND — app goes to background; Maestro cannot see ANY app views while this dialog is showing, even though the bottom-nav is visually below it
* Never hardcode GreenMail port (e.g. 3025) — use port 0 so the OS picks a free ephemeral port; hardcoded ports cause `Address already in use` on CI or parallel test runs
* Maestro `runFlow when: visible` checks the condition ONCE at execution time — place it immediately after `launchApp` so the dialog has had a chance to appear before the check runs
* `pm clear` (used by Maestro `clearState: true`) resets runtime permissions on API 33+ — every fresh-state flow run will trigger the POST_NOTIFICATIONS dialog on Android 13+ unless the flow dismisses it

## 2026-06-25: module-health-emails (Phases 6–7)

### Went Well
* `emailSenderOverride` companion seam in `ModuleHealthWorker` mirrors `repoOverride` — same pattern, zero friction
* `FakeEmailSender` with `sentCount` + `lastSubject` + `nextResult` covered all 5 email worker tests cleanly
* `SettingsRepository.create(context)` falls back to plain SharedPreferences in Robolectric (keystore unavailable) — seeding via `getSharedPreferences("ema_companion_settings")` works transparently
* `suppressEmailSwitchListener` flag on `MaterialSwitch` prevents cascade when `updateEmailAlertsDisplay()` sets `isChecked` programmatically
* Inline setup section (LinearLayout in card, visibility toggle) simpler to test than a dialog — no `ShadowDialog` traversal needed
* `shadowOf(fragment.requireActivity()).nextStartedActivity` for Robolectric intent assertions on Fragment-fired intents

### Didn't Work
* Worker email tests initially called `repo.setEmailAddress()` — that method is on `SettingsRepository`, not `ModuleHealthRepository`; caught at compile time

### Avoid
* Email credential setters live on `SettingsRepository`, not `ModuleHealthRepository` — don't conflate the two repos in tests
* Any `MaterialSwitch` set programmatically inside `refreshAllDisplayedValues()` needs a suppress-listener guard; without it the listener fires and re-writes prefs on every import/reset
* PostToolUse hook fires per-edit on every UX file — batch all layout/string/fragment edits, invoke `write-user-guide` exactly once at the end (existing lesson, re-confirmed)

## 2026-06-24: WorkManager periodic task force-run limitations

### Went Well
* WorkManager DB lives in `no_backup/androidx.work.workdb`, not `databases/` — check there when diagnosing
* Job scheduler historical stats (`dumpsys jobscheduler`) show START/STOP timestamps: near-instant stop (< 20ms) = `doWork()` never ran; WorkManager called `jobFinished()` immediately
* Notification channel + POST_NOTIFICATIONS grant can be confirmed without a real notification via `dumpsys notification --noredact`

### Didn't Work
* `cmd jobscheduler run -f <pkg> <jobId>` does NOT reliably execute `doWork()` for `PeriodicWorkRequest` — WorkManager's SystemJobService checks its own internal state machine and calls `jobFinished()` immediately if the work spec is not in ENQUEUED state (e.g. between periods)
* Each force-run starts a new job ID (1→2→3) — re-querying `dumpsys jobscheduler` for the ID is needed each time, but this is beside the point since none of them ran the worker

### Avoid
* Don't use `cmd jobscheduler run -f` to test WorkManager workers — use an instrumented test with `WorkManagerTestInitHelper` and `TestDriver.setAllConstraintsMet()`, or wait for the real scheduled time
* Don't write XML directly via `run-as tee` to seed prefs while the app is running — the in-memory cache isn't updated; force-stop first, seed, then launch

## 2026-06-23: module-health-tile integration + factory reset tests

### Went Well
* `ModuleHealthRepository` direct constructor (not `forTest`) gives full prefs isolation in integration tests — pass isolated SharedPreferences for health, daily, and log
* MockWebServer enqueue order matches fetch order: repo fetches dates oldest-first (`dayBefore → yesterday → today`), so enqueue responses in that same order
* `server.requestCount` is the simplest way to assert "only N API calls made" in an integration test — no fake client needed
* Factory reset already cleared `PREFS_DAILY` in `SettingsFragment.showFactoryResetDialog()` — test just confirmed it; no production change needed
* `Dispatchers.Unconfined` on `OkHttpEmaApiClient` makes socket calls inline in tests — no coroutine timing issues

### Didn't Work
* `SettingsFragmentTest.setUp()` was missing `ema_module_health_daily` clear — cross-test leakage risk from the daily cache prefs

### Avoid
* When adding a new SharedPreferences store to any repo, add it to BOTH `SettingsFragmentTest.setUp()` (test isolation) AND `showFactoryResetDialog()` (production reset) — missing either causes subtle bugs
* `ModuleHealthRepository.forTest()` uses `ApiCallLogRepository.create(context)` which shares the global `ema_api_log` prefs — use direct constructor for full test isolation when log contents matter

## 2026-06-23: throttle-reset abstraction (ThrottleResettable)

### Went Well
* Root cause obvious from reading `SettingsFragment`: `invalidateApiThrottle()` only reset `ApiUsageRepository`, no equivalent call existed for `ModuleHealthRepository`'s `KEY_LAST_CHECK`
* `ThrottleResettable` interface in `core/api/` + `listOf(usageRepository, moduleHealthRepository).forEach { it.resetThrottle() }` = all future tiles just implement the interface — zero manual hookup in fragment
* Moving throttle-reset logic into the repo (`resetThrottle()`) keeps the fragment thin and the repo self-contained — better than fragment calling multiple setter methods
* Adding `ema_module_health` clear to `SettingsFragmentTest.setUp()` avoids cross-test leakage from the new prefs

### Didn't Work
* Nothing notable — straightforward interface extraction

### Avoid
* Each new tile repo must implement `ThrottleResettable` and be added to `tileRepositories` in `SettingsFragment.onViewCreated` — forgetting this repeats the exact bug that was just fixed
* `SettingsFragmentTest.setUp()` must clear every SharedPreferences file that SettingsFragment touches — add new prefs stores to setUp when adding tile repos

## 2026-06-17: import path skipped throttle reset

### Went Well
* On-device diagnosis from plain prefs: `run-as <pkg> cat .../shared_prefs/ema_api_usage.xml` — `lastFetchEpochMs` still old value (not 0) PROVED `invalidateApiThrottle()` never ran, before touching code
* Compared timestamp delta (now − lastFetch = 870 min ≫ 10-min throttle) to rule out "throttle still active" as the cause

### Didn't Work
* `invalidateApiThrottle()` lived ONLY in per-field `onSave`; import (`handleImport`→`refreshAllDisplayedValues`) changed connection creds but never reset throttle/error → next Home visit honored stale throttle, no fetch. Fix: call it in `refreshAllDisplayedValues()` (shared by import + factory reset)
* User "invalid credentials" did nothing because `SettingRowView.attemptSave` gates `onSave` behind `validator` — invalid-FORMAT input never saves, so never invalidates; need format-valid-but-wrong creds to exercise the error path

### Avoid
* New mutation belongs at EVERY entry point of a state change, not just the per-field edit — import/factory-reset are config-change paths too (spec: changing a connection setting clears throttle)
* Import with blank/absent creds → `isConfigured()` false → silent `ConfigurationError` (no log, no error, no count), NOT an auth error — expected, don't chase it as a bug
* Behavior-only fix in a Fragment ≠ UX change — skip `write-user-guide` (no layout/menu/nav/string/visible change) despite the PostToolUse hook reminder

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

## 2026-06-11: Robolectric appcompat AlertDialog + nav orphaned back stack (condensed)

### Avoid
* Appcompat `AlertDialog` in Robolectric: use `ShadowDialog.getLatestDialog()` cast to `androidx.appcompat.app.AlertDialog` (not `ShadowAlertDialog`/platform class); needs `Robolectric.buildActivity(AppCompatActivity)`, not Application context
* Don't push a gated screen over the start destination — breaks `popUpTo`; set it AS start destination instead
* `bottomNav.selectedItemId = id` alone doesn't fire the nav listener in Robolectric (re-confirmed 2026-07-06) — test/drive via `NavigationUI.onNavDestinationSelected`
* Batch UX edits; invoke `write-user-guide` once at end, not per edit (recurring lesson — PostToolUse hook fires per-edit, not per-task)
