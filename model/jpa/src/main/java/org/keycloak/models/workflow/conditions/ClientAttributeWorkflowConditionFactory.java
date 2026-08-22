package org.keycloak.models.workflow.conditions;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.workflow.WorkflowConditionProviderFactory;

public class ClientAttributeWorkflowConditionFactory implements WorkflowConditionProviderFactory<ClientAttributeWorkflowConditionProvider> {

    public static final String ID = "has-client-attribute";

    @Override
    public ClientAttributeWorkflowConditionProvider create(KeycloakSession session, String keyValuePair) {
        return new ClientAttributeWorkflowConditionProvider(session, keyValuePair);
    }

    @Override
    public String getId() {
        return ID;
    }

}
