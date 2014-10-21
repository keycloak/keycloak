Keycloak Multi Tenants Example - Business module
================================================

This module represents a business application that would be protected by Keycloak. Our example simulates an application that collects metrics about a node (machine or virtual machine) and sends it to a backend. The backend needs to know to which client the data belongs to. As Keycloak doesn't knows how to differentiate the requests, a resolver inside the backend is queried at each request (``HttpHeaderRealmKeycloakConfigResolver``). With this, Keycloak then proceed with authenticating and authorizing the request.

The backend is a simple JAX-RS with EJB and Security annotations and simply logs the username and realm that the request belongs to. In a real application, it would store the metrics, do some calculation, and so on.

The OAuth Client module is a simple Java class with a main method. It needs to be configured based on the registration information. It first gets an Access Token from a Keycloak server and then uses this as Bearer token in a request to the backend. In our example, we use an OAuth Client, but in the real world, this could also be a public client (like a HTML5 application), a third party application, mobile application, ... All it's required is that the client knows what's the realm name and how to get a token from a Keycloak server.

