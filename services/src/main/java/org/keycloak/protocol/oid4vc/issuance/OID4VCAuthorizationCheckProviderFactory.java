package org.keycloak.protocol.oid4vc.issuance;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointCheckProvider;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpointCheckProviderFactory;

public class OID4VCAuthorizationCheckProviderFactory implements AuthorizationEndpointCheckProviderFactory {

    @Override
    public AuthorizationEndpointCheckProvider create(KeycloakSession session) {
        return new OID4VCAuthorizationCheckProvider(session);
    }

    @Override
    public String getId() {
        return "oid4vci-auth-checker";
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
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }
}
