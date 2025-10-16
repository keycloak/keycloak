package org.keycloak.testframework.server;

public interface KeycloakServer {

    void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder, boolean tlsEnabled);

    void stop();

    String getBaseUrl();

    String getManagementBaseUrl();

}
