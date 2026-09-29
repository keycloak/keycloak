package org.keycloak.protocol.oidc.verifier;

public class TokenVerifierProviderManager {

    /**
     * Additional verifications of access token provided by {@link TokenVerifierProvider}
     *
     * @param context for the token verification
     */
    public void additionalAccessTokenVerifications(TokenVerifierProvider.TokenVerifierProviderContext context) {
        context.session().getAllProviders(TokenVerifierProvider.class)
                .forEach(tokenVerifierProvider -> tokenVerifierProvider.additionalAccessTokenVerifications(context));
    }
}
