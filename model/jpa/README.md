# Rolling updates database compatibility

In order to track database schema changes that are compatible/incompatible with the `rolling-updates` feature, this module
makes use of the `db-compatibility-verifier-maven-plugin`. See `misc/db-compaotibility-verifier/README.md` for detailed
usage instructions.

The rolling-update:v2 feature only supports rolling updates of Keycloak patch releases, therefore database changes
are only tracked in release branches and not `main`.


## Tracking supported database changes

All Liquibase ChangeSets at branch time are considered supported by the `rolling-updates:v2` feature, as this is the
initial database state from the perspective of the current release stream. When creating a new release branch, a "snapshot"
of all known Liquibase ChangeSets in this module are recorded using the `db-compatibility-verifier:snapshot`
maven plugin. This generates two JSON files: a "supported" file with all known ChangeSets and an "unsupported"
file initialized with an empty array. Both of these files must be committed to the repository.

A snapshot can be created by executing:

```
./mvnw clean install -am -pl model/jpa -Pdb-changeset-snapshot -DskipTests
```


## Verifying all database changes are tracked

The `db-compatibility-verifier:verify` plugin is used as part of the `model/jpa` test phase to ensure that
any Liquibase changeset added during the release branches lifecycle are tracked in either the supported or unsupported files.
If one of more unrecorded ChangeSet is detected, contributors need to determine if the ChangeSet is compatible with a
rolling update. If the change is not compatible, then it must be recorded in the unsupported file. Conversely, if it is
compatible it must be recorded in the supported file.

Execution of the `db-compatibility-verifier:verify` plugin can be skipped during the test phase by specifying: `-Ddb.verify.skip=true`.

## Adding a supported database change

To add an individual ChangeSet to the supported file users can execute:

```
./mvnw -pl model/jpa org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:supported \
  -Ddb.verify.changeset.id=<id> \
  -Ddb.verify.changeset.author=<author> \
  -Ddb.verify.changeset.filename=<filename>
```

If multiple ChangeSets exist, and they are all compatible with rolling updates, the following can be used to add all changes:

```
./mvnw -pl model/jpa org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:supported \
  -Ddb.verify.changeset.addAll=true
```


## Adding an unsupported database change

To add an individual ChangeSet to the supported file users can execute:

```
./mvnw -pl model/jpa org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:unsupported \
  -Ddb.verify.changeset.id=<id> \
  -Ddb.verify.changeset.author=<author> \
  -Ddb.verify.changeset.filename=<filename>
```

If multiple ChangeSets exist, and they are all compatible with rolling updates, the following can be used to add all changes:

```
./mvnw -pl model/jpa org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:unsupported \
  -Ddb.verify.changeset.addAll=true
```