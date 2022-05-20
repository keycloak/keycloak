## Building from source

Ensure you have JDK 8 (or newer), Maven 3.5.4 (or newer) and Git installed

    java -version
    mvn -version
    git --version

**NOTE**

If you use Maven 3.8.1 or newer, please add the following mirror to your local
`~/.m2/settings.xml` to avoid build errors:
```
<mirrors>
  <mirror>
    <id>jboss-public-repository-group-https</id>
    <mirrorOf>jboss-public-repository-group</mirrorOf>
    <name>Jboss public https</name>
    <url>https://repository.jboss.org/nexus/content/groups/public/</url>
  </mirror>
</mirrors>
```
See [KEYCLOAK-17812](https://issues.redhat.com/browse/KEYCLOAK-17812) for more details.

---    
First clone the Keycloak repository:
    
    git clone https://github.com/keycloak/keycloak.git
    cd keycloak
    
To build Keycloak run:

    mvn clean install
    
This will build all modules and run the testsuite. 

To build the ZIP distribution run:

    mvn clean install -Pdistribution
    
Once completed you will find distribution archives in `distribution`.

To build only the server run:

    mvn -Pdistribution -pl distribution/server-dist -am -Dmaven.test.skip clean install

---
**NOTE**

Classes from `org.keycloak.testsuite.*` packages aren't suitable to be used in production.

---

### Building Quarkus Distribution

Please, take a look at this [documentation](../quarkus/README.md).

## Starting Keycloak

To start Keycloak during development first build as specified above, then run:

    mvn -f testsuite/utils/pom.xml exec:java -Pkeycloak-server 

When running testsuite, by default an account with username `admin` and password `admin` will be created within the master realm at start.

To start Keycloak from the server distribution first build the distribution it as specified above, then run:

    tar xfz distribution/server-dist/target/keycloak-<VERSION>.tar.gz
    cd keycloak-<VERSION>
    bin/standalone.sh
    
To stop the server press `Ctrl + C`.


## Working with the codebase

We don't currently enforce a code style in Keycloak, but a good reference is the code style used by WildFly. This can be 
retrieved from [Wildfly ide-configs](https://github.com/wildfly/wildfly-core/tree/main/ide-configs).To import formatting 
rules, see following [instructions](http://community.jboss.org/wiki/ImportFormattingRules).

If your changes require updates to the database read [Updating Database Schema](updating-database-schema.md).

If your changes require introducing new dependencies or updating dependency versions please discuss this first on the
dev mailing list. We do not accept new dependencies to be added lightly, so try to use what is available.
