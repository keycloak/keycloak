package org.keycloak.models.workflow;

import java.util.Set;

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
    public Set<ResourceType> getTypes() {
        return Set.of(ResourceType.USERS);
    }

    @Override
    public String getHelpText() {
        return "Adds a required action to the user";
    }
}
