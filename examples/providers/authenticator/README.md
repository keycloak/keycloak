Example Custom Authenticator
===================================================

This is an example of defining a custom Authenticator and Required action.  This example is explained in the user documentation
of Keycloak.   To deploy, build this directory then take the jar and copy it to providers directory. Alternatively you can deploy as a module by running:

    KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.secret-question --resources=target/authenticator-required-action-example.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-services,org.jboss.resteasy.resteasy-jaxrs,javax.ws.rs.api"

Then registering the provider by editing keycloak-server.json and adding the module to the providers field:

    "providers": [
        ....
        "module:org.keycloak.examples.secret-question"
    ],


You then have to copy the secret-question.ftl and secret-question-config.ftl files to the themes/base/login directory.

After you do all this, you then have to reboot keycloak.  When reboot is complete, you will need to log into
the admin console to create a new flow with your new authenticator.

If you go to the Authentication menu item and go to the Flow tab, you will be able to view the currently
defined flows.  You cannot modify an built in flows, so, to add the Authenticator you
have to copy an existing flow or create your own.

Next you have to register your required action.
Click on the Required Actions tab.  Click on the Register button and choose your new Required Action.
Your new required action should now be displayed and enabled in the required actions list.

I'm hoping the UI is intuitive enough so that you
can figure out for yourself how to create a flow and add the Authenticator and Required Action.  We're looking to add a screencast
to show this in action.
