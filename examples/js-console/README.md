Basic JavaScript Example
========================

Start and configure Keycloak
----------------------------

Start Keycloak:

    bin/standalone.sh

Open the Keycloak admin console, click on Add Realm, click on 'Choose a JSON file', select example-realm.json and click Upload.

Deploy the JS Console to Keycloak by running:

    mvn install wildfly:deploy

Open the console at http://localhost:8080/js-console and login with username: 'user', and password: 'password'.
