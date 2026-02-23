Executing tests
===============

Running the tests
-----------------

It is recommended to take a look at [../testsuite/integration-arquillian/HOW-TO-RUN.md](../testsuite/integration-arquillian/HOW-TO-RUN.md).

Browser
-------

The testsuite uses Selenium. By default it uses the HtmlUnit WebDriver, but can also be executed with Chrome or Firefox.

To run the tests with Firefox add `-Dbrowser=firefox` or for Chrome add `-Dbrowser=chrome`

Database
--------

By default the testsuite uses an embedded H2 database to test with other databases see [Database Testing](tests-db.md).

Test utils
==========

All the utils can be executed from the directory testsuite/utils:

    cd testsuite/utils

Keycloak server
---------------

To start a basic Keycloak server for testing run:

    mvn exec:java -Pkeycloak-server
    
or run org.keycloak.testsuite.KeycloakServer from your favourite IDE!
     
When starting the server it can also import a realm from a json file:

    mvn exec:java -Pkeycloak-server -Dimport=testrealm.json
    
When starting the server, https transport can be set up by setting keystore containing the server certificate
and https port, optionally setting the truststore.

    mvn exec:java -Pkeycloak-server \
        -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
        -Djavax.net.ssl.keyStore=/path/to/keystore.jks \
        -Djavax.net.ssl.keyStorePassword=CHANGEME \
        -Dkeycloak.port.https=8443

### Configuration

KeycloakServer can be configured with system properties, or through the file `~/.keycloak-server.properties`. Using the 
file allows persisting the settings and can also be more convenient when running from an IDE.

Available options include:

|Key| Description                                                                        |
|---|------------------------------------------------------------------------------------|
|keycloak.port| Set the HTTP port for the server                                         |
|keycloak.port.https| Set the HTTPS port for the server                                  |
|keycloak.bind.address| Set the bind address for the server                              |
|resources| Loads theme resources directly from the workspace and disables caching       |
|keycloak.theme.dir| Specifies an alternative location to load custom themes from        |
|keycloak.theme.cacheTemplates| Disables theme template caching                          |
|keycloak.theme.cacheThemes| Disables theme caching                                      |
|keycloak.theme.staticMaxAge| Sets the client-side caching max-age for theme resources   |
|import| Imports a realm from json at startup                                            |

### Default admin account

The default admin within the master realm are created with credentials:
* Username: `admin`
* Password: `admin`

The Keycloak test suite server will automatically create the new master realm user when the following conditions are met
* Property `keycloak.createAdminUser` is set to `true` (defaults to `true` if not present)
* There is no existing user within the master realm

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
    
or run org.keycloak.testsuite.TotpGenerator from your favourite IDE!

Once started copy/paste the totp secret and press enter. To use a new secret just copy/paste and press enter again.

Mail server
-----------

The Keycloak testsuite includes a standalone mock mail server for testing email-related features such as email verification,
password reset, and other email notifications.

**Prerequisite:**

