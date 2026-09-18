package org.keycloak.tests.authz.config;

import org.keycloak.common.Profile;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class AuthzScriptsServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.features(Profile.Feature.SCRIPTS)
                .dependency("org.keycloak.tests", "keycloak-tests-custom-providers")
                .dependency("org.keycloak.tests", "keycloak-tests-custom-scripts")
                .dependency("org.keycloak.testsuite", "integration-arquillian-testsuite-providers");
    }
}
