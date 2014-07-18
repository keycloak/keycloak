Self Bootstrapping Keycloak Server and Application
==========================================================

To get this running boot up JBoss AS7, EAP or WildFly.

To deploy to AS7 run:

    mvn clean install jboss-as:deploy

To deploy to WildFly run:

    mvn -Pwildfly clean install wildfly:deploy

This is an example of bundling two wars: a keycloak server war and application WAR together so that keycloak is bootstrapped out of the
box.  The structure of the example is:

* The aerogear UPS application [http://localhost:8080/aerogear-ups/ups/view.jsp](http://localhost:8080/aerogear-ups/ups/view.jsp)
* The aerogear UPS admin [http://localhost:8080/aerogear-ups/admin/admin.html](http://localhost:8080/aerogear-ups/admin/admin.html)
* The aerogear security admin (keycloak)  [http://localhost:8080/auth/admin/aerogear/console/index.html](http://localhost:8080/auth/admin/aerogear/console/index.html)
* The aerogear user account page (keycloak)  [http://localhost:8080/auth/realms/aerogear/account](http://localhost:8080/auth/realms/aerogear/account)
* All of these are managed under one realm "aerogear"
* The login page, admin console, and account pages all use the "aerogear" theme

If you click on any of those URLS, you are brought to a log-in screen.  Username: admin Password: admin.  You will be asked
to change your password.  Once you are logged in, you have SSO to any of those links.

Notes on implementation:

* There is a testrealm.json file that is used to bootstrap the realm in the auth-server/ project
* Notice that there is a context-param in auth-server/web.xml called keycloak.import.realm.resources.  This sets up the import of the json file
* If you open up testrealm.json, notice that all urls are relative.  Keycloak will now extrapolate the host and port based
on the request if the configured urls are just a path and don't have a schem, host, and port.
* In the auth project, there is a AerogearThemeProvider class.  This sets up classloader access to the "aerogear" themes
* In the auth project, there is a UpsSecurityApplication class.  The sole purpose of this class is to disable the "master"
  realm by deleting the master realm's "admin" user
* In the auth project resources/, there are files there to set up all the themes
* In app, there is a BootstrapListener class.  This obtains the config context of the adapter and initializes it.
* Notice that BootstrapListener class sets up a relative URL.  Also notice that the application is a "public" client.  This is so that
we don't have to query the database for the application's secret.  Also notice that the realm key is not set.  Keycloak adapter
will now query the auth server url for the public key of the realm.

Problems:
* Biggest problem is SSL.  You have to crack open web.xml to set up a confidential security constraint.  You also have
to change the adapter config and the realm config to make SSL required.
* You need to set this logging config in standalone.xml otherwise you will get a lot of warning messages when accessing the admin console

            <logger category="org.jboss.resteasy.core.ResourceLocator">
                <level name="ERROR"/>
            </logger>

