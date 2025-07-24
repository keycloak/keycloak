package org.keycloak.protocol.oidc.federation;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.wellknown.WellKnownProvider;
import org.keycloak.wellknown.WellKnownProviderFactory;

public class OpenIdFederationWellKnownProviderFactory implements WellKnownProviderFactory {

    public static final String PROVIDER_ID = "openid-federation";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new OpenIdFederationWellKnownProvider(session);
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

