Keycloak Example - Simple Multi Tenancy
=======================================

The following example was tested on Wildfly 8.1.0.Final and should be compatible with any JBoss AS, JBoss EAP or Wildfly that supports Java EE 7.

This example demonstrates the simplest possible scenario for Keycloak Multi Tenancy support. Multi Tenancy is understood on this context as a single application (WAR) that is deployed on a single or clustered application server, authenticating users from *different realms* against a single or clustered Keycloak server.

The multi tenancy is achieved by having one realm per tenant on the server side and a per-request decision on which realm to authenticate the request against.

This example contains only the minimal bits required for a multi tenant application.

This example is composed of the following parts:

- ProtectedServlet - A servlet that displays the username and realm from the current user
- PathBasedKeycloakConfigResolver - A configuration resolver that takes the realm based on the path: /simple-multitenant/tenant2 means that the realm is "tenant2".

Step 1: Setup a basic Keycloak server
--------------------------------------------------------------
Install Keycloak server and start it on port 8180. Please, refer to the [Reference Guide](http://docs.jboss.org/keycloak/docs/1.0.1.Final/userguide/html_single/index.html) on how to setup a Keycloak server. You can use the _appliance_ distribution or, if you have docker installed, run ``docker run -p 8180:8080 jboss/keycloak`` for a quick start. For the appliance distribution, you might need to pass the following option to start it on port 8180: ``-Djboss.socket.binding.port-offset=100``

Once the Keycloak server is up and running, import the two realms from "src/main/resources/", namely:

- tenant1-realm.json
- tenant2-realm.json

Step 2: Deploy and run the example
--------------------------------------------------------------

- Build and deploy this sample's WAR file
- Access [http://localhost:8080/simple-multitenant/tenant1](http://localhost:8080/simple-multitenant/tenant1) and login as ``user-tenant1``, password ``user-tenant1``
- Access [http://localhost:8080/simple-multitenant/tenant2](http://localhost:8080/simple-multitenant/tenant2) and login as ``user-tenant2``, password ``user-tenant2``
