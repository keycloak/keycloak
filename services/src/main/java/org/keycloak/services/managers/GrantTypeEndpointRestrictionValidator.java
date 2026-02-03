package org.keycloak.services.managers;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.DefaultTokenContextEncoderProvider;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.protocol.oidc.grants.OAuth2GrantType;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;

import org.jboss.logging.Logger;

/**
 * Validates that tokens are only used on endpoints allowed by their grant type.
 * This ensures Pre-Authorized Code tokens are restricted to the credential endpoint,
 * and other grant types only access their intended endpoints.
 */
public class GrantTypeEndpointRestrictionValidator implements TokenVerifier.Predicate<AccessToken> {
    private static final Logger logger = Logger.getLogger(GrantTypeEndpointRestrictionValidator.class);

    private final KeycloakSession session;

    private GrantTypeEndpointRestrictionValidator(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Creates a TokenVerifier.Predicate for grant type endpoint restriction validation.
     * Can be used with TokenVerifier.withChecks() for inline verification.
     *
     * @param session The Keycloak session
     * @return A predicate that validates grant type restrictions
     */
    public static TokenVerifier.Predicate<AccessToken> check(KeycloakSession session) {
        return new GrantTypeEndpointRestrictionValidator(session);
    }

    @Override
    public boolean test(AccessToken token) throws VerificationException {
        validate(token);
        return true;
    }

    /**
     * Validates that the token is allowed for the current endpoint based on its grant type.
     *
     * @param token The access token to validate
     * @throws VerificationException  if token validation fails
     * @throws ErrorResponseException if server configuration is broken
     */
    private void validate(AccessToken token) throws VerificationException {
        try {
            // Get the grant type from the token
            String grantType = recoverGrantType(token);

            // If no specific grant type, allow the token for backward compatibility
            if (grantType == null) {
                return;
            }

            // Get the grant type provider to verify endpoint restrictions
            OAuth2GrantType grantTypeProvider = session.getProvider(OAuth2GrantType.class, grantType);
            if (grantTypeProvider == null) {
                // This is a server configuration error - grant type provider not registered
                logger.errorf("Grant type restriction provider not available for: %s - server misconfiguration", grantType);
                throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR,
                        "Internal error: grant type restriction provider not available", Response.Status.INTERNAL_SERVER_ERROR);
            }

            if (!grantTypeProvider.isTokenAllowed(session, token)) {
                throw new VerificationException("Token is not allowed for this endpoint. Grant type: " + grantType);
            }
        } catch (ErrorResponseException | VerificationException e) {
            throw e;
        } catch (Exception e) {
            logger.errorf(e, "Error checking grant type restriction");
            throw new VerificationException("Error verifying grant type restrictions: " + e.getMessage(), e);
        }
    }

    /**
     * Recover the grant type from the token context.
     * Handles legacy tokens and various token formats gracefully.
     *
     * @param token The access token to extract grant type from
     * @return The grant type, or null if no specific grant type is assigned
     * @throws VerificationException  if token context is invalid
     * @throws ErrorResponseException if server configuration is broken
     */
    private String recoverGrantType(AccessToken token) throws VerificationException {
        TokenContextEncoderProvider encoder = session.getProvider(TokenContextEncoderProvider.class);
        if (encoder == null) {
            logger.error("Token context encoder provider not available - server misconfiguration");
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR,
                    "Internal error: token context encoder not available", Response.Status.INTERNAL_SERVER_ERROR);
        }

        AccessTokenContext tokenContext;
        try {
            tokenContext = encoder.getTokenContextFromTokenId(token.getId());
        } catch (IllegalArgumentException e) {
            // Token ID format is invalid or unknown - treat as legacy token
            logger.debugf("Cannot decode token context from token ID, treating as legacy token: %s", e.getMessage());
            return null;
        }

        if (tokenContext == null) {
            throw new VerificationException("Invalid token context");
        }

        String grantType = tokenContext.getGrantType();
        if (grantType == null || grantType.isEmpty() || DefaultTokenContextEncoderProvider.UNKNOWN.equals(grantType)) {
            // Standard Keycloak token without specific grant-type context.
            // We allow these to maintain backward compatibility with standard OIDC flows.
            return null;
        }

        return grantType;
    }
}
