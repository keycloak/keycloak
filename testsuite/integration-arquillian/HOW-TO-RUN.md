How To Run various testsuite configurations
===========================================

## Base steps

It's recommended to build the workspace including distribution.


    cd $KEYCLOAK_SOURCES
    mvn clean install -DskipTests=true
    cd distribution
    mvn clean install

### Running tests in the development mode (Keycloak on embedded undertow)

After build sources and distribution, it is possible to run the base testsuite

    mvn -f testsuite/integration-arquillian/pom.xml clean install

Running single test can be achieved for example like this

    mvn -f testsuite/integration-arquillian/pom.xml clean install -Dtest=LoginTest

By default, the development setup is used with the Keycloak server deployed on
embedded undertow server. That setup doesn't even require to build the distribution or re-build
the distribution after doing changes in the code.

For example when you do some fix in some class in the `services` module, you can re-build just that module

    mvn -f services/pom.xml clean install

And then re-run the LoginTest (or any other test you wish) and the changes should be applied when running the tests.

If you use Intellij Idea, you don't even need to re-build anything with the maven. After doing any
change in the codebase, the change is immediately effective when running the test with Junit runner. 

### Running tests in the production mode (Keycloak on Wildfly)

For the "production" testing, it is possible to run the Keycloak server deployed on real Wildfly server.
This can be achieved by add the `auth-server-wildfly` profile when running the testsuite.

    mvn -f testsuite/integration-arquillian/pom.xml -Pauth-server-wildfly clean install

Unlike the "development" setup described above, this requires re-build the whole distribution
after doing any change in the code.

## Debugging - tips & tricks

### Arquillian debugging

Adding this system property when running any test:


    -Darquillian.debug=true

will add lots of info to the log. Especially about:
* The test method names, which will be executed for each test class, will be written at the proper running order to the log at the beginning of each test class(done by KcArquillian class).
* All the triggered arquillian lifecycle events and executed observers listening to those events will be written to the log
* The bootstrap of WebDriver will be unlimited. By default there is just 1 minute timeout and test is cancelled when WebDriver is not bootstrapped within it.

### WebDriver timeout

By default, WebDriver has 10 seconds timeout to load every page and it timeouts with error after that. Use this to increase timeout to 1 hour instead:


    -Dpageload.timeout=3600000


### Surefire debugging

For debugging, the best is to run the test from IDE and debug it directly. When you use embedded Undertow (which is by default), then JUnit test, Keycloak server
and adapter are all in the same JVM and you can debug them easily. If it is not an option and you are forced to test with Maven and Wildfly (or EAP), you can use this:


    -Dmaven.surefire.debug=true

Or slightly longer version (that allows you to specify debugging port as well as wait till you attach the debugger):

    -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006 -Xnoagent -Djava.compiler=NONE"


and you will be able to attach remote debugger to the test. Unfortunately server and adapter are running in different JVMs, so this won't help to debug those.

### JBoss auth server debugging

When tests are run on JBoss based container (WildFly/EAP) there is possibility to attach a debugger, by default on localhost:5005.

The server won't wait to attach the debugger. There are some properties what can change the default behaviour.

    -Dauth.server.debug.port=$PORT
    -Dauth.server.debug.suspend=y

More info: http://javahowto.blogspot.cz/2010/09/java-agentlibjdwp-for-attaching.html

### JBoss app server debugging

Analogically, there is the same behaviour for JBoss based app server as for auth server. The default port is set to 5006. There are app server properties.

    -Dapp.server.debug.port=$PORT
    -Dapp.server.debug.suspend=y
    
When you are debugging cluster adapter tests (For example OIDCAdapterClusterTest) you may use ports 7901 and 7902 for the app
server nodes. Tests are usually using 2 cluster adapter nodes.    

## Testsuite logging

It is configured in `testsuite/integration-arquillian/tests/base/src/test/resources/log4j.properties` . You can see that logging of testsuite itself (category `org.keycloak.testsuite`) is debug by default.

When you run tests with undertow (which is by default), there is logging for Keycloak server and adapter (category `org.keycloak` ) in `info` when you run tests from IDE, but `off` when
you run tests with maven. The reason is that, we don't want huge logs when running mvn build. However using system property `keycloak.logging.level` will override it. This can be used for both IDE or maven.
So for example using `-Dkeycloak.logging.level=debug` will enable debug logging for keycloak server and adapter.

For more fine-tuning of individual categories, you can look at log4j.properties file and temporarily enable/disable them here.

### Wildfly server logging

When using Keycloak on Wildfly/EAP, there is INFO logging level enabled by default for most of the java packages.
You can use those system properties to enable DEBUG logging for particular packages:


* `-Dinfinispan.logging.level=DEBUG` - for package `org.infinispan`
* `-Dorg.keycloak.services.scheduled=DEBUG` - for package `org.keycloak.services.scheduled`

You can use value `TRACE` if you want to enable even TRACE logging.

There is no support for more packages ATM, you need to edit the file `testsuite/integration-arquillian/servers/auth-server/jboss/common/jboss-cli/add-log-level.cli`
and add packages manually.

## Run tests against remote container

### remote server tests

note: if there is a need to run server on http only testsuite providers has to be re-builded with `-Dauth.server.ssl.required=false`

    mvn -f testsuite/integration-arquillian/pom.xml clean install -Pauth-server-wildfly -Dauth.server.ssl.required=false -DskipTests

unzip prepared server:

    unzip -q testsuite/integration-arquillian/servers/auth-server/jboss/wildfly/target/integration-arquillian-servers-auth-server-wildfly-*.zip

create admin user:

    sh auth-server-wildfly/bin/add-user-keycloak.sh -r master -u admin -p admin

start the server:

    sh auth-server-wildfly/bin/standalone.sh \
        -Dauth.server.ssl.required=false \
        -Djboss.socket.binding.port-offset=100 \
        -Dauth.server.http.port=8180 \
        -Dauth.server.https.port=8543

run base testsuite:

    mvn -f testsuite/integration-arquillian/tests/base/pom.xml clean install -Pauth-server-remote -Dauth.server.ssl.required=false

note: it is also possible to run tests against server running on different host and port using `-Dauth.server.host=${server.host}` and `-Dauth.server.http.port=${server.port}`. The testsuite currently doesn't work with port 80.

### remote adapter tests

