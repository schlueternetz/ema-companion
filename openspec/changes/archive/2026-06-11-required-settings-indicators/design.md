## Context

`SettingRowView` is a custom view used for all editable settings rows. It has `label`, `value`, `isMasked`, `suffix`, `validator`, and `onSave` properties. When a required field is empty, the `value` property is `""` and the row shows only the label and an edit button — no visual cue that the field is needed.

`SettingsFragment` already knows which fields are required (it calls `checkConfigurationAndUpdateNav()` after each save) but does not communicate that knowledge back to the individual rows.

## Goals / Non-Goals

**Goals:**
- Required-but-empty fields show a "Required" hint so users know what to fill in
- The hint disappears once the field has a value
- No change to validation logic, save behaviour, or navigation-unlock logic

**Non-Goals:**
- Marking fields as required after a failed save attempt (this is not a form-submission flow)
- Showing a summary banner or count of missing fields
- Changing the existing error message shown for invalid input

## Decisions

**Decision: `isRequired` property on `SettingRowView`, hint shown as `suffixText` on the `TextInputLayout` when value is empty.**

The `TextInputLayout` already has `suffixText` (used for " kW"). A parallel `hintText`-style indicator for the read-only state is the lightest touch: set a `"Required"` string on the `setting_value` TextView's hint (visible only when the text is empty) when `isRequired = true`. This requires no new views — `TextView` shows hint text automatically when its text is empty.

Alternatives considered:
- Red border / tint on the row: too alarming for an onboarding state; reserved for validation errors.
- A separate badge/chip view: additional view complexity for a simple hint.
- Showing "Required" as the `value` string: would require the Fragment to track and restore the original value, adding state complexity.

**Decision: `SettingsFragment` sets `isRequired = true` on the five solar-array fields at wire-up time; `onSave` callback does not need to change.**

When `onSave` fires and sets a non-empty `value`, the `setting_value` TextView's text becomes non-empty, which automatically suppresses the hint. No extra flag-clearing needed.

## Risks / Trade-offs

- Hint colour is theme-driven (`android:textColorHint`) and will match the current theme automatically — no risk of contrast failures.
- If the user saves a field then factory-resets, `SettingsFragment` re-wires all rows from scratch (calls `wireSystemCapacity()` etc.) so `isRequired` will be re-applied correctly.
