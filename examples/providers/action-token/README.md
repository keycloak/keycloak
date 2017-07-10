Example Action Token Invoking External Application
==================================================

This example shows how to invoke external application within the authentication
flow. It is implemented by cooperation of required action and action token:

1. During authentication, a required action that mandates cooperation with
   external application is invoked.

2. This required action prepares an action token for the current authentication
   session and redirects to the application, passing the action token along.

3. The application does whatever it is suited for (in this example, lets the
   user set values of two attributes).

4. Application uses the action token obtained in Step 2. to return back to
   authentication flow, providing the required action with its own signed token
   containing values entered by the user.

5. The handler handling that action token takes values of the fields and sets
   the attributes of authenticating user accordingly.

How To Run the Example
======================

To deploy copy target/action-token-example.jar to providers directory. Alternatively you can deploy as a module by running:

    KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.action-token --resources=target/action-token-example.jar --dependencies=org.keycloak.keycloak-common,org.keycloak.keycloak-core,org.keycloak.keycloak-services,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private,org.jboss.logging,javax.api,javax.ws.rs.api"

Then registering the provider by editing `standalone/configuration/standalone.xml` and adding the module to the providers element:

    <providers>
        ...
        <provider>module:org.keycloak.examples.action-token</provider>
    </providers>

Then edit `standalone/configuration/standalone.xml`, add the following spi declarations:

    <spi name="actionTokenHandler">
        <provider name="external-app-notification" enabled="true">
            <properties>
                <property name="hmacSecret" value="aSqzP4reFgWR4j94BDT1r+81QYp/NYbY9SBwXtqV1ko="/>
            </properties>
        </provider>
    </spi>
    <spi name="required-action">
        <provider name="redirect-to-external-application" enabled="true">
            <properties>
                <property name="external-application-url" value="http://localhost:18081/external-action.jsp?token={TOKEN}"/>
            </properties>
        </provider>
    </spi>

Then start (or restart) the server. Once started open the admin console, select
Required Actions tab in Authentication group, and register the new
"Redirect to external application" action. Now select a user and add that
operation into Required User Actions field.

This example requires a web server with the external application. This is
executed by running the following command in the action-token example directory:

    mvn jetty:run

After this try to logout and login again with the user that you have changed
above. You should be redirected to external application and back to login, and
when you check the attributes of that user in admin console, you will see the
entered valued propagated into attribute values.
