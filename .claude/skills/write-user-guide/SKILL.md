---
name: write-user-guide
description: Writes or updates the English user guide in docs/user-guide/ when the app frontend (UX) changes — layouts, activities, fragments, menus, or navigation. Use after any UI change is complete. Do NOT use for backend-only changes, test files, or build config edits.
allowed-tools: Read Write Glob
---

After a UI change, write or update the user guide at `docs/user-guide/user-guide.md`.

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

## Rules

- **English only** — no German text, no raw string resource keys
- **User perspective** — describe what the user sees and can do, not how it is implemented
- **Accurate to current state** — only document what actually exists in the UI; if a screen is a placeholder, say so briefly
- **Concise** — short, clear sentences; no padding

## Gotchas

- Changes to `docs/user-guide/user-guide.md` are tracked in git — run `git diff` after the skill runs to confirm the output looks correct before committing.
- If a screen is still a placeholder (e.g. "Hello World"), document it as such; do not invent features that don't exist yet.
- `strings.xml` contains both English and German strings — use only the English (`values/strings.xml`) values, never `values-de/`.
