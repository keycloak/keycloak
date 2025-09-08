package org.keycloak.testframework.server;

public interface KeycloakServer {

    void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder);

    void stop();

    String getBaseUrl();

    String getManagementBaseUrl();

    boolean isTlsEnabled();
}
