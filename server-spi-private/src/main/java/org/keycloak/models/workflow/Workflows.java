package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public final class Workflows {

    public static WorkflowConditionProvider getConditionProvider(KeycloakSession session, String name, String expression) {
        return getConditionProviderFactory(session, name).create(session, expression);
    }

    private static WorkflowConditionProviderFactory<WorkflowConditionProvider> getConditionProviderFactory(KeycloakSession session, String providerId) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        WorkflowConditionProviderFactory<WorkflowConditionProvider> providerFactory = (WorkflowConditionProviderFactory<WorkflowConditionProvider>) sessionFactory.getProviderFactory(WorkflowConditionProvider.class, providerId);

        if (providerFactory == null) {
            throw new WorkflowInvalidStateException("Could not find condition provider: " + providerId);
        }

        return providerFactory;
    }

}
