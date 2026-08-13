package org.keycloak.testframework.server;

import org.keycloak.testframework.config.Config;

public interface KeycloakServer {

    void start(KeycloakServerConfigBuilder keycloakServerConfigBuilder, boolean tlsEnabled);

    void stop();

    String getBaseUrl();

    String getManagementBaseUrl();

    static boolean getDependencyHotDeployEnabled() {
        return Boolean.parseBoolean(Config.getValueTypeConfig(KeycloakServer.class, "hot.deploy", "false", String.class));
    }

}
