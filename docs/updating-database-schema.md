Updating Database Schema
========================

Keycloak supports automatically migrating the database to a new version. This is done by applying one or more change-sets
to the existing database. This means if you need to do any changes to database schemas you need to create 
a change-set that can transform the schema as well as any existing data.

This includes changes to:
 
* Realm entities
* User entities
* User session entities
* Event entities

 
Creating a change-set
-------------------------

We use Liquibase to support updating the database. The change-sets are located in 
[`model/jpa/src/main/resources/META-INF`](../model/jpa/src/main/resources/META-INF).
There's a separate file for each release that requires database changes.

To manually create a change-set add a new file in the above location with the name `jpa-changelog-<version>.xml`. This file 
should contain a single `change-set` with `id` equal to the next version to be released and `author` set to the value `keycloak` 
. Then look at Liquibase documentation on how to write this file. Other option can be to look at existing changesets for the inspiration
and create your changeset based on them. Add a reference to this file in
[`jpa-changelog-master.xml`](../model/jpa/src/main/resources/META-INF/jpa-changelog-master.xml).
The file should have a single change-set and the id of the change-set should be the next version to be released. 

Testing database migration
--------------------------

The first step may be to re-build Keycloak and check if server is started fully. This means that server can be started against
empty DB with your changes. You can either use testing server based on embedded undertow or build fully distribution. See 
[`building.md`](building.md) and [`tests.md`](tests.md) for more details. Also see [`tests-db.md`](tests-db.md) for testing on the
more production non-H2 database.

It is good idea to run DB migration afterwards. See [`testsuite/integration-arquillian/HOW-TO-RUN.md`](../testsuite/integration-arquillian/HOW-TO-RUN.md#db-migration-test)
for more details.