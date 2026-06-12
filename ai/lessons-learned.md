# AI Lessons Learned

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
