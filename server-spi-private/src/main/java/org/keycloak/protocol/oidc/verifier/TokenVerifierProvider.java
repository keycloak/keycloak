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
     * Additional verifications of access token provided by this provider
     *
     * @param context for the token verification
     */
    void additionalAccessTokenVerifications(TokenVerifierProviderContext context);

    /** Verifies sender binding after token validation and, for token exchange, client authorization. */
    default void verifyTokenBinding(AccessToken token, TokenVerificationContext context) throws VerificationException {
    }

    @Override
    default void close() {
    }

    record TokenVerifierProviderContext(TokenVerifier<AccessToken> accessTokenVerifier, TokenVerificationContext verificationContext) {

        public TokenVerifierProviderContext(TokenVerifier<AccessToken> accessTokenVerifier, KeycloakSession session, RealmModel realm, UriInfo uriInfo) {
            this(accessTokenVerifier, new TokenVerificationContext(session, realm, uriInfo, null, null, null, TokenVerificationContext.VerificationMode.VALIDATION_ONLY));
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
