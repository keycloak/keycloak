# Keycloak Broker: Google Social Identity Provider Quickstarts

What is it?
-----------

This example demonstrates how to use Social Ientity Providers with KeyCloak to authenticate users. In this case,
users are authenticated with Google using KeyCloak Identity Broker capabilities using the oAuth 2 protocol.

From this example, you'll learn how to:

* Setup a social identity provider for a specific realm
* Store tokens from a social identity provider and use these tokens to invoke the social provider API

Basically, once you try to access the application for the first time, you'll be redirected to KeyCloak's login page.
In this page you'll note that there is a "Google" button that allows you to authenticate with Google Identity Provider.

After clicking the "Google" button, you'll be redirected to Google's login page from where you must authenticate
and grant the necessary permissions to KeyCloak in order to access your personal information from Google.

If everything is fine, Google will redirect you back to KeyCloak and at this point you'll be asked to provide some
basic profile information in order to create a new user in KeyCloak based on your social account. Once you update your profile,
you'll be authenticated and redirected to the application.

Basically, what the application does is obtain some basic information for the authenticated user and also allow users to
load their profile from Google. For that, this application demonstrates how to retrieve the token issued by a social provider
for the authenticated user and use this token to invoke Google's API.

Make sure you've set up a application in Google
--------------------------------------

This example application requires you to create a Google Application. How to create it is beyond the scope of this
documentation.

Please take a look on [Google Developer Console](https://cloud.google.com/console/project) for more details.

Once you have a Google Application configured, you need to obtain both **Client ID** and **Client Secret** and update the
**google-identity-provider-realm.json** configuration file with these information. There you'll find a section as follows:

        "identityProviders": [
                {
                  "id" : "google",
                  "providerId" : "google",
                  "name" : "Google",
                  "enabled": true,
                  "updateProfileFirstLogin" : "true",
                  "storeToken" : "true",
                  "config": {
                    "clientId": "CHANGE_CLIENT_ID",
                    "clientSecret": "CHANGE_CLIENT_SECRET"
                  }
                }
            ]

Please, update both *clientId* and *clientSecret* configuration options with the **Client ID** and **Client Secret**.

Make sure to use the correct redirect URI to be used as URL on Google. The Google will redirect to this URI after finish authentication. For this example, it's the URL
[http://localhost:8080/auth/realms/google-identity-provider-realm/broker/google](http://localhost:8080/auth/realms/google-identity-provider-realm/broker/google) .
You can also determine the redirect URI from Keycloak admin console (It's in Identity provider settings for Google provider).

Make sure you've set up the Keycloak Server
--------------------------------------
The Keycloak Appliance Distribution comes with a preconfigured Keycloak server (based on Wildfly).  You can use it out of
the box to run these demos.  So, if you're using this, you can head to Step 2.

Alternatively, you can install the Keycloak Server onto any EAP 6.x, or Wildfly 8.x server, but there is
a few steps you must follow.

Obtain latest keycloak-war-dist-all.zip.  This distro is used to install Keycloak onto an existing JBoss installation.
This installs the server.

    $ cd ${wildfly.jboss.home}/standalone
    $ cp -r ${keycloak-war-dist-all}/deployments .

To be able to run the demos you also need to install the Keycloak client adapter. For Wildfly:

    $ cd ${wildfly.home}
    $ unzip ${keycloak-war-dist-all}/adapters/keycloak-wildfly-adapter-dist.zip

For JBoss EAP 6.x

    $ cd ${eap.home}
    $ unzip ${keycloak-war-dist-all}/adapters/keycloak-eap6-adapter-dist.zip

For JBoss AS 7.1.1:

    $ cd ${as7.home}
    $ unzip ${keycloak-war-dist-all}/adapters/keycloak-as7-adapter-dist.zip

Unzipping the adapter ZIP only installs the JAR files.  You must also add the Keycloak Subsystem to the server's
configuration (standalone/configuration/standalone.xml).

    <server xmlns="urn:jboss:domain:1.4">

        <extensions>
            <extension module="org.keycloak.keycloak-subsystem"/>
            ...
        </extensions>

        <profile>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.0"/>
            ...
        </profile>

Boot Keycloak Server
---------------------------------------
Where you go to start up the Keycloak Server depends on which distro you installed.

From appliance:

```
$ cd keycloak/bin
$ ./standalone.sh
```


From existing Wildfly/EAP6/AS7 distro

```
$ cd ${wildfly.jboss.home}/bin
$ ./standalone.sh
```


Import the Test Realm
---------------------------------------
Next thing you have to do is import the test realm for the demo.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost:8080/auth/admin/master/console/#/create/realm](http://localhost:8080/auth/admin/master/console/#/create/realm)

Import the **google-identity-provider-realm.json** file that is in the google-authentication/ example directory.


Start JBoss Enterprise Application Platform 6 or WildFly with the Web Profile
-------------------------

1. Open a command line and navigate to the root of the JBoss server directory.
2. The following shows the command line to start the server with the web profile:

        For Linux:   JBOSS_HOME/bin/standalone.sh
        For Windows: JBOSS_HOME\bin\standalone.bat


Build and Deploy the Quickstart
-------------------------

_NOTE: The following build command assumes you have configured your Maven user settings. If you have not, you must include Maven setting arguments on the command line. See [Build and Deploy the Quickstarts](../README.md#build-and-deploy-the-quickstarts) for complete instructions and additional options._

1. Make sure you have started the JBoss Server as described above.
2. Open a command line and navigate to the root directory of this quickstart.
3. Type this command to build and deploy the archive:

        For EAP 6:     mvn clean package jboss-as:deploy
        For WildFly:   mvn -Pwildfly clean package wildfly:deploy

4. This will deploy `target/google-authentication.war` to the running instance of the server.


Access the application
---------------------

The application will be running at the following URL: <http://localhost:8080/google-authentication>.


Undeploy the Archive
--------------------

1. Make sure you have started the JBoss Server as described above.
2. Open a command line and navigate to the root directory of this quickstart.
3. When you are finished testing, type this command to undeploy the archive:

        For EAP 6:     mvn jboss-as:undeploy
        For WildFly:   mvn -Pwildfly wildfly:undeploy


Debug the Application
------------------------------------

If you want to debug the source code or look at the Javadocs of any library in the project, run either of the following commands to pull them into your local repository. The IDE should then detect them.

        mvn dependency:sources
        mvn dependency:resolve -Dclassifier=javadoc