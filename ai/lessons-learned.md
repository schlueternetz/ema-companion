# AI Lessons Learned

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
