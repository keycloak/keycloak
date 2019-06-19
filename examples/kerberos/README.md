Keycloak Example - Kerberos Credential Delegation
=================================================

This example requires that Keycloak is configured with Kerberos/SPNEGO authentication. It's showing how the forwardable TGT is sent from
the Keycloak auth-server to the application, which deserializes it and authenticates with it to further Kerberized service, which in the example is LDAP server.

Example is using built-in ApacheDS Kerberos server  and the realm with preconfigured federation provider and `gss delegation credential` protocol mapper.
It also needs to enable forwardable ticket support in Kerberos configuration and your browser.

Detailed steps:

**1)** Build and deploy this sample's WAR file. For this example, deploy on the same server that is running the Keycloak Server (the easiest way is to use Keycloak Demo distribution), although this is not required for real world scenarios.

If Keycloak Server is running locally, you can deploy the WAR using maven:

    mvn wildfly:deploy

**2)** Open `kerberosrealm.json` file for edit. Find `keyTab` config property, and adjust the path to `http.keytab` file, which is in project's root directory, to be an absolute path.
For example:
```
   "keyTab" : "/home/user1/devel/keycloak/examples/kerberos/http.keytab"
```

On Windows you have to use forward slashes or double backslashes (\\) - e.g.`c:/Users/User1/devel/keycloak/examples/kerberos/http.keytab`.

You can also move the file to another location if you want.
**WARNING**: In production, keytab file should be in secured location accessible only to the user under which the Keycloak server is running.


**3)** Run Keycloak server and import `kerberosrealm.json` into it through admin console. This will import realm with sample application
and configured LDAP federation provider with Kerberos/SPNEGO authentication support enabled and with `gss delegation credential` protocol mapper 
added to the application.

**WARNING:** It's recommended to use JDK8 to run Keycloak server. For JDK7 you may be faced with the bug described [here](http://darranl.blogspot.cz/2014/09/kerberos-encrypteddata-null-key-keytype.html) . 
Alternatively you can use OpenJDK7 but in this case you will need to use aes256-cts-hmac-sha1-96 for both KDC and Kerberos client configuration. For server, 
you can add system property to the command when running ApacheDS Kerberos server `-Dkerberos.encTypes=aes256-cts-hmac-sha1-96` (see below) and for 
client add encryption types to configuration file like `/etc/krb5.conf` (but they should be already available. See below).

Also if you are on Linux, make sure that record like:
```
127.0.0.1       localhost
```
is in your `/etc/hosts` before other records for the 127.0.0.1 host to avoid issues related to incompatible reverse lookup (Ensure the similar for other OS as well)

**4)** Install kerberos client. This is platform dependent. If you are on Fedora, Ubuntu or RHEL, you can install package `freeipa-client`, which contains Kerberos client and bunch of other stuff.


**5)** Configure Kerberos client (On linux it's in file `/etc/krb5.conf` ). You need to configure `KEYCLOAK.ORG` realm for host `localhost` and enable `forwardable` flag, which is needed 
for credential delegation example, as application needs to forward Kerberos ticket and authenticate with it against LDAP server. 
See [this file](../../testsuite/integration-arquillian/tests/base/src/test/resources/kerberos/test-krb5.conf) for inspiration.
On OS X the file to edit (or create) is `/Library/Preferences/edu.mit.Kerberos` with the same syntax as `krb5.conf`.
On Windows the file to edit (or create) is `c:\Windows\krb5.ini` with the same syntax as `krb5.conf`.

**6)**  Run ApacheDS based LDAP server. You can run the command like this (assuming you're in the `kerberos` directory with this example):

```
mvn exec:java -Pkerberos
```

This will also automatically import the LDIF from `kerberos-example-users.ldif` of kerberos example into the LDAP server. Replace with your own LDIF file if you want different users.

A bit more details about embedded Kerberos server in [Executing Tests](https://github.com/keycloak/keycloak/blob/master/docs/tests.md#kerberos-server).

  
**7)** Configure browser (Firefox, Chrome or other) and enable SPNEGO authentication and credential delegation for `localhost` . 
Consult the documentation of your browser and OS on how to do it. For example in Firefox it can be done by adding `localhost` to 
both `network.negotiate-auth.trusted-uris` and `network.negotiate-auth.delegation-uris` and switch `network.negotiate-auth.allow-non-fqdn` to `true`. 
A bit more details in [Executing Tests](https://github.com/keycloak/keycloak/blob/master/docs/tests.md#kerberos-server).  
 
 
**8)** Test the example. Obtain kerberos ticket by running command from Terminal / CMD:
```
kinit hnelson@KEYCLOAK.ORG
```
with password `secret` .

Then in your web browser open `http://localhost:8080/kerberos-portal` . You should be logged-in automatically through SPNEGO without displaying Keycloak login screen.
Keycloak will also transmit the delegated GSS credential to the application inside access token and application will be able to login with this credential
to the LDAP server and retrieve some data from it (Actually it just retrieve few simple data about authenticated user himself).


Troubleshooting
---------------

You followed the instructions, but things don't seem to be working. Follow these instructions to troubleshoot.

**1)** Make sure to use the default user in all Terminal / CMD sessions. Do not use 'sudo' or 'su'.
The reason is that when you open Firefox, it will open within the context of currently signed in user. And it will use that user's Kerberos ticket to perform authentication.
When you obtain Kerberos ticket using Terminal session, you have to be that same user, otherwise the ticket will not be visible to the browser.

