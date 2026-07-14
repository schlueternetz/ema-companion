---
name: widget-preview
description: Regenerates the Android widget-picker preview for a Glance home-screen widget after its visual content changes. Use after editing a Glance widget composable (feature/widgets/*Widget.kt), its shared visual helpers (WidgetTheme.kt, WidgetTextStyles.kt, WidgetChartRenderer.kt), or a *_widget_info.xml file. Do NOT use for non-visual widget changes (data/repo/throttle logic) that don't change what the widget looks like.
allowed-tools: Read Edit Write Glob PowerShell
---

Keeps each widget's entry in the Android widget picker showing an accurate preview of the widget itself, instead of the generic robot-head placeholder Android shows when no usable preview is set.

## Why `previewLayout`, not `previewImage`

This app's `minSdk = 31` (Android 12+), which is exactly the version that introduced `android:previewLayout` on `appwidget-provider` — a plain Android View XML layout that the OS inflates and renders **live** in the widget picker. Since every supported device is API 31+, this is used exclusively; there is no need for a bitmap `previewImage` fallback for older OSes. (A previous attempt pointed `previewImage` at `@drawable/ic_launcher_foreground` — an adaptive-icon foreground layer, not a real bitmap — which is why the robot head showed instead.)

`previewLayout` also means "generating a preview image" requires no emulator, no screenshot, no cropping pipeline: it's just an XML layout with representative sample data, kept in sync by hand (by this skill) whenever the real widget's look changes.

## Step 1 — Identify what changed and which widget(s) it affects

- A single widget file changed (e.g. `TodayProductionWidget.kt`) → only that widget's preview needs updating.
- A shared visual helper changed (`WidgetTheme.kt`, `WidgetTextStyles.kt`, `WidgetChartRenderer.kt`, `WidgetTapTarget.kt`) → re-check all three widgets' previews (`TodayProductionWidget`, `ProductionSummaryWidget`, `ProductionHistoryWidget`), since they share the same theme/type-scale.
- A `res/xml/*_widget_info.xml` changed → confirm its `android:previewLayout` still points at a layout that matches the current widget size/shape (`android:targetCellWidth`/`targetCellHeight`).

## Step 2 — Read the current widget composable

Read the Glance `@Composable TestContent()` in the changed widget file to see its current structure: what labels are shown (via `context.getString(R.string....)`), what values, and whether it renders a chart (`WidgetChartRenderer.renderHourlyChart`/`renderHistoryChart` → an `Image`).

## Step 3 — Write/update the static preview layout

File: `code/ema-companion/app/src/main/res/layout/<widget_name>_widget_preview.xml` (snake_case of the widget class, e.g. `today_production_widget_preview.xml`).

Rules:
- Root view has `android:theme="@style/Theme.EMACompanion"` — the widget-picker host does not otherwise apply the app's theme, and this is what makes `?attr/colorSurface` / `?attr/colorOnSurface` / `?attr/colorPrimary` resolve to this app's Material3 `DayNight` palette (the closest plain-View equivalent to `WidgetTheme.kt`'s `GlanceTheme` defaults — both are day/night aware).
- Color mapping from Glance to View attrs:
  | Glance (`GlanceTheme.colors.*` / `WidgetTextStyles`) | View XML |
  |---|---|
  | `background` | `?attr/colorSurface` |
  | `onSurface` (value/title text) | `?attr/colorOnSurface` |
  | `onSurfaceVariant` (header text) | `?attr/colorOnSurfaceVariant` |
  | chart bars / accents | `?attr/colorPrimary` |
- Reuse real string resources for any label that also appears in the live widget (e.g. `@string/home_today_title`, `@string/home_today_total_label`, `@string/home_history_this_month_label`). For sample **values** that need concrete numbers (the real widget formats live data), use the dedicated `widget_preview_*` sample strings in `values/strings.xml` / `values-de/strings.xml` (add a new one if a widget needs a new sample value) — never hardcode literal text in the layout, per ADR-003.
- For chart-bearing widgets (Today Production, Production History), approximate the chart as a row of static `View` bars of varying `layout_height`, weighted to fill width, tinted `?attr/colorPrimary` — there's no real data at preview time, so a schematic bar silhouette is the honest representation, not a fabricated exact chart.
- Match the widget's approximate proportions from its `res/xml/*_widget_info.xml` (`android:minWidth`/`minHeight`), but the layout itself should use `match_parent`/weights, not fixed dp — the system scales it to whatever cell size the user drags it to.

## Step 4 — Point the widget info XML at it

In `code/ema-companion/app/src/main/res/xml/<widget_name>_widget_info.xml`, set:

```xml
android:previewLayout="@layout/<widget_name>_widget_preview"
```

Remove any `android:previewImage` on the same element — `previewLayout` takes precedence when both are present, and leaving a stale/broken `previewImage` around invites the exact bug this skill exists to prevent.

## Step 5 — Verify

```powershell
Set-Location code\ema-companion
.\gradlew.bat :app:processDebugResources
```

This is enough to catch a bad resource reference (`?attr/colorSurface` misspelled, missing string, unresolvable layout) without a full build. A resource-compile failure means a typo in the layout or a missing string — fix and re-run.

There is no automated test for widget-picker preview rendering (it's OS chrome, not app UI) — a resource-compile pass is the correct bar here, not a Robolectric/Maestro check.

## Gotchas

- `previewLayout` is API 31+ only — but `minSdk = 31` means this is a non-issue, not a gap to work around. Don't add a `previewImage` bitmap "fallback" for pre-31 devices this app doesn't support.
- The preview layout is a **plain Android View XML**, not Compose/Glance — you cannot reuse `@Composable` functions from the widget package inside it, only mirror their visual result.
- Sample data belongs in string resources (`widget_preview_*`), not hardcoded layout text — this keeps the preview localized like the rest of the app and satisfies ADR-003.
- Keep the sample values plausible (e.g. a "this month" total larger than a "today" total) — a preview with visually nonsensical numbers undermines the point of showing a live layout.
