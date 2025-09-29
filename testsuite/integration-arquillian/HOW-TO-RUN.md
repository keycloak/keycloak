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

### Running tests in the production mode (Keycloak on Quarkus)

For the "production" testing, it is possible to run the Keycloak server deployed on real Quarkus server.
This can be achieved by add the `auth-server-quarkus` profile when running the testsuite.

    mvn -f testsuite/integration-arquillian/pom.xml -Pauth-server-quarkus clean install

Unlike the "development" setup described above, this requires re-build the whole distribution
after doing any change in the code.

### Running tests using an embedded server

For test driven development, it is possible to run the Keycloak server deployed on real Quarkus server.
This can be achieved by add the `auth-server-quarkus-embedded` profile when running the testsuite.

    mvn -f testsuite/integration-arquillian/pom.xml -Pauth-server-quarkus-embedded clean install -Dtest=LoginTest

After running this command, you should also be able to run tests from your IDE. For that, make sure you have the `auth-server-quarkus-embedded` profile enabled.

When running in embedded mode, the `build` phase happens every time the server is started, and it is based on the same configuration used during a full-distribution test run(e.g.: `auth-server-quarkus` profile is active).

There are a few limitations when running tests. The well-known limitations are:

* FIPS tests not working
* Re-starting the server during a test execution is taking too much metaspace. Need more investigation.

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

### Auth server debugging

See below in the "Quarkus" section.

### JBoss app server debugging

Analogically, there is the same behaviour for JBoss based app server as for auth server. The default port is set to 5006. There are app server properties.

    -Dapp.server.debug.port=$PORT
    -Dapp.server.debug.suspend=y
    
When you are debugging cluster adapter tests (For example SAMLAdapterClusterTest) you may use ports 7901 and 7902 for the app
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

## Run adapter tests

Running the tests with SAML adapter (OIDC java adapters were removed):

### Wildfly

Build the application servers

    mvn clean install -DskipTests -Pbuild-app-servers -f testsuite/integration-arquillian/servers/app-server/pom.xml

Run tests with SAML applications deployed on Wildfly:

    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
       clean install \
       -Papp-server-wildfly \
       -Dtest=org.keycloak.testsuite.adapter.**

## Migration test

### DB migration test

The `MigrationTest` test will:
- Start database on docker container. Docker/Podman on your laptop is a requirement for this test.
- Start Keycloak old version 19.0.3.
- Import realm and add some data to the database.
- Stop Keycloak 19.0.3.
- Start latest Keycloak, which automatically updates DB from 19.0.3.
- Perform a couple of tests to verify data after the update are correct.
- Stop MariaDB docker container. In case of a test failure, the MariaDB container is not stopped, so you can manually inspect the database.

The first version of Keycloak on Quarkus is version `17.0.0`, but the initial versions have a complete different set of boot options that make co-existance impossible.
Therefore the first version that can be tested is `19.0.3`.
You can execute those tests as follows:
```
export OLD_KEYCLOAK_VERSION=19.0.3
export DATABASE=mariadb

mvn -B -f testsuite/integration-arquillian/pom.xml \
  clean install \
  -Pjpa,auth-server-quarkus,db-$DATABASE,auth-server-migration \
  -Dtest=MigrationTest \
  -Dmigration.mode=auto \
  -Dmigrated.auth.server.version=$OLD_KEYCLOAK_VERSION \
  -Dmigration.import.file.name=migration-realm-$OLD_KEYCLOAK_VERSION.json \
  -Dauth.server.ssl.required=false \
  -Dauth.server.db.host=localhost
```

The `DATABASE` variable can be: `mariadb`, `mysql`, `postgres`, `mssql` or `oracle`.
As commented `OLD_KEYCLOAK_VERSION` can only be `19.0.3` right now.

For the available versions of old keycloak server, you can take a look to [this directory](tests/base/src/test/resources/migration-test) .

### DB migration test with manual mode

Same test as above, but it uses manual migration mode. During startup of the new Keycloak server, Liquibase won't automatically perform DB update, but it
just exports the needed SQL into the script. This SQL script then needs to be manually executed against the DB.
Then there is another startup of the new Keycloak server against the DB, which already has SQL changes applied and
the same test as in `auto` mode (MigrationTest) is executed to test that data are correct.

