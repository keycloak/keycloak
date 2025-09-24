package org.keycloak.models.workflow.conditions;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class RoleWorkflowConditionFactory implements WorkflowConditionProviderFactory<RoleWorkflowConditionProvider> {

    public static final String ID = "has-role";
    public static final String EXPECTED_ROLES = "roles";

    @Override
    public RoleWorkflowConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new RoleWorkflowConditionProvider(session, config.get(EXPECTED_ROLES));
    }

    @Override
    public RoleWorkflowConditionProvider create(KeycloakSession session, List<String> configParameters) {
        return new RoleWorkflowConditionProvider(session, configParameters);
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
