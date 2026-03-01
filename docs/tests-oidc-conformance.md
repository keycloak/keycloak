Executing OIDC Conformance Testsuite
====================================

Run and configure Keycloak on Openshift
---------------------------------------
First step is to run Keycloak server in the environment, where it is available online, so the OIDC conformance testsuite can connect to it.

1) Take a look at https://github.com/keycloak/openshift-keycloak-cartridge for how to run Keycloak on Openshift. Follow the instructions until you have 
openshift instance with Keycloak 2.3.0.CR1 or later available on some URL like https://keycloak-mposolda.rhcloud.com/auth .
 
 
2) Admin user needs to be manually created on command line on Openshift cartridge. Then cartridge needs to be restarted. See Keycloak docs for details.


3) To successfully run OP-Rotation-RP-Sig test, it is good if you re-configure default minTimeBetweenRequests for publicKeyStorage. In $KEYCLOAK_HOME/standalone/configuration/standalone.xml do those changes:
```
<spi name="publicKeyStorage">
    <provider name="infinispan" enabled="true">
        <properties>
            <property name="minTimeBetweenRequests" value="-1"/>
        </properties>
    </provider>
</spi>
```            
and then restart server.
            
Reason: Keycloak supports rotation and dynamically downloading client's keys from configured jwks_url. However by default there is 10 seconds timeout 
between 2 requests for download public keys to avoid DoS attacks. 
The OIDC test OP-Rotation-RP-Sig registers client, then login user for the 1st time (which downloads new client keys) and 
then immediately rotates client keys and expects refreshToken request to succeed with new key. This is just the testing scenario. 
In real production environment, clients will very unlikely do something like this. Hence just for testing purposes is DoS protection disabled by set -1 here.
 
                                                                                                               
4) Login to admin console and create some user with basic claims filled (email, first name, last name). 
It's suggested that his username or email will have your domain attached to it (eg. "john@keycloak-mposolda.rhcloud.com" ), so that in OP-Req-login_hint test, you will be able to login as the "hinted" user.


5) Allow anonymous dynamic client registration from the OIDC host. In admin console go to "Client registration" -> "Client registration policies" -> "Trusted hosts" and add new trusted host:
 ```
 op.certification.openid.net
 ```

click "+" , then "Save".

This is needed because by default the anonymous dynamic client registration is not allowed from any host, so is effectively disabled. 

Note that OIDC conformance testsuite registers new client for each test. So you can maybe also remove "Consent required" policy if you don't want to see consent screen during almost every test.
You can maybe also increase limit by Max Clients policy (it is 200 by default, so likely should be sufficient).


Run conformance testsuite
-------------------------

Full instructions on http://openid.net/certification/testing/ . 

So what I did was:

1) Go to https://op.certification.openid.net:60000/


2) Fill issuer `https://keycloak-mposolda.rhcloud.com/auth/realms/master`


3) Configured the testing instance like this (second line are my answers):

Q: Does the OP have a .well-known/openid-configuration endpoint?
A: Yes

Q: Do the provider support dynamic client registration?
A: Yes

Q: Which subject type do you want to use by default?
A: Public

Q: Which response type should be used by default?
A: Code (this is just for OIDC Basic profile)

Q: Select supported features:
A: JWT signed with algorithm other than "none"

Q: Test specific request parameters:
Login Hint: john (this means that OP-Req-login_hint test will use user like "john@keycloak-mposolda.rhcloud.com" as it automatically attaches domain name to it for some reason).

Nothing else filled
 

4) After setup, you will be redirected to the testing application. Something like `https://op.certification.openid.net:60720/` and can run individual tests.
Some tests require some manual actions (eg. delete cookies). The conformance testsuite should guide you.



Update the openshift cartridge with latest Keycloak
---------------------------------------------------

Once some issue is fixed on Keycloak side, you may want to doublecheck if test on OIDC conformance side is passing. Hence you may want to test with JARs from latest
Keycloak main instead of the "official release" Keycloak JARs from cartridge.
 
Openshift allows to connect with SSH and restart the cartridge. So you may use something like this on your laptop (example with the fix in module keycloak-services ). 

On your laptop
````bash
cd $KEYCLOAK_SOURCES
cd services
mvn clean install
scp target/keycloak-server-spi-2.1.0-SNAPSHOT.jar 51122e382d5271c5ca0000bc@keycloak-mposolda.rhcloud.com:/tmp/
ssh 51122e382d5271c5ca0000bc@keycloak-mposolda.rhcloud.com
````

Then on the machine:

1) update the version in `/var/lib/openshift/51122e382d5271c5ca0000bc/wildfly/modules/system/add-ons/keycloak/org/keycloak/keycloak-server-spi/main/modules.xml`
 
2) Replace JAR and restart server:

````bash
cp /tmp/keycloak-server-spi-2.1.0-SNAPSHOT.jar /var/lib/openshift/51122e382d5271c5ca0000bc/wildfly/modules/system/add-ons/keycloak/org/keycloak/keycloak-server-spi/main/
ps aux | grep java
kill -9 <PID>
cd /var/lib/openshift/51122e382d5271c5ca0000bc/wildfly/bin
./standalone.sh -b 127.3.168.129 -bmanagement=127.3.168.129 -Dh2.bindAddress=127.3.168.129
````

Wait for the server to start. Then rerun the OIDC test with the updated cartridge.

Another possibility is to test with pure Wildfly Openshift cartridge and always install the latest keycloak-overlay to it.
