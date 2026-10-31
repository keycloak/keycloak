package org.keycloak.models.workflow.events;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowEventProvider;
import org.keycloak.models.workflow.WorkflowEventProviderFactory;

public class UserRemoveCredentialWorkflowEventFactory implements WorkflowEventProviderFactory<WorkflowEventProvider> {

    public static final String ID = "user-remove-credential";

    @Override
    public WorkflowEventProvider create(KeycloakSession session, String configParameter) {
        return new UserRemoveCredentialWorkflowEventProvider(session, configParameter, this.getId());
    }

    @Override
    public String getId() {
        return ID;
    }
}
