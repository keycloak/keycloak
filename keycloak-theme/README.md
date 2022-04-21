# Keycloak Theme (Maven Build)

This directory contains the Maven build for the Keycloak theme. It allows the theme to be built as a JAR which can be included when running the Keycloak server.

## Building

```bash
mvn install
```

## Deployment

First build the this repository with the instructions above, then [build the Keycloak sever](https://github.com/keycloak/keycloak/blob/main/docs/building.md). Start the Keycloak server and navigate to `Realm Settings` ➡️ `Themes` and set admin theme to `keycloak.v2`.
