package org.keycloak.requestfilter;

import org.keycloak.Config;
import org.keycloak.provider.ProviderFactory;

public interface RequestFilterProviderFactory extends ProviderFactory<RequestFilterProvider> {
    @Override
    default void init(Config.Scope config) {

    }
}