Of course make sure to obtain the ticket:

```
kinit hnelson@KEYCLOAK.ORG
```
with password `secret`.


**2)** On Linux make sure that the first entry in your /etc/hosts file is:
```
127.0.0.1  localhost
```

Even if it already contains a similar entry like:

    127.0.0.1  localhost.localdomain localhost

Make sure to insert the short one before the existing one.

**3)** Make sure you have properly adjusted the path to `http.keytab` file in `kerberosrealm.json`.
On Windows either use `/` as path delimiter or `\\` (two backslashes).

**4)** Make sure that you have configured Firefox attributes via about:config url, and set `network.negotiate-auth.trusted-uris` and `network.negotiate-auth.delegation-uris` to `localhost`,
and `network.negotiate-auth.allow-non-fqdn` to `true`.



Symptoms and solutions
----------------------

Here are some typical errors, and how to overcome them. It often helps to close and reopen browser, or restart servers in order for remedy to take effect.


### Symptom

  There is an error when starting embedded LDAP server:

```
GSSException: Invalid name provided (Mechanism level: KrbException: Cannot locate default realm)
```
### Solution

  Make sure that krb5.conf file exists - location and file name is OS specific. See step no. 5 of the instructions.


### Symptom

  Browser redirects to normal login screen. There are no errors in Wildfly log.

### Solution

  Make sure to perform `kinit`, and properly configure Firefox. See points no. 1, and no. 4 above.


### Symptom

  Browser redirects to a normal login screen. There is a warning in Wildfly log:

```
﻿11:31:48,267 WARN  [org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator] (default task-6) GSS Context accepted, but no context initiator recognized. Check your kerberos configuration and reverse DNS lookup configuration
```

  There is also a warning similar to the following in Embedded LDAP log:

```
11:31:47,923 WARN  [org.apache.directory.server.KERBEROS_LOG] No server entry found for kerberos principal name HTTP/localhost.localdomain@KEYCLOAK.ORG
11:31:47,925 WARN  [org.apache.directory.server.KERBEROS_LOG] Server not found in Kerberos database (7)
```

### Solution

  Make sure that 127.0.0.1 reverse resolution returns 'localhost'. See point no. 2 above.


### Symptom

  Browser redirects to a normal login screen. There is a stacktrace in Wildfly log:
```
15:10:04,531 WARN  [org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator] (default task-3) SPNEGO login failed: java.security.PrivilegedActionException: GSSException: Failure unspecified at GSS-API level (Mechanism level: Invalid argument (400) - Cannot find key of appropriate type to decrypt AP REP - DES3 CBC mode with SHA1-KD)
   at java.security.AccessController.doPrivileged(Native Method)
   at javax.security.auth.Subject.doAs(Subject.java:422)
   at org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator.authenticate(SPNEGOAuthenticator.java:46)
```

### Solution

  Make sure `http.keytab` is available at the location specified in `kerberosrealm.json`. See point no. 3 above.


### Symptom

  Browser opens /kerberos-portal page, but reports an error retrieving user details from LDAP. There is a stacktrace in Wildfly log:
```
15:29:39,685 ERROR [stderr] (default task-6) javax.naming.AuthenticationException: GSSAPI [Root exception is javax.security.sasl.SaslException: GSS initiate failed [Caused by GSSException: No valid credentials provided (Mechanism level: Server not found in Kerberos database (7) - Server not found in Kerberos database)]]
15:29:39,687 ERROR [stderr] (default task-6) 	at com.sun.jndi.ldap.sasl.LdapSasl.saslBind(LdapSasl.java:169)
15:29:39,687 ERROR [stderr] (default task-6) 	at com.sun.jndi.ldap.LdapClient.authenticate(LdapClient.java:236)
15:29:39,689 ERROR [stderr] (default task-6) 	at com.sun.jndi.ldap.LdapCtx.connect(LdapCtx.java:2788)
```

### Solution

  Make sure `http.keytab` is available in location specified in `kerberosrealm.json`. See point no. 3 above. Also delete embedded server's cache directory:

    rm -rf /tmp/server-work-keycloakDS


### Symptom
```
17:32:19,825 ERROR [stderr] (default task-24) org.keycloak.common.util.KerberosSerializationUtils$KerberosSerializationException: Null credential given as input. Did you enable kerberos credential delegation for your web browser and mapping of gss credential to access token?, Java version: 1.8.0_60, runtime version: 1.8.0_60-b27, vendor: Oracle Corporation, os: 4.1.6-200.fc22.x86_64
17:32:19,826 ERROR [stderr] (default task-24) 	at org.keycloak.common.util.KerberosSerializationUtils.deserializeCredential(KerberosSerializationUtils.java:109)
17:32:19,827 ERROR [stderr] (default task-24) 	at org.keycloak.example.kerberos.GSSCredentialsClient.getUserFromLDAP(GSSCredentialsClient.java:42)
```

### Solution

  Make sure to properly configure Firefox. See point no. 4 above.

