package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

public class RestartWorkflowStepProviderFactory implements WorkflowStepProviderFactory<RestartWorkflowStepProvider> {

    public static final String ID = "restart";

    private final RestartWorkflowStepProvider provider = new RestartWorkflowStepProvider();

    @Override
    public RestartWorkflowStepProvider create(KeycloakSession session, ComponentModel model) {
        return provider;
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
