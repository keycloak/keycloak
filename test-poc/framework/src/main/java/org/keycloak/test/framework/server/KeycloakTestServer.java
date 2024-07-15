package org.keycloak.test.framework.server;

import java.util.Map;

public interface KeycloakTestServer {

    void start(KeycloakTestServerConfig serverConfig, Map<String, String> databaseConfig);

    void stop();

    String getBaseUrl();

}
