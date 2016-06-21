Example Domain Extension 
========================

To run, deploy as a module by running:

    $KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.domain-extension-example --resources=target/domain-extension-example.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-services,org.keycloak.keycloak-model-jpa,org.keycloak.keycloak-server-spi,javax.ws.rs.api"

Then registering the provider by editing keycloak-server.json and adding the module to the providers field:

    "providers": [
        ....
        "module:org.keycloak.examples.domain-extension-example"
    ],

Then start (or restart) the server. Once started do xyz TODO.