Example Realm REST Resource provider
====================================

You can deploy the provider by running: `mvn package` and dropping the jar under `$KEYCLOAK_HOME/providers`

When the server has started, open http://localhost:8080/realms/master/hello and look for the message _Hello master_.
You can also invoke the endpoint for other realms by replacing `master` with the realm name in the above url.
