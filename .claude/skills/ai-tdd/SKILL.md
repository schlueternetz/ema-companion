---
name: ai-tdd
description: Implement a task using AI Test-Driven Development (AI-TDD) — the Red/Green/Refactor cycle. Use when implementing any feature task, or when invoked by opsx:apply.
---

> **Scope**: test execution and implementation only. To deploy and run the app manually on the emulator, use `/local-android-dev`.

## What is AI-TDD

AI-TDD is the Red/Green/Refactor cycle executed with an AI coding assistant:

1. **Red** — Write a failing test that specifies the expected behavior
2. **Green** — Write the minimal implementation to make the test pass
3. **Refactor** — Clean up code and tests without breaking anything

Every implementation task goes through this cycle. No production code is written before a failing test exists.

## Inputs

- A task description (from `tasks.md` or from the user)
- Relevant specs (`openspec/changes/<name>/specs/`) for behavior requirements
- Existing code context (read the affected files first)

## Workflow

### Step 1 — Red: write a failing test

Choose the test layer per [ADR-002](../../../docs/adr/002-testing-strategy.md):
- **Unit test** (`src/test/`, JUnit4): pure logic, no Android framework dependency — default choice
- **Robolectric** (`src/test/`, `@RunWith(RobolectricTestRunner::class)`): needs Android `Context` or framework class but not a real device
- **Integration test** (`src/test/`): tests the API client or data flow end-to-end against the local mock API service
- **Maestro** (`maestro/`): critical UI flows only; use sparingly

Write the test:
- Name it after the behavior being specified (e.g., `settingsRepository_returnsDefaultLanguageWhenEmpty`)
- Cover the primary scenario from the spec; add edge cases if obvious
- Do NOT write implementation yet

Run the tests to confirm the new test fails:
```powershell
Set-Location "code\ema-companion"
.\gradlew.bat testDebugUnitTest   # unit tests
# or
.\gradlew.bat connectedDebugAndroidTest   # instrumented tests (emulator must be running)
```

Confirm: the new test fails, existing tests still pass.

### Step 2 — Green: write minimal implementation

Write the smallest amount of production code that makes the failing test pass. Do not over-engineer or add unrequested behavior.

Run the tests again to confirm:
```powershell
.\gradlew.bat testDebugUnitTest
```

Confirm: the new test passes, all existing tests still pass.

### Step 3 — Refactor

Review both the implementation and the test for:
- Clarity: rename variables, extract well-named methods
- Duplication: consolidate repeated logic
- Simplicity: remove anything not needed to satisfy the tests

Run tests once more after any refactor to confirm nothing broke.

### Step 4 — Done

The task is complete when:
- At least one test specifies the behavior
- All tests pass
- `./gradlew ktlintCheck` passes
- **For UI tasks**: Robolectric test includes an `AccessibilityValidator` check (ATF), and `./gradlew lint` reports no new accessibility violations — per [ADR-003](../../../docs/adr/003-platform-localization-accessibility.md)

Mark the task complete in `tasks.md`: `- [ ]` → `- [x]`

Invoke the `lessons-learned` skill after each completed task cycle.

## Guardrails

- Never write production code before a failing test exists
- Keep Green step minimal — only what makes the test pass
- If a task has no testable behavior (e.g., adding a string resource, updating a dependency), skip Red/Green and note why
- If the test cannot be made to fail (behavior already exists), document it and move to refactor
- If running instrumented tests, the emulator must be running — invoke `/local-android-dev` if needed
