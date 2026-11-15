# Jackson 3 representation adapters

This module provides Jackson 3 serializers and deserializers for Keycloak's shared representation annotations.
The adapters are used by the server and the Jackson 3 admin client.

The module requires Java 17. Mapper configuration and provider registration are handled by the consuming modules.

## Synchronizing public client sources

The public Java clients are maintained in [keycloak/keycloak-client](https://github.com/keycloak/keycloak-client).
When synchronizing the Jackson 3 admin client, include `org/keycloak/json/jackson3/**/*.java` from the
`keycloak-core-jackson3` sources JAR alongside the admin-client sources. Source JARs are generated with the
`jboss-release` Maven profile.

The adapters are compiled as part of the Java 17 client. The shared `core` and `common` sources retain their Java 8 baseline.
