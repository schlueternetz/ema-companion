---
name: write-adr
description: Write a new Architecture Decision Record and keep all cross-references in sync. Use whenever a new ADR is created or an existing one is significantly updated. Do NOT use for minor edits to an existing ADR.
allowed-tools: Read Write Edit Glob
---

When a new ADR is created (or an existing one significantly updated), write the ADR file and then update every file that cross-references it. Missing this step is the most common documentation drift in this project.

## Step 1 — Determine the ADR number

Use Glob on `docs/adr/*.md` and sort by name to find the highest existing number, then use the next one (e.g. if 001–004 exist, use 005).

## Step 2 — Write the ADR

Create `docs/adr/<NNN>-<kebab-title>.md` using this structure:

```markdown
# ADR-<NNN>: <Title>

**Status:** Accepted  
**Date:** <YYYY-MM-DD>

## Context

[Why this decision was needed — what problem it solves, constraints that apply.]

## Decision

[What was decided, described precisely enough to implement from.]

## Alternatives Considered

[Other options that were evaluated and why they were rejected.]

## Consequences

[What changes as a result — what becomes easier, what becomes harder, what is now required.]
```

Rules:
- **Decisions must be implementable** — vague guidance belongs in AGENTS.md, not an ADR
- **Alternatives Considered is required** — at least one alternative with a reason for rejection
- **Status is always "Accepted"** — draft ADRs are not committed; propose-then-commit outside this skill

## Step 3 — Update `docs/getting-started.md`

Add a row to the ADR table:

```markdown
| [ADR-<NNN>](adr/<NNN>-<kebab-title>.md) | <Title> |
```

The table is in the "Architecture Decision Records" section. Keep rows in numeric order.

## Step 4 — Update `AGENTS.md` (if the ADR defines agent behaviour)

If the ADR constrains how agents write code, choose tests, organize files, or handle any implementation task, add a summary under `## Key Conventions` in `AGENTS.md`:

```markdown
**<Short label>** (ADR-<NNN>):
- <One-line rule agents must follow>
- <Another rule if needed>
```

Only add rules agents will act on directly. Skip this step if the ADR is purely about tooling, infrastructure, or product decisions that do not change how code is written. When skipping, note the reason explicitly in the checklist below.

## Checklist (verify before finishing)

- [ ] `docs/adr/<NNN>-*.md` created and complete (Context, Decision, Alternatives, Consequences)
- [ ] `docs/getting-started.md` ADR table updated
- [ ] `AGENTS.md` Key Conventions updated — or skipped with reason: ___
- [ ] `git diff docs/` reviewed to confirm all three files changed as expected

## Gotchas

- **`getting-started.md` is easy to forget** — it is rarely open during ADR work. Always check the checklist before declaring done; this drift is the exact failure that motivated this skill.
- **The `AGENTS.md` step is conditional, not optional** — "skip" requires a documented reason in the checklist. An undocumented skip looks identical to a forgotten step.
- **Glob returns files in modification order by default** — sort by name, not by mod time, to get the correct highest ADR number.
- **The ADR number must be zero-padded to three digits** — `005`, not `5`. The table and file name must match.
