package org.keycloak.models.workflow;

import java.util.Set;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class UnlinkUserStepProviderFactory implements WorkflowStepProviderFactory<UnlinkUserStepProvider> {

    public static final String ID = "unlink-user";

    @Override
    public UnlinkUserStepProvider create(KeycloakSession session, ComponentModel model) {
        return new UnlinkUserStepProvider(session, model);
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
}
