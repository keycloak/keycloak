# Keycloak Migration — Sync Knowledge Files

This prompt checks that migration prompts and README.md are consistent with the current rules and specs. Run this after editing any rule or spec file.

No build required — this is a pure text consistency check.

All paths below are relative to the Keycloak project root. The migration tool directory is `tests/migration-util/ai-migration-tool/`.

Files to synchronize:
- `tests/migration-util/ai-migration-tool/README.md` — project instructions, directory structure, key conventions
- `tests/migration-util/ai-migration-tool/prompts/MIGRATION_PROMPT.md` — complete migration procedure (single source of truth)
- `tests/migration-util/ai-migration-tool/prompts/UPDATE_RULES_PROMPT.md` — rules/specs verification procedure

Against source-of-truth files:
- `tests/migration-util/ai-migration-tool/rules/*.md` — migration transformation rules
- `tests/migration-util/ai-migration-tool/specs/*.md` — reference data specs

---

## Step 1: Verify directory structure in README.md

Read `tests/migration-util/ai-migration-tool/README.md` and compare its directory structure listing against actual files:

```bash
cd tests/migration-util/ai-migration-tool
ls rules/*.md | sed 's|.*/||' | sort
ls specs/*.md | sed 's|.*/||' | sort
ls prompts/*.md | sed 's|.*/||' | sort
ls checklist/*.md checklist/*.sh | sed 's|.*/||' | sort
ls .claude/skills/*/SKILL.md 2>/dev/null
```

Check that:
1. Every rule file is listed in the directory structure
2. Every spec file is listed in the directory structure
3. Every skill is listed in the directory structure
4. Every prompt file is listed in the directory structure
5. Every checklist file is listed in the directory structure
6. No listed file is missing from disk

Fix any discrepancies by updating `README.md`.

---

## Step 2: Verify key conventions in README.md

Read the "Key Conventions" section of `tests/migration-util/ai-migration-tool/README.md` and cross-check against rules:

1. **test-compile not compile** — must be mentioned (from `rules/core.md`)
2. **Minimal RealmConfig over fromJson** — must be mentioned (from `rules/core.md`)
3. **OAuthClient ref, not custom ClientConfig** — must be mentioned (from `rules/oauth.md`)
4. **@InjectUser roles limitation** — must be mentioned (from `rules/core.md`)
5. **Page object fill+submit pattern** — must be mentioned (from `rules/webdriver.md`)
6. **Builder replacements (RoleBuilder -> RoleConfigBuilder)** — must be mentioned (from `rules/core.md`)
7. **addX() chaining** — `.build()` returns representation, not RealmConfigBuilder (from `rules/core.md`)
8. **do*() vs builder methods on OAuthClient** — must be mentioned (from `rules/oauth.md`)
9. **Event assertions pattern** — must be mentioned (from `rules/events.md`)

If any convention is missing or outdated, update `tests/migration-util/ai-migration-tool/README.md`.

---

## Step 3: Verify MIGRATION_PROMPT.md matches rules

### 3.1 Knowledge files section

Read the "Knowledge Files" section and verify:
1. Every rule file in `rules/` is listed in the Knowledge Files section with an accurate description
2. Every spec file in `specs/` is listed with an accurate description
3. No file is listed that doesn't exist

### 3.2 Key gotchas section

Read the "Key Gotchas" section and cross-check each bullet against the actual rules. Look for:
- Outdated advice (e.g., wrong method names, wrong chaining patterns)
- Missing gotchas that are documented in rules
- Gotchas that contradict what the rules say

### 3.3 Migration procedure steps

Verify each step references the correct:
- Build command (`test-compile` not `compile`)
- File paths
- Phase ordering (checklist update BEFORE commit)

### 3.4 Pattern checklist

Compare the pattern checklist in Phase 1.4 against the patterns covered in the rule files. Every major pattern in the rules should appear in the checklist.

### 3.5 Rule loading triggers

Verify Phase 1.5 correctly maps patterns to rule/spec files:
- Every rule file has at least one trigger
- Every spec file referenced in triggers actually exists
- New specs added to `specs/` are reachable via triggers

### 3.6 Key pitfalls

Read Phase 3 "Key pitfalls to watch for". Cross-check each pitfall against:
- `rules/core.md` — realm config, user roles, server config
- `rules/oauth.md` — OAuthClient ref, TestApp architecture
- `rules/webdriver.md` — fill+submit pattern, test isolation
- `rules/lifecycle.md` — realm.dirty(), cookie clearing

Look for missing pitfalls or outdated advice.

---

## Step 4: Verify UPDATE_RULES_PROMPT.md is current

### 4.1 Import path table

Check the "Known critical paths to always verify" table. Verify each import path against the actual codebase. Remove paths that no longer exist, add new critical paths.

### 4.2 Method signature checks

Verify that the method lists in Step 4 (Events API, OAuth API, ManagedRealm API, etc.) match the actual framework classes. Look for:
- Methods that were added to framework classes but not to the verification list
- Methods listed that no longer exist
- Changed signatures (different parameter types or counts)

### 4.3 Shell commands

Verify that all `find`, `grep`, and `ls` commands in the prompt use correct paths relative to the project root.

---

## Step 5: Verify quick-reference files

### 5.1 `rules/quick-reference.md`
Cross-check each section against its source rule file. Verify no transformation has become outdated (wrong method name, wrong import, wrong pattern).

### 5.2 `rules/pattern-index.md`
Verify each pattern → solution mapping is still correct. Check that new patterns added to rule files since last sync have been added to the index.

### 5.3 `rules/decision-trees.md`
Verify each decision tree's branches match current rules. Check that decision outcomes reference correct rule file sections.

### 5.4 `specs/common-errors.md`
Verify each error/fix pair is still valid. Check that anti-patterns listed still apply. Add any new errors discovered during recent migrations.

---

## Step 6: Cross-check code examples

Extract code examples from `MIGRATION_PROMPT.md`. Verify they don't contain patterns that contradict the rules:

1. **No `.build()` on addX() chaining** — `realm.addUser().build()` should NOT be shown as returning to RealmConfigBuilder
2. **No `loginPage.login(u, p)`** — should use `fillLogin(u, p)` + `submit()`
3. **No custom ClientConfig for OAuthClient clientId** — should use `ref`
4. **No `@InjectUser` with roles** — should use `RealmConfigBuilder.addUser()`
5. **No `./mvnw compile`** — should use `./mvnw test-compile`
6. **No `tests/base/target/classes`** — should use `tests/base/target/test-classes`

---

## Step 7: Report

```
=== SYNC REPORT ===

README.md:
  Directory structure: [up to date / N files added/removed]
  Key conventions: [up to date / N missing/outdated]

MIGRATION_PROMPT.md:
  Knowledge files: [up to date / N missing]
  Key gotchas: [up to date / N missing/outdated]
  Procedure steps: [up to date / N issues]
  Pattern checklist: [complete / N missing]
  Rule triggers: [complete / N missing]
  Key pitfalls: [up to date / N missing/outdated]

UPDATE_RULES_PROMPT.md:
  Import paths: [up to date / N outdated]
  Method signatures: [up to date / N outdated]
  Shell commands: [up to date / N broken]

Code examples:
  Anti-patterns found: [none / list]

Status: ALL CONSISTENT / UPDATES APPLIED
```
