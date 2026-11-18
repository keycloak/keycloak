package org.keycloak.protocol.oidc.verifier;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.representations.AccessToken;

/**
 * Provider allows to inject custom token verifiers, which will be used during verifications of Keycloak tokens (EG. during admin REST or account REST calls)
 */
public interface TokenVerifierProvider extends Provider {

    /**
     * Adds restrictions shared by endpoint-defined verification, OAuth access token validation, and resource access.
     *
     * @param context for the token verification
     */
    void additionalAccessTokenVerifications(TokenVerifierProviderContext context);

    /** Adds checks on OAuth access token claims, including when the token is presented to a resource. */
    default void addAccessTokenClaimChecks(TokenVerifierProviderContext context) {
    }

    /** Verifies sender binding against the request. Resource access includes the token presentation in the context. */
    default void verifyTokenBinding(AccessToken token, TokenVerificationContext context) throws VerificationException {
    }

    /** Verifies binding after token exchange authorizes the client. Override when exchange uses different proof semantics. */
    default void verifyTokenExchangeBinding(AccessToken token, TokenVerificationContext context) throws VerificationException {
        verifyTokenBinding(token, context);
    }

    @Override
    default void close() {
    }

    record TokenVerifierProviderContext(TokenVerifier<AccessToken> accessTokenVerifier, TokenVerificationContext verificationContext) {

        public TokenVerifierProviderContext(TokenVerifier<AccessToken> accessTokenVerifier, KeycloakSession session, RealmModel realm, UriInfo uriInfo) {
            this(accessTokenVerifier, new TokenVerificationContext(session, realm, uriInfo, null, null, null));
        }

        public KeycloakSession session() {
            return verificationContext.session();
        }

        public RealmModel realm() {
            return verificationContext.realm();
        }

        public UriInfo uriInfo() {
            return verificationContext.uriInfo();
        }
    }
}
