package org.keycloak.tests.workflow.config;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class InviteUserStepServerConfig implements KeycloakServerConfig {

    public static final String HOSTNAME_URL = "http://localhost:8080";

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config
                .option("spi-workflow--default--executor-blocking", Boolean.TRUE.toString())
                .option("hostname", HOSTNAME_URL);
    }
}
