---
name: kc-database-changelog
description: Create or review Keycloak Liquibase changelog entries that must work for automatic migration and migration-strategy=manual SQL generation. Use for schema changes, data migrations, preconditions, customChange implementations, and index creation or replacement.
---

# Keycloak database changelog

Use `docs/updating-database-schema.md` as the source of truth.
Automatic migration and manual script generation are separate execution paths that must produce the same final schema.
Upgrades need not be sequential, so never assume a starting version: any changeset can follow any earlier changeset of any release.

## Workflow

1. Choose the target changelog: the last file included in `jpa-changelog-master.xml`, unless a release tag for that version exists.
   Only then create a new `jpa-changelog-<version>.xml` and include it in the master file.
   Read it, adjacent recent changelogs, and every custom Java change it invokes.
2. Trace the database state before and after each changeset for both modes and for each starting point: an empty database, any earlier release, and a database that recorded an earlier changeset but lacks its object.
   For a data migration, also trace the representation of the data: custom changes of earlier releases are only in the script, so the generation database holds the data in the older form.
3. Check every precondition and set `onSqlOutput` explicitly, because the default is `IGNORE`; never use `FAIL`:
   - `TEST` only when the generation database has all state needed for the decision at every starting version.
     To tell persisted history from a changeset only emitted to the current script, use `ChangeSetPersistedPrecondition` with `TEST` instead of `changeSetExecuted`, as in `or(indexExists, not(ChangeSetPersistedPrecondition))`.
   - `IGNORE` when an earlier statement in the same script, from this or any earlier release, creates or changes that state.
4. For a new index, use `createIndex` with a `not indexExists` guard when an operator may have created it, `dbms` and `modifySql` for database-specific variants, and an upgrade-guide entry for a table that can exceed the `index-creation-threshold` (default 300000 rows).
   For a deferred or replaced index, follow the patterns in `jpa-changelog-26.8.0.xml`.
5. Make each `customChange` return SQL that is valid when applied later, without relying on direct mutations or on earlier script statements being visible to its Java logic.
   Do not skip on a missing table or column unless that proves there is no data to migrate; it can mean the data is still in the older representation, which the task must then read.
6. Preserve database-specific quoting and behavior.
   In a released changelog, never change a changeset's ID, author, file, changes, or `modifySql`, because they are part of its identity and checksum.
   Comments and preconditions are not checksummed and may be corrected, but test the changed behavior for databases that have not applied the changeset yet.

## Validation

- Inspect the generated SQL and apply it to the intended starting database.
- Run the H2 empty-database parity test and keep `ManualMigrationParity` free of new exceptions.
  It is a distribution test, so build the distribution first (`./mvnw clean install -DskipTests -Pdistribution`, see `docs/building.md`), then run from the `quarkus` directory:

  ```shell
  ../mvnw -pl tests/integration test -Dtest=ManualMigrationEmptyDatabaseDistTest#testScriptProducesTheSchemaOfTheAutomaticMigration
  ```

- Run the same check on each affected database family.
  These storage tests need the `test-database` profile and a container runtime:

  ```shell
  ../mvnw -pl tests/integration test -Ptest-database -Dtest=PostgreSQLDistTest#testManualScriptProducesTheSchemaOfTheAutomaticMigration
  ```

  Use `MariaDBDistTest` or `MySQLDistTest` for those databases.
  SQL Server and Oracle have no such test, so inspect the generated manual script instead of reporting them as covered.
- For deferred-index changes, run `DatabaseIndexCheckerTest`, for example `KC_TEST_DATABASE=postgres ./mvnw -pl tests/base test -Dtest=DatabaseIndexCheckerTest` (see `test-framework/docs/RUNNING_TESTS.md`).
  No migration test sets the threshold and their tables are far below the default of 300000 rows, so to exercise deferral start a database from an earlier release with rows in the table and a low `--spi-connections-liquibase--quarkus--index-creation-threshold`.
  Check for the "Following index should be created" log line, the recorded changeset, and the missing index, then whether the index checker creates it or only logs the SQL on that database.
- For a custom change that reads data, generate the script from the oldest release that can hold that data, with rows in the tables it reads, not only from an empty database or 19.0.3.
- For an upgrade with existing data, run `MigrationTest` with `-Dmigration.mode=auto` and `-Dmigration.mode=manual` (see `testsuite/integration-arquillian/HOW-TO-RUN.md#db-migration-test`).
- Confirm that both paths produce identical schema objects and persisted changelog entries.
