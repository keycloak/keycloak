package org.keycloak.protocol.oid4vc.issuance.credentialoffer.preauth;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public interface PreAuthCodeHandlerFactory extends ProviderFactory<PreAuthCodeHandler> {

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
