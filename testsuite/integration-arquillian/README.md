# Keycloak Integration Testsuite with Arquillian

## Usage

Running the tests: `mvn test`

## General info

**Exactly one** container has to be selected to run the Keycloak Server.
This container will be used by all tests during the test run (Maven build).

Undertow is default.

### Containers Supported for Keycloak Server

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| Undertow | `auth-server-undertow` | `-Dauth.server.container=auth-server-undertow` (default) | |
| Wildfly 9 | `auth-server-wildfly` | `-Dauth.server.container=auth-server-wildfly` or `-Pauth-server-wildfly` | `keycloak-demo-dist` |

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

## Admin Console Tests

Tests for admin console are located in **test sources** under `org/keycloak/testsuite/console/`.
Related WebDriver page objects and other non-test classes are located on the same path in **main sources**.

Admin console tests are **enabled by default**. The can be disabled by using `-Pno-console`.


## Adapter Tests

Adapter tests are located in **test sources** under `org/keycloak/testsuite/adapter/`.
Related non-test classes can be found on the same path in **main sources**.

Adapter tests are **disabled by default**. They need to be enabled by profiles.
Multiple profiles can be enabled for a single test run (Maven build).

### Containers Supported for Adapter Tests

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| Wildfly 9 Relative | `auth-server-wildfly` | `-Pauth-server-wildfly` | `keycloak-demo-dist` servers both as auth-server and app-server (relative test scenario) |
| Wildfly 9 | `app-server-wildfly` | `-Papp-server-wildfly` | `wildfly-dist`, `keycloak-adapter-dist-wf9` |
| Wildfly 9 Vanilla | `app-server-wildfly-vanilla` | `-Papp-server-wildfly-vanilla` (cannot be used with `-Papp-server-wildfly`) | `wildfly-dist`, `keycloak-adapter-dist-wf9` |
| JBoss AS 7 | `app-server-as7` | `-Papp-server-as7` | `jboss-as-dist`, `keycloak-adapter-dist-as7` |

See the relevant container definitions in `arquillian.xml` located in the **test resources** folder.

### Adapter Test Types

1. Using **test servlets**. All tests extending class `AbstractServletsAdapterTest`
2. Using **example/demo wars**. All tests extending class `AbstractExamplesAdapterTest`

### Relative vs Non-relative scenario

| Scenario | Description | Realm config (server-side) | Adapter config (client-side) |
| --- | --- | --- | --- |
| **Relative** | Both Keycloak Server and test apps running in the same container. | clients' `redirect-uris` can be relative | `auth-server-url` can be relative |
| **Non-relative** | Test apps run in a different container than Keycloak Server. | clients' `redirect-uris` need to include FQDN of the app server | `auth-server-url` needs to include FQDN of the auth server|

### Adapter Libraries Location

1. **Provided.** By container, e.g. as a subsystem.
2. **Bundled.** In the deployed war.


## Supported Browsers

| Browser | Maven |
| --- | --- | 
| PhantomJS | `-Dbrowser=phantomjs` (defatult) |
| Firefox | `-Dbrowser=firefox` |
