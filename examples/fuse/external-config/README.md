Keycloak Example - Externalized keycloak.json 
=======================================

The following example was tested on JBoss Fuse 6.3 and shows a way to package an OSGi compatible .war file that does not
 include keycloak.json file in the .war archive but that automatically loads it based on a naming convention.
 
To enable the functionality you need to add this section to your `web.xml`:

```
    <context-param>
        <param-name>keycloak.config.resolver</param-name>
        <param-value>org.keycloak.adapters.osgi.PathBasedKeycloakConfigResolver</param-value>
    </context-param>
```

That component will use `keycloak.config` or `karaf.etc` java properties to look for a base folder to look for the configuration.
 
Inside one of those folders it will look for a file called `<your_web_context>-keycloak.json`.

For this example you need to copy the file `external-config-keycloak.json` to your JBoss Fuse `etc/` folder.

Once you have done that and once you added feature `keycloak-fuse-6.3-example` (See [here](../README.md) for more details), you can try to access the endpoint: http://localhost:8181/external-config/index.html