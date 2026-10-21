package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class UserAttributeWorkflowConditionFactory implements WorkflowConditionProviderFactory<UserAttributeWorkflowConditionProvider> {

    public static final String ID = "has-user-attribute";

    @Override
    public UserAttributeWorkflowConditionProvider create(KeycloakSession session, String keyValuePair) {
        return new UserAttributeWorkflowConditionProvider(session, keyValuePair);
    }

    @Override
    public String getId() {
        return ID;
    }

}
