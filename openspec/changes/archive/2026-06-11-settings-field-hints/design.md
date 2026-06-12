## Context

The `SettingRowView` custom view (`feature/settings/`) is a reusable row that shows a label, current value, and an edit/save/cancel button set. It has no mechanism to surface contextual help. The five Solar Array Settings fields (App ID, App Secret, System ID, ECU ID, System Capacity) require values that users must look up in specific screens of the EMA app — a task that is non-obvious to first-time users.

## Goals / Non-Goals

**Goals:**
- Add an optional info icon (`ⓘ`) to `SettingRowView` that shows an `AlertDialog` with hint text when tapped.
- Wire the five Solar Array Settings fields with English and German hint strings (content in `proposal.md`).
- Update the user guide to document the same navigation paths in prose.

**Non-Goals:**
- Adding hints to App Settings or API Settings fields — those fields are self-explanatory.
- Inline tooltip bubbles, anchored popups, or any non-dialog UX — a simple `AlertDialog` is sufficient and consistent with existing dialog patterns in the app.

## Decisions

### D1: Optional `hintText` property on `SettingRowView`, not a separate view

The hint is an optional capability of the existing row, not a new component. Adding a nullable `hintText: String?` property to `SettingRowView` keeps the change self-contained. When `null` the info button stays `GONE`; when set it becomes `VISIBLE`.

*Alternative considered:* a separate `HintableSettingRowView` subclass — rejected because it duplicates the class hierarchy for a single optional icon.

### D2: `AlertDialog` for hint display

An `AlertDialog` requires no extra dependencies and is accessible, dismissible with Back, and works well on both phone and tablet form factors.

*Alternative considered:* `TooltipCompat` / `TooltipPopup` — rejected because it requires long-press, not a tap, and has poor discoverability on Android.

### D3: Info icon hidden during edit mode

When the user enters edit mode the row shows only the text field, save, and cancel buttons — the info and edit buttons are already hidden. The info button follows the same `GONE` visibility rule as the edit button, requiring no new state tracking.

### D4: Hint text as `String` in string resources (EN + DE)

All UI text must be localised per ADR-003. Each field gets one string resource per locale. The hint strings are plain text (no styled spans needed in the XML layout stack).

## Risks / Trade-offs

- **Row crowding on narrow screens**: The info icon sits beside the value + edit icon. On narrow phones, label + value + info + edit could feel tight. Mitigation: the info icon is inserted between value and edit button; the icons are 48 dp each, consistent with existing touch targets. The reference device (Lenovo Tab P11 Plus) has ample width.

- **Translation quality**: German hint strings will be authored by the developer. Strings follow the same pattern as the English originals and reference proper-noun EMA app menu paths, limiting meaningful mistranslation risk.

## Migration Plan

No migration needed. The feature is purely additive — existing `SettingRowView` usages without a `hintText` value are unaffected (info button stays `GONE`).
