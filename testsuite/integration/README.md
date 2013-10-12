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

TOTP codes
----------

To generate totp codes without Google authenticator run:

    mvn exec:java -Ptotp -Dsecret='PJBX GURY NZIT C2JX I44T S3D2 JBKD G6SB'
    
or run org.keycloak.testutils.TotpGenerator from your favourite IDE!

Replace value of -Dsecret with the secret from the totp configuration page (remember quotes!)

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

