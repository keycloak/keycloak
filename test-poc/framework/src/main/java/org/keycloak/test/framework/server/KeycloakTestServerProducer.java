package org.keycloak.test.framework.server;

import static org.keycloak.test.framework.server.KeycloakTestServerProperties.*;
import static org.keycloak.test.framework.server.KeycloakTestServerType.*;

public class KeycloakTestServerProducer {

    public static KeycloakTestServer createKeycloakTestServerInstance(KeycloakTestServerType keycloakTestServerType) {
        KeycloakTestServer server;

        switch (keycloakTestServerType) {
            case STANDALONE: server = new StandaloneKeycloakTestServer();
            case REMOTE: server = new RemoteKeycloakTestServer();
            default: server = new EmbeddedKeycloakTestServer();
        }
        return server;
    }

    public static KeycloakTestServer createKeycloakTestServerInstance() {
        return createKeycloakTestServerInstance(getKeycloakTestServerType(KEYCLOAK_TEST_SERVER_ENV_VALUE));
    }

    public static KeycloakTestServerType getKeycloakTestServerType(String envVal) {
        KeycloakTestServerType type;

        switch (envVal) {
            case KEYCLOAK_TEST_SERVER_PROP_STANDALONE: type = STANDALONE;
            case KEYCLOAK_TEST_SERVER_PROP_REMOTE: type = REMOTE;
            default: type = EMBEDDED;
        }
        return type;
    }
}
