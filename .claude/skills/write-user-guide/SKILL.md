---
name: write-user-guide
description: Writes or updates the English user guide in docs/user-guide/ when the app frontend (UX) changes — layouts, activities, fragments, menus, or navigation. Use after any UI change is complete. Do NOT use for backend-only changes, test files, or build config edits.
allowed-tools: Read Write Glob Bash
---

After a UI change, write or update the **English** user guide at `docs/user-guide/user-guide.md`, then regenerate the German translation (Step 3). `Bash` is used only to run the mermaid CLI (`mmdc`) for diagram rendering in Step 3.

## Step 1 — Read the current UI

Gather context from these locations:

- Layout XML files: `code/ema-companion/app/src/main/res/layout/`
- Activities and Fragments: `code/ema-companion/app/src/main/java/`
- String resources: `code/ema-companion/app/src/main/res/values/strings.xml`
- Navigation graphs: `code/ema-companion/app/src/main/res/navigation/` (if present)
- Menu resources: `code/ema-companion/app/src/main/res/menu/` (if present)
- App overview: `README.md`

## Step 2 — Write the guide

Write to `docs/user-guide/user-guide.md`. Use this structure:

```
# EMA Companion User Guide

## Overview
[What the app does and who it is for]

## Getting Started
[How to install and open the app; first-run experience]

## Screens

### [Screen Name]
[What the user sees, what actions are available, how to navigate]

## [Additional feature sections as needed]
```

## Step 3 — Regenerate the German translation

The English files in `docs/user-guide/` are the **single source of truth** and the only version rendered on GitHub. German is **in-app only**: it is generated from the English source and committed **into the app asset folder** `code/ema-companion/app/src/main/assets/user-guide/` (NOT `docs/`) — never authored or hand-edited. After Step 2, regenerate German:

1. **Translate the guide.** Translate `docs/user-guide/user-guide.md` → `code/ema-companion/app/src/main/assets/user-guide/user-guide-de.md` (natural, fluent German; keep all markdown syntax and links unchanged; point the diagram image at `system-context-de.png`). Use the actual in-app German UI labels from `values-de/strings.xml` (e.g. *Einstellungen*, *Benutzerhandbuch*, *Werksreset*), not ad-hoc translations. Prepend this generated-marker as the very first line (it is invisible in-app — no `HtmlPlugin` is registered):

   ```
   <!-- GENERATED from user-guide.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
   ```

2. **Translate the diagram source.** For each `docs/user-guide/*.mmd`, translate **only the quoted label strings** into a transient `*-de.mmd` (write it into the asset folder; it is gitignored there and discarded after rendering). **Never translate mermaid ids** — e.g. in `Person(owner, "APsystems Solar Owner")` translate only `"APsystems Solar Owner"`, never `owner`. A leaked id breaks rendering.

3. **Render the diagrams via mmdc** (local only; mmdc is not assumed on `PATH`, so use `npx`):

   ```bash
   ASSETS=code/ema-companion/app/src/main/assets/user-guide
   MMDC="npx -p @mermaid-js/mermaid-cli@11.15.0 mmdc"   # pin the version for reproducible PNG bytes
   $MMDC -i "$ASSETS/system-context-de.mmd" -o "$ASSETS/system-context-de.png"
   $MMDC -i docs/user-guide/system-context.mmd -o docs/user-guide/system-context.png
   ```

   The German PNG renders into the asset folder; the English PNG re-renders in `docs/` to stay in sync with its `.mmd` source. Pin the mermaid-cli version (above) so re-renders are byte-stable and don't churn git. The first `npx mmdc` run downloads Chromium (~300 MB) and is slow; subsequent runs take a few seconds.

4. **Discard the transient German `.mmd`** — only `user-guide-de.md` and `system-context-de.png` (both in the asset folder) are committed. The `*-de.mmd` is gitignored.

## Rules

- **English source is English only** — `user-guide.md` and `*.mmd` contain no German and no raw string resource keys.
- **German is generated, never authored** — produce it in Step 3 from the English source; never hand-edit a `*-de.*` file.
- **User perspective** — describe what the user sees and can do, not how it is implemented
- **Accurate to current state** — only document what actually exists in the UI; if a screen is a placeholder, say so briefly
- **Concise** — short, clear sentences; no padding

## Gotchas

- Changes to `docs/user-guide/user-guide.md` are tracked in git — run `git diff` after the skill runs to confirm the output looks correct before committing.
- If a screen is still a placeholder (e.g. "Hello World"), document it as such; do not invent features that don't exist yet.
- `strings.xml` contains both English and German strings — use only the English (`values/strings.xml`) values, never `values-de/`.
- **German lives in the app, not docs/:** write `user-guide-de.md` and `system-context-de.png` into `code/ema-companion/app/src/main/assets/user-guide/` (committed there via a `.gitignore` negation), never into `docs/` (which would publish German on GitHub). The English files copied into that asset folder by the build are gitignored; the `*-de.*` files are the committed exceptions.
- **Committed vs transient:** commit `user-guide-de.md` and `system-context-de.png`; the intermediate `system-context-de.mmd` is transient and gitignored — never commit it.
- **Diagram translation is label-only:** translate quoted strings in the `.mmd`, never ids. If `mmdc` errors, a label likely leaked into an id.
- The in-app German guide is selected automatically by locale (`UserGuideFragment`), falling back to English when a `*-de.md` is absent — so a missing translation degrades gracefully.
