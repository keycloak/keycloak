package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class RoleWorkflowConditionFactory implements WorkflowConditionProviderFactory<RoleWorkflowConditionProvider> {

    public static final String ID = "has-role";

    @Override
    public RoleWorkflowConditionProvider create(KeycloakSession session, String expectedRole) {
        return new RoleWorkflowConditionProvider(session, expectedRole);
    }

    @Override
    public String getId() {
        return ID;
    }

}
