---
name: lessons-learned
description: Writes a summary of lessons learned to a log file so it avoids making the same mistakes again. Use when making any code or documentation change. Do NOT use for general writing or non-PR summaries.
allowed-tools: Read Write
---

After making code or documentation changes, write a summary of lessons learned to @ai/lessons-learned.md so it avoids making the same mistakes again.

Write in caveman style: short, direct, no fluff. One line per bullet — just the fact. No full sentences, no "we", no "I", no trailing explanations unless critical. Keep the file under 5k tokens; trim oldest/least useful entries if needed.

## {Date}: {Short title}
### Went Well
* <!-- what worked, one line -->

### Didn't Work
* <!-- what wasted time, one line -->

### Avoid
* <!-- anti-pattern + why, one line -->