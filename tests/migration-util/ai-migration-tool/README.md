# AI Migration Tool

This directory contains knowledge files and commands for AI-assisted migration of Keycloak tests from the legacy Arquillian testsuite (`testsuite/`) to the new Test Framework (`tests/`).

## Agent-Agnostic Prompts

All procedures are in agent-agnostic prompt files that work with any AI agent (Claude, Gemini, Copilot, ChatGPT, etc.). For Claude Code, the `/skill` commands are thin wrappers that reference them.

- `prompts/MIGRATION_PROMPT.md` — Complete test migration procedure (`/migrate-test`)
- `prompts/UPDATE_RULES_PROMPT.md` — Rules/specs verification against codebase (`/update-rules`)
- `prompts/SYNC_KNOWLEDGE_PROMPT.md` — Consistency check across all knowledge files (`/sync-knowledge`)

## Directory Structure

```
ai-migration-tool/
├── README.md                — This file
├── CLAUDE.md                — Claude Code auto-loader (references README.md)
├── prompts/                 — Agent-agnostic procedure prompts
│   ├── MIGRATION_PROMPT.md      — Complete migration procedure
│   ├── UPDATE_RULES_PROMPT.md   — Rules/specs verification procedure
│   └── SYNC_KNOWLEDGE_PROMPT.md — Knowledge consistency check
├── checklist/               — Migration progress tracking
│   ├── MIGRATION_CHECKLIST.md   — Auto-generated progress tracker
│   └── generate-checklist.sh    — Regenerates MIGRATION_CHECKLIST.md from current codebase state
├── .claude/skills/          — Skills (thin wrappers referencing prompt files)
│   ├── migrate-test/SKILL.md      — /migrate-test <TestClassName>
│   ├── update-rules/SKILL.md      — /update-rules
│   └── sync-knowledge/SKILL.md    — /sync-knowledge
├── rules/                   — Migration transformation rules (OLD → NEW patterns)
│   ├── quick-reference.md   — One-page cheat sheet (~80% of migrations). Read FIRST.
│   ├── pattern-index.md     — O(1) lookup: legacy pattern → exact solution + rule location
│   ├── decision-trees.md    — Flowcharts for ambiguous choices (realm config, user roles, base class, etc.)
│   ├── core.md              — Base classes, realm config, imports, JUnit 4→6, admin client factory
│   ├── events.md            — Event and admin event assertion migration
│   ├── lifecycle.md         — Cleanup, time manipulation, ordering, conditional execution
│   ├── oauth.md             — OAuthClient injection and method reference
│   ├── server-and-registration.md — Server URL, feature flags, server config, testingClient, RunOnServer
│   ├── utilities.md         — Utility class mapping, testingClient replacement, email, crypto
│   └── webdriver.md         — WebDriver, page objects, creating new pages
└── specs/                   — Reference data about the codebase (tables, lists, counts)
    ├── keycloak-test-framework.md — Injection annotations, config builders, managed resources, events API
    ├── keycloak-tests-module.md   — Abstract base classes, common configs, utility classes, custom providers
    ├── arquillian-testsuite.md    — Legacy module structure, base class hierarchy, infrastructure classes
    ├── testrealm-json.md          — Users, clients, roles in testrealm.json (when to use vs minimal RealmConfig)
    ├── page-objects.md            — Page object catalog: available vs missing, usage frequency, name mappings
    ├── custom-providers.md        — Custom provider catalog: migrated vs not-yet-migrated, SPI registration
    ├── base-class-flattening.md   — Flattening recipes for each legacy base class (fields → @Inject*, methods → inline)
    ├── reference-tests.md         — Complex migrated tests to use as templates, organized by pattern
    └── common-errors.md           — Compilation errors, runtime failures, anti-patterns with fixes
```

## Scope Constraints

The migration tool is only allowed to create or modify files within:
- `tests/` — new test module
- `testsuite/` — legacy test module (deletions only)
- `test-framework/` — framework module (only when creating new page objects or providers)

Do NOT modify any files outside these three directories.

## Key Conventions

- All maven commands use `./mvnw`, not `mvn`
- Use `test-compile` not `compile` — test classes are in `src/test/java/`
- The mechanical migration script is at `tests/migration-util/migrate.sh` — always run it FIRST
- The commit script is at `tests/migration-util/commit-migration.sh` — preserves git history
- Submodule `@Inject*` annotations are NOT in `org.keycloak.testframework.annotations.*` — they are in their respective module packages (oauth, ui, remote, mail). See `rules/core.md` imports section.
- `do*()` methods on OAuthClient return responses directly. Builder methods (without `do` prefix) return request objects with `.send()`.
- Event assertions use `events.poll()` + `EventAssertion.assertSuccess()` — there is no `events.assertion()` or `events.events()` API.
- Prefer minimal `RealmConfig` over `@InjectRealm(fromJson = "testrealm.json")`. See `specs/testrealm-json.md`.
- OAuthClient auto-creates its client via TestApp. Use `ref` for unique clientIds, NOT custom `ClientConfig`. See `rules/oauth.md`.
- `@InjectUser` does NOT support `roles()` / `clientRoles()` — use `RealmConfigBuilder.addUser()` instead.
- Do NOT add new methods to framework page objects — use the existing fill + submit pattern.
- `RoleBuilder` -> `RoleConfigBuilder`, `GroupBuilder` -> `GroupConfigBuilder`. See `rules/core.md`.
- `addUser()`, `addClient()`, `addGroup()`, `addRole()` on `RealmConfigBuilder` return the nested builder. `.build()` returns the representation, NOT back to `RealmConfigBuilder`. Use separate calls: `realm.addUser("x").password("p"); realm.addClient("y").secret("s"); return realm;`
- If a Keycloak class is missing at compile time, add the dependency to the test module's `pom.xml` with `<scope>test</scope>`. See `rules/core.md` "Missing Maven Dependencies".
- `SimpleHttpDefault` → `@InjectSimpleHttp` (JSON API calls) or `@InjectHttpClient` (low-level HTTP). Remove manual `HttpClient` lifecycle.
- `TokenUtil` → do NOT port. Replace with `oAuthClient.client("direct-grant", "password").doPasswordGrantRequest(user, pass).getAccessToken()`. See `rules/utilities.md`.
