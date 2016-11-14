Example Event Store that stores events in memory
================================================

To deploy copy target/event-store-mem-example.jar to providers directory. Alternatively you can deploy as a module by running:

    KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.event-inmem --resources=target/event-store-mem-example.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private"

Then registering the provider by editing `standalone/configuration/standalone.xml` and adding the module to the providers element:

    <providers>
        ...
        <provider>module:org.keycloak.examples.event-inmem</provider>
    </providers>

Then edit `standalone/configuration/standalone.xml`, change:

    <spi name="eventsStore">
        <default-provider>jpa</default-provider>
    </spi>

to:

    <spi name="eventsStore">
        <default-provider>in-mem</default-provider>
    </spi>

Then start (or restart)the server. Once started open the admin console, select your realm, then click on Events, 
followed by config. Set the toggle for Enabled to ON. After this try to logout and login again then open the Events tab 
again in the admin console to view events from the in-mem provider.
