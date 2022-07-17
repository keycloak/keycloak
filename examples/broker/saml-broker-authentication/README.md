# Keycloak Broker: Brokering a Keycloak SAML v2 Identity Provider Quickstart

What is it?
-----------

This example demonstrates how to broker a SAML Identity Provider in Keycloak. In this case, the SAML Identity Provider
belongs to a different realm than the application and we want to trust users from one realm to authenticate and access the
applications in another realm.

There are two main realms in this example:

* **saml-broker-realm.json**: the realm where the user belongs and that provides a SAML v2 Identity Provider.
* **saml-broker-authentication-realm.json**: the realm with all the necessary configuration to set up the application and the
identity provider responsible for brokering.

From this example you'll understand how to set up an identity provider in order to broker an external
SAML identity provider and allow users from different domains/realms to authenticate and access applications in a realm.

The *saml-broker-realm* realm provides two important configuration. The first one is the user that we are going to use to
authenticate with the SAML Identity Provider.

    {
        "username" : "user",
        "enabled": true,
        "email" : "user@saml-broker-realm",
        "firstName": "User",
        "lastName": "SAML",
        "credentials" : [
            { "type" : "password",
                "value" : "password" }
        ]
    }

The second one is an application to configure a SAML Identity Provider to authenticate requests from the other realm.

    {
        "name": "http://localhost:8080/auth/",
        "enabled": true,
        "redirectUris": [
          "http://localhost:8080/auth/broker/saml-broker-authentication-realm/saml-identity-provider"
        ],
      "attributes": {
        "saml.assertion.signature": "true",
        "saml.server.signature": "true",
        "saml.signature.algorithm": "RSA_SHA256",
        "saml.client.signature": "true",
        "saml.authnstatement": "true",
        "saml.signing.private.key": "MIICWwIBAAKBgQDVG8a7xGN6ZIkDbeecySygcDfsypjUMNPE4QJjis8B316CvsZQ0hcTTLUyiRpHlHZys2k3xEhHBHymFC1AONcvzZzpb40tAhLHO1qtAnut00khjAdjR3muLVdGkM/zMC7G5s9iIwBVhwOQhy+VsGnCH91EzkjZ4SVEr55KJoyQJQIDAQABAoGADaTtoG/+foOZUiLjRWKL/OmyavK9vjgyFtThNkZY4qHOh0h3og0RdSbgIxAsIpEa1FUwU2W5yvI6mNeJ3ibFgCgcxqPk6GkAC7DWfQfdQ8cS+dCuaFTs8ObIQEvU50YzeNPiiFxRA+MnauCUXaKm/PnDfjd4tPgru7XZvlGh0wECQQDsBbN2cKkBKpr/b5oJiBcBaSZtWiMNuYBDn9x8uORj+Gy/49BUIMHF2EWyxOWz6ocP5YiynNRkPe21Zus7PEr1AkEA5yWQOkxUTIg43s4pxNSeHtL+Ebqcg54lY2xOQK0yufxUVZI8ODctAKmVBMiCKpU3mZQquOaQicuGtocpgxlScQI/YM31zZ5nsxLGf/5GL6KhzPJT0IYn2nk7IoFu7bjn9BjwgcPurpLA52TNMYWQsTqAKwT6DEhG1NaRqNWNpb4VAkBehObAYBwMm5udyHIeEc+CzUalm0iLLa0eRdiN7AUVNpCJ2V2Uo0NcxPux1AgeP5xXydXafDXYkwhINWcNO9qRAkEA58ckAC5loUGwU5dLaugsGH/a2Q8Ac8bmPglwfCstYDpl8Gp/eimb1eKyvDEELOhyImAv4/uZV9wN85V0xZXWsw==",
        "saml.signing.certificate": "MIIDdzCCAl+gAwIBAgIEbySuqTANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE1MDEyODIyMTYyMFoXDTE3MTAyNDIyMTYyMFowbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAII/K9NNvXi9IySl7+l2zY/kKrGTtuR4WdCI0xLW/Jn4dLY7v1/HOnV4CC4ecFOzhdNFPtJkmEhP/q62CpmOYOKApXk3tfmm2rwEz9bWprVxgFGKnbrWlz61Z/cjLAlhD3IUj2ZRBquYgSXQPsYfXo1JmSWF5pZ9uh1FVqu9f4wvRqY20ZhUN+39F+1iaBsoqsrbXypCn1HgZkW1/9D9GZug1c3vB4wg1TwZZWRNGtxwoEhdK6dPrNcZ+6PdanVilWrbQFbBjY4wz8/7IMBzssoQ7Usmo8F1Piv0FGfaVeJqBrcAvbiBMpk8pT+27u6p8VyIX6LhGvnxIwM07NByeSUCAwEAAaMhMB8wHQYDVR0OBBYEFFlcNuTYwI9W0tQ224K1gFJlMam0MA0GCSqGSIb3DQEBCwUAA4IBAQB5snl1KWOJALtAjLqD0mLPg1iElmZP82Lq1htLBt3XagwzU9CaeVeCQ7lTp+DXWzPa9nCLhsC3QyrV3/+oqNli8C6NpeqI8FqN2yQW/QMWN1m5jWDbmrWwtQzRUn/rh5KEb5m3zPB+tOC6e/2bV3QeQebxeW7lVMD0tSCviUg1MQf1l2gzuXQo60411YwqrXwk6GMkDOhFDQKDlMchO3oRbQkGbcP8UeiKAXjMeHfzbiBr+cWz8NYZEtxUEDYDjTpKrYCSMJBXpmgVJCZ00BswbksxJwaGqGMPpUKmCV671pf3m8nq3xyiHMDGuGwtbU+GE8kVx85menmp8+964nin"
      }
    }

