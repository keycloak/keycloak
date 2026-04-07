package org.keycloak.tests.authz.services.config;

import org.keycloak.common.Profile;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class DeployedScriptPolicyServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config
                .features(Profile.Feature.SCRIPTS)
                .dependency("org.keycloak.tests", "keycloak-tests-custom-scripts");
    }
}
