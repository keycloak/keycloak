package org.keycloak.test.framework.server;

import org.keycloak.Keycloak;
import org.keycloak.common.Version;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class EmbeddedKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(List<String> rawOptions) {
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
