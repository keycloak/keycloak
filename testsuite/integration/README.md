Executing testsuite
===================

Browser
-------

The testsuite uses Sellenium. By default it uses the HtmlUnit WebDriver, but can also be executed with Chrome or Firefox.

To run the tests with Firefox add `-Dbrowser=firefox` or for Chrome add `-Dbrowser=chrome`


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

