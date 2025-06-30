package org.keycloak.test.framework.server;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(KeycloakTestServerConfig serverConfig) {
        System.setProperty("keycloakAdmin", "admin");
        System.setProperty("keycloakAdminPassword", "admin");

        List<String> rawOptions = new LinkedList<>();
        rawOptions.add("start-dev");
//        rawOptions.add("--db=dev-mem"); // TODO With dev-mem there's an issue as the H2 DB isn't stopped when restarting embedded server
        rawOptions.add("--cache=local");

        if (!serverConfig.features().isEmpty()) {
            rawOptions.add("--features=" + String.join(",", serverConfig.features()));
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