note: if there is a need to run server on http only testsuite providers has to be re-builded with `-Dauth.server.ssl.required=false`

    mvn -f keycloak/testsuite/integration-arquillian/pom.xml clean install -Pauth-server-wildfly -Papp-server-wildfly -Dauth.server.ssl.required=false -DskipTests ${MVN_DEFAULT_ARGS}

unzip prepared servers:

    unzip -q keycloak/testsuite/integration-arquillian/servers/auth-server/jboss/wildfly/target/integration-arquillian-servers-auth-server-wildfly-*.zip
    unzip -q keycloak/testsuite/integration-arquillian/servers/app-server/jboss/wildfly/target/integration-arquillian-servers-app-server-wildfly-*.zip

create admin user:

    sh auth-server-wildfly/bin/add-user-keycloak.sh -r master -u admin -p admin

start both servers:

    sh auth-server-wildfly/bin/standalone.sh \
        -Dauth.server.ssl.required=false \
        -Djboss.socket.binding.port-offset=100 \
        -Dauth.server.http.port=8180 \
        -Dauth.server.https.port=8543

    sh app-server-wildfly/bin/standalone.sh \
        -Djboss.socket.binding.port-offset=200 \
        -Dapp.server.ssl.required=false

run other/adapters/jboss/remote tests:

    mvn -f keycloak/testsuite/integration-arquillian/tests/other/adapters/jboss/remote/pom.xml clean install \
        -Pauth-server-remote,app-server-remote \
        -Dauth.server.ssl.required=false \
        -Dapp.server.ssl.required=false

### Running tests against container not produced by the testsuite

For running the testsuite, it is necessary to install/deploy so-called testsuite-providers. The testsuite rely on 
testsuite-providers in many test scenarios for example for checking fired events, moving in time etc. When using
keycloak from `integration-arquillian-servers-auth-server-wildfly-*.zip`, it should not be necessary to do any steps 
because testsuite-providers are already included in this archive. However, when a clean keycloak is used, e.g. 
openshift image, testsuite-providers jar file is deployed to the container in the beginning of test run. 
To be able to deploy the jar to the container, arquillian has to have an access to the management port. 

For example, to run testsuite against image in openshift, we need to first forward 9990 port from the running pod.
```shell script
oc port-forward "${POD}" 9990:9990
```
where ${POD} is a name of the pod

Now just run testsuite against the image in openshift:
```shell script
mvn clean install -f testsuite/integration-arquillian/tests/base/pom.xml \
    -Pauth-server-remote \
    -Dauth.server.ssl.required=false \
    -Dauth.server.host="${HOST}" \
    -Dauth.server.http.port=80 \
    -Dauth.server.management.host=127.0.0.1 \
    -Dauth.server.management.port=9990
```
where ${HOST} is url of keycloak, for example: `keycloak-keycloak.192.168.42.91.nip.io`.

## Run adapter tests

### Undertow
    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
        -Dtest=org.keycloak.testsuite.adapter.**.*Test

### Jetty

At the moment we can run the testsuite with Jetty `9.2` and `9.4`.
Each version has its corresponding profile:

* Jetty `9.2`: `app-server-jetty92`
* Jetty `9.4`: `app-server-jetty94`

Here's how to run the tests with Jetty `9.4`:

    mvn -f testsuite/integration-arquillian/pom.xml \
        -Papp-server-jetty94 \
        -Dtest=org.keycloak.testsuite.adapter.**.*Test

### Wildfly

    # Run tests
    mvn -f testsuite/integration-arquillian/pom.xml \
       clean install \
       -Papp-server-wildfly \
       -Dtest=org.keycloak.testsuite.adapter.**

### Tomcat

We run testsuite with Tomcat 7, 8 and 9. For specific versions see properties `${tomcat[7,8,9].version}` in parent [pom.xml](../../pom.xml).

To run tests on Tomcat:

````
mvn -f testsuite/integration-arquillian/pom.xml \
       clean install \
       -Papp-server-tomcat[7,8,9] \
       -Dtest=org.keycloak.testsuite.adapter.**
````

### Wildfly with legacy non-elytron adapter

    mvn -f testsuite/integration-arquillian/pom.xml \
       clean install \
       -Dskip.elytron.adapter.installation=true \
       -Dskip.adapter.offline.installation=false \
       -Papp-server-wildfly \
       -Dtest=org.keycloak.testsuite.adapter.**


### Wildfly deprecated

This is usually previous version of WildFly application server right before current version.
See the property `wildfly.deprecated.version` in the file [pom.xml](pom.xml) ) .

    mvn -f testsuite/integration-arquillian/pom.xml \
       clean install \
       -Pauth-server-wildfly \
       -Papp-server-wildfly-deprecated \
       -Dtest=org.keycloak.testsuite.adapter.**


### JBoss Fuse 6.3

1) Download JBoss Fuse 6.3 to your filesystem. It can be downloaded from http://origin-repository.jboss.org/nexus/content/groups/m2-proxy/org/jboss/fuse/jboss-fuse-karaf
Assumed you downloaded `jboss-fuse-karaf-6.3.0.redhat-229.zip`

2) Install to your local maven repository and change the properties according to your env (This step can be likely avoided if you somehow configure your local maven settings to point directly to Fuse repo):

    mvn install:install-file \
      -DgroupId=org.jboss.fuse \
      -DartifactId=jboss-fuse-karaf \
      -Dversion=6.3.0.redhat-229 \
      -Dpackaging=zip \
      -Dfile=/mydownloads/jboss-fuse-karaf-6.3.0.redhat-229.zip


3) Prepare Fuse and run the tests (change props according to your environment, versions etc):


    # Prepare Fuse server
    mvn -f testsuite/integration-arquillian/servers/pom.xml \
      clean install \
      -Papp-server-fuse63 \
      -Dfuse63.version=6.3.0.redhat-229

    # Run the Fuse adapter tests
    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
      clean install \
      -Pauth-server-wildfly \
      -Papp-server-fuse63 \
      -Dauth.server.ssl.required=false \
      -Dadditional.fuse.repos=,$REPO \
      -Dtest=*.fuse.*


### JBoss Fuse 7.X

1) Download JBoss Fuse 7 to your filesystem. It can be downloaded from http://origin-repository.jboss.org/nexus/content/groups/m2-proxy/org/jboss/fuse/fuse-karaf  (Fuse 7.3 or higher is required)
Assumed you downloaded `fuse-karaf-7.3.0.fuse-730065-redhat-00002.zip`

