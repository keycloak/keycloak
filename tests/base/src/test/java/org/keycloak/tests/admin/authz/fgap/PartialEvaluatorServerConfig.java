package org.keycloak.tests.admin.authz.fgap;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class PartialEvaluatorServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.spiOption("authorization", "authorization", "jpa-in-parameters-limit-threshold", "5");
    }
}
