Example Event Listener that prints events to System.out
=======================================================

To deploy copy target/event-listener-sysout-example.jar to providers directory. Alternatively you can deploy as a module by running:

    KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.event-sysout --resources=target/event-listener-sysout-example.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private"

Then registering the provider by editing `standalone/configuration/standalone.xml` and adding the module to the providers element:

    <providers>
        ...
        <provider>module:org.keycloak.examples.event-sysout</provider>
    </providers>

Then start (or restart) the server. Once started open the admin console, select your realm, then click on Events, 
followed by config. Click on Listeners select box, then pick sysout from the dropdown. After this try to logout and 
login again to see events printed to System.out.

The example event listener can be configured to exclude certain events, for example to exclude REFRESH_TOKEN and
CODE_TO_TOKEN events add the following to `standalone.xml`:

    ...
    <spi name="eventsListener">
        <provider name="sysout">
            <properties>
                <property name="exclude-events" value="[&quot;REFRESH_TOKEN&quot;, &quot;CODE_TO_TOKEN&quot;]"/>
            </properties>
        </provider
    </spi>