2) Install to your local maven repository and change the properties according to your env (This step can be likely avoided if you somehow configure your local maven settings to point directly to Fuse repo):


    mvn install:install-file \
      -DgroupId=org.jboss.fuse \
      -DartifactId=fuse-karaf \
      -Dversion=7.3.0.fuse-730065-redhat-00002 \
      -Dpackaging=zip \
      -Dfile=/mydownloads/fuse-karaf-7.3.0.fuse-730065-redhat-00002.zip


3) Prepare Fuse and run the tests (change props according to your environment, versions etc):


    # Prepare Fuse server
    mvn -f testsuite/integration-arquillian/servers/pom.xml \
      clean install \
      -Papp-server-fuse7x \
      -Dfuse7x.version=7.3.0.fuse-730065-redhat-00002

    # Run the Fuse adapter tests
    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
      clean test \
      -Papp-server-fuse7x \
      -Dauth.server.ssl.required=false \
      -Dadditional.fuse.repos=,$REPO \
      -Dtest=*.fuse.*


### EAP6 with Hawtio

1) Download JBoss EAP 6.4.0.GA zip

2) Install to your local maven repository and change the properties according to your env (This step can be likely avoided if you somehow configure your local maven settings to point directly to EAP repo):


    mvn install:install-file \
      -DgroupId=org.jboss.as \
      -DartifactId=jboss-as-dist \
      -Dversion=7.5.21.Final-redhat-1 \
      -Dpackaging=zip \
      -Dfile=/mydownloads/jboss-eap-6.4.0.zip


3) Download Fuse EAP installer (for example from http://origin-repository.jboss.org/nexus/content/groups/m2-proxy/com/redhat/fuse/eap/fuse-eap-installer/6.3.0.redhat-220/ )

4) Install previously downloaded file manually


    mvn install:install-file \
      -DgroupId=com.redhat.fuse.eap \
      -DartifactId=fuse-eap-installer \
      -Dversion=6.3.0.redhat-347 \
      -Dpackaging=jar \
      -Dfile=/fuse-eap-installer-6.3.0.redhat-347.jar


5) Prepare EAP6 with Hawtio and run the test


    # Prepare EAP6 and deploy hawtio
    mvn -f testsuite/integration-arquillian/servers \
      clean install \
      -Pauth-server-wildfly \
      -Papp-server-eap6 \
      -Dapp.server.jboss.version=7.5.21.Final-redhat-1 \
      -Dfuse63.version=6.3.0.redhat-347

    # Run the test
    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
      clean install \
      -Pauth-server-wildfly \
      -Papp-server-eap6 \
      -Dtest=EAP6Fuse6HawtioAdapterTest


## Migration test

### DB migration test

This test will:
 - start MySQL DB on docker container. Docker on your laptop is a requirement for this test.
 - start Keycloak 4.8.3.Final (replace with the other version if needed)
 - import realm and add some data to MySQL DB
 - stop Keycloak 4.8.3.Final
 - start latest Keycloak, which automatically updates DB from 4.8.3.Final
 - Perform couple of tests to verify data after the update are correct
 - Stop MySQL DB docker container. In case of a test failure, the MySQL container is not stopped, so you can manually inspect the database.


Run the test (Update according to your DB connection, versions etc):


    export OLD_KEYCLOAK_VERSION=4.8.3.Final

    mvn -B -f testsuite/integration-arquillian/pom.xml \
      clean install \
      -Pjpa,auth-server-wildfly,db-mariadb,auth-server-migration \
      -Dauth.server.jboss.startup.timeout=900 \
      -Dtest=MigrationTest \
      -Dmigration.mode=auto \
      -Dmigrated.auth.server.version=$OLD_KEYCLOAK_VERSION \
      -Dprevious.product.unpacked.folder.name=keycloak-$OLD_KEYCLOAK_VERSION \
      -Dmigration.import.file.name=migration-realm-$OLD_KEYCLOAK_VERSION.json \
      -Dauth.server.ssl.required=false \
      -Djdbc.mvn.version=2.2.4


For the available versions of old keycloak server, you can take a look to [this directory](tests/base/src/test/resources/migration-test) .

### DB migration test with manual mode

Same test as above, but it uses manual migration mode. During startup of the new Keycloak server, Liquibase won't automatically perform DB update, but it
just exports the needed SQL into the script. This SQL script then needs to be manually executed against the DB.
Then there is another startup of the new Keycloak server against the DB, which already has SQL changes applied and
the same test as in `auto` mode (MigrationTest) is executed to test that data are correct.

The test is executed in same way as the "auto" DB migration test with the only difference
that you need to use property `migration.mode` with the value `manual` .

    -Dmigration.mode=manual


## Server configuration migration test
This will compare if Wildfly configuration files (standalone.xml, standalone-ha.xml, domain.xml)
are correctly migrated from previous version

    mvn -f testsuite/integration-arquillian/tests/other/server-config-migration/pom.xml \
      clean install \
      -Dmigrated.version=1.9.8.Final-redhat-1

For the available versions, take a look at the directory [tests/other/server-config-migration/src/test/resources/standalone](tests/other/server-config-migration/src/test/resources/standalone)


