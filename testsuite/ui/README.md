Testing admin console with Arquillian
=====================================

There are currently two ways of running the tests with help of the Arquillian.

Remote mode
----------

Just simply typle `mvn verify` and you are all set. This requires the instance of Wildfly with embedded Keycloak to be already running.

Managed mode
------------

You need to pass two arguments to Maven, first is location of your Wildfly server with embedded Keycloak and the other is name of the profile.

    mvn verify -Pwildfly-8-with-keycloak-managed -DjbossHome=/your/server/location
