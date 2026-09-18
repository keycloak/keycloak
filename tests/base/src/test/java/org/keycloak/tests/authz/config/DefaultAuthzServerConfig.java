package org.keycloak.tests.authz.config;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 * Shared server config for migrated authz tests. Deploys custom authz policy providers and the
 * legacy testsuite providers required by {@code KeycloakTestingClient} ({@code /testing} endpoints).
 */
public class DefaultAuthzServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers")
                .dependency("org.keycloak.testsuite", "integration-arquillian-testsuite-providers");
    }
}
