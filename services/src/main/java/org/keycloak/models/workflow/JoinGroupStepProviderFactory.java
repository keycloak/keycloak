package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class JoinGroupStepProviderFactory implements WorkflowStepProviderFactory<JoinGroupStepProvider> {

    public static final String ID = "join-group";

    @Override
    public JoinGroupStepProvider create(KeycloakSession session, ComponentModel model) {
        return new JoinGroupStepProvider(session, model);
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
        return "Adds user to one or more groups";
    }
}
