Example Realm REST Resource provider
====================================

To deploy copy target/hello-rest-example.jar to providers directory. Alternatively you can deploy as a module by running:

    $KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.hello-rest-example --resources=target/hello-rest-example.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-server-spi,javax.ws.rs.api"

Then registering the provider by editing keycloak-server.json and adding the module to the providers field:

    "providers": [
        ....
        "module:org.keycloak.examples.hello-rest-example"
    ],

Then start (or restart) the server. Once started open http://localhost:8080/realms/master/hello and you should see the message _Hello master_.
You can also invoke the endpoint for other realms by replacing `master` with the realm name in the above url.