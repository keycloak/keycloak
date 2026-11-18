package org.keycloak.protocol.oidc.verifier;

import java.util.Set;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProvider.TokenVerifierProviderContext;
import org.keycloak.representations.AccessToken;

public class TokenVerifierProviderManager {

    /** Adds token restrictions and, for resource authorization, the binding check. */
    public void additionalAccessTokenVerifications(TokenVerifier<AccessToken> verifier, TokenVerificationContext context) {
        additionalAccessTokenVerifications(new TokenVerifierProviderContext(verifier, context));
    }

    public void additionalAccessTokenVerifications(TokenVerifierProviderContext checks) {
        TokenVerificationContext context = checks.verificationContext();
        Set<TokenVerifierProvider> providers = context.session().getAllProviders(TokenVerifierProvider.class);
        providers.forEach(provider -> provider.additionalAccessTokenVerifications(checks));
        if (context.mode().isResourceRequest()) {
            checks.accessTokenVerifier().withChecks(token -> {
                verifyTokenBinding(providers, token, context);
                return true;
            });
        }
    }

    /** The standard exchange grant calls this after authorizing the requesting client. */
    public void verifyTokenExchangeBinding(KeycloakSession session, AccessToken token) throws VerificationException {
        verifyTokenBinding(session.getAllProviders(TokenVerifierProvider.class), token, TokenVerificationContext.forTokenExchange(session, null));
    }

    private void verifyTokenBinding(Set<TokenVerifierProvider> providers, AccessToken token, TokenVerificationContext context) throws VerificationException {
        for (TokenVerifierProvider provider : providers) {
            provider.verifyTokenBinding(token, context);
        }
    }
}