The *saml-broker-authentication-realm.json* realm provides the configuration for the application and also the necessary
configuration to set up an identity provider to broker the SAML Identity Provider from the other realm.

    {
      "id" : "saml-identity-provider",
      "providerId" : "saml",
      "name" : "SAML v2 Identity Provider",
      "enabled": true,
      "updateProfileFirstLogin" : "true",
      "storeToken" : "true",
      "addReadTokenRoleOnCreate": true,
      "config": {
          "singleSignOnServiceUrl": "http://localhost:8080/auth/realms/saml-broker-realm/protocol/saml",
          "singleLogoutServiceUrl": "http://localhost:8080/auth/realms/saml-broker-realm/protocol/saml",
          "nameIDPolicyFormat": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
          "signingCertificate": "MIIDdzCCAl+gAwIBAgIEbySuqTANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE1MDEyODIyMTYyMFoXDTE3MTAyNDIyMTYyMFowbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAII/K9NNvXi9IySl7+l2zY/kKrGTtuR4WdCI0xLW/Jn4dLY7v1/HOnV4CC4ecFOzhdNFPtJkmEhP/q62CpmOYOKApXk3tfmm2rwEz9bWprVxgFGKnbrWlz61Z/cjLAlhD3IUj2ZRBquYgSXQPsYfXo1JmSWF5pZ9uh1FVqu9f4wvRqY20ZhUN+39F+1iaBsoqsrbXypCn1HgZkW1/9D9GZug1c3vB4wg1TwZZWRNGtxwoEhdK6dPrNcZ+6PdanVilWrbQFbBjY4wz8/7IMBzssoQ7Usmo8F1Piv0FGfaVeJqBrcAvbiBMpk8pT+27u6p8VyIX6LhGvnxIwM07NByeSUCAwEAAaMhMB8wHQYDVR0OBBYEFFlcNuTYwI9W0tQ224K1gFJlMam0MA0GCSqGSIb3DQEBCwUAA4IBAQB5snl1KWOJALtAjLqD0mLPg1iElmZP82Lq1htLBt3XagwzU9CaeVeCQ7lTp+DXWzPa9nCLhsC3QyrV3/+oqNli8C6NpeqI8FqN2yQW/QMWN1m5jWDbmrWwtQzRUn/rh5KEb5m3zPB+tOC6e/2bV3QeQebxeW7lVMD0tSCviUg1MQf1l2gzuXQo60411YwqrXwk6GMkDOhFDQKDlMchO3oRbQkGbcP8UeiKAXjMeHfzbiBr+cWz8NYZEtxUEDYDjTpKrYCSMJBXpmgVJCZ00BswbksxJwaGqGMPpUKmCV671pf3m8nq3xyiHMDGuGwtbU+GE8kVx85menmp8+964nin",
          "wantAuthnRequestsSigned": true,
          "forceAuthn": true,
          "validateSignature": true,
          "postBindingResponse": true,
          "postBindingAuthnRequest": true
      }
    }

Basically, once you try to access the application for the first time, you'll be redirected to *saml-broker-authentication-realm*'s login page.
In this page you'll note that there is a "SAML v2 Identity Provider" button that allows you to authenticate with the SAML Identity Provider
in *saml-broker-realm* realm.

