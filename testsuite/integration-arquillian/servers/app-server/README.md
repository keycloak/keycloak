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
* __`eap6` EAP 6__ Requires access to EAP product repo, or setting `-Deap6.version` to public EAP 6 Alpha.
* __`eap` EAP 7__ Requires access to EAP product repo.
* __`relative`__ Activate with `-Papp-server-relative`.
 * __`wildfly` Relative Wildfly 10__ Based on [`auth-server/jboss/wildfly`](../auth-server/README.md). Activate with `-Pauth-server-wildfly`.
 * __`eap` Relative EAP 7__ Based on [`auth-server/jboss/eap`](../auth-server/README.md). Activate with `-Pauth-server-eap`.

### Adapter Configs Location

* __Provided__ (in standalone.xml as secure-deployment) WIP
* __Bundled__ (in war) - Default.

### SSL

Configures SSL in `standalone.xml`. See profile `ssl`.

## App Server - Tomcat
Submodules are enabled with profiles: `-Papp-server-MODULE`
### Modules
* __`tomcat8` Tomcat 8__
* __`tomcat9` Tomcat 9__