The test is executed in same way as the "auto" DB migration test with the only difference
that you need to use property `migration.mode` with the value `manual` .

    -Dmigration.mode=manual

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
different last name. Profile information is used for assertion after login, so you have to set it to be same as
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
* **Supported test modules:** `console`
* **Supported version:** latest stable
* **Driver download required:** [GeckoDriver](https://github.com/mozilla/geckodriver/releases)
* **Run with:** `-Dbrowser=firefox -Dwebdriver.gecko.driver=path/to/geckodriver`; optionally you can specify `-Dfirefox_binary=path/to/firefox/binary`

#### Google Chrome
* **Supported test modules:** `console`
* **Supported version:** latest stable
* **Driver download required:** [ChromeDriver](https://sites.google.com/a/chromium.org/chromedriver/) that corresponds with your version of the browser
* **Run with:** `-Dbrowser=chrome -Dwebdriver.chrome.driver=path/to/chromedriver`

#### [DEPRECATED] Mozilla Firefox with legacy driver
* **Supported test modules:** `console`
* **Supported version:** [52 ESR](http://ftp.mozilla.org/pub/firefox/releases/52.9.0esr/) ([Extended Support Release](https://www.mozilla.org/en-US/firefox/organizations/))
* **Driver download required:** no
* **Run with:** `-Dbrowser=firefox -DfirefoxLegacyDriver=true -Dfirefox_binary=path/to/firefox-52-esr/binary`

#### Automatic driver downloads
You can rely on automatic driver downloads which is provided by [Arquillian Drone](http://arquillian.org/arquillian-extension-drone/#_automatic_download). To do so just omit the `-Dwebdriver.{browser}.driver` CLI argument when running the tests.
By default latest driver version is always downloaded. To download a specific version, add `-DfirefoxDriverVersion` or `-DchromeDriverVersion` CLI argument.

## Disabling TLS (SSL) in the tests

All tests are executed with TLS by default. In order to disable it, you need to switch the `auth.server.ssl.required` property off.
Here's an example:

    mvn -f testsuite/integration-arquillian/pom.xml \
          clean install \
      -Dauth.server.ssl.required=false

NOTE: You can also do it ad-hoc from your IDE, however some tests (like AuthZ or JS Adapter tests) require rebuilt test applications.
so please make sure you rebuild all `testsuite/integration-arquillian` child modules.

## Cluster tests

Cluster tests use 2 backend servers (Keycloak on Quarkus or Keycloak on Undertow), 1 frontend loadbalancer server node and one shared DB. Invalidation tests don't use loadbalancer.
The browser usually communicates directly with the backend node1 and after doing some change here (eg. updating user), it verifies that the change is visible on node2 and user is updated here as well.

Failover tests use loadbalancer and they require the setup with the distributed infinispan caches switched to have 2 owners (default value is 1 owner). Otherwise failover won't reliably work.


The setup includes:

*  a load balancer on embedded Undertow (SimpleUndertowLoadBalancer)
*  two clustered nodes of Keycloak server on Wildfly/EAP or on embedded undertow
*  shared DB
    
### Cluster tests with Keycloak on Quarkus

Make sure the `testsuite/integration-arquillian/servers/auth-server/quarkus` module was built as follows:

    mvn -f testsuite/integration-arquillian/servers/auth-server/quarkus/pom.xml clean install \
         -Pauth-server-cluster-quarkus

Run tests using the `auth-server-cluster-quarkus` profile and with a database which is not H2:

     mvn -f testsuite/integration-arquillian/tests/base/pom.xml clean install \
     -Pauth-server-cluster-quarkus,db-postgres \
     -Dsession.cache.owners=2  \
     -Dtest=AuthenticationSessionFailoverClusterTest
     
Alternatively, you can perform both steps using the following command:

    mvn -f testsuite/integration-arquillian/pom.xml clean install \
    -Pauth-server-cluster-quarkus,db-postgres \
    -Dsession.cache.owners=2 \
    -Dtest=AuthenticationSessionFailoverClusterTest
     
---
**NOTE**

Right now, tests are using a H2 database.

To run tests using a different database such as PostgreSQL, add the following properties into the `testsuite/integration-arquillian/servers/auth-server/quarkus/src/main/content/conf/keycloak.conf` configuration file:

```
# HA using PostgreSQL
%ha.datasource.dialect=org.hibernate.dialect.PostgreSQLDialect
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

* `auth-server-cluster-quarkus`

Then run any cluster test as usual.

### Cluster tests with Keycloak on embedded undertow

    mvn -f testsuite/integration-arquillian/tests/base/pom.xml \
    -Pauth-server-cluster-undertow,db-mysql \
    -Dsession.cache.owners=2 \
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
    -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak -Dresources
    -Dsession.cache.owners=2

Invalidation tests (subclass of `AbstractInvalidationClusterTest`) don't need last two properties.


#### Run cluster environment from IDE

This mode is useful for develop/manual tests of clustering features. You will need to manually run keycloak backend nodes and loadbalancer.

1) Run KeycloakServer server1 with:

    -Dkeycloak.connectionsJpa.url=jdbc:mysql://localhost/keycloak -Dkeycloak.connectionsJpa.driver=com.mysql.jdbc.Driver
    -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak
    -Dresources

and argument: `-p 8181`

2) Run KeycloakServer server2 with same parameters but argument: `-p 8182`

3) Run loadbalancer (class `SimpleUndertowLoadBalancer`) without arguments and system properties. Loadbalancer runs on port 8180, so you can access Keycloak on `http://localhost:8180/auth`

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
        -Pauth-server-quarkus

When running the test, add the following arguments to the command line:

    -Pauth-server-quarkus -Pauth-server-enable-disable-feature -Dfeature.name=docker -Dfeature.value=enabled

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
Note you can also set port for example to `*:5005` or `my-host:5005` to set the bind host.

By default, quarkus server is started in the testsuite and you need to attach remote debugger to it during running. You can
use `auth.server.debug.suspend=y` to "suspend" server startup when running testsuite, which means that server startup is blocked
until debugger is attached.

More info: http://javahowto.blogspot.cz/2010/09/java-agentlibjdwp-for-attaching.html

## Cookies testing
In order to reproduce some specific cookies behaviour in browsers (like SameSite policies or 3rd party cookie blocking),
some subset of tests needs to be ran with different hosts for auth server and app/IdP server in order to simulate third
party contexts. Those hosts must be different from localhost as that host has some special treatment from browsers. At
the same time both hosts must use different domains to be considered cross-origin, e.g. `localtest.me` and
`127.0.0.1.xip.io`. NOT `app1.localtest.me` and `app2.localtest.me`!!

Also, those new cookies policies are currently not yet enabled by default (which will change in the near future). To test
those policies, you need the latest stable Firefox together with `firefox-strict-cookies` profile. This profile sets the
browser to Firefox, configures the proper cookies behavior and makes Firefox to run in the headless mode (which is ok
because this is not UI testing). For debugging purposes you can override the headless mode with `-DfirefoxArguments=''`. 

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
java.lang.RuntimeException: java.net.UnknownHostException: keycloak.localtest.me: nodename nor servname provided, 
or not known at org.keycloak.testsuite.util.OAuthClient.doWellKnownRequest(OAuthClient.java:1032)
at org.keycloak.testsuite.url.DefaultHostnameTest.assertBackendForcedToFrontendWithMatchingHostname(
DefaultHostnameTest.java:226)
...
```
when running these tests on your local machine. This happens when something on your machine or network is blocking DNS queries to [nip.io](https://nip.io)
In order to avoid using external services for DNS resolution, the tests are executed using a local host file by setting the `-Djdk.net.hosts.file=${project.build.testOutputDirectory}/hosts_file` 
system property.

## FIPS 140-2 testing

### Unit tests

```
mvn clean install -f crypto/fips1402
```

To run unit tests with the BouncyCastle approved mode, which is more strict in the used crypto algorithms:
```
mvn clean install -f crypto/fips1402 -Dorg.bouncycastle.fips.approved_only=true
```

### Integration tests

On the FIPS enabled platform with FIPS enabled OpenJDK 21, you can run this to test against a Keycloak server on Quarkus
with FIPS 140-2 integration enabled

```
mvn -B -f testsuite/integration-arquillian/pom.xml \
  clean install \
  -Pauth-server-quarkus,auth-server-fips140-2 \
  -Dcom.redhat.fips=false
```
NOTE 1: The property `com.redhat.fips` is needed so that testsuite itself is executed in the JVM with FIPS disabled. However
most important part is that Keycloak itself is running on the JVM with FIPS enabled. You can check log from server startup and
there should be messages similar to those:
```
2022-10-11 19:34:29,521 DEBUG [org.keycloak.common.crypto.CryptoIntegration] (main) Using the crypto provider: org.keycloak.crypto.fips.FIPS1402Provider
2022-10-11 19:34:31,072 TRACE [org.keycloak.common.crypto.CryptoIntegration] (main) Java security providers: [ 
 KC(BCFIPS version 1.000203, FIPS-JVM: enabled) version 1.0 - class org.keycloak.crypto.fips.KeycloakFipsSecurityProvider, 
 BCFIPS version 1.000203 - class org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider, 
 BCJSSE version 1.001202 - class org.bouncycastle.jsse.provider.BouncyCastleJsseProvider,
]
```

### BCFIPS approved mode

For running testsuite with server using BCFIPS approved mode, those additional properties should be added when running tests:
```
-Dauth.server.fips.mode=strict \
-Dauth.server.supported.keystore.types=BCFKS \
-Dauth.server.keystore.type=bcfks \
-Dauth.server.supported.rsa.key.sizes=2048,3072,4096
```
The log should contain `KeycloakFipsSecurityProvider` mentioning "Approved mode". Something like:
```
KC(BCFIPS version 1.000203 Approved Mode, FIPS-JVM: enabled) version 1.0 - class org.keycloak.crypto.fips.KeycloakFipsSecurityProvider,
```

## Aurora DB Tests
To run the Aurora DB tests on a local machine, do the following:

1. Provision an Aurora DB:
```bash
AURORA_CLUSTER="example-cluster"
AURORA_REGION=eu-west-1
AURORA_PASSWORD=TODO
source ./.github/scripts/aws/rds/aurora_create.sh
```

2. Execute the store integration tests:
```bash
TESTS=`testsuite/integration-arquillian/tests/base/testsuites/suite.sh database`
mvn test -Pauth-server-quarkus -Pdb-aurora-postgres -Dtest=$TESTS  -Dauth.server.db.host=$AURORA_ENDPOINT -Dkeycloak.connectionsJpa.password=$AURORA_PASSWORD -pl testsuite/integration-arquillian/tests/base
```

3. Teardown Aurora DB instance:
```bash
./.github/scripts/aws/rds/aurora_delete.sh
```
