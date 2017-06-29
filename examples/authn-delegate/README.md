Authentication Delegation Example with Authentication Provider and User Storage Provider
===================================
In this context, Authentication Delegation stands for delegating authentication to an existing external authentication server on behalf of keycloak's browser-based authentication mechanism. It might be said that it be the variant of Identity Brokering except for not using standard protocols for Identity Federation such as OpenID Connect and SAMLv2.

This example consists of three components.

* **providers** : Authentication Provider and User Storage Provider.
* **authn-server** : An external authentication server to which keycloak delegates authentication.
* **client** : A client application.

In this example, each of keycloak, authn-server, and client runs on each different wildfly server.

* **keycloak** : runs on port 8080
* **authn-serer** : runs on port 8280
* **client** : runs on port 8380

After completing mvn install, please deploy each .jar and .war in target directory onto each corresponding wildfly server.

On keycloak's admin console, please load authn-delegation-realm.json for realm settings for this example.

After that, access [http://localhost:8380/authn-delegation-client/](http://localhost:8380/authn-delegation-client/)
