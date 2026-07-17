# AI Lessons Learned

## 2026-07-17: configurable-tiles-widgets Maestro verification (task 1.14)

### Went Well
* Ran /qa's steps manually (fast checks already green, installDebug, maestro test in CI order) to close deferred task 1.14
* Full suite (a-home-screen, bottom-nav, email-alerts) passed 3/3 after 2 retries — env flakes, not a real regression
* 1st fail: Pixel Launcher ANR mid-flow, not just at launch — same class as 2026-07-14 lesson but hit past the launch-only dismiss guard
* 2nd fail: App-ID inputText took 18s (vs <1s normal) — corrupted/incomplete text under driver load; save silently no-op'd (validator rejected it), flow moved to next field without checking it stuck
* Screenshot showed Settings tab still selected (not Home) after "Tap on homeFragment... COMPLETED" — Home tab disabled because isConfigured() false, tapping a disabled tab is a no-op; decisive signal it was upstream input corruption not a nav bug
* Clean retry (no concurrent Gradle/python procs) passed first try — bare retry sufficient, no code fix needed
* Maestro CLI not on PATH in PowerShell; at ~/.maestro/bin, works via Bash with PATH export — saved to cross-session memory (reference-maestro-cli.md), was searched for from scratch this session

### Didn't Work
* python3 (WindowsApps alias) couldn't open a Maestro debug json path with literal parens (`commands-(flow).json`) even with full path — Read tool worked fine instead
* Maestro heartbeat "file locked" errors spammed the log every ~5s — didn't fail the flow, stale KeyValueStore lock noise, not a real error signal

### Avoid
* Don't treat one Maestro flow failure as a real regression without a screenshot check — ANR dialogs and driver-under-load input corruption both look like "assertion false" with no other clue; check `C:\Users\micro\.maestro\tests\<timestamp>\` first every time
* Don't assume corrupted input = wrong text saved — validator silently REJECTS malformed input (field stays "Required"), flow has no per-field save assertion; a stressed driver corrupting ONE early field can cascade into a much-later, confusingly-unrelated failure
* Don't re-search for maestro CLI location every session — now in memory (reference-maestro-cli.md); check memory before `which`/`find`

## 2026-07-16: notification-alert-levels docs tasks (5.1/5.2 already done, just unchecked)

### Went Well
* `git log -- <file>` + `git merge-base --is-ancestor <commit> HEAD` proved ADR-008 and both settings.md/settings-de.md were already fully rewritten and on `main` (commit `b832875`) before touching anything — read actual file content too, not just git log, to confirm content matched the task description
* Ticked both checkboxes with zero code/doc changes instead of redoing already-correct work

### Didn't Work
* N/A — caught before any wasted edit

### Avoid
* Don't assume an unchecked `tasks.md` box means work is undone — a prior session can finish real work and forget to flip the checkbox; verify current file state + git log before implementing "from scratch"

## 2026-07-16: ci-6hr-hang (raw Thread + WorkManager deadlocks Robolectric SQLite)

### Went Well
* CI run showed `conclusion: cancelled` not `failure` on `testDebugUnitTest` — 6h wall time = GitHub's default job timeout killed it, not a real assertion failure; distinguishing signal from `failure`-conclusion runs
* Reproduced locally via loop: `timeout 50 ./gradlew test --tests X --rerun` repeated ~15x, hang hit on attempt 2-3 — matches "verified via 8x rerun, still shipped a hang" from prior session, more reps needed to catch a rare deadlock
* `jstack <GradleWorkerMain pid>` mid-hang (found pid via `jps -l`) was decisive: "SDK 33 Main Thread" parked in the test's OWN `runBlocking { firstStarted.await() }`, while the spawned raw `Thread` was RUNNABLE-but-stuck inside `WorkTagDao_Impl` → `SQLiteCursor.getDatabase()` (Robolectric shadow) — real deadlock, not coroutine-cancellation flakiness the previous fix assumed
* Root cause: Robolectric's whole test method runs on ONE simulated "main thread" (`Sandbox.runOnMainThread`); WorkManager's `SynchronousExecutor` makes `enqueueUniqueWork`'s Room/SQLite bookkeeping run synchronously on whichever thread calls it — calling it from a second raw `Thread` starves Robolectric's SQLite shadow of the main-thread Looper it needs, while that main thread sits blocked waiting on the raw Thread's signal — circular wait
* Fix: delete the raw `Thread`, call `ApiSyncScheduler.requestResyncAfterSettingsChange()` directly on the test's own (Robolectric main) thread — safe because `CoroutineWorker.doWork()` itself still dispatches onto `Dispatchers.Default` (no `setWorkerCoroutineContext` override), so the enqueue call returns as soon as the worker *starts*, well before `refresh()` reaches `gate.await()`
* Verified: 15x local rerun loop clean after fix (vs. hung by attempt 2-3 before)

