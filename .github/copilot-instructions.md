<!-- Review instructions from https://docs.github.com/en/copilot/tutorials/customize-code-review
     and https://dev.to/techgirl1908/how-i-taught-github-copilot-code-review-to-think-like-a-maintainer-3l2c
     It seems like the initial complaint is that it's overly verbose and hallucinates, rather than
     misses problems, so the instructions are to keep it concise and confident, rather than calling
     out specific things to check -->

# Copilot Instructions for `keycloak/keycloak`

This file is derived guidance, mainly for code reviews. The canonical resources are `AGENTS.md`, `CONTRIBUTING.md`, `PR-CHECKLIST.md` and `docs/`, and they take precedence over this file.

## Code Review

### Review Philosophy

* Only comment when you have HIGH CONFIDENCE (>85%) that an issue exists
* Be concise: one or two sentences per review item when possible
* Be thorough: report as many high and medium issues as possible
* Focus on actionable feedback, not observations
* Review with defense in depth in mind: when a change fixes or touches an issue, consider related situations (similar code paths, other callers, other entry points) that may need a follow-up. Related changes that make the project more resilient are allowed and should not be flagged as unrelated.
* When reviewing text, only comment on clarity issues if the text is genuinely confusing or could lead to errors.

### Keycloak Policy to Flag

* New test files under `testsuite/` (deprecated); new tests belong in `tests/`. Limited additions to existing `testsuite/` tests, mainly bug-regression cases, are permitted (see `testsuite/DEPRECATED.md`)
* New test frameworks or mock frameworks
* Unrelated changes, formatting or refactoring
* Behavior changes without tests or docs
* A PR without a linked issue (`Closes #NNNN`), commits without a `Signed-off-by` sign-off, or more than one commit
* Use of AI agents that is not disclosed in the PR description
* Security vulnerabilities reported publicly instead of following `SECURITY.md`

### Skip Low Value Feedback

Do not comment on:

* Formatting
* Minor naming suggestions
* Multiple issues in one comment

## Cloud Agent

* Use the Maven Wrapper (`./mvnw`), not system Maven. Build setup and toolchain versions are in `docs/building.md`.
* Keep changes focused. Do not refactor or reformat unrelated code.
* Include tests and docs when behavior changes.
* New integration tests go in `tests/` (see `tests/docs/README.md`), never in `testsuite/`. Do not introduce new test frameworks or mocks (see `docs/tests-development.md`).
* Run Spotless before finishing: `./mvnw -Pspotless spotless:apply`
* Prefer targeted module builds over full-repo runs (full runs can take hours):
  * Install a module and its upstream modules: `./mvnw install -DskipTests -pl <module> -am`
  * Test only that module: `./mvnw test -pl <module>`
* For UI changes in `js/`, see `js/apps/admin-ui/CONTRIBUTING.md` and `js/CODING_GUIDELINES.md`.
