---
name: skill-reviewer
description: Reviews Claude Code skill files for basic requirements and quality. Invoke when the user asks to review, audit, check, or improve a skill file; says "what do you think" about a skill; or has a SKILL.md open in the IDE and asks any question about it. Can also be invoked directly via /skill-reviewer.
allowed-tools: Read, Grep, Glob
user-invocable: true
---
When the user adds or edits a skill file under `.claude/skills/`, read the relevant `SKILL.md` and review it against the checklist below. Output a concise pass/fail summary with one bullet per check, followed by a "Suggestions" section listing any actionable improvements. If everything passes, say so explicitly.

## Checklist

- `frontmatter` (required) — The file must open with a `---` … `---` YAML block. **Fail all required checks** if frontmatter is absent or malformed.
- `name` (required) — Lowercase letters, numbers, and hyphens only. Max 64 characters. Must match the directory name. **Fail** if missing or mismatched.
- `description` (required) — Max 1,024 characters. Must answer: *What does the skill do?* and *When should Claude use it?* Trigger conditions should be specific (e.g. file paths, user phrases, imports). **Fail** if vague or missing either answer.
- `allowed-tools` (optional) — Should be as narrow as the skill's task allows. **Suggest** tightening if broad tools like `Bash` or `*` are listed without clear justification.
- `user-invocable` (optional) — If present, must be a boolean (`true` or `false`). **Suggest** adding it when the skill is intended to be triggered by a `/skill-name` slash command.
- `output format` — The skill should specify how Claude should respond (checklist, summary, inline suggestions, etc.). **Suggest** adding one if absent.
- Progressive disclosure — If `SKILL.md` exceeds 500 lines or embeds inline scripts/large reference tables, **suggest** splitting into `scripts/`, `references/`, or `assets/` subdirectories and linking from `SKILL.md`.

## Progressive Disclosure Structure
 - `scripts/` — Executable code
 - `references/` — Additional documentation
 - `assets/` — Images, templates, or other data files