package org.keycloak.tests.model;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class CustomProvidersServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
    }
}
