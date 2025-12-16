package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class GrantRoleStepProviderFactory implements WorkflowStepProviderFactory<GrantRoleStepProvider> {

    public static final String ID = "grant-role";

    @Override
    public GrantRoleStepProvider create(KeycloakSession session, ComponentModel model) {
        return new GrantRoleStepProvider(session, model);
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
        return "Grant a role to a user.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}
