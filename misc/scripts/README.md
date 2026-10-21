## dependency-report.sh

Search for a dependency in the Keycloak project to identify where it is used, and if there are multiple versions in use.

For example:

```
misc/scripts/dependency-report.sh org.twitter4j:twitter4j-core
```

Will output a report like:

```
===================================================================================================
Dependency tree for org.twitter4j:twitter4j-core
---------------------------------------------------------------------------------------------------
org.keycloak:keycloak-services:jar:999.0.0-SNAPSHOT
\- org.twitter4j:twitter4j-core:jar:4.1.2:compile
org.keycloak:keycloak-crypto-fips1402:jar:999.0.0-SNAPSHOT
\- org.keycloak:keycloak-services:jar:999.0.0-SNAPSHOT:compile
   \- org.twitter4j:twitter4j-core:jar:4.1.2:compile
...
```

# kcw

Provides a quick and convenient way of starting a Keycloak server, supporting a specific version, a locally built version,
or the nightly release.

Examples:

```
kcw dev start-dev
kcw nightly start --hostname=mykeycloak
```

For more details run `kcw help`.