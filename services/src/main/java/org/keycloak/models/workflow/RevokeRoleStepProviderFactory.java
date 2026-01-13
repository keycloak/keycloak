package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class RevokeRoleStepProviderFactory implements WorkflowStepProviderFactory<RevokeRoleStepProvider> {

    public static final String ID = "revoke-role";

    @Override
    public RevokeRoleStepProvider create(KeycloakSession session, ComponentModel model) {
        return new RevokeRoleStepProvider(session, model);
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
        return "Revokes roles assigned to the user";
    }
}
