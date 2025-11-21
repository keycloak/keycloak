package org.keycloak.models.workflow;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface WorkflowConditionProviderFactory<P extends WorkflowConditionProvider> extends ProviderFactory<P>, EnvironmentDependentProviderFactory {

    P create(KeycloakSession session, String configParameter);

    @Override
    default P create(KeycloakSession session) {
        throw new IllegalStateException("Use create(KeycloakSession session, MultivaluedHashMap<String, String> config) instead.");
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.WORKFLOWS);
    }
}