## Admin Console UI tests
The UI tests are real-life, UI focused integration tests. Hence they do not support the default HtmlUnit browser. Only the following real-life browsers are supported: Mozilla Firefox, Google Chrome and Internet Explorer. For details on how to run the tests with these browsers, please refer to [Different Browsers](#different-browsers) chapter.

The UI tests are focused on the Admin Console. They are placed in the `console` module and are disabled by default.

The tests also use some constants placed in [test-constants.properties](tests/base/src/test/resources/test-constants.properties). A different file can be specified by `-Dtestsuite.constants=path/to/different-test-constants.properties`

In case a custom `settings.xml` is used for Maven, you need to specify it also in `-Dkie.maven.settings.custom=path/to/settings.xml`.

#### Execution example
```
mvn -f testsuite/integration-arquillian/tests/other/console/pom.xml \
    clean test \
    -Dbrowser=firefox \
    -Dfirefox_binary=/opt/firefox-45.1.1esr/firefox
```

## Spring Boot adapter tests

Currently we are testing Spring Boot with three different containers `Tomcat 8`, `Undertow` and `Jetty [9.2, 9.3, 9.4]`. We are testing two versions of Spring Boot 1.5.x and 2.1.x. All versions are specified in [root pom.xml](../../pom.xml) (see properties `spring-boot15.version` and `spring-boot21.version`).

To run tests execute following command. Default version of Spring Boot is 1.5.x, to run tests with version 2.1.x add profile `-Pspringboot21`

```
mvn -f testsuite/integration-arquillian/tests/other/springboot-tests/pom.xml \
    clean test \
    -Dadapter.container=[tomcat|undertow|jetty92|jetty93|jetty94] \
    [-Pspringboot21]
```

Note: Spring Boot 21 doesn't work with jetty92 and jetty93, only jetty94 is tested.

## Base UI tests
Similarly to Admin Console tests, these tests are focused on UI, specifically on the parts of the server that are accessed by an end user (like Login page, or Account Console).
They are designed to work with mobile browsers (alongside the standard desktop browsers). For details on the supported browsers and their configuration please refer to [Different Browsers chapter](#different-browsers).
#### Execution example
```
mvn -f testsuite/integration-arquillian/tests/other/base-ui/pom.xml \
    clean test \
    -Pandroid \
    -Dappium.avd=Nexus_5X_API_27
```

## Disabling features
Some features in Keycloak can be disabled. To run the testsuite with a specific feature disabled use the `auth.server.feature` system property. For example to run the tests with authorization disabled run:
```
mvn -f testsuite/integration-arquillian/tests/base/pom.xml clean test -Pauth-server-wildfly -Dauth.server.feature=-Dkeycloak.profile.feature.authorization=disabled
```
## WebAuthN tests
These tests cover feature W3C WebAuthn, which provides us a lot of possibilities how to include 2FA or MFA to our authentication flows. 
For testing the feature, it's necessary to use various devices, which support WebAuthn. 
However, we are not able to physically test those devices as in a real world, but we create a virtual authenticators, which should behave the same.
The support for the Virtual Authenticators came from Selenium 4.

#### Run all WebAuthN tests
```
mvn -f testsuite/integration-arquillian/tests/other/pom.xml clean test \
    -Dbrowser=chrome -Pwebauthn
```

**Note:** You can also execute those tests with `chromeHeadless` browser in order to not open a new window.

#### Troubleshooting

If you try to run WebAuthn tests with Chrome browser and you see error like:

```
Caused by: java.lang.RuntimeException: Unable to instantiate Drone via org.openqa.selenium.chrome.ChromeDriver(Capabilities):
  org.openqa.selenium.SessionNotCreatedException: session not created: This version of ChromeDriver only supports Chrome version 78
```

It could be because version of your locally installed chrome browser is not compatible with the version of chrome driver. Check what is the version
of your chrome browser (You can open URL `chrome://version/` for the details) and then check available versions from the `https://chromedriver.chromium.org/downloads` .
Then run the WebAuthn tests as above with the additional system property for specifying version of your chrome driver. For example:
```
-DchromeDriverVersion=77.0.3865.40
```

**For Windows**: Probably, you encounter issues with execution those tests on the Windows platform due to Chrome Driver is not available.
In this case, please define the path to the local Chrome Driver by adding this property `-Dwebdriver.chrome.driver=C:/path/to/chromedriver.exe`.

**Warning:** Please, be aware the WebAuthn tests are still in a development phase and there is a high chance those tests will not be stable.

## Social Login
The social login tests require setup of all social networks including an example social user. These details can't be
shared as it would result in the clients and users eventually being blocked. By default these tests are skipped.

To run the full test you need to configure clients in Google, Facebook, GitHub, Twitter, LinkedIn, Microsoft, PayPal and
StackOverflow. See the server administration guide for details on how to do that. You have to use URLs like
`http://localhost:8180/auth/realms/social/broker/google/endpoint` (with `google` replaced by the name
of given provider) as an authorized redirect URL when configuring the client. Further, you also need to create a sample user
that can login to the social network.

The details should be added to a standard properties file. For some properties you can use shared common properties and
override when needed. Or you can specify these for all providers. All providers require at least clientId and
clientSecret (StackOverflow also requires clientKey).

An example social.properties file looks like:

    common.username=sampleuser@example.org
    common.password=commonpassword
    common.profile.firstName=Foo
    common.profile.lastName=Bar
    common.profile.email=sampleuser@example.org

    google.clientId=asdfasdfasdfasdfsadf
    google.clientSecret=zxcvzxcvzxcvzxcv

    facebook.clientId=asdfasdfasdfasdfsadf
    facebook.clientSecret=zxcvzxcvzxcvzxcv
    facebook.profile.lastName=Test

In the example above the common username, password and profile are shared for all providers, but Facebook has a
different last name. Profile informations are used for assertion after login, so you have to set them to be same as
user profile information returned by given social login provider for used sample user.

Some providers actively block bots so you need to use a proper browser to test. Either Firefox or Chrome should work.

To run the tests run:

    mvn -f testsuite/integration-arquillian/pom.xml \
          clean install \
          -Pauth-server-wildfly \
          -Dtest=SocialLoginTest \
          -Dbrowser=chrome \
          -Dsocial.config=/path/to/social.properties

To run individual social provider test only you can use option like `-Dtest=SocialLoginTest#linkedinLogin`

## Different Browsers
You can use many different real-world browsers to run the integration tests.
Although technically they can be run with almost every test in the testsuite, they can fail with some of them as the tests often require specific optimizations for given browser. Therefore, only some of the test modules have support to be run with specific browsers.

#### Mozilla Firefox
* **Supported test modules:** `console`, `base-ui`
* **Supported version:** latest stable
* **Driver download required:** [GeckoDriver](https://github.com/mozilla/geckodriver/releases)
* **Run with:** `-Dbrowser=firefox -Dwebdriver.gecko.driver=path/to/geckodriver`; optionally you can specify `-Dfirefox_binary=path/to/firefox/binary`

#### Google Chrome
* **Supported test modules:** `console`, `base-ui`
* **Supported version:** latest stable
* **Driver download required:** [ChromeDriver](https://sites.google.com/a/chromium.org/chromedriver/) that corresponds with your version of the browser
* **Run with:** `-Dbrowser=chrome -Dwebdriver.chrome.driver=path/to/chromedriver`

#### Internet Explorer
* **Supported test modules:** `console`, `base-ui`
* **Supported version:** 11
* **Driver download required:** [Internet Explorer Driver Server](http://www.seleniumhq.org/download/); recommended version [3.5.1 32-bit](http://selenium-release.storage.googleapis.com/3.5/IEDriverServer_Win32_3.5.1.zip)
* **Run with:** `-Dbrowser=internetExplorer -Dwebdriver.ie.driver=path/to/IEDriverServer.exe -Dauth.server.ssl.required=false`
Note: We currently do not support SSL in IE.

#### Apple Safari
* **Supported test modules:** `base-ui`
* **Supported version:** latest stable
* **Driver download required:** no (the driver is bundled with macOS)
* **Run with:** `-Dbrowser=safari`

#### [DEPRECATED] Mozilla Firefox with legacy driver
* **Supported test modules:** `console`
* **Supported version:** [52 ESR](http://ftp.mozilla.org/pub/firefox/releases/52.9.0esr/) ([Extended Support Release](https://www.mozilla.org/en-US/firefox/organizations/))
* **Driver download required:** no
* **Run with:** `-Dbrowser=firefox -DfirefoxLegacyDriver=true -Dfirefox_binary=path/to/firefox-52-esr/binary`

#### Automatic driver downloads
You can rely on automatic driver downloads which is provided by [Arquillian Drone](http://arquillian.org/arquillian-extension-drone/#_automatic_download). To do so just omit the `-Dwebdriver.{browser}.driver` CLI argument when running the tests.
By default latest driver version is always downloaded. To download a specific version, add `-DfirefoxDriverVersion`, `-DchromeDriverVersion` or `-DieDriverVersion` CLI argument.

#### Mobile browsers
The support for testing with the mobile browsers is implemented using the [Appium](http://appium.io/) project.
This means the tests can be run with a real mobile browser in a real mobile OS. However, only emulators/simulators of mobile devices are supported at the moment (no physical devices) in our testsuite.

First, you need to install the Appium server. If you have Node.js and npm installed on your machine, you can do that with: `npm install -g appium`. For further details and requirements please refer to the [official Appium documentation](http://appium.io/docs/en/about-appium/intro/).
The tests will try to start the Appium server automatically but you can do it manually as well (just by executing `appium`).

To use a mobile browser you need to create a virtual device. The most convenient way to do so is to install the desired platform's IDE - either [Android Studio](https://developer.android.com/studio/) (for Android devices) or [Xcode](https://developer.apple.com/xcode/) (for iOS devices) - then you can create a device (smartphone/tablet) there. For details please refer to documentation of those IDEs.

#### Google Chrome on Android
* **Supported test modules:** `base-ui`
* **Supported host OS:** Windows, Linux, macOS
* **Supported browser version:** latest stable
* **Supported mobile OS version:** Android 7.x, 8.x
* **Run with:** `mvn clean test -Pandroid -Dappium.avd=name_of_the_AVD` where AVD is the name of your Android Virtual Device (e.g. `Nexus_5X_API_27`)

**Tips & tricks:**
* If the AVD name contains any spaces, you need to replace them with underscores when specifying the `-Dappium.avd=...`.
* It's probable that a freshly created device will contain an outdated Chrome version. To update to the latest version (without using the Play Store) you need to download an `.apk` for Chrome and install it with `adb install -r path/to/chrome.apk`.
* Chrome on Android uses ChromeDriver similarly to regular desktop Chrome. The ChromeDriver is bundled with the Appium server. To use a newer ChromeDriver please follow the [Appium documentation](http://appium.io/docs/en/writing-running-appium/web/chromedriver/).

#### Apple Safari on iOS
* **Supported test modules:** `base-ui`
* **Supported host OS:** macOS
* **Supported browser version:** _depends on the mobile OS version_
* **Supported mobile OS version:** iOS 11.x
* **Run with:** `mvn clean test -Pios -Dappium.deviceName=device_name` where the device name is your device identification (e.g. `iPhone X`)

## Disabling TLS (SSL) in the tests

All tests are executed with TLS by default. In order to disable it, you need to switch the `auth.server.ssl.required` property off.
Here's an example:

    mvn -f testsuite/integration-arquillian/pom.xml \
          clean install \
      -Dauth.server.ssl.required=false

NOTE: You can also do it ad-hoc from your IDE, however some tests (like AuthZ or JS Adapter tests) require rebuilt test applications.
so please make sure you rebuild all `testsuite/integration-arquillian` child modules.

## Cluster tests

Cluster tests use 2 backend servers (Keycloak on Wildfly/EAP or Keycloak on Undertow), 1 frontend loadbalancer server node and one shared DB. Invalidation tests don't use loadbalancer.
The browser usually communicates directly with the backend node1 and after doing some change here (eg. updating user), it verifies that the change is visible on node2 and user is updated here as well.

Failover tests use loadbalancer and they require the setup with the distributed infinispan caches switched to have 2 owners (default value is 1 owner). Otherwise failover won't reliably work.


The setup includes:

*  a load balancer on embedded Undertow (SimpleUndertowLoadBalancer)
*  two clustered nodes of Keycloak server on Wildfly/EAP or on embedded undertow
*  shared DB

### Cluster tests with Keycloak on Wildfly

After you build the distribution, you run this command to setup servers and run cluster tests using shared Docker database:

    mvn -f testsuite/integration-arquillian/pom.xml \
    -Pauth-server-wildfly,auth-server-cluster,db-mysql,jpa \
    -Dsession.cache.owners=2 \
    -Dbackends.console.output=true \
    -Dauth.server.log.check=false \
    -Dfrontend.console.output=true \
    -Dtest=org.keycloak.testsuite.cluster.**.*Test clean install
    
### Cluster tests with Keycloak on Quarkus

Make sure the `testsuite/integration-arquillian/servers/auth-server/quarkus` module was built as follows:

    mvn -f testsuite/integration-arquillian/servers/auth-server/quarkus/pom.xml clean install \
         -Pauth-server-cluster-quarkus

Run tests using the `auth-server-cluster-quarkus` profile:

     mvn -f testsuite/integration-arquillian/tests/base/pom.xml clean install \
     -Pauth-server-cluster-quarkus \
     -Dsession.cache.owners=2  \
     -Dtest=AuthenticationSessionFailoverClusterTest
     
Alternatively, you can perform both steps using the following command:

    mvn -f testsuite/integration-arquillian/pom.xml clean install \
    -Pauth-server-cluster-quarkus \
    -Dsession.cache.owners=2 \
    -Dtest=AuthenticationSessionFailoverClusterTest
     
---
**NOTE**

Right now, tests are using a H2 database.

To run tests using a different database such as PostgreSQL, add the following properties into the `testsuite/integration-arquillian/servers/auth-server/quarkus/src/main/content/conf/keycloak.conf` configuration file:

```
# HA using PostgreSQL
%ha.datasource.dialect=org.hibernate.dialect.PostgreSQL9Dialect
%ha.datasource.driver = org.postgresql.xa.PGXADataSource
%ha.datasource.url = jdbc:postgresql://localhost/keycloak
%ha.datasource.username = keycloak
%ha.datasource.password = password
```

The `ha` profile is automatically set when running clustering tests. 

This is temporary and database configuration should be more integrated with the test suite once we review Quarkus configuration.

---
     
#### Run cluster tests from IDE on Quarkus

Activate the following profiles:

* `quarkus`
* `auth-server-cluster-quarkus`

Then run any cluster test as usual.

### Cluster tests with Keycloak on embedded undertow

    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
    -Pauth-server-cluster-undertow,db-mysql \
    -Dsession.cache.owners=2 \
    -Dkeycloak.connectionsInfinispan.sessionsOwners=2 \
    -Dbackends.console.output=true \
    -Dauth.server.log.check=false \
    -Dfrontend.console.output=true \
    -Dtest=org.keycloak.testsuite.cluster.**.*Test clean install

Note that after update, you might encounter `org.infinispan.commons.CacheException: Initial state transfer timed out for cache org.infinispan.CONFIG`
error in some environments. This can be fixed by adding `-Djava.net.preferIPv4Stack=true` parameter to the command above.

#### Run cluster tests from IDE on embedded undertow

The test uses Undertow loadbalancer on `http://localhost:8180` and two embedded backend Undertow servers with Keycloak on `http://localhost:8181` and `http://localhost:8182` .
You can use any cluster test (eg. AuthenticationSessionFailoverClusterTest) and run from IDE with those system properties (replace with your DB settings):

    -Dauth.server.undertow=false -Dauth.server.undertow.cluster=true -Dauth.server.cluster=true
    -Dkeycloak.connectionsJpa.url=jdbc:mysql://localhost/keycloak -Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver
    -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak -Dkeycloak.connectionsInfinispan.clustered=true -Dresources
    -Dkeycloak.connectionsInfinispan.sessionsOwners=2 -Dsession.cache.owners=2

Invalidation tests (subclass of `AbstractInvalidationClusterTest`) don't need last two properties.


#### Run cluster environment from IDE

This mode is useful for develop/manual tests of clustering features. You will need to manually run keycloak backend nodes and loadbalancer.

1) Run KeycloakServer server1 with:

    -Dkeycloak.connectionsJpa.url=jdbc:mysql://localhost/keycloak -Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver
    -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak -Dkeycloak.connectionsInfinispan.clustered=true
    -Dkeycloak.connectionsInfinispan.sessionsOwners=2 -Dresources

and argument: `-p 8181`

2) Run KeycloakServer server2 with same parameters but argument: `-p 8182`

3) Run loadbalancer (class `SimpleUndertowLoadBalancer`) without arguments and system properties. Loadbalancer runs on port 8180, so you can access Keycloak on `http://localhost:8180/auth`

## Cross-DC tests

Cross-DC tests use 2 data centers, each with one automatically started and one manually controlled backend servers,
and 1 frontend loadbalancer server node that sits in front of all servers.
The browser usually communicates directly with the frontent node and the test controls where the HTTP requests
land by adjusting load balancer configuration (e.g. to direct the traffic to only a single DC).

For an example of a test, see [org.keycloak.testsuite.crossdc.ActionTokenCrossDCTest](tests/base/src/test/java/org/keycloak/testsuite/crossdc/ActionTokenCrossDCTest.java).

The cross DC requires setting a profile specifying the used cache server.
Use `cache-server-infinispan` Maven profile for Infinispan 10 or higher, or `cache-server-legacy-infinispan` profile for Infinispan 9 and lower.
Use `cache-server-datagrid` Maven profile for Datagrid 8 or higher, or `cache-server-legacy-datagrid` profile for Datagrid 7 and lower.

To specify a custom Java platform to run the cache server it is possible to set parameter: `-Dcache.server.java.home=<PATH_TO_JDK>`.

### Cache Authentication

With WildFLy/EAP based auth server option it is possible to enable authentication for the HotRod protocol by enabling profile `cache-auth`.

It is possible to specify additional parameters:
- `-Dhotrod.sasl.mechanism`: SASL mechanism used by the hotrod protocol. Default value is `DIGEST-MD5`.
- `-Dkeycloak.connectionsInfinispan.hotrodProtocolVersion`: Version of the hotrod protocol.

Example: `-Pauth-server-wildfly,cache-server-infinispan,cache-auth -Dhotrod.sasl.mechanism=SCRAM-SHA-512`

Note: The cache authentication is not implemented for `SAMLAdapterCrossDCTest`.

Note: The `cache-auth` profile currently doesn't work with the legacy Infinispan/Datagrid modules. See: [KEYCLOAK-18336](https://issues.redhat.com/browse/KEYCLOAK-18336).

### Data Grid

Since Datagrid does not distribute `infinispan-server` zip artifact, for `cache-server-datagrid` profile it is
necessary to download the artifact and install it to local Maven repository. For Red Hat Data Grid 8 and above, the command is the following:

    mvn install:install-file \
    -DgroupId=com.redhat -DartifactId=datagrid -Dpackaging=zip -Dclassifier=bin -DgeneratePom=true \
    -Dversion=${DATAGRID_VERSION} -Dfile=redhat-datagrid-${DATAGRID_VERSION}-server.zip

For Data Grid 7 and older use: `-Dfile=jboss-datagrid-${DATAGRID_VERSION}-server.zip`.

### Run Cross-DC Tests from Maven

Note: Profile `auth-servers-crossdc-undertow` currently doesn't work (see [KEYCLOAK-18335](https://issues.redhat.com/browse/KEYCLOAK-18335)).
Use `-Pauth-servers-crossdc-jboss,auth-server-wildfly` instead.

a) Prepare the environment. Compile the infinispan server and eventually Keycloak on JBoss server.

a1) If you want to use **Undertow** based Keycloak container, you just need to download and prepare the
Infinispan/JDG test server via the following command:

  `mvn -Pcache-server-infinispan,auth-servers-crossdc-undertow -f testsuite/integration-arquillian -DskipTests clean install`

*note: 'cache-server-infinispan' can be replaced by 'cache-server-datagrid'*

a2) If you want to use **JBoss-based** Keycloak backend containers instead of containers on Embedded Undertow,
 you need to prepare both the Infinispan/JDG test server and the Keycloak server on Wildfly/EAP. Run following command:

  `mvn -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly -f testsuite/integration-arquillian -DskipTests clean install`

*note: 'cache-server-infinispan' can be replaced by 'cache-server-datagrid'*

*note: 'auth-server-wildfly' can be replaced by 'auth-server-eap'*

By default JBoss-based containers use TCP-based h2 database. It can be configured to use real DB spawn in Docker, e.g. with following command:

  `mvn -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly,jpa,db-mariadb -f testsuite/integration-arquillian -DskipTests clean install`

b1) For **Undertow** Keycloak backend containers, you can run the tests using the following command (adjust the test specification according to your needs):

  `mvn -Pcache-server-infinispan,auth-servers-crossdc-undertow -Dtest=org.keycloak.testsuite.crossdc.**.*Test -pl testsuite/integration-arquillian/tests/base clean install`

*note: 'cache-server-infinispan' can be replaced by 'cache-server-datagrid'*

*note: It can be useful to add additional system property to enable logging:*

  `-Dkeycloak.infinispan.logging.level=debug`

b2) For **JBoss-based** Keycloak backend containers, you can run the tests like this:

  `mvn -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly -Dtest=org.keycloak.testsuite.crossdc.**.*Test -pl testsuite/integration-arquillian/tests/base clean install`

*note: 'cache-server-infinispan' can be replaced by 'cache-server-datagrid'*

*note: 'auth-server-wildfly can be replaced by auth-server-eap'*

**note**:
For **JBoss-based** Keycloak backend containers on real DB, the previous commands from (a2) and (b2) can be "squashed" into one. E.g.:

  `mvn -f testsuite/integration-arquillian -Dtest=org.keycloak.testsuite.crossdc.**.*Test -Pcache-server-infinispan,auth-servers-crossdc-jboss,auth-server-wildfly,jpa,db-mariadb clean install`


### Run Cross-DC Tests from Intellij IDEA

Note: Profile `auth-servers-crossdc-undertow` which is required in step (3) currently doesn't work (see [KEYCLOAK-18335](https://issues.redhat.com/browse/KEYCLOAK-18335)).

First we will manually download, configure and run infinispan servers. Then we can run the tests from IDE against the servers.
It's more effective during development as there is no need to restart infinispan server(s) among test runs.

1) Download infinispan server of corresponding version (See "infinispan.version" property in [root pom.xml](../../pom.xml))
from http://infinispan.org/download/ and go through the steps from the
[Keycloak Cross-DC documentation](http://www.keycloak.org/docs/latest/server_installation/index.html#jdgsetup) for setup infinispan servers.

The difference to original docs is, that you need to have JDG servers available on localhost with port offsets. So:

* The TCPPING hosts should be like this:

```xml
<property name="initial_hosts">localhost[8610],localhost[9610]"</property>
```

* The port offset when starting node `jdg1` should be like: `-Djboss.socket.binding.port-offset=1010` and when
starting the `jdg2` server, then `-Djboss.socket.binding.port-offset=2010` . In both cases, the bind address should be just
default `localhost` (In other words, the `-b` switch can be omitted).

So assume you have both Infinispan/JDG servers up and running.

2) Setup MySQL database or some other shared database.

3) Ensure that `org.wildfly.arquillian:wildfly-arquillian-container-managed` is on the classpath when running test. On Intellij, it can be
done by going to: `View` -> `Tool Windows` -> `Maven projects`. Then check profile `cache-server-infinispan` and `auth-servers-crossdc-undertow`.
The tests will use this profile when executed.

