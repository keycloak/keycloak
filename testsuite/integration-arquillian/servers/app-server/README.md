# Keycloak Arquillian Integration TestSuite - Test Servers - App Servers

- [Keycloak Arquillian Integration TestSuite](../../README.md)
- [Keycloak Arquillian Integration TestSuite - Test Servers](../README.md)
- [Keycloak Arquillian Integration TestSuite - Test Servers - Auth Server](../auth-server/README.md)
- Keycloak Arquillian Integration TestSuite - Test Servers - App Servers

## App Server - JBoss

JBoss-based container with installed and configured Keycloak adapter.

Submodules are enabled with profiles: `-Papp-server-MODULE`

### Modules

* __`wildfly9` Wildfly 9__
* __`wildfly10` Wildfly 10__
* __`wildfly` Wildfly 11__

### Adapter Configs Location

* __Provided__ (in standalone.xml as secure-deployment) WIP
* __Bundled__ (in war) - Default.

### SSL

Configures SSL in `standalone.xml`. See profile `ssl`.
