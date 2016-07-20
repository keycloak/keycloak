Keycloak Example - Basic Authentication
=======================================

The following example was tested on Wildfly 8.1.0.Final and JBoss EAP 6.3. It should be compatible with any JBoss AS, JBoss EAP or Wildfly that supports Java EE 7.

This example demonstrates basic authentication support for a Keycloak protected REST service. However, more importantly it enables a REST service to be secured using both basic and bearer token authentication, which is useful where the service needs to be accessed both as part of a single signon session, and also as a standalone REST service.


Step 1: Setup a basic Keycloak server
--------------------------------------------------------------
Install Keycloak server and start it on port 8080. Check the Reference Guide if unsure on how to do it.

Once the Keycloak server is up and running, import the realm basicauthrealm.json.


Step 2: Deploy and run the example
--------------------------------------------------------------

- Build and deploy this sample's WAR file. For this example, deploy on the same server that is running the Keycloak Server, although this is not required for real world scenarios.

- Open a command window and perform the following command:

    curl http://admin:password@localhost:8080/basicauth/service/echo?value=hello

(If we navigate directly to http://localhost:8080/basicauth/service/echo?value=hello, we get an error in the browser because the request is not authenticated).

This should result in the value 'hello' being returned as a response.

Simply change the username (currently 'admin') or password (currently 'password') in the command to see an "Unauthorized" response.


