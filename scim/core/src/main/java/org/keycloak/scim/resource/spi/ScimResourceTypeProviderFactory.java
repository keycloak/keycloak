package org.keycloak.scim.resource.spi;

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderFactory;

public interface ScimResourceTypeProviderFactory<P extends ScimResourceTypeProvider<?>> extends ProviderFactory<P>, EnvironmentDependentProviderFactory {

    @Override
    default boolean isSupported(Scope config) {
        return Profile.isFeatureEnabled(Feature.SCIM_API);
    }
}
