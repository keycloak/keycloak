package org.keycloak.models.workflow;

import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class UnlinkUserStepProviderFactory implements WorkflowStepProviderFactory<UnlinkUserStepProvider> {
    
    public static final String ID = "unlink-user";

    @Override
    public UnlinkUserStepProvider create(KeycloakSession session, ComponentModel model) {
        return new UnlinkUserStepProvider(session, model);
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
    public Set<ResourceType> getSupportedResourceTypes() {
        return Set.of(ResourceType.USERS);
    }

    @Override
    public String getHelpText() {
        return "Unlink a user from a configured Identity Provider or from all Identity Providers.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
}
