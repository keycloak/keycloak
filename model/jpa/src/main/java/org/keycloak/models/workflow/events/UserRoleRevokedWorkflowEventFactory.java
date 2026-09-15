package org.keycloak.models.workflow.events;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowEventProvider;
import org.keycloak.models.workflow.WorkflowEventProviderFactory;

public class UserRoleRevokedWorkflowEventFactory implements WorkflowEventProviderFactory<WorkflowEventProvider> {

    public static final String ID = "user-role-revoked";

    @Override
    public WorkflowEventProvider create(KeycloakSession session, String configParameter) {
        return new UserRoleRevokedWorkflowEventProvider(session, configParameter, this.getId());
    }

    @Override
    public String getId() {
        return ID;
    }
}
