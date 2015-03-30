Keycloak Example - Kerberos Credential Delegation
=================================================

This example requires that Keycloak is configured with Kerberos/SPNEGO authentication. It's showing how the forwardable TGT is sent from
the Keycloak auth-server to the application, which deserializes it and authenticates with it to further Kerberized service, which in the example is LDAP server.

Example is using built-in ApacheDS Kerberos server from the keycloak testsuite and the realm with preconfigured federation provider and `gss delegation credential` protocol mapper.
It also needs to enable forwardable ticket support in Kerberos configuration and your browser.

Detailed steps:

**1)** Build and deploy this sample's WAR file. For this example, deploy on the same server that is running the Keycloak Server, although this is not required for real world scenarios.


**2)** Copy `http.keytab` file from the root directory of example to `/tmp` directory (On Linux):

```
cp http.keytab /tmp/http.keytab
```

Alternative is to configure different location for `keyTab` property in `kerberosrealm.json` configuration file (On Windows this will be needed).
Note that in production, keytab file should be in secured location accessible just to the user under which is Keycloak server running.


**3)** Run Keycloak server and import `kerberosrealm.json` into it through admin console. This will import realm with sample application
and configured LDAP federation provider with Kerberos/SPNEGO authentication support enabled and with `gss delegation credential` protocol mapper 
added to the application.

**WARNING:** It's recommended to use JDK8 to run Keycloak server. For JDK7 you may be faced with the bug described [here](http://darranl.blogspot.cz/2014/09/kerberos-encrypteddata-null-key-keytype.html) . 
Alternatively you can use OpenJDK7 but in this case you will need to use aes256-cts-hmac-sha1-96 for both KDC and Kerberos client configuration. For server, 
you can add system property to the maven command when running ApacheDS Kerberos server `-Dkerberos.encTypes=aes256-cts-hmac-sha1-96` (see below) and for 
client add encryption types to configuration file like `/etc/krb5.conf` (but they should be already available. See below).

Also if you are on Linux, make sure that record like:
```
127.0.0.1       localhost
```
is in your `/etc/hosts` before other records for the 127.0.0.1 host to avoid issues related to incompatible reverse lookup (Ensure the similar for other OS as well)


**4)** Configure Kerberos client (On linux it's in file `/etc/krb5.conf` ). You need to configure `KEYCLOAK.ORG` realm and enable `forwardable` flag, which is needed 
for credential delegation example, as application needs to forward Kerberos ticket and authenticate with it against LDAP server. 
See [this file](https://github.com/keycloak/keycloak/blob/master/testsuite/integration/src/main/resources/kerberos/test-krb5.conf) for inspiration.

**5)**  Run ApacheDS based Kerberos server embedded in Keycloak. Easiest is to checkout keycloak sources, build and then run KerberosEmbeddedServer 
as shown here: 

```
git clone https://github.com/keycloak/keycloak.git
mvn clean install -DskipTests=true
cd testsuite/integration
mvn exec:java -Pkerberos
```

More details about embedded Kerberos server in [testsuite README](https://github.com/keycloak/keycloak/blob/master/testsuite/integration/README.md#kerberos-server).

  
**6)** Configure browser (Firefox, Chrome or other) and enable SPNEGO authentication and credential delegation for `localhost` . 
In Firefox it can be done by adding `localhost` to both `network.negotiate-auth.trusted-uris` and `network.negotiate-auth.delegation-uris` . 
More info in [testsuite README](https://github.com/keycloak/keycloak/blob/master/testsuite/integration/README.md#kerberos-server).  
 
 
**7)** Test the example. Obtain kerberos ticket by running command from CMD (on linux):
```
kinit hnelson@KEYCLOAK.ORG
```
with password `secret` .

Then in your web browser open `http://localhost:8080/kerberos-portal` . You should be logged-in automatically through SPNEGO without displaying Keycloak login screen.
Keycloak will also transmit the delegated GSS credential to the application inside access token and application will be able to login with this credential
to the LDAP server and retrieve some data from it (Actually it just retrieve few simple data about authenticated user himself).