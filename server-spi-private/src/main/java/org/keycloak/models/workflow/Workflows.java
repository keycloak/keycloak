package org.keycloak.models.workflow;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

public final class Workflows {

    public static WorkflowConditionProvider getConditionProvider(KeycloakSession session, String name, String configParameter) {
        return getConditionProviderFactory(session, name).create(session, configParameter);
    }

    public static WorkflowConditionProviderFactory<WorkflowConditionProvider> getConditionProviderFactory(KeycloakSession session, String providerId) {
        return getProviderFactory(session, WorkflowConditionProvider.class, providerId);
    }

    public static WorkflowEventProvider getEventProvider(KeycloakSession session, String name, String configParameter) {
        return getEventProviderFactory(session, name).create(session, configParameter);
    }

    public static WorkflowEventProviderFactory<WorkflowEventProvider> getEventProviderFactory(KeycloakSession session, String providerId) {
        return getProviderFactory(session, WorkflowEventProvider.class, providerId);
    }

    public static WorkflowStepProvider getStepProvider(KeycloakSession session, WorkflowStep step) {
        RealmModel realm = session.getContext().getRealm();
        return getStepProviderFactory(session, step).create(session, realm.getComponent(step.getId()));
    }

    public static WorkflowStepProviderFactory<WorkflowStepProvider> getStepProviderFactory(KeycloakSession session, WorkflowStep step) {
        return getProviderFactory(session, WorkflowStepProvider.class, step.getProviderId());
    }

    private static <P extends Provider, F> F getProviderFactory(KeycloakSession session, Class<P> providerClass, String providerId) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        @SuppressWarnings("unchecked")
        F providerFactory = (F) sessionFactory.getProviderFactory(providerClass, providerId);

        if (providerFactory == null) {
            throw new WorkflowInvalidStateException("Could not find provider factory with id: " + providerId);
        }
        return providerFactory;
    }
}
