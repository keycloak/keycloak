package org.keycloak.tests.admin.model.workflow;

import org.keycloak.common.Profile.Feature;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class WorkflowsServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.features(Feature.WORKFLOWS);
    }
}
