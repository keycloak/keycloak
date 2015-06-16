Login, Distributed SSO, Distributed Logout, and OAuth Token Grant Examples
===================================
The following examples require Wildfly 8.x / 9.x, JBoss EAP 6.x, or JBoss AS 7.1.1.  Here's the highlights of the examples
* Delegating authentication of a web app to the remote authentication server via OAuth 2 protocols
* Distributed Single-Sign-On and Single-Logout
* Transferring identity and role mappings via a special bearer token (Skeleton Key Token).
* Bearer token authentication and authorization of JAX-RS services
* Obtaining bearer tokens via the OAuth2 protocol
* Interact with the Keycloak Admin REST API

There are multiple WAR projects.  These will all run on the same WildFly instance, but pretend each one is running on a different
machine on the network or Internet.
* **customer-app** A WAR application that does remote login using OAuth2 browser redirects with the auth server
* **customer-app-js** A pure HTML/Javascript application that does remote login using OAuth2 browser redirects with the auth server
* **customer-app-cli** A pure CLI application that does remote login using OAuth2 browser redirects with the auth server
* **product-app** A WAR application that does remote login using OAuth2 browser redirects with the auth server
* **admin-access-app** A WAR application that does remote REST login to admin console to obtain a list of realm roles from Admin REST API
* **angular-product-app** An Angular JS pure HTML5/Javascript application.
* **database-service** JAX-RS services authenticated by bearer tokens only. The customer and product app invoke on it to get data
* **third-party** Simple WAR that obtain a bearer token using OAuth2 using browser redirects to the auth-server.
* **third-party-cdi** Simple CDI/JSF WAR that obtain a bearer token using OAuth2 using browser redirects to the auth-server.

The UI of each of these applications is very crude and exists just to show our OAuth2 implementation in action.

_This demo is meant to run on the same server instance as the Keycloak Server!_


Step 1: Make sure you've set up the Keycloak Server
--------------------------------------
The Keycloak Demo distribution comes with a preconfigured Keycloak server (based on Wildfly 9).  You can use it out of
the box to run these demos.  So, if you're using this, you can head to Step 2.

Alternatively, you can install the Keycloak Server onto any EAP 6.x, or Wildfly 9.x server, but there is
a few steps you must follow. You need to obtain the latest Keycloak Overlay distribution. That distribution is used to install Keycloak onto an existing EAP / Wildfly installation
by providing all the necessary Keycloak Server modules, and configurations.

For Wildfly 9:

    $ cd ${wildfly.home}
    $ unzip ${keycloak-overlay.zip}

For JBoss EAP 6.x:

    $ cd ${jboss.eap6.home}
    $ unzip ${keycloak-overlay-eap6.zip}



To be able to run the demos you also need to install the Keycloak Adapter which extends your app server with KEYCLOAK authentication mechanism.


For Wildfly 9:

    $ cd ${wildfly.home}
    $ unzip ${keycloak-wf9-adapter-dist.zip}

For JBoss EAP 6.x

    $ cd ${jboss.eap6.home}
    $ unzip ${keycloak-eap6-adapter-dist.zip}

For JBoss AS 7.1.1:

    $ cd ${as7.home}
    $ unzip ${keycloak-as7-adapter-dist.zip}


In addition to unzipping the adapter .zip we also have to add the keycloak-adapter-subsystem to the app server's configuration file.
If you install into the same app server you installed Keycloak Overlay into then use standalone/configuration/standalone-keycloak.xml.
Otherwise use standalone/configuration/standalone.xml.

    <server xmlns="urn:jboss:domain:1.4">

        <extensions>
            ...
            <extension module="org.keycloak.keycloak-adapter-subsystem"/>
        </extensions>

        <profile>
            ...
            <subsystem xmlns="urn:jboss:domain:keycloak:1.1"/>
        </profile>
        
        ...
    </server>


WARNING: Note that we only target Wildfly 9, and EAP 6.4 for Keycloak Server. While you can still test examples running on AS 7.1.1, you may need to do a few additional changes in examples to point them to external Keycloak Server running on Wildfly 9 or EAP 6.4.
Specifically, "auth-server-url" attribute in keycloak.json files has to be set to an absolute URL since examples will run on a different app server than Keycloak Server.