- Keycloak server running (see [Keycloak server](#keycloak-server) section above)

### Running the Mail Server

To start the embedded test mail server for local development and testing:

Start the mail server:

```
./mvnw -f testsuite/utils/pom.xml exec:java -Pmail-server
```

or run `org.keycloak.testsuite.MailServer` from your favourite IDE!

The mail server will start on `localhost:3025` and display all received emails in the console.

### Configuring Keycloak

To configure Keycloak to use the test mail server:

1. Open your browser and login to the Keycloak Admin Console with admin credentials.

2. Select the realm you want to configure from the realm dropdown in the top-left corner (e.g., `master` or your test
   realm).

3. In the left sidebar, navigate to **Realm settings**.

4. Click on the **Email** tab.

6. Enter the following SMTP configuration values:

   | Field | Value               | Description                  |
   |-------|---------------------|------------------------------|
   | From  | `auto@keycloak.org` | Email address used as sender |
   | Host  | `localhost`         | Mail server hostname         |
   | Port  | `3025`              | Mail server port             |

7. Configure optional settings:
    - **Enable StartTLS**: Check this box (Yes), or leave it unchecked (No)
    - **Enable Authentication**: Leave unchecked (No)
    - **Enable SSL**: Leave unchecked (No)

8. Click **Save** to apply the configuration.

9. Optional Test: Configure an email address for the admin user if not configured already. Then return to the **Email** tab and click **Test connection** to verify the mail server is reachable. You should see a success message and the test email
   will appear in the mail server console output.

### Testing Email Features

Once configured, you can test various email-related features:

- **Email Verification**:
    1. In **Realm settings** → **Login** tab, enable "Verify email".
    2. Register a new user or update an existing user's email.
    3. Check the mail server console for the verification email.

- **Password Reset**:
    1. In **Realm settings** → **Login** tab, enable "Forgot password".
    2. Go to the login page and click "Forgot Password?".
    3. Enter a user's email and check the mail server console.

- **Update Email**:
    1. Change a user's email address in the Account Console.
    2. If email verification is enabled, check the mail server console for the confirmation email.

- **Event Notifications**:
    1. Configure email event listeners in **Realm settings** → **Events**
    2. Trigger events and check the mail server console for notifications.

All emails sent by Keycloak will be captured by the test mail server and displayed in the console output with full
content including subject, recipient, and message body.

LDAP server
-----------

To start a ApacheDS based LDAP server for testing LDAP sending run:
    
    mvn exec:java -Pldap
    
There are additional system properties you can use to configure (See LDAPEmbeddedServer class for details). Once done, you can create LDAP Federation provider
in Keycloak admin console with the settings like:
* Vendor: Other
* Connection URL: ldap://localhost:10389
* User DN Suffix: ou=People,dc=keycloak,dc=org
* Bind DN: uid=admin,ou=system
* Bind credential: secret

Kerberos server
---------------

To start a ApacheDS based Kerberos server for testing Kerberos + LDAP sending run:
    
    mvn exec:java -Pkerberos
    
There are additional system properties you can use to configure (See LDAPEmbeddedServer and KerberosEmbeddedServer class for details) but for testing purposes default values should be good.
By default ApacheDS LDAP server will be running on localhost:10389 and Kerberos KDC on localhost:6088 .

The alternative is to start Kerberos with the alternative realm KC2.COM instead of default KEYCLOAK.ORG. 
The ApacheDS server will be then started with all ports shifted by 1000 (EG. LDAP on 11389, Kerberos KDC on 7088).
This allows to start 2 kerberos servers in parallel to test things like Kerberos cross-realm trust:

    mvn exec:java -Pkerberos -Dkeycloak.kerberos.realm=KC2.COM
 

Once kerberos is running, you can create LDAP Federation provider in Keycloak admin console with same settings like mentioned in previous LDAP section. 
But additionally you can enable Kerberos authentication in LDAP provider with the settings like:

* Kerberos realm: KEYCLOAK.ORG
* Server Principal: HTTP/localhost@KEYCLOAK.ORG
* KeyTab: $KEYCLOAK_SOURCES/testsuite/integration-arquillian/tests/base/src/test/resources/kerberos/http.keytab (Replace $KEYCLOAK_SOURCES with correct absolute path of your sources)

Once you do this, you should also ensure that your Kerberos client configuration file is properly configured with KEYCLOAK.ORG domain. 
See [../testsuite/integration-arquillian/tests/base/src/test/resources/kerberos/test-krb5.conf](../testsuite/integration-arquillian/tests/base/src/test/resources/kerberos/test-krb5.conf) for inspiration. The location of Kerberos configuration file 
is platform dependent (In linux it's file `/etc/krb5.conf` )

Then you need to configure your browser to allow SPNEGO/Kerberos login from `localhost` .

Exact steps are again browser dependent. For Firefox see for example [http://www.microhowto.info/howto/configure_firefox_to_authenticate_using_spnego_and_kerberos.html](http://www.microhowto.info/howto/configure_firefox_to_authenticate_using_spnego_and_kerberos.html) . 
URI `localhost` must be allowed in `network.negotiate-auth.trusted-uris` config option. 

For Chrome, you just need to run the browser with command similar to this (more details in Chrome documentation):

```
/usr/bin/google-chrome-stable --auth-server-whitelist="localhost"
```


Finally test the integration by retrieve kerberos ticket. In many OS you can achieve this by running command from CMD like:
                                          
```
kinit hnelson@KEYCLOAK.ORG
```
                        
and provide password `secret`

Now when you access `http://localhost:8081/auth/realms/master/account` you should be logged in automatically as user `hnelson` .

Simple loadbalancer
-------------------

You can run class `SimpleUndertowLoadBalancer` from IDE. By default, it executes the embedded undertow loadbalancer running on `http://localhost:8180`, which communicates with 2 backend Keycloak nodes 
running on `http://localhost:8181` and `http://localhost:8182` . See javadoc for more details.
 

Create many users or offline sessions
-------------------------------------
Run testsuite with the command like this:

```
mvn exec:java -Pkeycloak-server -DstartTestsuiteCLI
```

Alternatively if you want to use your MySQL database use the command like this (replace properties values according your DB connection):

```
mvn exec:java -Pkeycloak-server -Dkeycloak.connectionsJpa.url=jdbc:mysql://localhost/keycloak -Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak -DstartTestsuiteCLI
```

Then once CLI is started, you can use command `help` to see all the available commands. 

### Creating many users

For create many users you can use command `createUsers` 
For example this will create 500 users `test0, test1, test2, ... , test499` in realm `demo` and each 100 users in separate transaction. All users will be granted realm roles `user` and `admin` :

```
createUsers test test demo 0 500 100 user,admin
```

Check count of users:

```
getUsersCount demo
```

Check if concrete user was really created:

```
getUser demo test499
```

### Creating many offline sessions

For create many offline sessions you can use command `persistSessions` . For example create 50000 sessions (each 500 in separate transaction) with command:

```
persistSessions 50000 500
```

Once users or sessions are created, you can restart to ensure the startup import of offline sessions will be triggered and you can see impact on startup time. After restart you can use command:

```
size
```

to doublecheck total count of sessions in infinispan (it will be 2 times as there is also 1 client session per each user session created)



