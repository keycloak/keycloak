package org.keycloak.encoding;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public interface ResourceEncodingProviderFactory extends ProviderFactory<ResourceEncodingProvider> {

    boolean encodeContentType(String contentType);

    /**
     * Clears any cached encoded resources. Implementations that do not cache encoded resources can leave this as a
     * no-op.
     */
    default void clearCache() {
    }

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    default void close() {
    }

}
