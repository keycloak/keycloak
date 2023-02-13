Admin Client Example
====================

Start and configure Keycloak
----------------------------

Start Keycloak:

    bin/standalone.sh

Open the Keycloak admin console, click on Add Realm, click on 'Choose a JSON file', select example-realm.json and click Upload.

Deploy the Admin Client Example to Keycloak by running:

    mvn install wildfly:deploy

Open the console at http://localhost:8080/examples-admin-client. The page should list all applications in the example realm, with a link to the applications that have a baseUrl configured.
