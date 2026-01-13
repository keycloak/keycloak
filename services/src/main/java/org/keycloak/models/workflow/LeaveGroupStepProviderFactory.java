package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class LeaveGroupStepProviderFactory implements WorkflowStepProviderFactory<LeaveGroupStepProvider> {

    public static final String ID = "leave-group";

    @Override
    public LeaveGroupStepProvider create(KeycloakSession session, ComponentModel model) {
        return new LeaveGroupStepProvider(session, model);
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
        return "Removes a user from one or more groups";
    }
}
