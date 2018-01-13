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
should contain a single `change-set` with `id` equal to the next version to be released and `author` set to your email 
address. Then look at Liquibase documentation on how to write this file. Add a reference to this file in
[`jpa-changelog-master.xml`](../model/jpa/src/main/resources/META-INF/jpa-changelog-master.xml).
The file should have a single change-set and the id of the change-set should be the next version to be released. 

You can also have Liquibase and Hibernate create one for you. To do this follow these steps:

1. Delete existing databases  
   `rm keycloak*h2.db`
2. Create a database of the old format:  
   `mvn -f connections/jpa-liquibase/pom.xml liquibase:update -Durl=jdbc:h2:keycloak`
3. Make a copy of the database:  
   `cp keycloak.h2.db keycloak-old.h2.db`    
3. Run KeycloakServer to make Hibernate update the schema:  
   `mvn -f testsuite/utils/pom.xml exec:java -Pkeycloak-server -Dkeycloak.connectionsJpa.url='jdbc:h2:keycloak' -Dkeycloak.connectionsJpa.databaseSchema='development-update'`
4. Wait until server is completely started, then stop it
5. View the difference:                                       
   `mvn -f connections/jpa-liquibase/pom.xml liquibase:diff -Durl=jdbc:h2:keycloak-old -DreferenceUrl=jdbc:h2:keycloak`
6. Create a change-set file:
   `mvn -f connections/jpa-liquibase/pom.xml liquibase:diff -Durl=jdbc:h2:keycloak-old -DreferenceUrl=jdbc:h2:keycloak -Dliquibase.diffChangeLogFile=changelog.xml`    
    
This will generate the file `changelog.xml`. Once it's generated edit the file and combine all `change-sets` into
a single `change-set` and change the `id` to the next version to be released and `author` to your email address. Then
follow the steps above to copy it to the correct location and update `jpa-changelog-master.xml`. You have to manually
add entries to the `change-set` to update existing data if required. 

When you have update the change-set Hibernate can validate the schema for you. First run:

    rm -rf keycloak*h2.db
    mvn -f testsuite/utils/pom.xml exec:java -Pkeycloak-server -Dkeycloak.connectionsJpa.url='jdbc:h2:keycloak' -Dkeycloak.connectionsJpa.databaseSchema='update'
    
Once the server has started fully, stop it and run:
    
    mvn -f testsuite/utils/pom.xml exec:java -Pkeycloak-server -Dkeycloak.connectionsJpa.url='jdbc:h2:keycloak' -Dkeycloak.connectionsJpa.databaseSchema='development-validate'


Testing database migration
--------------------------

Get the database from an old version of Keycloak that includes the demo applications. Start the server with this and test it.
