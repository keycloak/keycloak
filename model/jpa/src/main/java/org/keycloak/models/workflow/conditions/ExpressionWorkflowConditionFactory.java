package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class ExpressionWorkflowConditionFactory implements WorkflowConditionProviderFactory<ExpressionWorkflowConditionProvider> {

    public static final String ID = "expression";

    @Override
    public ExpressionWorkflowConditionProvider create(KeycloakSession session, String configParameter) {
        if (configParameter == null) {
            throw new IllegalArgumentException("Expected single configuration parameter (expression)");
        }
        return new ExpressionWorkflowConditionProvider(session, configParameter);
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
