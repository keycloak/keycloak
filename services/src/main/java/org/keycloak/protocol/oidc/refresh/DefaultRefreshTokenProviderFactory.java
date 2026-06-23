package org.keycloak.protocol.oidc.refresh;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultRefreshTokenProviderFactory implements RefreshTokenProviderFactory {

    private static final String PROVIDER_ID = "default";

    @Override
    public RefreshTokenProvider create(KeycloakSession session) {
        return new DefaultRefreshTokenProvider(session);
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
