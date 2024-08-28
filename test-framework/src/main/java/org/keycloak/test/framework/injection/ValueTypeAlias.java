package org.keycloak.test.framework.injection;

import org.keycloak.test.framework.database.TestDatabase;
import org.keycloak.test.framework.server.KeycloakTestServer;
import org.openqa.selenium.WebDriver;

import java.util.Map;

public class ValueTypeAlias {

    private static final Map<Class, String> aliases = Map.of(
            WebDriver.class, "browser",
            KeycloakTestServer.class, "server",
            TestDatabase.class, "database"
    );

    public static String getAlias(Class<?> clazz) {
        String alias = aliases.get(clazz);
        if (alias == null) {
            alias = clazz.getSimpleName();
        }
        return alias;
    }

}
