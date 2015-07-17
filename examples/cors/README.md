Keycloak CORS support
===================================
The following examples requires Wildfly 8.0.0, JBoss EAP 6.x, or JBoss AS 7.1.1.  This example simulates Browser CORS
requests.  While the examples will run on one machine, the servers/applications are configured to point to different domains:
* **localhost-auth** is where the Keycloak auth server lives
* **localhost-db** is where a database REST service lives
* **localhost** is where the Javascript application lives

In the demo you will visit the Javascript application and be redirected to Keycloak to login.  As part of the login process,
the javascript application will have to make a CORS request to the auth server (localhost-auth) to obtain a token.  After it logs in, the
application will make another CORS request to the REST database service (localhost-db).

Here are some of the configuration additions to this example to enable CORS:
1. The **angular-product** application in realm configuration has a Web Origin of **http://localhost:8080**.  When you log into
the angular-product application, Keycloak will add the Web Origins for that application to the token.  Any CORS request made
will check these allowed origins to make sure they match up with the Origin header the browser is sending
2. The **angular-product** application config (keycloak.json) points the auth-server at **http://localhost-auth:8080/auth**
3. The **database-service** config (keycloak.json) has an additional flag set **enable-cors**

Step 1: Edit your hosts file
--------------------------------------
The demo expects additional host mappings for localhost.  So, you need to edit your machine's host file (/etc/hosts or
C:\Windows\System32\drivers\etc\hosts) and add the following entries:


```
127.0.0.1 localhost-auth
127.0.0.1 localhost-db
```


Step 2: Make sure you've set up the Keycloak Server and have it running
--------------------------------------
You will run this demo on the same server as the keycloak server.  Its best to use the appliance as everything is all set up.
See documentation on how to set this up.

Step 3: Import the Test Realm
---------------------------------------
Next thing you have to do is import the test realm for the demo.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost-auth:8080/auth/admin/index.html#/create/realm](http://localhost-auth:8080/auth/admin/index.html#/create/realm)

Import the cors-realm.json file that is in the cors/ example directory.  Feel free to browse the setup of the realm,
particularly the angular-product application.


Step 4: Build and deploy
---------------------------------------
next you must build and deploy

```
cd cors
mvn clean install wildfly:deploy
```

Step 5: Login and Observe Apps
---------------------------------------
Try going to the customer app and view customer data:

[http://localhost:8080/angular-cors-product/index.html](http://localhost:8080/angular-cors-product/index.html)

This should take you to the auth-server login screen.  Enter username: bburke@redhat.com and password: password.  You
should be brought back to a simple and boring HTML page.  Click the Reload button to show the product listing.  Reload
causes an HTTP request to a different domain, this will trigger the browser's CORS protocol.





