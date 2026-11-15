---
name: kc-database-changelog
description: Create or review Keycloak Liquibase changelog entries that must work for automatic migration and migration-strategy=manual SQL generation. Use for schema changes, data migrations, preconditions, customChange implementations, and index creation or replacement.
---

# Keycloak database changelog

Use `docs/updating-database-schema.md` as the source of truth.
Treat automatic migration and manual script generation as separate execution paths that must produce the same final schema.

## Workflow

1. Read the target changelog, adjacent recent changelogs, and every custom Java change it invokes.
2. Trace the starting database state and the state expected after each changeset for both migration modes.
3. Check every precondition and choose `onSqlOutput` deliberately:
   - Use `TEST` only when the generation database contains all state needed for the decision.
   - Use `IGNORE` when script order guarantees state created or changed by an earlier statement.
   - Use `FAIL` when SQL generation cannot be made generally safe.
4. Do not use `changeSetExecuted` when the decision must distinguish persisted history from a changeset only emitted to the current script.
   Use `ChangeSetPersistedPrecondition` with the exact ID, author, and file instead.
5. For a deferred index, remember that `CustomCreateIndexChange` can record an automatic changeset without creating the index, while manual mode emits its SQL.
   When replacing an index under the same name, follow the persisted-history pattern in `jpa-changelog-26.8.0.xml`.
6. Ensure each `customChange` returns SQL valid when applied later and does not rely on direct mutations or on earlier script statements being visible to its Java generation logic.
7. Preserve database-specific quoting and behavior, and avoid rewriting historical changesets unless the task explicitly requires it.

## Validation

- Inspect the generated SQL and apply it to the intended starting database.
- Run `ManualMigrationEmptyDatabaseDistTest#testScriptProducesTheSchemaOfTheAutomaticMigration` and keep `ManualMigrationParity` free of new exceptions.
- Run `DatabaseIndexCheckerTest` for deferred-index changes.
- Run the affected database-specific migration tests for both an empty database and an upgrade with existing data.
- Confirm the automatic and manual paths have identical schema objects and persisted changelog entries.

Use the organization and index replacement changesets in `model/jpa/src/main/resources/META-INF/jpa-changelog-26.8.0.xml` as reference patterns.
