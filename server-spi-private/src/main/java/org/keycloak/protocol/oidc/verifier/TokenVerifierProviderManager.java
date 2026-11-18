package org.keycloak.protocol.oidc.verifier;

import java.util.List;
import java.util.Set;

import org.keycloak.TokenVerifier;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.verifier.TokenVerifierProvider.TokenVerifierProviderContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.TokenUtil;

public class TokenVerifierProviderManager {

    /** Applies SPI restrictions while preserving the token types configured by the endpoint. */
    public void addEndpointChecks(TokenVerifier<AccessToken> verifier, TokenVerificationContext context) {
        additionalAccessTokenVerifications(new TokenVerifierProviderContext(verifier, context));
    }

    /** Original SPI entry point for endpoint-defined checks. */
    public void additionalAccessTokenVerifications(TokenVerifierProviderContext checks) {
        checks.session().getAllProviders(TokenVerifierProvider.class).forEach(provider -> provider.additionalAccessTokenVerifications(checks));
    }

    public void addAccessTokenChecks(TokenVerifier<AccessToken> verifier, TokenVerificationContext context) {
        addAccessTokenChecks(verifier, context, context.session().getAllProviders(TokenVerifierProvider.class));
    }

    public void addResourceAccessChecks(TokenVerifier<AccessToken> verifier, TokenVerificationContext context) {
        Set<TokenVerifierProvider> providers = context.session().getAllProviders(TokenVerifierProvider.class);
        addAccessTokenChecks(verifier, context, providers);
        verifier.withChecks(token -> {
            for (TokenVerifierProvider provider : providers) {
                provider.verifyTokenBinding(token, context);
            }
            return true;
        });
    }

    private void addAccessTokenChecks(TokenVerifier<AccessToken> verifier, TokenVerificationContext context, Set<TokenVerifierProvider> providers) {
        // Set the default before extensions run so they can narrow the accepted token types.
        verifier.tokenType(Profile.isFeatureEnabled(Profile.Feature.DPOP)
                ? List.of(TokenUtil.TOKEN_TYPE_BEARER, TokenUtil.TOKEN_TYPE_DPOP)
                : List.of(TokenUtil.TOKEN_TYPE_BEARER));
        TokenVerifierProviderContext checks = new TokenVerifierProviderContext(verifier, context);
        for (TokenVerifierProvider provider : providers) {
            provider.additionalAccessTokenVerifications(checks);
            provider.addAccessTokenClaimChecks(checks);
        }
    }

    /** The standard exchange grant calls this after authorizing the requesting client. */
    public void verifyTokenExchangeBinding(KeycloakSession session, AccessToken token) throws VerificationException {
        TokenVerificationContext context = TokenVerificationContext.fromSession(session);
        for (TokenVerifierProvider provider : session.getAllProviders(TokenVerifierProvider.class)) {
            provider.verifyTokenExchangeBinding(token, context);
        }
    }
}
