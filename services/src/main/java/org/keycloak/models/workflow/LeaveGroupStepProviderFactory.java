package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class LeaveGroupStepProviderFactory implements WorkflowStepProviderFactory<LeaveGroupStepProvider> {

    public static final String ID = "leave-group";

    @Override
    public LeaveGroupStepProvider create(KeycloakSession session, ComponentModel model) {
        return new LeaveGroupStepProvider(session, model);
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.USERS;
    }

    @Override
    public String getHelpText() {
        return "Remove a user from a group.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}
