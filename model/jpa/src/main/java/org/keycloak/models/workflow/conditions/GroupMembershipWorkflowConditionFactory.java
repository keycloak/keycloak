package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class GroupMembershipWorkflowConditionFactory implements WorkflowConditionProviderFactory<GroupMembershipWorkflowConditionProvider> {

    public static final String ID = "is-member-of";

    @Override
    public GroupMembershipWorkflowConditionProvider create(KeycloakSession session, String configParameter) {
        return new GroupMembershipWorkflowConditionProvider(session, configParameter);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
