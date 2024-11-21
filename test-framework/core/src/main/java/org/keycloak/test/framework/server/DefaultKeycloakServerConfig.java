package org.keycloak.test.framework.server;

public class DefaultKeycloakServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config;
    }

}