Step 2: Start up the Keycloak Server
---------------------------------------

The exact command to start up the server depends on the installation method chosen in Step 1.

For Keycloak Demo distribution - which includes Keycloak Server, and Keycloak Adapter:

```
$ cd keycloak-demo
$ bin/standalone.sh
```


For Keycloak Server deployed to existing Wildfly 9 / EAP 6 server using Keycloak Overlay distribution:

```
$ cd ${jboss.home}
$ bin/standalone.sh -c standalone-keycloak.xml
```

For AS 7 / EAP 6 / Wildfly server containing example applications only - without the Keycloak Server:

```
$ cd ${jboss.home}
$ bin/standalone.sh
```
  

Step 3: Import the Test Realm
---------------------------------------
Next thing to do is to import the test realm for the demo.  Clicking on the below link will bring you to the
Create Realm page in the Admin UI.  The username/password is admin/admin.  Keycloak will ask you to
create a new admin password the first time you try to log in. You can simply re-enter admin/admin.

[http://localhost:8080/auth/admin/master/console/#/create/realm](http://localhost:8080/auth/admin/master/console/#/create/realm)

Import the testrealm.json file from examples/preconfigured-demo directory.


Step 4: Build and deploy
---------------------------------------
Next, we build and deploy

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

This should take you to the Keycloak Server login screen.  Enter username: bburke@redhat.com and password: password.

If you click on the products link, you'll be taken to the products app and see a product listing.  The redirects
are still happening, but the Keycloak Server knows you are already logged in so the login is bypassed.

If you click on the logout link of either the product or customer app, you'll be logged out of all the applications.

If you click on [http://localhost:8080/customer-portal-js](http://localhost:8080/customer-portal-js) you can invoke
the pure HTML/Javascript application.

Step 6: Traditional OAuth2 Example
----------------------------------
The customer and product apps use web forms for login.  The third-party app is the traditional OAuth2 usecase of a client wanting
to get permission to access user's data. To run this example open:

[http://localhost:8080/oauth-client](http://localhost:8080/oauth-client)

If you are already logged in, you will not be asked for a username and password, but will be redirected to
an oauth grant page. The page asks you if you want to grant certain permissions to the third-part app.

Step 7: Try the CLI Example
---------------------------
To try the CLI example run the following commands:

$ cd customer-app-cli
$ mvn exec:java

That will open a shell which lets you specify a few different commands. For example, type 'login' and press enter to login. Pressing enter with a blank line will display the available commands.

The CLI example has two alternative methods for login. When a browser is available the CLI opens the login form in a browser, and automatically retrieves the return code by starting a 
temporary web server on an available port. If there is no browser available, the login URL is printed in console. User can copy this URL to another computer that has a browser available. After successful login
the code is displayed which the user has to copy back to the application.

Step 8: Admin REST API
----------------------------------
Keycloak comes with an Admin REST API. This example demonstrates how an application remotely logs into Keycloak to obtain a token
which it then uses to access the Admin REST API.

[http://localhost:8080/admin-access](http://localhost:8080/admin-access)

If you are already logged in, you will not be asked for a username and password, and will be redirected straight to
an oauth grant page.  The page asks you to grant certain permissions to the third-part app.

Step 9: Angular JS Example
----------------------------------
An example shows how to secure an Angular JS application using Keycloak.

[http://localhost:8080/angular-product](http://localhost:8080/angular-product)

If you are already logged in, you will not be asked for a username and password, and will be redirected straight to
an oauth grant page.  The page asks you to grant certain permissions to the third-part app.


Step 9: Pure HTML5/Javascript Example
----------------------------------
A pure HTML5/Javascript example secured by Keycloak.

[http://localhost:8080/customer-portal-js](http://localhost:8080/customer-portal-js)

If you are already logged in, you will not be asked for a username and password, and will be redirected straight to
an oauth grant page.  The page asks you to grant certain permissions to the third-part app.

Admin Console
==========================

[http://localhost:8080/auth/admin/index.html](http://localhost:8080/auth/admin/index.html)