After clicking that button, you'll be redirected to *saml-broker-realm*'s login page from where you must authenticate using the
credentials for the default user:

    Username: user
    Password: password

If everything is fine, the *saml-broker-realm* realm will redirect you back to *saml-broker-authentication-realm* and at
this point you'll be asked to provide some basic profile information in order to create a new user in *saml-broker-authentication-realm* based
on the information returned by the SAML Identity Provider.
Once you update your profile, you'll be authenticated and redirected to the application.

At the end, the *user* user is now federated and can access the application in *saml-broker-authentication-realm* realm.

Import the necessary realms
--------------------------------------
Next thing you have to do is import the test realm for the demo.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost:8080/auth/admin/master/console/#/create/realm](http://localhost:8080/auth/admin/master/console/#/create/realm)

Import the following realms:

* **saml-broker-authentication-realm.json**
* **saml-broker-realm.json**

Make sure you've set up the Keycloak Server
--------------------------------------
The Keycloak Appliance Distribution comes with a preconfigured Keycloak server (based on Wildfly).  You can use it out of
the box to run these demos.  So, if you're using this, you can head to Step 2.

Alternatively, you can install the Keycloak Server onto any EAP 6.x, or Wildfly 8.x server, but there is
a few steps you must follow.

Obtain latest keycloak-war-dist-all.zip.  This distro is used to install Keycloak onto an existing JBoss installation.
This installs the server.

    $ cd ${wildfly.jboss.home}/standalone
    $ cp -r ${keycloak-war-dist-all}/deployments .

To be able to run the demos you also need to install the Keycloak client adapter. For Wildfly:

    $ cd ${wildfly.home}
    $ unzip ${keycloak-war-dist-all}/adapters/keycloak-wildfly-adapter-dist.zip

For JBoss EAP 6.x

    $ cd ${eap.home}
    $ unzip ${keycloak-war-dist-all}/adapters/keycloak-eap6-adapter-dist.zip

For JBoss AS 7.1.1:

    $ cd ${as7.home}
    $ unzip ${keycloak-war-dist-all}/adapters/keycloak-as7-adapter-dist.zip

Unzipping the adapter ZIP only installs the JAR files.  You must also add the Keycloak Subsystem to the server's
configuration (standalone/configuration/standalone.xml).

    <server xmlns="urn:jboss:domain:1.4">

        <extensions>
            <extension module="org.keycloak.keycloak-subsystem"/>
            ...
        </extensions>

        <profile>
            <subsystem xmlns="urn:jboss:domain:keycloak:1.0"/>
            ...
        </profile>

Boot Keycloak Server
---------------------------------------
Where you go to start up the Keycloak Server depends on which distro you installed.

From appliance:

```
$ cd keycloak/bin
$ ./standalone.sh
```


From existing Wildfly/EAP6/AS7 distro

```
$ cd ${wildfly.jboss.home}/bin
$ ./standalone.sh
```


Start JBoss Enterprise Application Platform 6 or WildFly with the Web Profile
-------------------------

1. Open a command line and navigate to the root of the JBoss server directory.
2. The following shows the command line to start the server with the web profile:

        For Linux:   JBOSS_HOME/bin/standalone.sh
        For Windows: JBOSS_HOME\bin\standalone.bat


Build and Deploy the Quickstart
-------------------------

_NOTE: The following build command assumes you have configured your Maven user settings. If you have not, you must include Maven setting arguments on the command line. See [Build and Deploy the Quickstarts](../README.md#build-and-deploy-the-quickstarts) for complete instructions and additional options._

1. Make sure you have started the JBoss Server as described above.
2. Open a command line and navigate to the root directory of this quickstart.
3. Type this command to build and deploy the archive:

        For EAP 6:     mvn clean package jboss-as:deploy
        For WildFly:   mvn clean package wildfly:deploy

4. This will deploy `target/saml-broker-authentication.war` to the running instance of the server.


Access the application
---------------------

The application will be running at the following URL: <http://localhost:8080/saml-broker-authentication>.


Undeploy the Archive
--------------------

1. Make sure you have started the JBoss Server as described above.
2. Open a command line and navigate to the root directory of this quickstart.
3. When you are finished testing, type this command to undeploy the archive:

        For EAP 6:     mvn jboss-as:undeploy
        For WildFly:   mvn -Pwildfly wildfly:undeploy


Debug the Application
------------------------------------

If you want to debug the source code or look at the Javadocs of any library in the project, run either of the following commands to pull them into your local repository. The IDE should then detect them.

        mvn dependency:sources
        mvn dependency:resolve -Dclassifier=javadoc
