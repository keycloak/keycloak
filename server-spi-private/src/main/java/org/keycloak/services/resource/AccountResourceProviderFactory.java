package org.keycloak.services.resource;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

/**
 * <p>A factory that creates {@link AccountResourceProvider} instances.
 */
public interface AccountResourceProviderFactory extends ProviderFactory<AccountResourceProvider>, EnvironmentDependentProviderFactory {

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.ACCOUNT_V3);
    }
}
