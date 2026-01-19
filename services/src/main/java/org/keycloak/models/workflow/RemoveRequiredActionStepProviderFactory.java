package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class RemoveRequiredActionStepProviderFactory implements WorkflowStepProviderFactory<RemoveRequiredActionStepProvider> {

    public static final String ID = "remove-user-required-action";

    @Override
    public RemoveRequiredActionStepProvider create(KeycloakSession session, ComponentModel model) {
        return new RemoveRequiredActionStepProvider(session, model);
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
        return "Removes a required action from a user";
    }
}
