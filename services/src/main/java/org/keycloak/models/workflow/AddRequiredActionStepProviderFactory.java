package org.keycloak.models.workflow;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class AddRequiredActionStepProviderFactory implements WorkflowStepProviderFactory<AddRequiredActionStepProvider> {

    public static final String ID = "set-user-required-action";

    @Override
    public AddRequiredActionStepProvider create(KeycloakSession session, ComponentModel model) {
        return new AddRequiredActionStepProvider(session, model);
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
        return "Adds a required action to the user";
    }
}
