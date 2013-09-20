Login, Distributed SSO, Distributed Logout, and Oauth Token Grant AS7 Examples
===================================
The following examples requires JBoss AS7 or EAP 6.1, and Resteasy 3.0.2 and has been tested on version EAP 6.1.  Here's the highlights of the examples
* Delegating authentication of a web app to the remote authentication server via OAuth 2 protocols
* Distributed Single-Sign-On and Single-Logout
* Transferring identity and role mappings via a special bearer token (Skeleton Key Token).
* Bearer token authentication and authorization of JAX-RS services
* Obtaining bearer tokens via the OAuth2 protocol

There are 5 WAR projects.  These all will run on the same jboss instance, but pretend each one is running on a different
machine on the network or Internet.
* **auth-server**: This is the keycloak SSO auth server
* **customer-app** A WAR applications that does remote login using OAUTH2 browser redirects with the auth server
* **product-app** A WAR applications that does remote login using OAUTH2 browser redirects with the auth server
* **database-service** JAX-RS services authenticated by bearer tokens only.  The customer and product app invoke on it
  to get data
* **third-party** Simple WAR that obtain a bearer token using OAuth2 using browser redirects to the auth-server.

The UI of each of these applications is very crude and exists just to show our OAuth2 implementation in action.


Step 1: Make sure you've upgraded Resteasy
--------------------------------------
The first thing you is upgrade Resteasy to 3.0.2 within JBoss as described [here](http://docs.jboss.org/resteasy/docs/3.0.2.Final/userguide/html/Installation_Configuration.html#upgrading-as7)


Step 2: Boot JBoss
---------------------------------------
Boot JBoss in 'standalone' mode.

Step 3: Build and deploy
---------------------------------------
next you must build and deploy

1. cd as7-eap-demo
2. mvn clean install
3. mvn jboss-as:deploy

Step 4: Login and Observe Apps
---------------------------------------
Try going to the customer app and viewing customer data:

[http://localhost:8080/customer-portal](http://localhost:8080/customer-portal)

This should take you to the main customer portal page with the option to select either the [Customer Listings](http://localhost:8080/customer-portal/customers/view.jsp ) or the [Customer Admin Interface](http://localhost:8080/customer-portal/admin/admin.jsp). Either of which will automatically open the auth-server login screen if the user is not currently authenticated. Login using one of the default user accounts:

Default users accounts:
username:bburke@redhat.com password:password roles:user
username:admin password:admin roles:admin
username:manager password:manager roles:user,admin

Note: the admin user is not automatically give the user role, as such the admin will not be allowed to access the customer listings. The 'manager' user has both the admin and user roles and will be able to access both.

If you click on the products link, you'll be take to the products app and show a product listing.  The redirects
are still happening, but the auth-server knows you are already logged in so the login is bypassed.

If you click on the logout link of either of the product or customer app, you'll be logged out of all the applications.

Step 5: Traditional OAuth2 Example
----------------------------------
The customer and product apps are logins.  The third-party app is the traditional OAuth2 usecase of a client wanting
to get permission to access a user's data.  To run this example

[http://localhost:8080/oauth-client](http://localhost:8080/oauth-client)

If you area already logged in, you will not be asked for a username and password, but you will be redirected to
an oauth grant page.  This page asks you if you want to grant certain permissions to the third-part app.

