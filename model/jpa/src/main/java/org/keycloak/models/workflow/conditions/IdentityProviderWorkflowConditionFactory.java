package org.keycloak.models.workflow.conditions;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class IdentityProviderWorkflowConditionFactory implements WorkflowConditionProviderFactory<IdentityProviderWorkflowConditionProvider> {

    public static final String ID = "has-identity-provider-link";
    public static final String EXPECTED_ALIASES = "alias";

    @Override
    public IdentityProviderWorkflowConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new IdentityProviderWorkflowConditionProvider(session, config.get(EXPECTED_ALIASES));
    }

    @Override
    public IdentityProviderWorkflowConditionProvider create(KeycloakSession session, List<String> configParameters) {
        return new IdentityProviderWorkflowConditionProvider(session, configParameters);
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
