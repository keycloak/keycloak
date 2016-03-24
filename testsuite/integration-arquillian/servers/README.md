# Keycloak Arquillian Integration TestSuite

[Keycloak Arquillian Integration TestSuite](../README.md)

## Test Servers

A set of modules that build test-server artifacts preconfigured for various test scenarios.
The artifacts are used by the Arquillian TestSuite.

### Auth Server

- JBoss
 - Wildfly 10
 - EAP 7
- Undertow

[Details...](auth-server/README.md)


### App Server

- JBoss
 - JBossAS 7
 - Wildfly 8, 9, 10
 - EAP 6, 7
 - Relative (Wildfly 10 / EAP 7)
- Karaf / Fuse
 - Karaf 3
 - Fuse 6.1, 6.2
- Tomcat
 - Tomcat 7, 8

[Details...](app-server/README.md)

### Load Balancer

- Wildfly + mod_cluster

