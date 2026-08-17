package org.keycloak.protocol.oid4vc.refresh;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.protocol.oidc.refresh.RefreshTokenProvider;
import org.keycloak.protocol.oidc.refresh.RefreshTokenProviderFactory;

public class OID4VCIRefreshTokenProviderFactory implements RefreshTokenProviderFactory, OID4VCEnvironmentProviderFactory {

    public static final String PROVIDER_ID = "oid4vci";

    @Override
    public RefreshTokenProvider create(KeycloakSession session) {
        return new OID4VCIRefreshTokenProvider(session);
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

    @Override
    public int order() {
        // Bigger priority than default refresh token provider
        return 10;
    }
}
