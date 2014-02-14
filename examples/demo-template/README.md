Login, Distributed SSO, Distributed Logout, and OAuth Token Grant Examples
===================================
The following examples requires Wildfly 8.0.0, JBoss EAP 6.x, or JBoss AS 7.1.1.  Here's the highlights of the examples
* Delegating authentication of a web app to the remote authentication server via OAuth 2 protocols
* Distributed Single-Sign-On and Single-Logout
* Transferring identity and role mappings via a special bearer token (Skeleton Key Token).
* Bearer token authentication and authorization of JAX-RS services
* Obtaining bearer tokens via the OAuth2 protocol

There are multiple WAR projects.  These will all run on the same WildFly instance, but pretend each one is running on a different
machine on the network or Internet.
* **customer-app** A WAR application that does remote login using OAuth2 browser redirects with the auth server
* **product-app** A WAR application that does remote login using OAuth2 browser redirects with the auth server
* **database-service** JAX-RS services authenticated by bearer tokens only. The customer and product app invoke on it to get data
* **third-party** Simple WAR that obtain a bearer token using OAuth2 using browser redirects to the auth-server.
* **third-party-cdi** Simple CDI/JSF WAR that obtain a bearer token using OAuth2 using browser redirects to the auth-server.

The UI of each of these applications is very crude and exists just to show our OAuth2 implementation in action.

_This demo is meant to run on the same server instance as the Keycloak Server!_


Step 1: Make sure you've set up the Keycloak Server
--------------------------------------
The Keycloak Appliance Distribution comes with a preconfigured Keycloak server (based on Wildfly).  You can use it out of
the box to run these demos.  So, if you're using this, you can head to Step 2.

Alternatively, you can install the Keycloak Server onto any JBoss AS 7.1.1, EAP 6.x, or Wildfly 8.x server, but there is
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

For Wildfly:

    <server xmlns="urn:jboss:domain:1.4">

        <extensions>
            <extension module="org.keycloak.keycloak-wildfly-subsystem"/>
            ...
        </extensions>

        <profile>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.0"/>
            ...
        </profile>

For JBoss 7.1.1 and EAP 6.x:

    <server xmlns="urn:jboss:domain:1.4">

        <extensions>
            <extension module="org.keycloak.keycloak-as7-subsystem"/>
            ...
        </extensions>

        <profile>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.0"/>
            ...
        </profile>


Step 2: Boot Keycloak Server
---------------------------------------
Where you go to start up the Keycloak Server depends on which distro you installed.

From appliance:
$ cd keycloak/bin
$ ./standalone.sh

From existing Wildfly/EAP6/AS7 distro
$ cd ${wildfly.jboss.home}/bin
$ ./standalone.sh

Step 3: Import the Test Realm
---------------------------------------
Next thing you have to do is import the test realm for the demo.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost:8080/auth/admin/index.html#/create/realm](http://localhost:8080/auth/admin/index.html#/create/realm)

Import the testrealm.json file that is in the preconfigured-demo/ example directory.


Step 4: Build and deploy
---------------------------------------
next you must build and deploy

1. cd preconfigured-demo
2. mvn clean install
3. mvn jboss-as:deploy

Please note that jboss-as:deploy may fail on Wildfly distributions.  This is because Wildfly 8.0.0.Final has turned
off a management interface.  You will hae to add this back in order to run the build.  Edit standalone/configuration/standalone.xml

Add the native-itnerface to <management> element's <management-interfaces>:

    <management>
        <management-interfaces>
            <native-interface security-realm="ManagementRealm">
                <socket-binding native="management-native"/>
            </native-interface>
            ...
        </management-interfaces>
    </management>

Then add a socket port mapping for the management interface :

    <socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
        <socket-binding name="management-native" interface="management" port="${jboss.management.native.port:9999}"/>



Step 5: Login and Observe Apps
---------------------------------------
Try going to the customer app and view customer data:

[http://localhost:8080/customer-portal/customers/view.jsp](http://localhost:8080/customer-portal/customers/view.jsp)

This should take you to the auth-server login screen.  Enter username: bburke@redhat.com and password: password.

If you click on the products link, you'll be taken to the products app and show a product listing.  The redirects
are still happening, but the auth-server knows you are already logged in so the login is bypassed.

If you click on the logout link of either of the product or customer app, you'll be logged out of all the applications.

Step 6: Traditional OAuth2 Example
----------------------------------
The customer and product apps are logins.  The third-party app is the traditional OAuth2 usecase of a client wanting
to get permission to access a user's data. To run this example open

[http://localhost:8080/oauth-client](http://localhost:8080/oauth-client)

If you area already logged in, you will not be asked for a username and password, but you will be redirected to
an oauth grant page.  This page asks you if you want to grant certain permissions to the third-part app.

Admin Console
==========================

[http://localhost:8080/auth/admin/index.html](http://localhost:8080/auth/admin/index.html)





