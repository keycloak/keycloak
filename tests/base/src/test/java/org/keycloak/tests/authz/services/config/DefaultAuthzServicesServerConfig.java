package org.keycloak.tests.authz.services.config;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class DefaultAuthzServicesServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
    }
}
