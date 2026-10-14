package org.keycloak.protocol.oidc.verifier;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public interface TokenVerifierProviderFactory extends ProviderFactory<TokenVerifierProvider> {

    @Override
    default void close() {

    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    default void init(Config.Scope config) {

    }
}
