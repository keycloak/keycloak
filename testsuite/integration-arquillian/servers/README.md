# Keycloak Arquillian Integration TestSuite - Test Servers

- [Keycloak Arquillian Integration TestSuite](../README.md)
- Keycloak Arquillian Integration TestSuite - Test Servers
- [Keycloak Arquillian Integration TestSuite - Test Servers - Auth Server](auth-server/README.md)
- [Keycloak Arquillian Integration TestSuite - Test Servers - App Servers](app-server/README.md)

## Test Servers

A set of modules that build test-server artifacts preconfigured for various test scenarios.
The artifacts are used by the Arquillian TestSuite.

### Auth Server

- JBoss
  - Wildfly 10
  - EAP 7
- Undertow


### App Server

- JBoss
  - JBossAS 7
  - Wildfly 8, 9, 10
  - EAP 6, 7
  - Relative (Wildfly 10 / EAP 7)
- Tomcat
  - Tomcat 7, 8, 9


### Load Balancer

- Wildfly + mod_cluster

