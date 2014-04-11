Self Bootstrapping Keycloak Server and Bundled Application
==========================================================

This is an example of bundling the Keycloak server with an app within the same WAR in an EAP 6.x environment.

* On bootup, a default realm is imported from WEB-INF/testrealm.json if it doesn't exist yet.
* On bootup, the adapter config is created on the fly and configured with the testrealm imported.
* The application is secured with keycloak (see jboss-web.xml)
* web.xml security constraints are set for the secured URLs that are secured by keycloak
* Because of weirdness with Resteasy 2.3.x, any secured JAX-RS urls from the application must have a security
constraint that denies all as they will be reachable in two places.  Under the Keycloak REST url "/rest" and under the
application's REST url "/database".
* Adapter config can be modified on the fly by getting the AdapterDeploymentContext from a servlet context attribute.
* You must specify a host-port context param so that the auth url for AdapterConfig can be set correctly.

* Run this demo by going to http://localhost:8080/app-bundle.  Then click on the url.