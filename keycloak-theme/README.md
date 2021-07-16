# Keycloak Admin Console V2 Maven Build

The Maven build prepares the admin console to be deployed as a theme on the Keycloak server.

# Build Instructions

```bash
mvn install
```

# Deployment

The jar created with `mvn install` needs to be deployed to a Maven repository. From there, it will become part of the Keycloak server build.

For development, you can also just copy the contents of `build/target/classes` to `<keycloak server>/themes/keycloak.v2`. Then restart the server.

# To Run

Until New Admin Console becomes the default, you will need to start Keycloak server like this:

```bash
$> bin/standalone.sh -Dprofile.feature.newadmin=enabled
```

Then go to `Realm Settings --> Themes` and set Admin Console Theme to `keycloak.v2`.
