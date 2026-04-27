# Rolling updates database compatibility

In order to track database schema changes that are compatible/incompatible with the `rolling-updates` feature, this module
makes use of the `db-compatibility-verifier-maven-plugin`. See `misc/db-compaotibility-verifier/README.md` for detailed
usage instructions.

The rolling-update:v2 feature only supports rolling updates of Keycloak patch releases, therefore database changes
are only tracked in release branches and not `main`.

## Tracking supported org.keycloak.migration.migrators.Migration implementations

All `org.keycloak.migration.migrators.Migration` implementations should be created in this module's `org.keycloak.migration.migrators`
package.

All `Migration` implementations in this module are considered supported by the `rolling-updates:v2` feature at branch creation time,
as this is the initial database state from the perspective of the current release stream. When creating a new release branch, a "snapshot"
of all known `Migration` implementations in this module is recorded using the `db-compatibility-verifier:snapshot`
maven plugin. This generates two JSON files: a "supported" file with all known Migrations and an "unsupported"
file initialized with an empty array. Both of these files must be committed to the repository.

A snapshot can be created by executing:

```
./mvnw clean install -am -pl model/storage-private -Pdb-snapshot -DskipTests
```

## Verifying all database changes are tracked

The `db-compatibility-verifier:verify` plugin is used as part of the `model/storage-private` test phase to ensure that
any `Migration` implementation added during the release branches lifecycle are tracked in either the supported or unsupported files.
If one of more unrecorded Migration is detected, contributors need to determine if the ChangeSet is compatible with a
rolling update. If the change is not compatible, then it must be recorded in the unsupported file. Conversely, if it is
compatible it must be recorded in the supported file.

Execution of the `db-compatibility-verifier:verify` plugin can be skipped during the test phase by specifying: `-Ddb.verify.skip=true`.

## Adding a supported database change

To add an individual Migration to the supported file users can execute:

```
./mvnw -pl model/storage-private org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:supported \
  -Ddb.verify.migration.class=org.keycloak.migration.migrators.MigrateTo<x_y_z>
```

## Adding an unsupported database change

To add an individual ChangeSet to the supported file users can execute:

```
./mvnw -pl model/storage-private org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:unsupported \
  -Ddb.verify.migration.class=org.keycloak.migration.migrators.MigrateTo<x_y_z>
```