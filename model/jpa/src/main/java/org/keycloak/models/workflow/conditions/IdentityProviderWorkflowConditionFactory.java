package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class IdentityProviderWorkflowConditionFactory implements WorkflowConditionProviderFactory<IdentityProviderWorkflowConditionProvider> {

    public static final String ID = "has-identity-provider-link";

    @Override
    public IdentityProviderWorkflowConditionProvider create(KeycloakSession session, String configParameter) {
        return new IdentityProviderWorkflowConditionProvider(session, configParameter);
    }

    @Override
    public String getId() {
        return ID;
    }

}
