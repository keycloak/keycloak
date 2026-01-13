package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class RemoveUserAttributeStepProviderFactory implements WorkflowStepProviderFactory<RemoveUserAttributeStepProvider> {

    public static final String ID = "remove-user-attribute";

    @Override
    public RemoveUserAttributeStepProvider create(KeycloakSession session, ComponentModel model) {
        return new RemoveUserAttributeStepProvider(session, model);
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
        return "Removes attributes from a user. Configure attributes to remove using the 'attribute' configuration key with the attribute names.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // No specific config properties exposed in the UI currently. Attributes are read from the 'attribute' config key.
        return List.of();
    }
}
