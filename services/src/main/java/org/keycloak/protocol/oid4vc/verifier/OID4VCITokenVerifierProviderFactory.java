package org.keycloak.protocol.oid4vc.verifier;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProvider;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProviderFactory;

public class OID4VCITokenVerifierProviderFactory implements TokenVerifierProviderFactory {

    private static final String PROVIDER_ID = "oid4vcvi";

    @Override
    public TokenVerifierProvider create(KeycloakSession session) {
        return new OID4VCITokenVerifierProvider(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
