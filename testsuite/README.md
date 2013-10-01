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

    mvn exec:java -Dexec.mainClass=org.keycloak.testutils.KeycloakServer
    
or just run KeycloakServer from your favourite IDE!
     
When starting the server it can also import a realm from a json file:

    mvn exec:java -Dexec.mainClass=org.keycloak.testutils.KeycloakServer -Dexec.args="-import testrealm.json"
    
You can also change the host and port the server is bound to:

    mvn exec:java -Dexec.mainClass=org.keycloak.testutils.KeycloakServer -Dexec.args="-b host -p 8080"

TOTP codes
----------

To generate totp codes without Google authenticator run:

    mvn exec:java -Dexec.mainClass=org.keycloak.testutils.TotpGenerator -Dexec.args="PJBX GURY NZIT C2JX I44T S3D2 JBKD G6SB"
    
or just run TotpGenerator from your favourite IDE!

Replace value of -Dexec.args with the secret from the totp configuration page

Mail server
-----------

To start a test mail server for testing email sending run:

    mvn exec:java -Dexec.mainClass=org.keycloak.testutils.MailServer
    
or just run MailServer from your favourite IDE!

To configure Keycloak to use the above server add:

    -Dkeycloak.mail.smtp.from=auto@keycloak.org -Dkeycloak.mail.smtp.host=localhost -Dkeycloak.mail.smtp.port=3025
