# Keycloak Integration Testsuite with Arquillian

*OUT OF DATE - NEEDS REWRITE*

## Usage

Running the tests: `mvn test` or `mvn clean test`

## Test suite

### Selecting container for Keycloak Server

The testsuite requires a container for Keycloak Server to be selected.
This container is used by all tests in the suite during a single test execution.

*By default* the tests run with server on embedded *Undertow*.
A different container can be selected with profile, e.g. `-Pauth-server-wildfly`.

### Containers Supported for Keycloak Server

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| **Undertow** | `auth-server-undertow` | **default** | `undertow-core`, `resteasy-undertow` |
| **Wildfly 9** | `auth-server-wildfly` | `-Pauth-server-wildfly` | `keycloak-server-dist` or `wildfly-dist`+`keycloak-server-overlay` |
| **EAP 6.4** | `auth-server-eap6` | `-Pauth-server-eap6` | `keycloak-server-dist` or `eap6-dist`+`keycloak-server-overlay` |

See the relevant container definitions in `arquillian.xml` located in the **test resources** folder.

### Test Class Hierarchy
```
AbstractKeycloakTest
├── AbstractAdminConsoleTest
└── AbstractAdapterTest
```

### AbstractKeycloakTest

Handles test realms. Provides Admin Client for REST operations.

* **@BeforeClass**
 1. Updates the admin password to enable the admin user.
* **@Before**
 1. Initiates admin client
 2. Imports test realms. (Loading test realms is overriden in subclasses.)
* **@After**
 1. Removes test realms.
 2. Closes admin client.

### ContainersTestEnricher

Manages *container lifecycles*.

`ContainersTestEnricher` is a custom Arquillian observer that handles lifecycles of auth server and app server containers for each test class.
Containers are started during `@BeforeClass` and shut down during `@AfterClass` event.

*Optionally* each test class can be annotated with `@AuthServerContainer("qualifier")` and `@AppServerConatiner("qualifier")` annotations 
to indicate containers required for the test.

* In case `@AuthServerContainer` is not present the *auth server qualifier* is loaded from `auth.server.container` property.
* In case `@AppServerContainer` is not present or it's value is the same as *auth server qualifier*, the app server isn't started for the test class.

## Admin Console Tests

Tests for admin console are located in `org/keycloak/testsuite/console`.
Related non-test classes are located on the same path in the **main sources**.

Admin console tests are **ENABLED by default**. They can be disabled by `-P no-console`.


## Adapter Tests

Adapter tests are located in `org/keycloak/testsuite/adapter`.
Related non-test classes can be found on the same path in the **main sources**.

Adapter tests are **DISABLED by default**. They can be enabled by profiles.
Multiple profiles can be enabled for a single test execution.

*Note:* When testing adapter with multiple containers in a single run it is better 
to use the `--fail-at-end` (`-fae`) strategy instead of the default `--fail-fast` one.
This will allow Maven to continue building other modules even if some of them have test failures.

### Containers Supported for Adapter Tests

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| **Wildfly 9** Relative | `auth-server-wildfly` | `-Pauth-server-wildfly` | `keycloak-server-dist` or `wildfly-dist`+`keycloak-server-overlay`, `keycloak-adapter-dist-wf9` |
| **Wildfly 9** | `app-server-wildfly` | `-Papp-server-wildfly` | `wildfly-dist`, `keycloak-adapter-dist-wf9` |
| **Wildfly 8** | `app-server-wildfly` | `-Papp-server-wildfly8` | `wildfly-dist:8.2.1.Final`, `keycloak-adapter-dist-wf8` |
| **JBoss AS 7** | `app-server-as7` | `-Papp-server-as7` | `jboss-as-dist`, `keycloak-adapter-dist-as7` |
| **Tomcat 8** | `app-server-tomcat` | `-Papp-server-tomcat` | `tomcat`, `keycloak-tomcat8-adapter-dist` |
| **Karaf 3** | `app-server-karaf` | `-Papp-server-karaf` | `apache-camel`, `apache-cxf`, `keycloak-osgi-features`, `keycloak-fuse-example-features` |

