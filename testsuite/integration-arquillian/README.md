# Keycloak Integration Testsuite with Arquillian

## Usage

Running the tests: `mvn test` or `mvn clean test`

## Test suite

### Selecting container for Keycloak Server

The testsuite requires a container for Keycloak Server to be selected.
This container is used by all tests in the test suite.
It can be selected using property `auth.server.container`.

By default the testsuite runs the server on embedded Undertow.

### Containers Supported for Keycloak Server

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| **Undertow** | `auth-server-undertow` | `-Dauth.server.container=auth-server-undertow` **default** | `undertow-core`, `resteasy-undertow` |
| **Wildfly 9** | `auth-server-wildfly` | `-Dauth.server.container=auth-server-wildfly` or `-Pauth-server-wildfly` | `keycloak-demo-dist` |

See the relevant container definitions in `arquillian.xml` located in the **test resources** folder.

### Test Class Hierarchy
```
AbstractKeycloakTest
├── AbstractAdminConsoleTest
├── AbstractAdapterTest
│   ├── AbstractServletsAdapterTest
│   └── AbstractExamplesAdapterTest
...
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
Containers are started during @BeforeClass - and shut down during @AfterClass event.

*Optionally* each test can be annotated with `@AuthServerContainer("qualifier")` and `@AppServerConatiner("qualifier")` annotations.

* In case `@AuthServerContainer` is not provided the *auth server qualifier* is loaded from `auth.server.container` property.
* In case `@AppServerContainer` is not provided or it's value is the same as *auth server qualifier*, the app server isn't started.

## Admin Console Tests

Tests for admin console are located in `org/keycloak/testsuite/console`.
Related non-test classes are located on the same path in the **main sources**.

Admin console tests are **ENABLED by default**. They can be disabled by `-P no-console`.


## Adapter Tests

Adapter tests are located in `org/keycloak/testsuite/adapter`.
Related non-test classes can be found on the same path in the **main sources**.

Adapter tests are **DISABLED by default**. They can be enabled by profiles.
Multiple profiles can be enabled for a single test run (Maven build).

### Containers Supported for Adapter Tests

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| **Wildfly 9** Relative | `auth-server-wildfly` | `-Pauth-server-wildfly` | `keycloak-demo-dist` servers both as auth-server and app-server (relative test scenario) |
| **Wildfly 9** | `app-server-wildfly` | `-Papp-server-wildfly` | `wildfly-dist`, `keycloak-adapter-dist-wf9` |
| **Wildfly 9** Vanilla | `app-server-wildfly-vanilla` | `-Papp-server-wildfly-vanilla` (mutually exclusive with `-Papp-server-wildfly`) | `wildfly-dist`, `keycloak-adapter-dist-wf9` |
| **JBoss AS 7** | `app-server-as7` | `-Papp-server-as7` | `jboss-as-dist`, `keycloak-adapter-dist-as7` |
| **Tomcat 8** | `app-server-tomcat` | `-Papp-server-tomcat` | `tomcat`, `keycloak-tomcat8-adapter-dist` |

See the relevant container definitions in `arquillian.xml` located in the **test resources** folder.

### Adapter Test Types

1. Using **test servlets**. All tests extending class `AbstractServletsAdapterTest`
2. Using **example/demo wars**. All tests extending class `AbstractExamplesAdapterTest`

### Relative vs Non-relative scenario

| Scenario | Description | Realm config (server-side) | Adapter config (client-side) |
| --- | --- | --- | --- |
| **Relative** | Both Keycloak Server and test apps running in the same container. | clients' `baseUrl`, `adminUrl` and `redirect-uris` can be relative | `auth-server-url` can be relative |
| **Non-relative** | Test apps run in a different container than Keycloak Server. | clients' `baseUrl`, `adminUrl` and `redirect-uris` need to include FQDN of the app server | `auth-server-url` needs to include FQDN of the auth server|

### Adapter Libraries Mode

1. **Provided.** By container, e.g. as a subsystem.
2. **Bundled.** In the deployed war. Used with `app-server-wildfly-vanilla` which doesn't have adapter subsystem installed.


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
 * `CustomUndertowContainer` - Custom undertow conatiner adapter.
 * `DeploymentArchiveProcessor` - Modifies adapter config before deployment on app server based on relative/non-relative scenario.
 * `URLProvider` - Fixes URLs injected by Arquillian which contain 127.0.0.1 instead of localhost.
 * `JiraTestExecutionDecider` - Skipping tests for unresolved JIRAs.

