package org.keycloak.cache;

import java.util.Set;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

public interface AlternativeLookupProviderFactory extends ProviderFactory<AlternativeLookupProvider> {
    @Override
    default Set<Class<? extends Provider>> dependsOn() {
        return Set.of(LocalCacheProvider.class);
    }
}