4) Run the LoginCrossDCTest (or any other test) with those properties. In shortcut, it's using MySQL database and
connects to the remoteStore provided by infinispan server configured in previous steps:

  `-Dauth.server.crossdc=true -Dauth.server.undertow.crossdc=true -Dcache.server.lifecycle.skip=true -Dkeycloak.connectionsInfinispan.clustered=true -Dkeycloak.connectionsJpa.url.crossdc=jdbc:mysql://localhost/keycloak -Dkeycloak.connectionsJpa.driver.crossdc=com.mysql.jdbc.Driver -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak -Dkeycloak.connectionsInfinispan.clustered=true -Dkeycloak.connectionsInfinispan.remoteStorePort=12232 -Dkeycloak.connectionsInfinispan.remoteStorePort.2=13232 -Dkeycloak.connectionsInfinispan.sessionsOwners=1 -Dsession.cache.owners=1 -Dkeycloak.infinispan.logging.level=debug -Dresources`

**NOTE**: Tests from package `manual` (eg. SessionsPreloadCrossDCTest) needs to be executed with managed containers.
So skip steps 1,2 and add property `-Dmanual.mode=true` and change "cache.server.lifecycle.skip" to false `-Dcache.server.lifecycle.skip=false` or remove it.

5) If you want to debug or test manually, the servers are running on these ports (Note that not all backend servers are running by default and some might be also unused by loadbalancer):

