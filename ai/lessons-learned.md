# AI Lessons Learned

## 2026-06-11: settings-field-hints — Robolectric + appcompat AlertDialog

### What Went Well
* Adding `hintText: String?` as an optional property directly on `SettingRowView` was clean and minimal — no new classes, no breaking changes, existing usages unaffected
* 4 of 5 hint-related tests (visibility and edit-mode behaviour) passed immediately after implementation with no iteration needed
* ktlint passed first try; no style issues from the new property or import

### What Didn't Work (Obstacles & Roadblocks)
* `ShadowAlertDialog.getLatestAlertDialog()` returns **null** for dialogs built with `androidx.appcompat.app.AlertDialog.Builder` — Robolectric's static tracking in `ShadowAlertDialog` does not capture appcompat dialogs even though appcompat extends the platform class
* `is android.app.AlertDialog` and `as android.app.AlertDialog` both **fail** in Robolectric for `androidx.appcompat.app.AlertDialog` instances — the class-loader instrumentation breaks normal Java instanceof semantics for appcompat library classes
* `AlertDialog.Builder(ApplicationProvider.getApplicationContext())` produces a dialog that is never shown in Robolectric (ShadowDialog also returns null) — Application context cannot host a dialog window

### ⚠️ Mistakes to Avoid Next Time
* **Do not use `ShadowAlertDialog.getLatestAlertDialog()`** for appcompat dialogs — always use `ShadowDialog.getLatestDialog()` and cast to `androidx.appcompat.app.AlertDialog` (not `android.app.AlertDialog`)
* **Do not use Application context for AlertDialog tests** — use `Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()` and set the theme on the activity before creating any view that will show a dialog
* **To read dialog message text in Robolectric**, use `dialog.window?.decorView?.findViewById<TextView>(android.R.id.message)` — the Robolectric shadow message APIs are unreliable for appcompat dialogs
* When adding `hintText` (or similar dialog-triggering properties), set `label` **before** `hintText` in `SettingsFragment` wire methods — the click listener captures `label` at the time `hintText` is set, so label must already be populated

---

## 2026-06-11: import-unlocks-nav bug — orphaned back stack

### What Went Well
* Reproduced the bug deterministically by calling `NavigationUI.onNavDestinationSelected(menuItem, navController)` directly in a Robolectric test — this is the exact code path a bottom-nav tap uses and applies the popUpTo/saveState/restoreState NavOptions, unlike a bare `navController.navigate(id)` which masked the bug (it passed while the real tap failed)
* On-device verification (adb screencap + uiautomator dump) confirmed the menu item was `enabled=true`/`clickable=true` yet navigation stayed on Settings — proving the issue was NavController state, not the menu-item enabled flag

### What Didn't Work (Obstacles & Roadblocks)
* `bottomNav.selectedItemId = id` does NOT trigger the `OnItemSelectedListener` in Robolectric (Material presenter chain isn't functional in shadow mode) — wasted effort trying to simulate a tap that way; calling `NavigationUI.onNavDestinationSelected` directly is the reliable substitute
* `adb logcat -d` returned empty on this emulator session — couldn't rely on logs; the deterministic Robolectric repro was faster than chasing device logs

### ⚠️ Mistakes to Avoid Next Time
* Don't gate navigation by pushing the gated screen on top of the start destination (`navController.navigate(settings)` over `home` start dest). That leaves an orphaned `[home, settings]` back stack that breaks `NavigationUI`'s `popUpTo(startDestination, saveState)` logic when returning to home. Instead, set the gated screen AS the start destination while unconfigured: re-inflate the graph, `graph.setStartDestination(R.id.settingsFragment)`, assign `navController.graph = graph` before `setupWithNavController`
* A passing `navController.navigate(id)` test does NOT prove bottom-nav navigation works — the menu path adds NavOptions that can change the outcome. Test through `NavigationUI.onNavDestinationSelected` when the bug is about tab taps

## 2026-06-11: api-settings-improvements implementation

### What Went Well
* Reading all existing source files (repository, fragment, layout, tests) before writing any code gave a complete picture upfront and prevented mid-task surprises
* Doing the TDD Red phase for the repository (task 2.4) before implementing (2.1–2.3) worked cleanly — compile errors are an acceptable Red signal, not just test failures
* The `hardcodedConsumedRequests` named constant pattern (design decision) makes the placeholder easy to locate and replace later — deliberately naming it instead of inlining `800` paid off immediately in tests
* Updating `refreshAllDisplayedValues()` for the new field was easy to remember because the fragment read revealed it upfront; it's a list of parallel assignments that makes omissions obvious

### What Didn't Work (Obstacles & Roadblocks)
* The `PostToolUse` write-user-guide hook fires on every Edit to a UX file — SettingsFragment.kt alone triggered it 5 times during a single task, creating repeated interruptions; the right strategy (defer to end) was already known from the previous session but the hook still fires regardless
* Forgetting to read `SettingsRepository.kt` before the first Edit attempt caused a "file not read" tool error — even for a file read earlier in the session context, the tool requires a Read call in the current tool sequence

### ⚠️ Mistakes to Avoid Next Time
* Always explicitly `Read` a file in the current tool sequence before `Edit`, even if its contents are visible in conversation context — the Edit tool enforces this independently
* When updating `isConfigured()` to add a new required field, immediately search for all test helpers that configure settings (e.g. `configureSettings()` in `MainActivityTest`) — they will need the new field or configured-state tests will silently fail
* Don't invoke `write-user-guide` after each individual Edit to a UX file — batch all UX changes first, then invoke once; the hook is a reminder, not a gate

## 2026-06-10: required-settings-indicators implementation

### What Went Well
* `TextView.hint` is the right tool for "Required" indicators — it auto-shows when text is empty and auto-hides when text is set, requiring zero extra logic in `onSave` callbacks
* Adding `isRequired` as a property on `SettingRowView` with a private `updateRequiredHint()` helper kept the change self-contained and easy to test
* Batching string resource tasks before TDD tasks avoided a compile error during the red phase — the string resource was already available when the implementation ran

### What Didn't Work (Obstacles & Roadblocks)
* The `PostToolUse` write-user-guide hook fires on every individual file edit, not once per task or session — wiring 5 fields triggered the hook 5 times mid-task, interrupting flow
* Gradle's task cache marked tests as `UP-TO-DATE` after the first run; `--rerun` flag is needed to confirm the red/green cycle for targeted test runs

### ⚠️ Mistakes to Avoid Next Time
* Don't invoke `write-user-guide` mid-implementation when multiple sequential edits to UX files are planned — wait until all code changes are complete, then invoke once at the end
* Always use `--rerun` when running targeted Robolectric tests immediately after a new property is added; stale cache can mask whether new tests actually passed
* In `SettingRowView`, `updateRequiredHint()` reads `value` directly — in `SettingsFragment` wire methods, set `isRequired` before `value` so the hint renders correctly on first load (current code already does this; don't reverse the order)
