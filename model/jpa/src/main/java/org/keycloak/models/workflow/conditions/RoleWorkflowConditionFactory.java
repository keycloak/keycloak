package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

import com.google.auto.service.AutoService;

@AutoService(WorkflowConditionProviderFactory.class)
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