See the relevant container definitions in `tests/adapter/<container>/src/main/xslt/arquillian.xsl`.

***Important:*** Arquillian cannot load multiple controllers for JBossAS/Wildfly containers in a single run (because same class name)
but a different controller is required for JBossAS7/EAP6 than for WF8/9. Because of this:

 - Adapter tests for *Wildfly 8/9* cannot be run against server on *EAP 6*. `-Papp-server-wildfly*` ⇒ `!auth-server-eap6`
 - Adapter tests for *JBossAS 7* can only be run against server on *EAP 6*. `-Papp-server-as7,auth-server-eap6`

### Adapter Test Types

1. Using **test servlets**.
2. Using **example/demo wars**.

```
AbstractKeycloakTest
└── AbstractAdapterTest
    ├── AbstractServletsAdapterTest
    |   ├── Relative…
    |   ├── Wildfly…
    |   ├── Tomcat…
    |   …
    └── AbstractExampleAdapterTest
        ├── AbstractDemoExampleAdapterTest
        |   ├── Relative…
        |   ├── Wildfly…
        |   ├── Tomcat…
        |   …
        ├── AbstractBasicAuthExampleAdapterTest
        |   ├── Relative…
        |   ├── Wildfly…
        |   ├── Tomcat…
        |   …
        …
```

### Relative vs Non-relative scenario

The test suite can handle both types.
It automatically modifies imported test realms and deployments' adapter configs based on scenario type.

| Scenario | Description | Realm config (server side) | Adapter config (client side) |
| --- | --- | --- | --- |
| **Relative** | Both Keycloak Server and test apps running in the same container. | client `baseUrl`, `adminUrl` and `redirect-uris` can be relative | `auth-server-url` can be relative |
| **Non-relative** | Test apps run in a different container than Keycloak Server. | client `baseUrl`, `adminUrl` and `redirect-uris` need to include FQDN of the app server | `auth-server-url` needs to include FQDN of the auth server|

### Adapter Libraries Mode

1. **Provided.** By container, e.g. as a subsystem. *Default.*
2. **Bundled.** In the deployed war in `/WEB-INF/libs`. Enable with `-Dadapter.libs.bundled`. *Wildfly only*.

### Adapter Config Mode

1. ~~**Provided.** In `standalone.xml` using `secure-deployment`. *Wildfly only.*~~ WIP
2. **Bundled.** In the deployed war in `/WEB-INF/keycloak.json`. *Default.*

### Adapters Test Coverage

| Module | Coverage | Supported Containers |
| --- | --- | --- |
| ***Test Servlets*** | Good | All |
| **Demo** | Minimal, WIP | `auth-server-wildfly` (relative) |
| **Admin Client** |  |
| **Cordova** |  |
| **CORS** |  |
| **JS Console** | Good | `auth-server-wildfly` (relative) |
| **Providers** |  |
| Themes |  |
| Multitenancy | WIP |  |
| **Basic Auth** | Good | All |
| **Fuse** | Good | `app-server-karaf` |
| SAML |  |
| LDAP |  |
| Kerberos |  |

## Supported Browsers

| Browser | Maven |
| --- | --- | 
| **PhantomJS** | `-Dbrowser=phantomjs` **default** |
| **Firefox** | `-Dbrowser=firefox` |


## Custom Arquillian Extensions

Custom extensions are registered in `META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension`.

* Multiple containers extension
 * Replaces Arquillian's default container handling.
 * Allows to manage multiple container instances of different types within a single test run.
 * Allows to skip loading disabled containers based on `enabled` config property in `arquillian.xml`.
* Custom extension
 * `ContainersTestEnricher` - Handles lifecycles of auth-server and app-server.
 * `CustomUndertowContainer` - A custom container controller for JAX-RS-enabled Undertow with Keycloak Server.
 * `DeploymentArchiveProcessor` - Modifies adapter config before deployment on app server based on relative/non-relative scenario.
 * `URLProvider` - Fixes URLs injected by Arquillian which contain 127.0.0.1 instead of localhost.
 * `JiraTestExecutionDecider` - Skipping tests for unresolved JIRAs.

