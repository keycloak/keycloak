package org.keycloak.test.framework.server;

public interface KeycloakTestServer {

    void start(KeycloakTestServerConfig serverConfig);

    void stop();

    String getBaseUrl();

}
