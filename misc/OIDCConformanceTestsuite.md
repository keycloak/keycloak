Executing OIDC Conformance Testsuite
====================================

Run Keycloak on Openshift
-------------------------
First step is to run Keycloak server in the environment, where it is available online, so the OIDC conformance testsuite can connect to it.

1) Take a look at https://github.com/keycloak/openshift-keycloak-cartridge for how to run Keycloak on Openshift. Follow the instructions until you have 
openshift instance with Keycloak 2.0.0.CR1 available on some URL like https://keycloak-mposolda.rhcloud.com/auth .
 
2) Admin user needs to be manually created on command line on Openshift cartridge. Then cartridge needs to be restarted. See Keycloak docs for details.

3) Login to Keycloak admin console. Create confidential client `openidd` with redirect_uri `https://op.certification.openid.net:60720/authz_cb` . 
This points to the testing client deployed by OIDC conformance testsuite. You will need to change the port later based on where your OIDC conformance testing app will be running.  
                                                                                                               
4) Create some user with basic claims filled (email, first name, last name).

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
A: No (See below for how to run with dynamic client registration)

Q: redirect_uris
Non-editable value: https://op.certification.openid.net:60720/authz_cb
Copy/paste that and use it as valid redirect_uri in Keycloak admin console for your Openshift client (See above paragraph `Run Keycloak on Openshift` )

Q: client_id:
A: openidd

Q: client_secret:
A: 98d90dd1-9d2e-43ad-a46b-1daeec3f5133 (copy/paste from your client in KC admin console)

Q: Which subject type do you want to use by default?
A: Public

Q: Which response type should be used by default?
A: Code (this is just for OIDC Basic profile)

Q: Select supported features:
A: JWT signed with algorithm other than "none"

Q: Test specific request parameters:
Nothing filled
 

4) After setup, you will be redirected to the testing application. Something like `https://op.certification.openid.net:60720/` and can run individual tests.
Some tests require some manual actions (eg. delete cookies). The conformance testsuite should guide you.

Run conformance testsuite with Dynamic client registration
----------------------------------------------------------
1) The steps are similar to above, however for question:

Q: Do the provider support dynamic client registration?
The answer will be: Yes

Then you don't need to configure redirect_uris, client_id and client_secret.

2) With the setup from previous point, OIDC Conformance testsuite will dynamically register new client in Keycloak. But you also need to allow the anonymous
 client registration requests from the OIDC conformance to register clients.
 
 So you need to login to Keycloak admin console and in tab "Initial Access Tokens" for realm master, you need to fill new trusted host. Fill the hostname "op.certification.openid.net" and enable big 
 count of registrations for it (1000 or so) as running each test will register new client. 


Update the openshift cartridge with latest Keycloak
---------------------------------------------------

Once some issue is fixed on Keycloak side, you may want to doublecheck if test on OIDC conformance side is passing. Hence you may want to test with JARs from latest
Keycloak master instead of the "official release" Keycloak JARs from cartridge.
 
Openshift allows to connect with SSH and restart the cartridge. So you may use something like this on your laptop (example with the fix in module keycloak-services ). 

On your laptop
````bash
cd $KEYCLOAK_SOURCES
cd services
mvn clean install
scp target/keycloak-services-2.1.0-SNAPSHOT.jar 51122e382d5271c5ca0000bc@keycloak-mposolda.rhcloud.com:/tmp/
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
