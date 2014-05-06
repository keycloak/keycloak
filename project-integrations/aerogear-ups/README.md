Self Bootstrapping Keycloak Server and Application
==========================================================

This is an example of bundling two wars: a keycloak server war and application WAR together so that keycloak is bootstrapped out of the
box.

* There is a testrealm.json file that is used to bootstrap the realm in the auth-server/ project
* Notice that there is a context-param in auth-server/web.xml called keycloak.import.realm.resources.  This sets up the import of the json file
* If you open up testrealm.json, notice that all urls are relative.  Keycloak will now extrapolate the host and port based
on the request if the configured urls are just a path and don't have a schem, host, and port.
* In app, there is a BootstrapListener class.  This obtains the config context of the adapter and initializes it.
* Notice that this class sets up a relative URL.  Also notice that the application is a "public" client.  This is so that
we don't have to query the database for the application's secret.  Also notice that the realm key is not set.  Keycloak adapter
will now query the auth server url for the public key of the realm.

Problems:
* Biggest problem is SSL.  You have to crack open web.xml to set up a confidential security constraint.  You also have
to change the adapter config and the realm config to make SSL required.
* You need to set this logging config in standalone.xml otherwise you will get a lot of warning messages when accessing the admin console

            <logger category="org.jboss.resteasy.core.ResourceLocator">
                <level name="ERROR"/>
            </logger>

