package org.keycloak.encoding;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public interface ResourceEncodingProviderFactory extends ProviderFactory<ResourceEncodingProvider> {

    boolean encodeContentType(String contentType);

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
