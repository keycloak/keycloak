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

The changesets are in [`model/jpa/src/main/resources/META-INF`](../model/jpa/src/main/resources/META-INF), with a separate file for each release that requires database changes.

Add the changeset to the file of the unreleased version, which is the last one included in [`jpa-changelog-master.xml`](../model/jpa/src/main/resources/META-INF/jpa-changelog-master.xml) unless a release tag for that version exists.
Do not add changesets to the file of a released version, and never change the ID, author, file, changes, or `modifySql` of a released changeset, because Liquibase identifies recorded changesets by those and compares their checksum.
Comments and preconditions are not part of the checksum, so they may be corrected in a released changelog, for example a wrong `onSqlOutput` or a misleading comment.
A precondition change still alters what runs for databases that have not yet applied the changeset, so test it as for a new changeset.
If there is no file for the upcoming release, create `jpa-changelog-<version>.xml` and reference it from the master file.
Use `keycloak` as the author and a unique ID that starts with the target release.
Keep operations in separate changesets when they need different preconditions or a defined order.
Use recent changelogs as examples and consult the Liquibase documentation for supported changes.

Supporting automatic and manual migration
-----------------------------------------

An automatic migration applies each generated SQL statement to the connected database before Liquibase evaluates the next changeset.
With `migration-strategy=manual`, Liquibase writes the statements to a script instead.
An empty database is still initialized automatically unless `--spi-connections-jpa--quarkus--initialize-empty=false` is also set, so use both options to generate a script for the empty-database case.
The database used to generate the script is not changed by those statements, so a precondition or custom change cannot observe a table, column, index, or data update emitted earlier in the same script.

Review every changeset as both a live update and a script-generation step.
Check whether its decision depends on the database before the script, on the result of an earlier statement in the script, or on persisted Liquibase history.

Starting versions
-----------------

Upgrades do not have to be sequential, so a database at 22.0 can be migrated directly to the latest release.
Liquibase runs every changeset not recorded in `DATABASECHANGELOG`, in order, so a changeset can follow any earlier changeset of any release, not only those of its own changelog file.
The generation database of a script stays at the starting version and lacks the objects of every changeset since then.

Never assume a starting version, such as the previous release.
Check each changeset for these starting points:

* an empty database, where every earlier changeset is in the script
* a database at any earlier release, where the changesets since that release are in the script
* a database that recorded an earlier changeset but lacks its object, as with a deferred index

For a data migration, also check the representation of the data at each starting point.
A data change of an earlier release, such as a custom change that moves a value to a new column, is only part of the script and has not run on the generation database.
That database still holds the data in the older form, so a changeset that reads data must read the form its starting version has.

Choosing `onSqlOutput`
----------------------

`onSqlOutput` controls preconditions while Liquibase generates SQL:

* `TEST` evaluates the precondition against the database used to generate the script and includes or skips the changeset from that result.
  Use it when that database has all the state the precondition needs for every possible starting version, such as a database type check.
* `IGNORE` skips the evaluation and emits the changeset unconditionally, while the precondition still protects automatic migration.
  Use it when an earlier changeset in the same script creates or alters the state being tested, or when the starting version may predate it.

`IGNORE` is the default when `onSqlOutput` is omitted, so state the value you intend.
Do not use `FAIL`: in script mode it always fails the precondition, so `onFail="MARK_RAN"` silently drops the changeset from the script and `HALT` aborts the script generation.

Do not use `TEST` for an `indexExists` or `columnExists` check when a changeset of any earlier release could create the target in the same script, because the generation database does not have it yet.
The organization migration in [`jpa-changelog-26.8.0.xml`](../model/jpa/src/main/resources/META-INF/jpa-changelog-26.8.0.xml) uses `IGNORE` for work that relies on a column added earlier in the same script.
A guard that only makes a changeset idempotent, such as `TEST` with `not columnExists` before an `addColumn` (see `26.8.0-org-domain-add-realm-id`), is fine when no earlier changeset of any release adds that column.
Do not copy a comment that says an object "existed before" a release: it holds only for starting versions after that object was added.

Checking persisted changeset history
------------------------------------

Liquibase's `changeSetExecuted` precondition reads its in-memory history, which during script generation also includes changesets that were only written to the script.
It cannot distinguish a changeset applied by an earlier Keycloak version from one emitted earlier in the current script.

Use [`ChangeSetPersistedPrecondition`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/ChangeSetPersistedPrecondition.java) when the decision must be based on the `DATABASECHANGELOG` table as it exists before the script is run.
Supply the exact changeset ID, author, and file, where `file` must match `FILENAME` in that table, for example `META-INF/jpa-changelog-26.5.0.xml`.

The precondition runs against the generation database, so use it with `onSqlOutput="TEST"`; with `IGNORE` it is never evaluated.
Combine it with an existence check, as in `or(indexExists, not(ChangeSetPersistedPrecondition))`.
The changeset then runs if the object exists, or if the changeset that creates it is not persisted, because the starting version predates it and the script emits it earlier.
It is skipped only when that changeset is persisted but the object is missing, which is how a deferred index looks.

Creating and replacing indexes
------------------------------

A new index is a `createIndex` changeset.
Add a `not indexExists` precondition with `onSqlOutput="TEST"` and `onFail="MARK_RAN"` when an operator may already have created an index with that name, for example from SQL logged for a deferred index.
This is safe in script mode only if no changeset of any earlier release can create the index in the same script.
Use a `dbms` precondition when only some databases get the index, and `modifySql` for database-specific syntax such as a filter.
`26.8.0-51854-add-filtered-broker-session-idx` does both for PostgreSQL and SQL Server.

