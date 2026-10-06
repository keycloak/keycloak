package org.keycloak.protocol.oidc.verifier;

import jakarta.ws.rs.core.UriInfo;

import org.keycloak.TokenVerifier;
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

    @Override
    default void close() {
    }

    // Add more fields if needed...
    record TokenVerifierProviderContext(TokenVerifier<AccessToken> accessTokenVerifier, KeycloakSession session, RealmModel realm, UriInfo uriInfo) {
    }
}
