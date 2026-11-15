Updating Database Schema
========================

Keycloak supports automatically migrating the database to a new version by applying Liquibase changesets to the existing database.
A schema change must transform both the schema and any existing data.

This includes changes to:

* Realm entities
* User entities
* User session entities
* Event entities

Creating a changeset
--------------------

The changesets are in [`model/jpa/src/main/resources/META-INF`](../model/jpa/src/main/resources/META-INF).
There is a separate file for each release that requires database changes.

Create `jpa-changelog-<version>.xml` in that directory and reference it from [`jpa-changelog-master.xml`](../model/jpa/src/main/resources/META-INF/jpa-changelog-master.xml).
Use `keycloak` as the author and a unique ID that starts with the target release.
Keep related operations in separate changesets when they need different preconditions or need to run in a defined order.
Use existing recent changelogs as examples and consult the Liquibase documentation for supported changes.

Supporting automatic and manual migration
-----------------------------------------

An automatic migration applies each generated SQL statement to the connected database before Liquibase evaluates the next changeset.
With `migration-strategy=manual`, Liquibase writes statements to a script instead.
The database used to generate that script is not changed by those statements, so a precondition or custom change cannot observe a table, column, index, or data update emitted earlier in the same script.

Review every changeset as both a live update and a script-generation step.
In particular, check whether its decision depends on the database before the script, the result of an earlier statement in the script, or persisted Liquibase history.

Choosing `onSqlOutput`
----------------------

`onSqlOutput` controls preconditions while Liquibase generates SQL:

* `TEST` evaluates the precondition against the database used to generate the script and includes or skips the changeset from that result.
  Use it when that database already contains all state needed by the precondition, such as a database type check or an object that must exist before the migration starts.
* `IGNORE` does not evaluate the precondition during script generation and emits the changeset unconditionally.
  The precondition still protects automatic migration.
  Use it when an earlier changeset in the same script creates or alters the state being tested and the script order guarantees the required state.
* `FAIL` rejects SQL generation for the changeset instead of making a decision from the generation database.
  Use it when neither testing nor ignoring the precondition can produce a generally safe script.

The organization migration in [`jpa-changelog-26.8.0.xml`](../model/jpa/src/main/resources/META-INF/jpa-changelog-26.8.0.xml) demonstrates `IGNORE` for work that relies on a column added earlier in the same script.
Do not use `TEST` for an `indexExists` or `columnExists` check merely because it is correct during automatic migration; the object might exist only after an earlier script statement is applied.

Checking persisted changeset history
------------------------------------

Liquibase's `changeSetExecuted` precondition reads its in-memory history.
During script generation that history also includes changesets written to the script but not applied to the database.
It therefore cannot distinguish a changeset applied in an earlier Keycloak version from one only emitted earlier in the current script.

Use [`ChangeSetPersistedPrecondition`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/ChangeSetPersistedPrecondition.java) when the decision must be based on the `DATABASECHANGELOG` table as it exists before the generated script is run.
Supply the exact changeset ID, author, and changelog file.
The index replacement changesets in `jpa-changelog-26.8.0.xml` show this pattern.

Creating and replacing indexes
------------------------------

[`CustomCreateIndexChange`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/CustomCreateIndexChange.java) may defer index creation during an automatic migration when a table exceeds the configured row-count threshold.
The changeset is still recorded as executed, and the database index checker creates the missing index after startup where supported or logs the SQL for an operator.
Manual migration always emits the index statement.

Account for this difference when a later release drops or replaces that index.
For a drop followed by creation under the same name, emit the drop if the index currently exists or if its creating changeset is not in persisted history.
This allows a script that emitted the old index earlier to replace it while avoiding a drop when automatic migration previously deferred the old index.
See the `26.8.0-51851-drop-old-expiration-*` changesets for complete examples.

Writing custom Java changes
---------------------------

A `customChange` is asked to generate `SqlStatement` objects in both modes.
Its Java generation logic runs against the connected database even when Liquibase is only writing a script.
Generate SQL that will be valid when the script is later applied, and do not mutate data directly from the generation logic.
Do not make generation depend on schema or data produced only by earlier statements in the same script.
Prefer set-based, repeatable SQL and database-aware object quoting.
[`JpaUpdate26_8_0_PopulateOrgDomainRealmId`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/JpaUpdate26_8_0_PopulateOrgDomainRealmId.java) is a compact example.

Testing database migration
--------------------------

Verify a changelog against an empty database and against an upgrade database containing representative existing data.
Test both automatic migration and generation plus execution of a manual migration script on every affected database family.

The main safety net is `testScriptProducesTheSchemaOfTheAutomaticMigration` in [`ManualMigrationEmptyDatabaseDistTest`](../quarkus/tests/integration/src/test/java/org/keycloak/it/cli/dist/ManualMigrationEmptyDatabaseDistTest.java).
It uses [`ManualMigrationParity`](../quarkus/tests/integration/src/test/java/org/keycloak/it/storage/database/ManualMigrationParity.java) to compare tables, columns, keys, indexes, sequences, and persisted changesets produced by the two modes.
Do not add a parity-test exception for a new intentional change; make both migration modes agree.

Changes involving deferred indexes must also keep [`DatabaseIndexCheckerTest`](../tests/base/src/test/java/org/keycloak/tests/db/DatabaseIndexCheckerTest.java) passing.
Run the database-specific migration tests because quoting, filtered indexes, data types, and online index support differ between vendors.
See [`tests.md`](tests.md), [`tests-db.md`](tests-db.md), and the [database migration test instructions](../testsuite/integration-arquillian/HOW-TO-RUN.md#db-migration-test) for setup and commands.
