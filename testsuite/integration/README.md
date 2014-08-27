Executing testsuite
===================

Browser
-------

The testsuite uses Sellenium. By default it uses the HtmlUnit WebDriver, but can also be executed with Chrome or Firefox.

To run the tests with Firefox add `-Dbrowser=firefox` or for Chrome add `-Dbrowser=chrome`

Mongo
-----

The testsuite is executed with JPA model implementation with data saved in H2 database by default. To run testsuite with Mongo model, just add property `-Dkeycloak.realm.provider=mongo` when executing it.
This single property will cause that mongo will be used for realm-model, user-model and audit.

Note that this will automatically run embedded Mongo database on localhost/27018 and it will stop it after whole testsuite is finished.
So you don't need to have Mongo installed on your laptop to run mongo execution tests.

Test utils
==========

Keycloak server
---------------

To start a basic Keycloak server for testing run:

    mvn exec:java -Pkeycloak-server
    
or run org.keycloak.testutils.KeycloakServer from your favourite IDE!
     
When starting the server it can also import a realm from a json file:

    mvn exec:java -Pkeycloak-server -Dimport=testrealm.json
    
### Live edit of html and styles

The Keycloak test server can load resources directly from the filesystem instead of the classpath. This allows editing html, styles and updating images without restarting the server. To make the server use resources from the filesystem start with:

    mvn exec:java -Pkeycloak-server -Dresources
    
You can also specify the theme directory used by the server with:

    mvn exec:java -Pkeycloak-server -Dkeycloak.theme.dir=<PATH TO THEMES DIR>
    
For example to use the example themes run the server with:

    mvn exec:java -Pkeycloak-server -Dkeycloak.theme.dir=examples/themes
    
**NOTE:** If `keycloak.theme.dir` is specified the default themes (base, rcue and keycloak) are loaded from the classpath

### Run server with Mongo model

To start a Keycloak server with identity model data persisted in Mongo database instead of default JPA/H2 you can run:

    mvn exec:java -Pkeycloak-server -Dkeycloak.realm.provider=mongo -Dkeycloak.user.provider=mongo -Dkeycloak.audit.provider=mongo

By default it's using database `keycloak` on localhost/27017 and it uses already existing data from this DB (no cleanup of existing data during bootstrap). Assumption is that you already have DB running on localhost/27017 . Use system properties to configure things differently:

    mvn exec:java -Pkeycloak-server -Dkeycloak.realm.provider=mongo -Dkeycloak.user.provider=mongo -Dkeycloak.eventStore.provider=mongo -Dkeycloak.connectionsMongo.host=localhost -Dkeycloak.connectionsMongo.port=27017 -Dkeycloak.connectionsMongo.db=keycloak -Dkeycloak.connectionsMongo.clearOnStartup=false

Note that if you are using Mongo model, it would mean that Mongo will be used for audit as well. You may need to use audit related properties for configuration of Mongo if you want to override default ones (For example keycloak.audit.mongo.host, keycloak.audit.mongo.port etc)

TOTP codes
----------

To generate totp codes without Google authenticator run:

    mvn exec:java -Ptotp
    
or run org.keycloak.testutils.TotpGenerator from your favourite IDE!

Once started copy/paste the totp secret and press enter. To use a new secret just copy/paste and press enter again.

Mail server
-----------

To start a test mail server for testing email sending run:

    mvn exec:java -Pmail-server
    
or run org.keycloak.testutils.MailServer from your favourite IDE!

To configure Keycloak to use the above server add the following system properties:

    keycloak.mail.smtp.from=auto@keycloak.org
    keycloak.mail.smtp.host=localhost
    keycloak.mail.smtp.port=3025
    
For example if using the test utils Keycloak server start it with:

    mvn exec:java -Pkeycloak-server -Dkeycloak.mail.smtp.from=auto@keycloak.org -Dkeycloak.mail.smtp.host=localhost -Dkeycloak.mail.smtp.port=3025