* *Loadbalancer* -> "http://localhost:8180/auth"

* *auth-server-undertow-cross-dc-0_1* -> "http://localhost:8101/auth"

* *auth-server-undertow-cross-dc-0_2-manual* -> "http://localhost:8102/auth"

* *auth-server-undertow-cross-dc-1_1* -> "http://localhost:8111/auth"

* *auth-server-undertow-cross-dc-1_2-manual* -> "http://localhost:8112/auth"


## Run Docker Authentication test

First, validate that your machine has a valid docker installation and that it is available to the JVM running the test.
The exact steps to configure Docker depend on the operating system.

By default, the test will run against Undertow based embedded Keycloak Server, thus no distribution build is required beforehand.
The exact command line arguments depend on the operating system.


### General guidelines

If docker daemon doesn't run locally, or if you're not running on Linux, you may need
 to determine the IP of the bridge interface or local interface that Docker daemon can use to connect to Keycloak Server.
 Then specify that IP as additional system property called *host.ip*, for example:

    -Dhost.ip=192.168.64.1

If using Docker for Mac, you can create an alias for your local network interface:

    sudo ifconfig lo0 alias 10.200.10.1/24

Then pass the IP as *host.ip*:

    -Dhost.ip=10.200.10.1


If you're running a Docker fork that always lists a host component of an image on `docker images` (e.g. Fedora / RHEL Docker)
use `-Ddocker.io-prefix-explicit=true` argument when running the test.


