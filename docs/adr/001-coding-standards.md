# ADR-001: Coding Standards

**Status:** Accepted  
**Date:** 2026-06-07

## Context

The project needs consistent coding standards to ensure code quality, keep dependencies current, and establish a repeatable implementation workflow. These decisions apply to all code in `code/ema-companion/`.

## Decisions

### Linting: ktlint

All Kotlin code is enforced by [ktlint](https://github.com/pinterest/ktlint) via the `org.jlleitschuh.gradle.ktlint` Gradle plugin. The plugin is applied project-wide and runs as part of the build.

- Run manually: `./gradlew ktlintCheck`
- Auto-fix: `./gradlew ktlintFormat`
- CI gate: lint must pass before merging

### Dependency updates: Dependabot

[Dependabot](https://docs.github.com/en/code-security/dependabot) is configured in `.github/dependabot.yml` to scan Gradle dependencies weekly (Monday) and open PRs automatically. This keeps transitive dependencies current and security patches applied without manual tracking.

### Testing: AI Test-Driven Development (AI-TDD)

All feature implementation follows the AI-TDD workflow — an AI-assisted variant of the Red/Green/Refactor cycle:

1. **Red** — Write a failing test that specifies the expected behavior from the spec
2. **Green** — Write the minimal implementation to make the test pass
3. **Refactor** — Clean up code and tests while keeping all tests green

This is the required workflow for all implementation tasks, including those driven by `/opsx:apply`.

For the full test layer strategy (unit, Robolectric, integration, Maestro) and guidance on which layer to use for each task, see [ADR-002](002-testing-strategy.md).

## Consequences

- Every implemented behavior has a corresponding test before the implementation exists
- Linting failures block merges, keeping the codebase consistently formatted
- Dependabot PRs require review but reduce the cost of staying up to date
- The AI-TDD workflow adds a test-writing step per task but produces a test suite as a natural byproduct of implementation
- The test layer for each task is chosen per ADR-002; Espresso is not used
