package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

import java.util.List;
import java.util.Map;

public class ExpressionWorkflowConditionFactory implements WorkflowConditionProviderFactory<ExpressionWorkflowConditionProvider> {

    public static final String ID = "expression";
    public static final String EXPRESSION = "expression";

    @Override
    public ExpressionWorkflowConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new ExpressionWorkflowConditionProvider(session, config.getOrDefault(EXPRESSION, List.of()).stream().findFirst().orElse(""));
    }

    @Override
    public ExpressionWorkflowConditionProvider create(KeycloakSession session, List<String> configParameters) {
        if (configParameters.size() > 1) {
            throw new IllegalArgumentException("Expected single configuration parameter (expression)");
        }
        return create(session, Map.of(EXPRESSION, configParameters));
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