### Fedora

On Fedora one way to set up Docker server is the following:

    # install docker
    sudo dnf install docker

    # configure docker
    # remove --selinux-enabled from OPTIONS
    sudo vi /etc/sysconfig/docker

    # create docker group and add your user (so docker wouldn't need root permissions)
    sudo groupadd docker && sudo gpasswd -a ${USER} docker && sudo systemctl restart docker
    newgrp docker

    # you need to login again after this


    # make sure Docker is available
    docker pull registry:2

You may also need to add an iptables rule to allow container to host traffic

    sudo iptables -I INPUT -i docker0 -j ACCEPT

Then, run the test passing `-Ddocker.io-prefix-explicit=true`:

    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
        clean test \
        -Dtest=DockerClientTest \
        -Dkeycloak.profile.feature.docker=enabled \
        -Ddocker.io-prefix-explicit=true


### macOS

On macOS all you need to do is install Docker for Mac, start it up, and check that it works:

    # make sure Docker is available
    docker pull registry:2

Be especially careful to restart Docker server after every sleep / suspend to ensure system clock of Docker VM is synchronized with
that of the host operating system - Docker for Mac runs inside a VM.


Then, run the test passing `-Dhost.ip=IP` where IP corresponds to en0 interface or an alias for localhost:

    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
        clean test \
        -Dtest=DockerClientTest \
        -Dkeycloak.profile.feature.docker=enabled \
        -Dhost.ip=10.200.10.1



