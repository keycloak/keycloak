What is this module about?
-------------------------

This module contains integration tests for testing the SSSD features of Keycloak.

Prerequisites
-------------

To run tests inside this module, one needs to have a linux machine configured as an `IPA` client having SSSD
  service started with Infopipe support.

How does one run the tests?
--------------------------

*All the commands are intended to be run from the root `keycloak` project directory.*

First build the distribution of keycloak:
```
mvn clean install -B -DskipTests -Pdistribution
```

It may fail in the end, but it's not a problem as far as it creates a zip distribution of Keycloak inside
distribution/server-dist/target.

Then build the integration-arquillian-servers-auth-server-wildfly artifact:
```
mvn clean install -B -Pauth-server-wildfly -f testsuite/integration-arquillian/servers/pom.xml
```

And then, finally, it's possible to run the tests:
```
mvn test -f testsuite/integration-arquillian/tests/other/sssd/ -Pauth-server-wildfly -Psssd-testing
```