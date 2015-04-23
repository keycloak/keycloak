Testing admin console with Arquillian
=====================================

There are currently two ways of running the tests with help of Arquillian.

Remote mode
----------

Just simply typle `mvn verify` and you are all set. This requires the instance of Wildfly with embedded Keycloak to be already running.

Managed mode
------------

You need to pass two arguments to Maven, first is location of your Wildfly server with embedded Keycloak and the other is name of the profile.

    mvn verify -Pwildfly-8-with-keycloak-managed -DjbossHome=/your/server/location

Browser
-------

There are currently two supported browsers - PhantomJS and Firefox. PhantomJS is the default one, in order to use Firefox just specify `-Dbrowser=firefox` parameter in the Maven command. 
