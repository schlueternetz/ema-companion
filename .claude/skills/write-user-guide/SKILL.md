---
name: write-user-guide
description: Writes or updates the English user guide in docs/user-guide/ when the app frontend (UX) changes — layouts, activities, fragments, menus, or navigation. Use after any UI change is complete and actually implemented in code. Do NOT use for backend-only changes, test files, build config edits, or content that only exists as an OpenSpec proposal/design (not yet implemented).
allowed-tools: Read Write Glob Bash
---

After a UI change, write or update the **English** user guide pages in `docs/user-guide/`, then regenerate the German translations (Step 3). `Bash` is used only to run the mermaid CLI (`mmdc`) for diagram rendering in Step 3.

**Gate: only for already-implemented behavior.** Before writing anything, confirm the behavior/numbers being documented actually exist in the current codebase (not merely an OpenSpec proposal's `design.md`/`proposal.md`). If the content is about a change that hasn't been implemented yet (an open `openspec/changes/<name>/` with unchecked `tasks.md` items), do not invoke this skill now — instead add or update a task in that change's `tasks.md` Documentation section so the guide gets written during `/opsx:apply`, once the described behavior is real. Writing ahead of implementation produces a guide that's either wrong (if the design changes before shipping) or immediately stale (rework when it ships) — both worse than waiting.

## Page structure

The guide is split into one file per screen, plus an index. Each page must be readable in under 3 minutes (≈600 words maximum). Only add a new page when a section is logically self-contained and genuinely too long to fit within the limit of an existing page — don't split mechanically.

| File | Contents |
|---|---|
| `user-guide.md` | Index: overview, getting started, navigation, links to screen pages |
| `home.md` | Home screen and all its tiles |
| `settings.md` | Settings screen: all sections; links to `import-export.md` for the workflow |
| `import-export.md` | Import and Export step-by-step |

Links between pages use relative `.md` paths (e.g. `[Home](home.md)`). The app's `UserGuideFragment` resolves them within the same `user-guide/` asset folder and automatically swaps to the `-de.md` sibling when the locale is German.

## Step 1 — Read the current UI

Gather context from these locations:

- Layout XML files: `code/ema-companion/app/src/main/res/layout/`
- Activities and Fragments: `code/ema-companion/app/src/main/java/`
- String resources: `code/ema-companion/app/src/main/res/values/strings.xml`
- Navigation graphs: `code/ema-companion/app/src/main/res/navigation/` (if present)
- Menu resources: `code/ema-companion/app/src/main/res/menu/` (if present)
- App overview: `README.md`

## Step 2 — Write the guide pages

Write or update each affected page in `docs/user-guide/`. Also write identical copies to `code/ema-companion/app/src/main/assets/user-guide/` (the English asset copies are gitignored but required at runtime — without them the app shows an error).

**Index page** (`user-guide.md`):
```
# EMA Companion User Guide

## Overview
[What the app does and who it is for — include the system-context diagram]

## Getting Started
[How to open the app; first-run experience]

## Navigation
[Bottom nav destinations]

## Sections
[Bulleted links to each screen page]

## What's Coming
[Planned features]
```

**Screen pages** (`home.md`, `settings.md`, etc.):
```
[User Guide](user-guide.md) › [Parent](parent.md) › Current Page   ← breadcrumb; omit middle crumb for direct children of the index

# [Screen Name]

[What the user sees, what actions are available, how to navigate]
```

The breadcrumb must be the very first line of every non-index page. All ancestors are links; the current page is plain text. For direct children of the index (e.g. `home.md`, `settings.md`): `[User Guide](user-guide.md) › Home`. For deeper pages (e.g. `import-export.md`): `[User Guide](user-guide.md) › [Settings](settings.md) › Import and Export`.

## Step 3 — Regenerate the German translations

The English files in `docs/user-guide/` are the **single source of truth** and the only version rendered on GitHub. German is **in-app only**: it is generated from the English source and committed **into the app asset folder** `code/ema-companion/app/src/main/assets/user-guide/` (NOT `docs/`) — never authored or hand-edited. After Step 2, regenerate German for every page that changed:

1. **Translate each page.** For each `docs/user-guide/<page>.md`, write `code/ema-companion/app/src/main/assets/user-guide/<page>-de.md` (natural, fluent German; keep all markdown syntax and link targets unchanged — `[Startseite](home.md)` not `[Startseite](home-de.md)`). Use the actual in-app German UI labels from `values-de/strings.xml` (e.g. *Einstellungen*, *Benutzerhandbuch*, *Werksreset*). The file must begin with the generated-marker comment, then the translated breadcrumb (for non-index pages), then the heading:

   ```
   <!-- GENERATED from <page>.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
   [Benutzerhandbuch](user-guide.md) › Startseite          ← translated breadcrumb, same rules as English
   
   # Startseite
   ```

   The breadcrumb must appear on every non-index German page immediately after the generated-marker. Translate the link label and the current-page text; keep the link target filenames in English (e.g. `user-guide.md`, `settings.md`). For deeper pages: `[Benutzerhandbuch](user-guide.md) › [Einstellungen](settings.md) › Importieren und Exportieren`.

2. **Translate the diagram source.** For each `docs/user-guide/*.mmd`, translate **only the quoted label strings** into a transient `*-de.mmd` (write it into the asset folder; it is gitignored there and discarded after rendering). **Never translate mermaid ids** — e.g. in `Person(owner, "APsystems Solar Owner")` translate only `"APsystems Solar Owner"`, never `owner`. A leaked id breaks rendering.

3. **Render the diagrams via mmdc** (local only; mmdc is not assumed on `PATH`, so use `npx`):

   ```bash
   ASSETS=code/ema-companion/app/src/main/assets/user-guide
   MMDC="npx -p @mermaid-js/mermaid-cli@11.15.0 mmdc"   # pin the version for reproducible PNG bytes
   $MMDC -i "$ASSETS/system-context-de.mmd" -o "$ASSETS/system-context-de.png"
   $MMDC -i docs/user-guide/system-context.mmd -o docs/user-guide/system-context.png
   ```

   The German PNG renders into the asset folder; the English PNG re-renders in `docs/` to stay in sync with its `.mmd` source. Pin the mermaid-cli version (above) so re-renders are byte-stable and don't churn git. The first `npx mmdc` run downloads Chromium (~300 MB) and is slow; subsequent runs take a few seconds.

4. **Discard the transient German `.mmd`** — only `*-de.md` and `*-de.png` (both in the asset folder) are committed. The `*-de.mmd` is gitignored.

## Rules

- **English source is English only** — `docs/user-guide/*.md` and `*.mmd` contain no German and no raw string resource keys.
- **German is generated, never authored** — produce it in Step 3 from the English source; never hand-edit a `*-de.*` file.
- **Link targets are always English filenames** — even in German pages, links point to `home.md` (not `home-de.md`); `localizeAssetPath` in `UserGuideFragment` handles the swap at load time.
- **Each page ≤ 3 minutes to read** — trim ruthlessly; if a page would exceed ~600 words, split it only if the new page is logically self-contained.
- **User perspective** — describe what the user sees and can do, not how it is implemented.
- **Accurate to current state** — only document what actually exists in the UI; if a screen is a placeholder, say so briefly.
- **Concise** — short, clear sentences; no padding.

## Gotchas

- **No links inside table cells — applies to all languages** — Markwon's `TablePlugin` does not propagate link spans within cells. Links in table cells render as plain text and are never clickable. Place any cross-page links (e.g. "See [Import and Export](import-export.md)") as a standalone paragraph outside the table. When translating, keep any standalone-paragraph links standalone — do not inline them into adjacent table cell descriptions.

- **Write English to BOTH locations**: `docs/user-guide/<page>.md` (git-tracked, GitHub source of truth) AND `code/ema-companion/app/src/main/assets/user-guide/<page>.md` (gitignored but required at runtime). Missing the asset copy means the app shows an error fallback instead of the guide.
- Changes to `docs/user-guide/` are tracked in git — **audit** with `git diff docs/user-guide/` after the skill runs to confirm the output looks correct before committing.
- If a screen is still a placeholder (e.g. "Hello World"), document it as such; do not invent features that don't exist yet.
- `strings.xml` contains both English and German strings — use only the English (`values/strings.xml`) values in English pages, never `values-de/`.
- **German lives in the app, not docs/:** write `*-de.md` and `*-de.png` into `code/ema-companion/app/src/main/assets/user-guide/` (committed there via a `.gitignore` negation), never into `docs/` (which would publish German on GitHub).
- **Committed vs transient:** commit `*-de.md` and `*-de.png`; the intermediate `system-context-de.mmd` is transient and gitignored — never commit it.
- **Diagram translation is label-only:** translate quoted strings in the `.mmd`, never ids. If `mmdc` errors, a label likely leaked into an id.
- The in-app German guide is selected automatically by locale (`UserGuideFragment`), falling back to English when a `*-de.md` is absent — so a missing translation degrades gracefully.
