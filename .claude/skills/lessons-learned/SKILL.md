---
name: lessons-learned
description: Writes a summary of lessons learned to a log file so it avoids making the same mistakes again. Use when making any code or documentation change. Do NOT use for general writing or non-PR summaries.
allowed-tools: Read Write
---

After making code or documentation changes, write a summary of lessons learned to @ai/lessons-learned.md so it avoids making the same mistakes again. 
Keep it short and focused on actionable insights. Keep the file under 10k tokens and clean up if required, keeping the most relevant insights. 
Use the following format to structure your summary:

## {Date}: {Short title of the lesson}
### What Went Well
* <!-- What accelerated development? (e.g., "New linter caught bugs early") -->
* <!-- What architecture choice felt right? -->

### What Didn't Work (Obstacles & Roadblocks)
* <!-- What wasted time? (e.g., "Mismatched environment variables took 2 hours to debug") -->
* <!-- What technical debt did we encounter? -->

### ⚠️ Mistakes to Avoid Next Time
* <!-- Crucial anti-patterns discovered (e.g., "Do not use X library with Y framework because...") -->
* <!-- Missing steps omitted during local testing -->