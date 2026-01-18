package org.keycloak.models.workflow;

import java.util.Set;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class GrantRoleStepProviderFactory implements WorkflowStepProviderFactory<GrantRoleStepProvider> {

    public static final String ID = "grant-role";

    @Override
    public GrantRoleStepProvider create(KeycloakSession session, ComponentModel model) {
        return new GrantRoleStepProvider(session, model);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<ResourceType> getTypes() {
        return Set.of(ResourceType.USERS);
    }

    @Override
    public String getHelpText() {
        return "Grants one or more roles to a user";
    }
}
