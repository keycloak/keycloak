package org.keycloak.test.framework.server;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;
import org.keycloak.test.framework.database.DatabaseConfig;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(KeycloakTestServerConfig serverConfig, DatabaseConfig databaseConfig) {

        List<String> rawOptions = new LinkedList<>();
        rawOptions.add("start-dev");
        rawOptions.add("--cache=local");

        if (!serverConfig.features().isEmpty()) {
            rawOptions.add("--features=" + String.join(",", serverConfig.features()));
        }

        serverConfig.adminUserName().ifPresent(username -> rawOptions.add("--bootstrap-admin-username=" + username));
        serverConfig.adminUserPassword().ifPresent(password -> rawOptions.add("--bootstrap-admin-password=" + password));

        //rawOptions.add("--db=" + databaseConfig.vendor()); // TODO With dev-mem there's an issue as the H2 DB isn't stopped when restarting embedded server
        if (databaseConfig.isExternal()) {
            rawOptions.add("--db-url-host=" + databaseConfig.urlHost());
            rawOptions.add("--db-username=" + databaseConfig.username());
            rawOptions.add("--db-password=" + databaseConfig.password());
        }


        serverConfig.options().forEach((key, value) -> rawOptions.add("--" + key + "=" + value));

        keycloak = Keycloak.builder()
                .setVersion(Version.VERSION)
                .start(rawOptions);
    }

    @Override
    public void stop() {
        try {
            keycloak.stop();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
