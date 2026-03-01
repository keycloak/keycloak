# Keycloak Arquillian Integration TestSuite - Test Servers - Auth Server

- [Keycloak Arquillian Integration TestSuite](../../README.md)
- [Keycloak Arquillian Integration TestSuite - Test Servers](../README.md)
- Keycloak Arquillian Integration TestSuite - Test Servers - Auth Server
- [Keycloak Arquillian Integration TestSuite - Test Servers - App Servers](../app-server/README.md)

### Common directory
 - Contains all necessary files for all Auth servers

### Auth Server Services
- Contains usually test providers and its associated factories used in the testsuite

## Auth servers

### Undertow
- Arquillian extension for running Keycloak server in embedded Undertow.
- Activated by default, or explicitly by __`-Pauth-server-undertow`__

### Quarkus
 - Builds keycloak server on top of used Quarkus with a particular version.
 - Activated by __`-Pauth-server-quarkus`__