### Running Docker test against Keycloak Server distribution

Make sure to build the distribution:

    mvn clean install -f distribution

Then, before running the test, setup Keycloak Server distribution for the tests:

    mvn -f testsuite/integration-arquillian/servers/pom.xml \
        clean install \
        -Pauth-server-wildfly

When running the test, add the following arguments to the command line:

    -Pauth-server-wildfly -Pauth-server-enable-disable-feature -Dfeature.name=docker -Dfeature.value=enabled

## Java 11 support
Java 11 requires some arguments to be passed to JVM. Those can be activated using `-Pjava11-auth-server` and
`-Pjava11-app-server` profiles, respectively.

## Running tests using Quarkus distribution

### Before Everything

Make sure you build the project using the `quarkus` profile as follows:

    mvn -Pdistribution,quarkus clean install

### Running tests
    
Run tests using the `auth-server-quarkus` profile:

    mvn -f testsuite/integration-arquillian/pom.xml clean install -Pauth-server-quarkus
    
### Debug the Server
    
Right now, the server runs in a separate process. To debug the server set `auth.server.debug` system property to `true`.

To configure the debugger port, set the `auth.server.debug.port` system property with any valid port number. Default is `5005`. 

## Cookies testing
In order to reproduce some specific cookies behaviour in browsers (like SameSite policies or 3rd party cookie blocking),
some subset of tests needs to be ran with different hosts for auth server and app/IdP server in order to simulate third
party contexts. Those hosts must be different from localhost as that host has some special treatment from browsers. At
the same time both hosts must use different domains to be considered cross-origin, e.g. `127.0.0.1.nip.io` and
`127.0.0.1.xip.io`. NOT `app1.127.0.0.1.nip.io` and `app2.127.0.0.1.nip.io`!!

Also, those new cookies policies are currently not yet enabled by default (which will change in the near future). To test
those policies, you need the latest stable Firefox together with `firefox-strict-cookies` profile. This profile sets the
browser to Firefox, configures the proper cookies behavior and makes Firefox to run in the headless mode (which is ok
because this is not UI testing). For debugging purposes you can override the headless mode with `-DfirefoxHeadless=false`. 

**Broker tests:**

    mvn clean install -f testsuite/integration-arquillian/tests/base \
                      -Pfirefox-strict-cookies \
                      -Dtest=**.broker.** \
                      -Dauth.server.host=[some_host] -Dauth.server.host2=[some_other_host]

**JS adapter tests:**

    mvn clean install -f testsuite/integration-arquillian/tests/base \
                      -Pfirefox-strict-cookies \
                      -Dtest=**.javascript.** \
                      -Dauth.server.host=[some_host] -Dauth.server.host2=[some_other_host]
                      
**General adapter tests**

    mvn clean install -f testsuite/integration-arquillian/tests/base \
                       -Pfirefox-strict-cookies \
                       -Dtest=**.adapter.** \
                       -Dauth.server.host=[some_host] -Dauth.server.host2=[some_other_host]

## Hostname Tests 
For changing the hostname in the hostname tests (e.g. [DefaultHostnameTest](https://github.com/keycloak/keycloak/blob/main/testsuite/integration-arquillian/tests/base/src/test/java/org/keycloak/testsuite/url/DefaultHostnameTest.java)),
we rely on [nip.io](https://nip.io) for DNS switching, so tests will work everywhere without fiddling with `etc/hosts` locally. 

### Tips & Tricks:
Although it _should_ work in general, you may experience an exception like this:
```
java.lang.RuntimeException: java.net.UnknownHostException: keycloak.127.0.0.1.nip.io: nodename nor servname provided, 
or not known at org.keycloak.testsuite.util.OAuthClient.doWellKnownRequest(OAuthClient.java:1032)
at org.keycloak.testsuite.url.DefaultHostnameTest.assertBackendForcedToFrontendWithMatchingHostname(
DefaultHostnameTest.java:226)
...
```
when running these tests on your local machine. This happens when something on your machine or network is blocking DNS queries to [nip.io](https://nip.io)
One possible workaround is to add a commonly used public dns server (e.g. 8.8.8.8 for google dns server) to your local 
networks dns configuration and run the tests. 
