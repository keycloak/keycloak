# Keycloak Arquillian Integration TestSuite - Test Servers - Auth Server

- [Keycloak Arquillian Integration TestSuite](../../README.md)
- [Keycloak Arquillian Integration TestSuite - Test Servers](../README.md)
- Keycloak Arquillian Integration TestSuite - Test Servers - Auth Server
- [Keycloak Arquillian Integration TestSuite - Test Servers - App Servers](../app-server/README.md)

## Auth Server - JBoss

Common configurations of Keycloak server on JBoss-based container (Wildfly/EAP).

### Submodules

#### `wildfly` Wildfly latest
 - Builds keycloak server on top of latest Wildfly.
 - Activated by __`-Pauth-server-wildfly`__

#### `eap` EAP 7
 - Builds keycloak server on top of latest EAP.
 - Activated by __`-Pauth-server-eap`__
 - Requires access to product repo.
 - Requires additional properties:
   - `product.version`
   - `product.unpacked.folder.name` (optional; the default value is based on the POM versions)
   - `product` to activate the product profile; NOT required if `product.unpacked.folder.name` is specified

### Server dist vs overlay

By default `keycloak-server-dist` artifact is used for the build.
By setting `-Dserver-overlay=true` you can switch to server overlay instead. See profile `server-overlay`.

### JPA

Configures Keycloak JDBC datasource in `standalone.xml`. See profile `jpa`.

### Cluster

Configures in `standalone-ha.xml`:
- h2 datasource over TCP (if not executed together with jpa)
- parameters of Keycloak Infinispan caches

See profile `auth-server-cluster`.

## Auth Server - Undertow

Arquillian extension for running Keycloak server in embedded Undertow.
