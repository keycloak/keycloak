package org.keycloak.services.trustchain;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class OpenIdFederationTrustChainProcessorFactory implements TrustChainProcessorFactory {

    public static final String PROVIDER_ID = "openid-federation";

    @Override
    public TrustChainProcessor create(KeycloakSession session) {
        return new OpenIdFederationTrustChainProcessor(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}

