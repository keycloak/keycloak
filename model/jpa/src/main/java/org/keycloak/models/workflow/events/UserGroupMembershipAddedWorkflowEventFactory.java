package org.keycloak.models.workflow.events;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowEventProvider;
import org.keycloak.models.workflow.WorkflowEventProviderFactory;

public class UserGroupMembershipAddedWorkflowEventFactory implements WorkflowEventProviderFactory<WorkflowEventProvider> {

    public static final String ID = "user-group-membership-added";

    @Override
    public WorkflowEventProvider create(KeycloakSession session, String configParameter) {
        return new UserGroupMembershipAddedWorkflowEventProvider(session, configParameter, this.getId());
    }

    @Override
    public String getId() {
        return ID;
    }
}
