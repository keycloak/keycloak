Login, Distributed SSO, Distributed Logout, and OAuth Token Grant Examples
===================================
The following examples requires Wildfly 8.0.0, JBoss EAP 6.x, or JBoss AS 7.1.1.  Here's the highlights of the examples
* Delegating authentication of a web app to the remote authentication server via OAuth 2 protocols
* Distributed Single-Sign-On and Single-Logout
* Transferring identity and role mappings via a special bearer token (Skeleton Key Token).
* Bearer token authentication and authorization of JAX-RS services
* Obtaining bearer tokens via the OAuth2 protocol
* Interact with the Keycloak Admin REST Api

There are multiple WAR projects.  These will all run on the same WildFly instance, but pretend each one is running on a different
machine on the network or Internet.
* **customer-app** A WAR application that does remote login using OAuth2 browser redirects with the auth server
* **customer-app-filter** A WAR application secured by the Keycloak Servlet Filter Adapter.  Useful for EE platforms that don't have an adapter.
* **customer-app-js** A pure HTML/Javascript application that does remote login using OAuth2 browser redirects with the auth server
* **customer-app-cli** A pure CLI application that does remote login using OAuth2 browser redirects with the auth server
* **product-app** A WAR application that does remote login using OAuth2 browser redirects with the auth server
* **admin-access-app** A WAR application that does remote REST login to admin console to obtain a list of realm roles from Admin REST API
* **angular-product-app** An Angular JS pure HTML5/Javascript application.
* **database-service** JAX-RS services authenticated by bearer tokens only. The customer and product app invoke on it to get data

The UI of each of these applications is very crude and exists just to show our OAuth2 implementation in action.

_This demo is meant to run on the same server instance as the Keycloak Server!_


Step 1: Make sure you've set up the Keycloak Server
--------------------------------------
The Keycloak Demo Distribution comes with a preconfigured Keycloak server (based on Wildfly).  You can use it out of
the box to run these demos.  So, if you're using this, you can head to Step 2.

Alternatively, you can install the Keycloak Server onto any EAP 6.x, or Wildfly 8.x server, but there is
a few steps you must follow. You need to obtain latest keycloak-war-dist-all.zip.  This distro is used to install Keycloak onto an existing JBoss installation.
This installs the server using a WAR file.

    $ cd ${jboss.as7.home}/standalone
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

WARNING: Note that we don't officially support Keycloak auth-server running on JBoss AS 7.1.1. You can still test examples running on AS 7.1.1,
but then you may need to do few additional changes in examples to point them into external Keycloak server running on WildFly or EAP 6.x.
This is especially changing "auth-server-url" in keycloak.json files to be non-relative as examples and auth-server will run on different server!

Unzipping the adapter ZIP only installs the JAR files.  You must also add the Keycloak Subsystem to the server's
configuration (standalone/configuration/standalone.xml).

For WildFly and JBoss EAP 6.x
    <server xmlns="urn:jboss:domain:1.4">

        <extensions>
            <extension module="org.keycloak.keycloak-subsystem"/>
            ...
        </extensions>

        <profile>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.0">
                <auth-server name="main-auth-server">
                    <enabled>true</enabled>
                    <web-context>auth</web-context>
                </auth-server>
            </subsystem>
            ...
        </profile>

For AS 7.1.1:
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

```
$ cd keycloak/bin
$ ./standalone.sh
```


From existing Wildfly/EAP6/AS7 distro

```
$ cd ${wildfly.jboss.home}/bin
$ ./standalone.sh
```


Step 3: Import the Test Realm
---------------------------------------
Next thing you have to do is import the test realm for the demo.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost:8080/auth/admin/master/console/#/create/realm](http://localhost:8080/auth/admin/master/console/#/create/realm)

Import the testrealm.json file that is in the preconfigured-demo/ example directory.


Step 4: Build and deploy
---------------------------------------
next you must build and deploy

```
cd preconfigured-demo
mvn clean install
```

On EAP6/AS7 run:

```
mvn jboss-as:deploy
```

Or for WildFly run:

```
mvn wildfly:deploy
```


Step 5: Login and Observe Apps
---------------------------------------
Try going to the customer app and view customer data:

[http://localhost:8080/customer-portal/customers/view.jsp](http://localhost:8080/customer-portal/customers/view.jsp)

This should take you to the auth-server login screen.  Enter username: bburke@redhat.com and password: password.

If you click on the products link, you'll be taken to the products app and show a product listing.  The redirects
are still happening, but the auth-server knows you are already logged in so the login is bypassed.

If you click on the logout link of either of the product or customer app, you'll be logged out of all the applications.

The example also shows different methods of client authentication. The customer-portal example is using traditional authentication with client_id and client_secret, 
but the product-portal example is using client authentication with JWT signed by client private key, which is retrieved from the keystore file inside the product-portal WAR.

Step 6: Try the CLI Example
---------------------------
To try the CLI example run the following commands:

$ cd customer-app-cli
$ mvn exec:java

This will open a shell that lets you specify a few different commands. For example type 'login' and press enter to login. Pressing enter with a blank line will display the available commands.

The CLI example has two alternative methods for login. When a browser is available the CLI opens the login form in a browser, and will automatically retrieve the return code by starting a 
temporary web server on a free port. If a browser is not available the URL to login is displayed on the CLI. The user can copy this URL to another computer that has a browser available. The code
is displayed to the user after login and the user has to copy this code back to the application.

Step 7: Admin REST API
----------------------------------
Keycloak has a Admin REST API.  This example shows an application making a remote direct login to Keycloak to obtain a token
then using that token to access the Admin REST API.

[http://localhost:8080/admin-access](http://localhost:8080/admin-access)

If you are already logged in, you will not be asked for a username and password, but you will be redirected to
an oauth grant page.  This page asks you if you want to grant certain permissions to the third-part app.

Step 8: Angular JS Example
----------------------------------
An Angular JS example using Keycloak to secure it.

[http://localhost:8080/angular-product](http://localhost:8080/angular-product)

If you are already logged in, you will not be asked for a username and password, but you will be redirected to
an oauth grant page.  This page asks you if you want to grant certain permissions to the third-part app.

Step 9: Pure HTML5/Javascript Example
----------------------------------
An pure HTML5/Javascript example using Keycloak to secure it.

[http://localhost:8080/customer-portal-js](http://localhost:8080/customer-portal-js)

If you are already logged in, you will not be asked for a username and password, but you will be redirected to
an oauth grant page.  This page asks you if you want to grant certain permissions to the third-part app.

Step 10: Service Account Example
================================
An example for retrieve service account dedicated to the Client Application itself (not to any user). 

[http://localhost:8080/service-account-portal](http://localhost:8080/service-account-portal)

Client authentication is done with OAuth2 Client Credentials Grant in out-of-bound request (Not Keycloak login screen displayed) .

Step 11: Offline Access Example
===============================
An example for retrieve offline token, which is then saved to the database and can be used by application anytime later. Offline token
is valid even if user is already logged out from SSO. Server restart also won't invalidate offline token. Offline token can be revoked by the user in 
account management or by admin in admin console.

[http://localhost:8080/offline-access-portal](http://localhost:8080/offline-access-portal)


Admin Console
==========================

[http://localhost:8080/auth/admin/index.html](http://localhost:8080/auth/admin/index.html)





