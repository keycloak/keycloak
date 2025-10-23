package org.keycloak.models.workflow.conditions;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class GroupMembershipWorkflowConditionFactory implements WorkflowConditionProviderFactory<GroupMembershipWorkflowConditionProvider> {

    public static final String ID = "is-member-of";
    public static final String EXPECTED_GROUPS = "groups";

    @Override
    public GroupMembershipWorkflowConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new GroupMembershipWorkflowConditionProvider(session, config.get(EXPECTED_GROUPS));
    }

    @Override
    public GroupMembershipWorkflowConditionProvider create(KeycloakSession session, List<String> configParameters) {
        return new GroupMembershipWorkflowConditionProvider(session, configParameters);
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
