package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class RestartWorkflowStepProviderFactory implements WorkflowStepProviderFactory<RestartWorkflowStepProvider> {

    public static final String ID = "restart";

    @Override
    public RestartWorkflowStepProvider create(KeycloakSession session, ComponentModel model) {
        return new RestartWorkflowStepProvider();
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(org.keycloak.models.KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public ResourceType getType() {
        // TODO: need to revisit this once we support more types as this provider should be usable for all resource types.
        return ResourceType.USERS;
    }

    @Override
    public String getHelpText() {
        return "Restarts the current workflow";
    }
}

