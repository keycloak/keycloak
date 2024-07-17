package org.keycloak.test.framework.server;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(KeycloakTestServerConfig serverConfig, Map<String, String> databaseConfig) {
        List<String> rawOptions = new LinkedList<>();
        rawOptions.add("start-dev");
        rawOptions.add("--cache=local");

        if (!serverConfig.features().isEmpty()) {
            rawOptions.add("--features=" + String.join(",", serverConfig.features()));
        }

        if (serverConfig.adminUserName() != null) {
            System.setProperty("keycloakAdmin", serverConfig.adminUserName());
        } else {
            System.getProperties().remove("keycloakAdmin");
        }
        if (serverConfig.adminUserPassword() != null) {
            System.setProperty("keycloakAdminPassword", serverConfig.adminUserPassword());
        } else {
            System.getProperties().remove("keycloakAdminPassword");
        }

        serverConfig.options().forEach((key, value) -> rawOptions.add("--" + key + "=" + value));
        databaseConfig.forEach((key, value) -> rawOptions.add("--" + key + "=" + value));

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
