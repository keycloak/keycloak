# Copilot Cloud Agent Instructions for `keycloak/keycloak`

## Repository at a glance
- Monorepo with a Maven multi-module Java codebase plus JavaScript/TypeScript UIs.
- Main server/runtime work is centered around `quarkus/`, `services/`, `server-spi*`, `model/`, and `testsuite/`.
- Frontend code lives in `js/` (PNPM workspace with apps/libs).

## Required toolchain
- Use Maven Wrapper, not system Maven: `./mvnw`.
- JDK: 17, 21, or 25 (17 is commonly used in CI/local checks).
- Node.js: 24+ for UI work (`js/apps/admin-ui/CONTRIBUTING.md`).
- PNPM workspace is managed from `js/` (`packageManager: pnpm@11.1.1`).

## How to work efficiently
1. Scope your change first (avoid broad refactors; keep unrelated files untouched).
2. Prefer targeted Maven module builds/tests over full-repo runs (full runs can take hours).
3. Mirror CI commands/profiles when possible to avoid local-vs-CI drift.
4. For Java formatting checks, run Spotless from repo root.

## High-value commands

### Formatting / lint checks
- `./mvnw -Pdocs,distribution,operator spotless:check`
- `./mvnw spotless:apply` (only when you intentionally want automatic formatting changes)

### Fast focused Maven build/test pattern
- Build a focused module with required profile(s):
  - `./mvnw install -Pdistribution -DskipTests -DskipExamples -DskipTestsuite -DskipAdapters -DskipDocs -pl <module> -am`
- Run tests for the same focused module:
  - `./mvnw test -Pdistribution -DskipExamples -DskipTestsuite -DskipAdapters -DskipDocs -pl <module> -am`

### Common full/large build references
- Build without tests: `./mvnw clean install -DskipTests`
- Build and test everything: `./mvnw clean install` (very slow)
- Build server distribution only: `./mvnw -pl quarkus/deployment,quarkus/dist -am -DskipTests clean install`

### UI workflow (when changing `js/`)
- From `js/`: `pnpm install`
- Lint: `pnpm lint` (in the relevant app/workspace)
- Build workspace: `pnpm build`

## Testing strategy guidance
- Start with tests closest to changed modules.
- Use root CI helper for unit-test module selection when needed:
  - `.github/scripts/find-modules-with-unit-tests.sh`
- `testsuite/integration-arquillian` tests are much heavier; run only when relevant.

## Repository-specific expectations
- Every PR should map to a GitHub issue and keep a focused scope (`CONTRIBUTING.md`).
- Include docs/tests when behavior changes.
- Do not introduce new test frameworks or broad formatting/refactoring unrelated to the task.

## Errors encountered during onboarding (and workaround)
- Error encountered:
  - `Could not find the selected project in the reactor: distribution/maven-plugins/licenses-processor`
- Cause:
  - The module is part of the `distribution` profile and is not in the active reactor by default.
- Workaround used:
  - Re-run with the profile enabled:
  - `./mvnw install -Pdistribution -DskipTests -DskipExamples -DskipTestsuite -DskipAdapters -DskipDocs -pl distribution/maven-plugins/licenses-processor -am`

