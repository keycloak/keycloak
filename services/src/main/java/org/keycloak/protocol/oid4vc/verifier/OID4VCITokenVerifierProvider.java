package org.keycloak.protocol.oid4vc.verifier;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProvider;


public class OID4VCITokenVerifierProvider implements TokenVerifierProvider {

    private final KeycloakSession session;

    public OID4VCITokenVerifierProvider(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public void additionalAccessTokenVerifications(TokenVerifierProviderContext context) {
        context.accessTokenVerifier().withChecks(new OID4VCITokenVerifier(context));
    }
}
