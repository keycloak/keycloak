package org.keycloak.test.framework.server;

public class KeycloakTestServerProducer {

    public static KeycloakTestServer createKeycloakTestServerInstance(String serverType) {
        KeycloakTestServer keycloakServer;

        switch (serverType) {
            case "embedded": keycloakServer = new EmbeddedKeycloakTestServer();
            case "remote": keycloakServer = new RemoteKeycloakTestServer();
            default: keycloakServer = new EmbeddedKeycloakTestServer();
        }
        return keycloakServer;
    }
}
