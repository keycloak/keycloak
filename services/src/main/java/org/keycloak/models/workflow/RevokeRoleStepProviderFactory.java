package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class RevokeRoleStepProviderFactory implements WorkflowStepProviderFactory<RevokeRoleStepProvider> {

    public static final String ID = "revoke-role";

    @Override
    public RevokeRoleStepProvider create(KeycloakSession session, ComponentModel model) {
        return new RevokeRoleStepProvider(session, model);
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
        return "Revoke a user role.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}
