package org.keycloak.tests.admin.model.workflow;

import org.keycloak.models.workflow.WorkflowsEventListenerFactory;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class WorkflowsScheduledTaskServerConfig extends WorkflowsBlockingServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return super.configure(config)
                .option("spi-events-listener--" + WorkflowsEventListenerFactory.ID + "--step-runner-task-interval", "1000");
    }
}
