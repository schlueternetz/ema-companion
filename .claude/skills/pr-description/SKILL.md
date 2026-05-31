---
name: pr-description
description: Writes pull request descriptions. Use when creating a PR, writing a PR, or when the user asks to summarize changes for a pull request. Do NOT use for general writing or non-PR summaries.
---

When writing a PR description:

1. Run `git diff main...HEAD` to see all changes on this branch
2. Write a description following this format:

## What
One sentence explaining what this PR does.

## Why
Brief context on why this change is needed

## Changes
- Bullet points of specific changes made
- Group related changes together
- Mention any files deleted or renamed

## Gotchas
- If `git diff main...HEAD` returns nothing, the branch has no commits ahead of main — tell the user.
- Does not create the PR; it only writes the description text.