[`CustomCreateIndexChange`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/CustomCreateIndexChange.java) applies to every `createIndex` changeset.
During an automatic migration it skips the index when the table has more rows than the `index-creation-threshold` option of the `connections-liquibase` provider, which defaults to 300000 and is disabled by zero or a negative value.
The rows are counted when the changeset runs, so whether an index is deferred depends on the data and on the starting version, and a table created earlier in the same migration is empty, so its index is created.
The changeset is still recorded as executed, and the database index checker creates the missing index after startup where supported, applying the same `modifySql`, or logs the SQL for an operator.
Manual migration always emits the index statement.
Add an upgrade guide entry for a new index on a table that can exceed the threshold, as the `EVENT_ENTITY` entry in [`changes-26_4_0.adoc`](documentation/upgrading/topics/changes/changes-26_4_0.adoc) does.

Account for the deferral when a later release drops or replaces an index.
For a drop followed by creation under the same name, emit the drop if the index exists or if its creating changeset is not in persisted history.
A script that emitted the old index earlier can then replace it, and automatic migration avoids dropping an index it deferred.
See the `26.8.0-51851-drop-old-expiration-*` changesets.

Writing custom Java changes
---------------------------

A `customChange` generates `SqlStatement` objects in both modes, and its Java logic runs against the connected database even when Liquibase only writes a script.
Generate SQL that is valid when the script is applied later, and do not mutate data from the generation logic.
Do not make generation depend on schema or data produced by earlier statements in the same script, nor on tables or columns added by changesets after the starting version.
For example, [`CustomKeycloakTask`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/CustomKeycloakTask.java) emits nothing when the generation database has no `REALM` table or no realm rows, so such a task cannot migrate data for a script generated against an empty database.
A missing table or column only justifies emitting nothing when no data can exist in any form.
It can also mean that the data is still in the representation an earlier release replaced: a 25.x database has no `IDENTITY_PROVIDER.ORGANIZATION_ID`, but its organizations and their identity provider links are in the `kc.org` entries of `IDENTITY_PROVIDER_CONFIG`.
Skipping on the missing column would drop that data from the script, so read the older representation as well; [`JpaUpdate26_0_0_IdentityProviderAttributesMigration`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/JpaUpdate26_0_0_IdentityProviderAttributesMigration.java) is the change that moves it.
Prefer set-based, repeatable SQL, which does not read data during generation, and database-aware object quoting, as in [`JpaUpdate26_8_0_PopulateOrgDomainRealmId`](../model/jpa/src/main/java/org/keycloak/connections/jpa/updater/liquibase/custom/JpaUpdate26_8_0_PopulateOrgDomainRealmId.java).

Testing database migration
--------------------------

Test automatic migration and the generation and execution of a manual script on an empty database and on an upgrade with existing data, for every affected database family.
The tests below start from an empty database and from 19.0.3.
For any other starting version, check by hand which objects of earlier changesets the generation database would lack.
A custom change that reads data needs a test that generates the script from the oldest release that can hold that data, with rows in the tables it reads: 19.0.3 has no organizations, so it cannot show how organization data is migrated from 25.x.

### Empty database

`testScriptProducesTheSchemaOfTheAutomaticMigration` in [`ManualMigrationEmptyDatabaseDistTest`](../quarkus/tests/integration/src/test/java/org/keycloak/it/cli/dist/ManualMigrationEmptyDatabaseDistTest.java) is the main safety net.
It uses [`ManualMigrationParity`](../quarkus/tests/integration/src/test/java/org/keycloak/it/storage/database/ManualMigrationParity.java) to compare the tables, columns, keys, indexes, sequences, and persisted changesets of both modes on H2.
Do not add a parity-test exception for a new intentional change; make both modes agree.

`testManualScriptProducesTheSchemaOfTheAutomaticMigration` in the PostgreSQL, MariaDB, and MySQL `*DistTest` classes of [`org.keycloak.it.storage.database.dist`](../quarkus/tests/integration/src/test/java/org/keycloak/it/storage/database/dist) runs the same check.
They need the `test-database` profile and a container runtime, see [`quarkus/README.md`](../quarkus/README.md#running-database-tests).
SQL Server and Oracle have no such comparison, so inspect the generated script for them.
Quoting, filtered indexes, data types, and online index support differ between vendors.

### Upgrade with existing data

`MigrationTest` upgrades a database created by an older release and checks the migrated data.
Run it with `-Dmigration.mode=auto` and `-Dmigration.mode=manual`, see the [database migration test instructions](../testsuite/integration-arquillian/HOW-TO-RUN.md#db-migration-test) and [`tests-db.md`](tests-db.md).

### Deferred indexes

Changes involving deferred indexes must keep [`DatabaseIndexCheckerTest`](../tests/base/src/test/java/org/keycloak/tests/db/DatabaseIndexCheckerTest.java) passing, see the [test framework documentation](../test-framework/docs/RUNNING_TESTS.md) for running it against a database.
No migration test sets the threshold, and their tables are far below the default of 300000 rows, so they do not defer an index.
To exercise deferral, start the server with a low threshold, for example `--spi-connections-liquibase--quarkus--index-creation-threshold=1`, against a database with rows in the table that is at an earlier release, because the migration only runs when the stored version differs from the server version.
Check that the log has "Following index should be created", that the changeset is in `DATABASECHANGELOG`, and that the index is missing.
The index checker then creates the index after startup or logs the SQL, depending on the database, so check the result on each affected database.

To only check that the server starts against an empty database, see [`building.md`](building.md) and [`tests.md`](tests.md).