### Didn't Work
* Prior session's "flaky-race fix" (`999f545`, see 2026-07-15 entry) added a `withTimeout(5_000)` guard against the wrong race — real bug was never coroutine-cancellation timing, it was the raw `Thread` touching WorkManager/Room at all; the timeout couldn't fire because the hang was upstream of it, inside the enqueue call itself
* "8x local rerun + real CI green" (previous session's confidence marker) wasn't enough reps to catch this — deadlock only manifested every 2nd-4th run per this session's own loop

### Avoid
* Never call `WorkManager.enqueueUniqueWork` (or anything touching Room/SQLite through it) from a raw `Thread`/non-main thread in a Robolectric test — Robolectric's SQLite shadow needs the paused main-thread Looper serviced; only the Robolectric-designated main thread can do that, a spawned Thread deadlocks against it
* Don't trust "N passing reruns" as proof a concurrency test is deadlock-free — a low-frequency real deadlock (not just an assertion race) needs many more reps (10-15+) and ideally a `jstack` capture plan before declaring it fixed
* When a CI job's `conclusion` is `cancelled` (not `failure`) after run duration ≈ the platform's default job timeout (6h on GitHub Actions), suspect a genuine hang/deadlock, not a flaky assertion — go straight to reproducing + thread-dumping rather than re-reading the last diff for logic bugs

## 2026-07-15: ci-two-bugs-root-cause (gh unavailable + Maestro index regression)

### Went Well
* curl + `$GITHUB_TOKEN` (no `gh`) pulled workflow runs/jobs/artifacts fine — confirms 2026-07-14 lesson still holds
* Per-commit CI run history showed failures started exactly at the gradle-wrapper/okhttp/robolectric/kotlin dependabot merge commit, not at any of the 4 "fix tests" commits after it
* `commands-(flow).json`'s `metadata.sequenceNumber` field (not array order) gives true execution order + per-step status/duration — raw array order in the JSON is scrambled
* Failed step's `error.hierarchyRoot` showed only 3 `setting_edit_button` nodes present (not 5) — rows scrolled off the TOP of the viewport are missing from the tree entirely, not just invisible
* Diffed the same step against an older pre-regression artifact (2026-07-13, same flow) — identical `tapOn` COMPLETED there, proving a real regression, not inherent flakiness
* Root fix: `childOf: id: <rowId>` selector instead of `index: N` for every `setting_edit_button` tap — scopes to the specific row's subtree regardless of scroll position, kills the whole fragility class instead of patching one occurrence

### Didn't Work
* Tried `gh run list` first despite this exact gap already being documented in this file's 2026-07-14 entry — wasted a turn, user had to redirect
* Assumed "Element not found" for `setting_edit_button` meant an app crash/ANR — hierarchy dump showed a normal, correctly-rendered screen; `taskbar_container`/`navbuttons_view` nodes are just persistent tablet gesture-nav chrome, not a launcher-foreground signal

### Avoid
* Don't reach for `gh` in this repo, ever — not installed; use `curl -H "Authorization: token $GITHUB_TOKEN"` against api.github.com instead (also in cross-session memory)
* Don't trust a `sort -u` resource-id list to mean "only one instance exists" — count actual occurrences (`grep -c`) before concluding "index out of range" vs "element truly absent"
* Don't use index-based Maestro selectors across a scroll that only guarantees the TARGET element visible — elements above/below it can silently drop from the accessibility tree; use `childOf: id:` to scope to a specific row's container instead

## 2026-07-14: ci-maestro-anr (Pixel Launcher ANR blocks all 3 flows, 8 consecutive CI failures)

### Went Well
* `maestro-debug` artifact (screenshots + commands-*.json + maestro.log) already uploaded on failure — didn't need to add anything to see the failure, just had to go look at it
* Screenshot alone was decisive: "Pixel Launcher isn't responding" system dialog sitting on top of an already fully-rendered Settings screen — proved app was fine, launcher process was the ANR'd one, not the app
* Fix: `runFlow: when: visible: id: "android:id/aerr_close"` block, same shape as the existing POST_NOTIFICATIONS-dialog dismissal, added to all 3 flow files right after the permission-dialog check

### Didn't Work
* `pyyaml` not installed by default in this shell — `pip install pyyaml` needed before `yaml.safe_load` could confirm the edited flow files still parse

### Avoid
* Don't assume "CI Maestro failure" = flaky/needs-more-timeout without first pulling the actual `maestro-debug` artifact from the failed run — the specific dialog only became clear from the screenshot + hierarchy dump
* A system dialog covering a fully-rendered, correct app screen is not an app bug — check what process the dialog's title names before assuming the assertion target itself is broken
* The launch-time ANR guard only fires once, right after `launchApp` — the same ANR can also appear mid-flow later on a loaded machine (see 2026-07-17 entry); a single `runFlow: when:` check doesn't protect the whole flow

## 2026-07-14: widget-preview-crash (RemoteViews rejects View/Space in previewLayout)

### Went Well
* Force-stop launcher + `adb shell input` long-press-home → Widgets → tap app row + screencap = full repro of widget-picker bug with zero manual interaction, no Maestro needed
* `InflateException: Class not allowed to be inflated android.view.View` in logcat = decisive; static analysis (resource compile, string refs, theme attrs) all looked fine and would never have caught this
* Fix verified twice: `:app:processDebugResources` (compiles) AND re-repro on-device after `installDebug` + launcher force-stop (actual preview renders) — resource compile passing does NOT mean RemoteViews will accept the layout

### Didn't Work
* Assuming `previewLayout` XML is inflated by a normal `LayoutInflater` — the widget picker actually inflates it via `RemoteViews`, which enforces a hidden view-class allowlist
* `<View>` and `<Space>` are NOT in that allowlist — both throw `Class not allowed to be inflated`, silently breaking ALL THREE widgets identically since they shared the same pattern

### Avoid
* Never use plain `<View>` or `<Space>` in an `android:previewLayout` XML — use `<ImageView android:background="...">` for solid-color bars, and `layout_marginBottom`/`layout_marginTop` instead of `<Space>` for gaps
* `:app:processDebugResources` passing is necessary but NOT sufficient to prove a previewLayout works — always do one live on-device check after writing/editing one

## 2026-07-14: notification-alert-levels (Off/Alerts Only/All for push + email)

### Went Well
* Shared `AlertLevel` enum + one pure `shouldAlert(level, previous, new)` fn reused for both push and email gating in `ModuleHealthWorker` — one truth source, plain-JUnit testable, no Robolectric needed
* Lazy read-time migration in `SettingsRepository` getter — old boolean key read once, translated, written under new key; zero explicit "migration step"
* `android:id/text1` (Android's own internal `select_dialog_item` row ID) + Maestro `index:` — locale-independent way to tap `AlertDialog.setItems()` rows

### Didn't Work
* First Maestro fix used `tapOn: text: "Alerts Only"` — emulator's system locale was German, dialog showed "Nur Warnungen", flow failed
* Own debugging (`adb shell am start` + `uiautomator dump`) triggered a real Home fetch against the local `ema-api-stub` between two Maestro runs — desynced its per-ECU cursor, next run failed looking like a regression
* Removing a boolean setting's persisted key outright meant `SettingsRepository`, `ModuleHealthWorker`, and `SettingsFragment` all had to move together in one pass before ANY layer would compile again

### Avoid
* Never use Maestro `text:` selectors for content whose locale isn't pinned — dump the real UI hierarchy and use a stable resource ID instead
* Don't run ad hoc `adb shell am start`/UI-inspection commands against the local stub between Maestro runs without `POST /__stub__/reset` right after
* Before assuming a setting already gates behavior because it's persisted + tested + UI-wired, check whether the actual consumer (a Worker's `doWork()`) reads it at all

## 2026-07-13: widget-preview skill (fix robot-head icon in widget picker)

### Went Well
* minSdk=31 means `android:previewLayout` (API 31+) always applies — no previewImage bitmap fallback needed, skip emulator/screenshot pipeline entirely
* Root cause of robot head: `previewImage` pointed at `ic_launcher_foreground`, an adaptive-icon foreground layer, not a real static bitmap
* Static plain-View XML layout themed via `android:theme="@style/Theme.EMACompanion"` on root — matches app's Material3 DayNight palette without needing Glance at all

### Didn't Work
* First pipe-test of a hook printed nothing for "matching" paths — synthetic test JSON itself was invalid (`\p`, `\e` not legal JSON escapes in a Windows path inside single-quoted bash)

### Avoid
* Don't hand-write a Windows path into a bash single-quoted JSON test string with single backslashes — JSON.parse throws before the hook logic ever runs
* UX-file hook and widget-file hook both fire on `res/values/strings.xml` — for OS-chrome-only content, judge and skip write-user-guide rather than invoking it reflexively

## 2026-07-13: support-buy-me-a-coffee (4th bottom-nav tab + email footer links)

### Went Well
* Reused `MainActivity.applyUnconfiguredNavState()` pattern instead of new logic — static screen needs no EMA config
* Skipped adding a Maestro flow that taps BMAC/website buttons — would fire real `ACTION_VIEW` intent and send emulator to external browser mid-flow; covered at Robolectric layer instead

### Didn't Work
* `a-home-screen.yaml` failed on `"0.42" is visible` — root cause was `ema-api-stub` server simply not running (nothing on port 8080), unrelated to this change

### Avoid
* Don't assume `ktlintCheck` is still only linting `.kts` — verify per-run instead of trusting a stale note
* Before blaming a Maestro flow failure on your own change, check whether it depends on infra you didn't touch (e.g. `ema-api-stub` on port 8080) — a connection-refused check is faster than re-reading unrelated code

## 2026-07-12: configurable-tiles-widgets (remove Current Production + tile/widget visibility settings)

### Went Well
* `core/HomeTile.kt`/`core/HomeWidget.kt` enums in `core/` not `feature/settings/` — used by 3 features, ADR-004 rule applies
* `SettingsRepository.isHourlyDataNeeded()`/`isDailyDataNeeded()`/`isModuleHealthDataNeeded()` as single source of truth for gating — Today Production tile counts as consumer of BOTH hourly and daily (best-day cards use daily) even though its own data is hourly
* `WidgetUpdater.enabledWidgets(settings): List<GlanceAppWidget>` exposed as pure testable fn instead of trying to spy on Glance's real `updateAll()`
* Gating `refresh(force=false)` calls with `if (isXDataNeeded())` around the EXISTING call (not new toggle-specific logic) → re-enabling a tile with an already-stale throttle timestamp fetches immediately next visit

### Didn't Work
* Deleting `ProductionRepository`/`ProductionSnapshot`/`ProductionSource` broke 6+ unrelated test files that only referenced them to satisfy `HomeFragment.sourceOverride` — a required test seam, not because they tested production behavior
* Manual `curl` against the local `ema-api-stub` to "check connectivity" during Maestro debugging silently consumed a scenario interaction — next Maestro run got a stale-cursor mismatch
* Removing the `getCurrentProduction`/minutely fetch broke the shared "Good Data" fixture: its `minutely` interaction was still `interactions[0]` in the strict-sequential matcher, but nothing calls minutely anymore — every real hourly request permanently mismatched cursor 0
* `./gradlew run` in `ema-api-stub` loads resources from disk at JVM start — editing the scenario JSON after the server is already running does nothing; must kill and restart the process

### Avoid
* Before running Maestro flows against the local stub, always `POST /__stub__/reset` immediately before the run — any manual curl/debug request in between silently advances the per-ECU cursor
* When removing a feature that was the FIRST call in a shared record/replay fixture's interaction order, the fixture's interaction list must be reordered/trimmed to match
* When deleting a data class/interface that backs a Fragment's test-injection companion seam, grep for the seam name across ALL test files, not just the ones that test the removed feature directly
