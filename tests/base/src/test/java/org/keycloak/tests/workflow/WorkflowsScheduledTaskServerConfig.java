package org.keycloak.tests.workflow;

import org.keycloak.models.workflow.WorkflowsEventListenerFactory;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class WorkflowsScheduledTaskServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return config.option("spi-events-listener--" + WorkflowsEventListenerFactory.ID + "--step-runner-task-interval", "1000");
    }
}
