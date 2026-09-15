package org.keycloak.tests.broker;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class BrokerServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
        return builder
                .dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
    }
}
