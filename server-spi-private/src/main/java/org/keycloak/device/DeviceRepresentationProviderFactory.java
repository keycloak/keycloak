package org.keycloak.device;

import java.util.Set;

import org.keycloak.Config;
import org.keycloak.cache.LocalCacheProvider;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

public interface DeviceRepresentationProviderFactory extends ProviderFactory<DeviceRepresentationProvider> {

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    default void close() {
    }

    @Override
    default Set<Class<? extends Provider>> dependsOn() {
        return Set.of(LocalCacheProvider.class);
    }
}
