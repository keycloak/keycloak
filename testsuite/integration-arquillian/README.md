# Keycloak Integration Testsuite with Arquillian

## Usage

Run the test suite: `mvn test`

## General info

### Containers Supported for Keycloak Server

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| Undertow | auth-server-undertow | Enabled by default | |
| Wildfly 9 | auth-server-wildfly | `-Pauth-server-wildfly` | keycloak-demo-dist |

See the relevant container definitions in `arquillian.xml` located in the **test resources** folder.

## Admin Console Tests

Tests for admin console are located in **test sources** under `org/keycloak/testsuite/console/`.
Related WebDriver page objects and other non-test classes are located in **main sources** on the same path.

Admin console tests are **enabled by default**. The can be disabled by using `-Pno-console`.


## Adapter Tests

Adapter tests are located in **test sources** under `org/keycloak/testsuite/adapter/`.
Related non-test classes can be found on the same path in **main sources**.

Adapter tests are **disabled by default**. The need to be enabled by profiles.

### Containers Supported for Adapter Tests

| Container | Arquillian Qualifier | Maven | Dependencies |
| --- | --- | --- | --- |
| Wildfly 9 Relative | auth-server-wildfly | `-Pauth-server-wildfly` | keycloak-demo-dist |
| Wildfly 9 | app-server-wildfly | `-Papp-server-wildfly` | wildfly-dist, keycloak-adapter-dist-wf9 |
| JBoss AS 7 | app-server-as7 | `-Papp-server-as7` | jboss-as-dist, keycloak-adapter-dist-as7 |


## Supported Browsers

| Browser | Maven |
| --- | --- | 
| PhantomJS | Enabled by defatult |
| Firefox | `-Dbrowser=firefox` |
