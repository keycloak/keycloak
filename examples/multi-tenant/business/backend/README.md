Keycloak Multi Tenants Example - Business backend module
================================================

Most of what this does is already explained on the parent's module and on the main example's README file. The interesting things to check on this module are:

- The ``HttpHeaderRealmKeycloakConfigResolver`` class, which exemplifies how to build a ``KeycloakConfigResolver``, used by Keycloak during the Request Authentication phase. This should return a complete ``KeycloakDeployment`` object, possibly build with information extracted from the request. In our example, we simply use the ``X-Keycloak-Realm`` HTTP Header to build the file name of the JSON file to load and pass an InputStream of this file to ``KeycloakDeploymentBuilder.build(InputStream);``.
- The ``MetricsService`` is very simple, and shows how to restrict a JAX-RS to allow only users with the role ``agent`` to access the service. It also shows how to extract the username and realm from Keycloak's principal.
- On ``web.xml``, we specify the context parameter ``keycloak.config.resolver``, pointing to our resolver class. That's all we need to tell Keycloak that we are in a multi tenant environment and that the configuration should be loaded based on the request, not based on the deployment.
