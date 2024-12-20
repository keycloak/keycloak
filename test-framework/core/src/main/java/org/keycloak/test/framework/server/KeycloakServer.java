package org.keycloak.test.framework.server;

public interface KeycloakServer {

    void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder);

    void stop();

    String getBaseUrl();

    String getManagementBaseUrl();
}
