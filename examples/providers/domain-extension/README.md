Example Domain Extension 
========================

To run, deploy as a module by running:

    $KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.domain-extension-example --resources=target/domain-extension-example.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-services,org.keycloak.keycloak-model-jpa,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private,javax.ws.rs.api,javax.persistence.api,org.hibernate,org.javassist,org.liquibase"


Then registering the provider by editing `standalone/configuration/standalone.xml` and adding the module to the providers element:

    <providers>
        ...
        <provider>module:org.keycloak.examples.domain-extension-example</provider>
    </providers>

Then start (or restart) the server.

Testing
-------
First you can create some example companies with these CURL requests.

````
curl -i --request POST http://localhost:8080/auth/realms/master/example/companies --data "{ \"name\": \"foo company\" }" --header "Content-type: application/json"
curl -i --request POST http://localhost:8080/auth/realms/master/example/companies --data "{ \"name\": \"bar company\" }" --header "Content-type: application/json"
````

Then you can look up all companies 

````
curl -i --request GET http://localhost:8080/auth/realms/master/example/companies --header "Accept: application/json"
````

If you create realm `foo` in Keycloak admin console and then replace the realm name in the URI (for example like `http://localhost:8080/auth/realms/foo/example/companies` ) you will see
that companies are scoped per-realm. So you will see different companies for realm `master` and for realm `foo` .


Testing with authenticated access
---------------------------------
Example contains the endpoint, which is accessible just for authenticated users. REST request must be authenticated with bearer access token
of authenticated user and the user must be in realm role `admin` in order to access the resource. You can run bash script from the current directory:
````
./invoke-authenticated.sh
````
The script assumes user `admin` with password `admin` exists in realm `master`. Also it assumes that you have `curl` installed.
