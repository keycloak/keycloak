Login, Distributed SSO, Distributed Logout, and Oauth Token Grant Wildfly Examples
===================================
The following examples requires Wildfly 8.0.0.  Here's the highlights of the examples
* Delegating authentication of a web app to the remote authentication server via OAuth 2 protocols
* Distributed Single-Sign-On and Single-Logout
* Transferring identity and role mappings via a special bearer token (Skeleton Key Token).
* Bearer token authentication and authorization of JAX-RS services
* Obtaining bearer tokens via the OAuth2 protocol

There are multiple WAR projects.  These all will run on the same jboss instance, but pretend each one is running on a different
machine on the network or Internet.
* **customer-app** A WAR applications that does remote login using OAUTH2 browser redirects with the auth server
* **product-app** A WAR applications that does remote login using OAUTH2 browser redirects with the auth server
* **database-service** JAX-RS services authenticated by bearer tokens only.  The customer and product app invoke on it
  to get data
* **third-party** Simple WAR that obtain a bearer token using OAuth2 using browser redirects to the auth-server.

The UI of each of these applications is very crude and exists just to show our OAuth2 implementation in action.

_This demo is meant to run on the same server instance as the Keycloak Server!_


Step 1: Make sure you've set up the Keycloak Server
--------------------------------------
If you've downloaded the Keycloak Appliance Distribution, there is already a Wildfly distro all set up for you.  This
Wildfly distro has the adapter jboss modules all installed as well as the keycloak server all set up.

If you want to install Keycloak Server and run the demo on an existing Wildfly instance:

Obtain latest keycloak-war-dist-all.zip.  This distro is used to install keycloak onto an existing JBoss installation

$ cd ${jboss.home}/standalone
$ cp -r ${keycloak-war-dist-all}/deployments .

To install the adapter:
$ cd ${jboss.home}
$ unzip ${keycloak-war-dist-al}/adapters/keycloak-wildfly-adapter-dist.zip

Step 2: Boot Keycloak Server
---------------------------------------
Where you go to start up the Keycloak Server depends on which distro you installed.

$ ./standalone.sh

Step 3: Import the Test Realm
---------------------------------------
Next thing you have to do is import the test realm for the demo.  Clicking on the below link will bring you to the
create realm page in the admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new password admin password before you can go to the create realm page.

[http://localhost:8080/auth-server/admin/index.html#/create/realm](http://localhost:8080/auth-server/admin/index.html#/create/realm)

Import the testrealm.json file that is in the wildfly-demo/ example directory.


Step 4: Build and deploy
---------------------------------------
next you must build and deploy

1. cd wildfly-demo
2. mvn clean install
3. mvn jboss-as:deploy

Step 5: Login and Observe Apps
---------------------------------------
Try going to the customer app and viewing customer data:

[http://localhost:8080/customer-portal/customers/view.jsp](http://localhost:8080/customer-portal/customers/view.jsp)

This should take you to the auth-server login screen.  Enter username: bburke@redhat.com and password: password.

If you click on the products link, you'll be take to the products app and show a product listing.  The redirects
are still happening, but the auth-server knows you are already logged in so the login is bypassed.

If you click on the logout link of either of the product or customer app, you'll be logged out of all the applications.

Step 6: Traditional OAuth2 Example
----------------------------------
The customer and product apps are logins.  The third-party app is the traditional OAuth2 usecase of a client wanting
to get permission to access a user's data.  To run this example

[http://localhost:8080/oauth-client](http://localhost:8080/oauth-client)

If you area already logged in, you will not be asked for a username and password, but you will be redirected to
an oauth grant page.  This page asks you if you want to grant certain permissions to the third-part app.

Admin Console
==========================

1. Login

Login:
[http://localhost:8080/auth-server/rest/saas/login](http://localhost:8080/auth-server/rest/saas/login)





