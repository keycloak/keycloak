picketlink-federation-saml-sp-post-basic: PicketLink Service Provider With a Basic Configuration using SAML HTTP POST Binding
===============================
Author: Pedro Igor  
Level: Intermediate  
Technologies: PicketLink Federation, SAML v2.0  
Summary: Basic example that demonstrates how to setup an application as a SAML v2.0 Service Provider using SAML HTTP POST Binding.  
Source: <https://github.com/jboss-developer/jboss-picketlink-quickstarts/>


What is it?
-----------

This example demonstrates Keycloak SAML 2.0 support in conjunction with a servlet secured by Picketlink's SAML SP client.


Make sure you've set up the Keycloak Server
--------------------------------------
The Keycloak Appliance Distribution comes with a preconfigured Keycloak server (based on Wildfly).  You can use it out of
the box to run these demos.  So, if you're using this, you can head to Step 2.

Alternatively, you can install the Keycloak Server onto any JBoss AS 7.1.1, EAP 6.x, or Wildfly 8.x server, but there is
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


Import the Test Realm
---------------------------------------
Next thing you have to do is import the test realm for the demo.  Clicking on the below link will bring you to the
create realm page in the Admin UI.  The username/password is admin/admin to login in.  Keycloak will ask you to
create a new admin password before you can go to the create realm page.

[http://localhost:8080/auth/admin/master/console/#/create/realm](http://localhost:8080/auth/admin/master/console/#/create/realm)

Import the testsaml.json file that is in the saml/ example directory.



Install Picketlink Modules into App server
------------------------------------------

If you are running this example with the Keycloak application distribution, you can skip this step.

You may have to upgrade your picketlink modules in your JBoss EAP or Wildfly distribution.  See Picketlink docs for more details.

Create the Security Domain for JBoss EAP
---------------
If you are running this example with the Keycloak application distribution, you can skip this step.


These steps assume you are running the server in standalone mode and using the default standalone.xml supplied with the distribution.

You configure the security domain by running JBoss CLI commands. For your convenience, this quickstart batches the commands into a `configure-security-domain-eap.cli` script provided in the root directory of this quickstart.

1. Before you begin, back up your server configuration file
    * If it is running, stop the JBoss server.
    * Backup the file: `JBOSS_HOME/standalone/configuration/standalone.xml`
    * After you have completed testing this quickstart, you can replace this file to restore the server to its original configuration.

2. Start the JBoss server by typing the following:

        For Linux:  JBOSS_HOME/bin/standalone.sh
        For Windows:  JBOSS_HOME\bin\standalone.bat
3. Review the `configure-security-domain-eap.cli` file in the root of this quickstart directory. This script adds the `sp` domain to the `security` subsystem in the server configuration and configures authentication access. Comments in the script describe the purpose of each block of commands.

4. Open a new command prompt, navigate to the root directory of this quickstart, and run the following command, replacing JBOSS_HOME with the path to your server:

        JBOSS_HOME/bin/jboss-cli.sh --connect --file=configure-security-domain-eap.cli

You should see the following result when you run the script:

        The batch executed successfully
        {
            "outcome" => "success",
        }


Create the Security Domain for WildFly
---------------
If you are running this example with the Keycloak application distribution, you can skip this step.

These steps assume you are running the server in standalone mode and using the default standalone.xml supplied with the distribution.

You configure the security domain by running JBoss CLI commands. For your convenience, this quickstart batches the commands into a `configure-security-domain-wildfly.cli` script provided in the root directory of this quickstart.

1. Before you begin, back up your server configuration file
    * If it is running, stop the JBoss server.
    * Backup the file: `JBOSS_HOME/standalone/configuration/standalone.xml`
    * After you have completed testing this quickstart, you can replace this file to restore the server to its original configuration.

2. Start the JBoss server by typing the following:

        For Linux:  JBOSS_HOME/bin/standalone.sh
        For Windows:  JBOSS_HOME\bin\standalone.bat
3. Review the `configure-security-domain-wildfly.cli` file in the root of this quickstart directory. This script adds the `sp` domain to the `security` subsystem in the server configuration and configures authentication access. Comments in the script describe the purpose of each block of commands.

4. Open a new command prompt, navigate to the root directory of this quickstart, and run the following command, replacing JBOSS_HOME with the path to your server:

        JBOSS_HOME/bin/jboss-cli.sh --connect --file=configure-security-domain-wildfly.cli

You should see the following result when you run the script:

        The batch executed successfully
        {
            "outcome" => "success",
        }



Review the Modified Server Configuration for EAP
-----------------------------------
If you are running this example with the Keycloak application distribution, you can skip this step.

If you want to review and understand newly added XML configuration, stop the JBoss server and open the  `JBOSS_HOME/standalone/configuration/standalone.xml` file.

The following `sp` security-domain was added to the `security` subsystem.

        <security-domain name="sp" cache-type="default">
            <authentication>
                <login-module code="org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule" flag="required"/>
            </authentication>
        </security-domain>

The configuration above defines a security-domain which will be used by the SP to authenticate users based on a SAML Assertion previously issued by a Identity Provider.

Review the Modified Server Configuration for WildFly
-----------------------------------
If you are running this example with the Keycloak application distribution, you can skip this step.

If you are using Wildfly, the security-domain should have the following configuration:

        <security-domain name="sp" cache-type="default">
            <authentication>
                <login-module code="org.picketlink.identity.federation.bindings.wildfly.SAML2LoginModule" flag="required"/>
            </authentication>
        </security-domain>


SAML SP-Initiated Single Sign-On
-----------------------------------

The SAML v2.0 specification defines a specific SSO mode called *SP-Initiated SSO*. In this mode, the SSO flow starts at the Service Provider side.
Please, take a look at the following documentation for more details:

1. [SAML v2.0 SP-Initiated SSO](https://docs.jboss.org/author/display/PLINK/SP-Initiated+SSO)


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
        For WildFly:   mvn -Pwildfly clean package wildfly:deploy

4. This will deploy `target/picketlink-federation-saml-sp-post-basic.war` to the running instance of the server.


Access the application
---------------------

The application will be running at the following URL: <http://localhost:8080/sales-post>.

*Note: A Service Provider alone is not very useful without an Identity Provider to authenticate users and issue SAML Assertions. Once you get this application deployed, please take a look at [About the PicketLink Federation Quickstarts](../README.md#about-the-picketlink-federation-quickstarts).*


Undeploy the Archive
--------------------

1. Make sure you have started the JBoss Server as described above.
2. Open a command line and navigate to the root directory of this quickstart.
3. When you are finished testing, type this command to undeploy the archive:

        For EAP 6:     mvn jboss-as:undeploy
        For WildFly:   mvn -Pwildfly wildfly:undeploy


Run the Quickstart in JBoss Developer Studio or Eclipse
-------------------------------------
You can also start the server and deploy the quickstarts from Eclipse using JBoss tools. For more information, see [Use JBoss Developer Studio or Eclipse to Run the Quickstarts](../README.md#use-jboss-developer-studio-or-eclipse-to-run-the-quickstarts) 


Debug the Application
------------------------------------

If you want to debug the source code or look at the Javadocs of any library in the project, run either of the following commands to pull them into your local repository. The IDE should then detect them.

        mvn dependency:sources
        mvn dependency:resolve -Dclassifier=javadoc