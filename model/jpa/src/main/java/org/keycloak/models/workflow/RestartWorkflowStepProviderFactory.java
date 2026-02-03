package org.keycloak.models.workflow;

import java.util.Set;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public final class RestartWorkflowStepProviderFactory implements WorkflowStepProviderFactory<RestartWorkflowStepProvider> {

    public static final String ID = "restart";
    public static final String CONFIG_POSITION = "position";

    @Override
    public RestartWorkflowStepProvider create(KeycloakSession session, ComponentModel model) {
        return new RestartWorkflowStepProvider(getPosition(model));
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        if (getPosition(model) < 0) {
            throw new ComponentValidationException("Position must be a non-negative integer");
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Set<ResourceType> getSupportedResourceTypes() {
        // Usable for all resource types.
        return Set.of(ResourceType.values());
    }

    @Override
    public String getHelpText() {
        return "Restarts the current workflow";
    }

    private int getPosition(ComponentModel model) {
        return model.get(CONFIG_POSITION, 0);
    }
}
