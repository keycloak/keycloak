package org.keycloak.tests.admin.model.policy;

import org.keycloak.models.policy.ResourcePolicyEventListenerFactory;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class RLMScheduledTaskServerConfig extends RLMServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return super.configure(config)
                .option("spi-events-listener--" + ResourcePolicyEventListenerFactory.ID + "--action-runner-task-interval", "1000");
    }
}
