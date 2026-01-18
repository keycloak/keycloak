package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

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

    public static WorkflowStepProvider getStepProvider(KeycloakSession session, WorkflowStep step) {
        RealmModel realm = session.getContext().getRealm();
        return getStepProviderFactory(session, step).create(session, realm.getComponent(step.getId()));
    }

    public static WorkflowStepProviderFactory<WorkflowStepProvider> getStepProviderFactory(KeycloakSession session, WorkflowStep step) {
        WorkflowStepProviderFactory<WorkflowStepProvider> factory = (WorkflowStepProviderFactory<WorkflowStepProvider>) session
                .getKeycloakSessionFactory().getProviderFactory(WorkflowStepProvider.class, step.getProviderId());

        if (factory == null) {
            throw new WorkflowInvalidStateException("Step not found: " + step.getProviderId());
        }

        return factory;
    }
}
