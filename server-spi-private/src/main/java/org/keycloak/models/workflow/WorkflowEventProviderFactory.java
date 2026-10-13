package org.keycloak.models.workflow;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface WorkflowEventProviderFactory <P extends WorkflowEventProvider> extends ProviderFactory<P>, EnvironmentDependentProviderFactory {

    P create(KeycloakSession session, String configParameter);

    @Override
    default P create(KeycloakSession session) {
        return create(session, null);
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.WORKFLOWS);
    }

    @Override
    default void init(Config.Scope config) {
        // no-op default
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
        // no-op default
    }

    @Override
    default void close() {
        // no-op default
    }
}
