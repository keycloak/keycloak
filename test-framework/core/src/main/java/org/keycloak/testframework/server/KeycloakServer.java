package org.keycloak.testframework.server;

import java.util.Map;

public interface KeycloakServer {

    void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder);

    void stop();

    String getBaseUrl();

    String getManagementBaseUrl();

    Map<String, String> getAdminClientSettings();
}
