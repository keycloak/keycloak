package org.keycloak.models.policy;

import java.util.List;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface ResourcePolicyConditionProviderFactory<P extends ResourcePolicyConditionProvider> extends ProviderFactory<P>, EnvironmentDependentProviderFactory {

    P create(KeycloakSession session, Map<String, List<String>> config);

    @Override
    default P create(KeycloakSession session) {
        throw new IllegalStateException("Use create(KeycloakSession session, MultivaluedHashMap<String, String> config) instead.");
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RESOURCE_LIFECYCLE);
    }
}
