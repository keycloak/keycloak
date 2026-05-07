# Database Compatibility Verifier Maven Plugin

## Overview

This Maven plugin is used to verify the database compatibility of Keycloak. It ensures that all database schema changes
are explicitly marked as either supported or unsupported by the rolling upgrades feature.

## Goals

The plugin provides the following goals:

*   `db-compatibility-verifier:snapshot`
*   `db-compatibility-verifier:verify`
*   `db-compatibility-verifier:supported`
*   `db-compatibility-verifier:unsupported`

## Usage

### `snapshot` - Creates a snapshot of the current database ChangeSets and org.keycloak.migration.migrators.Migration implementations.

This goal is used to create an initial snapshot of liquibase ChangeSets and org.keycloak.migration.migrators.Migration implementations.
It creates a supported and unsupported JSON file, specified via the `db.verify.supportedFile` and `db.verify.unsupportedFile` property, respectively.

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:snapshot  \
  -Ddb.verify.supportedFile=<relative-path-to-create-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-create-json-file> \
  -Ddb.verify.migration.package=org.keycloak.example # Optional java package containing org.keycloak.migration.migrators.Migration implementations
```

The `supportedFile` will be created with a record of all known ChangeSets and Migrations. The `unsupportedFile` will be initialized
with empty JSON arrays.

Each file is created with the following JSON format:

```json
{
  "changeSets" : [
    {
      "id" : "<id>",
      "author" : "<author>",
      "filename" : "<filename>"
    }
  ],
  "migrations" : [
    {
      "class" : "<fully-qualified-class-name>"
    }
  ]
}
```

### `verify` - Verifies that all detected ChangeSets and Migrations are recorded in either the supported or unsupported JSON files.

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:verify \
  -Ddb.verify.supportedFile=<relative-path-to-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-json-file>
```

### `supported` - Adds one or all missing ChangeSets, or Migration, to the supported JSON file

This goal is used to mark a ChangeSet as supported for rolling upgrades.

To mark a single ChangeSet as supported:

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:supported \
  -Ddb.verify.supportedFile=<relative-path-to-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-json-file> \
  -Ddb.verify.changset.id=<id> \
  -Ddb.verify.changset.author=<author> \
  -Ddb.verify.changset.filename=<filename>
```

To mark all missing ChangeSets as supported:

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:supported \
  -Ddb.verify.supportedFile=<relative-path-to-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-json-file> \
  -Ddb.verify.changset.addAll=true
```

To mark a Migration as supported:

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:supported \
  -Ddb.verify.supportedFile=<relative-path-to-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-json-file> \
  -Ddb.verify.migration.class=org.example.migration.MigrationExample
```

### `unsupported` - Adds one or all missing ChangeSets to the unsupported JSON file

This goal is used to mark a ChangeSet as unsupported for rolling upgrades.

To mark a single ChangeSet as unsupported:

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:unsupported \
  -Ddb.verify.supportedFile=<relative-path-to-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-json-file> \
  -Ddb.verify.changset.id=<id> \
  -Ddb.verify.changset.author=<author> \
  -Ddb.verify.changset.filename=<filename>
```

To mark all missing ChangeSets as unsupported:

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:unsupported \
  -Ddb.verify.supportedFile=<relative-path-to-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-json-file> \
  -Ddb.verify.changset.addAll=true
```

To mark a Migration as unsupported:

```bash
mvn org.keycloak:db-compatibility-verifier-maven-plugin:999.0.0-SNAPSHOT:unsupported \
  -Ddb.verify.supportedFile=<relative-path-to-json-file> \
  -Ddb.verify.unsupportedFile=<relative-path-to-json-file> \
  -Ddb.verify.migration.class=org.example.migration.MigrationExample
```
