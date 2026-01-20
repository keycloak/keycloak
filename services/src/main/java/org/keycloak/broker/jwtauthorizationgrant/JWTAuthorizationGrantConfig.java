package org.keycloak.broker.jwtauthorizationgrant;


import java.util.Map;

import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.JWKS_URL;
import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.USE_JWKS_URL;
import static org.keycloak.models.IdentityProviderModel.ISSUER;

public interface JWTAuthorizationGrantConfig {

    String JWT_AUTHORIZATION_GRANT_ENABLED = "jwtAuthorizationGrantEnabled";

    String JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED = "jwtAuthorizationGrantAssertionReuseAllowed";

    String JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION = "jwtAuthorizationGrantMaxAllowedAssertionExpiration";

    String JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG = "jwtAuthorizationGrantAssertionSignatureAlg";

    String JWT_AUTHORIZATION_GRANT_LIMIT_ACCESS_TOKEN_EXP = "jwtAuthorizationGrantLimitAccessTokenExp";

    String JWT_AUTHORIZATION_GRANT_ALLOWED_CLOCK_SKEW = "jwtAuthorizationGrantAllowedClockSkew";

    String PUBLIC_KEY_SIGNATURE_VERIFIER = "publicKeySignatureVerifier";

    String PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID = "publicKeySignatureVerifierKeyId";

    Map<String, String> getConfig();

    default boolean isJWTAuthorizationGrantEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_ENABLED, "false"));
    }

    default void setJWTAuthorizationGrantEnabled(boolean jwtAuthorizationGrantEnableds) {
        getConfig().put(JWT_AUTHORIZATION_GRANT_ENABLED, String.valueOf(jwtAuthorizationGrantEnableds));
    }

    default boolean isJWTAuthorizationGrantAssertionReuseAllowed() {
        return Boolean.parseBoolean(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED, "false"));
    }

    default int getJWTAuthorizationGrantMaxAllowedAssertionExpiration() {
        return Integer.parseInt(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION, "300"));
    }

    default String getJWTAuthorizationGrantAssertionSignatureAlg() {
        return getConfig().get(JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG);
    }

    default boolean isJwtAuthorizationGrantLimitAccessTokenExp() {
        return Boolean.parseBoolean(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_LIMIT_ACCESS_TOKEN_EXP, "false"));
    }

    default int getJWTAuthorizationGrantAllowedClockSkew() {
        String allowedClockSkew = getConfig().get(JWT_AUTHORIZATION_GRANT_ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew == null || allowedClockSkew.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(getConfig().get(JWT_AUTHORIZATION_GRANT_ALLOWED_CLOCK_SKEW));
        } catch (NumberFormatException e) {
            // ignore it and use default
            return 0;
        }
    }

    default String getPublicKeySignatureVerifier() {
        return getConfig().get(PUBLIC_KEY_SIGNATURE_VERIFIER);
    }

    default void setPublicKeySignatureVerifier(String signingCertificate) {
        if (signingCertificate == null) {
            getConfig().remove(PUBLIC_KEY_SIGNATURE_VERIFIER);
        } else {
            getConfig().put(PUBLIC_KEY_SIGNATURE_VERIFIER, signingCertificate);
        }
    }

    default String getPublicKeySignatureVerifierKeyId() {
        return getConfig().get(PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID);
    }

    default void setPublicKeySignatureVerifierKeyId(String publicKeySignatureVerifierKeyId) {
        if (publicKeySignatureVerifierKeyId == null) {
            getConfig().remove(PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID);
        } else {
            getConfig().put(PUBLIC_KEY_SIGNATURE_VERIFIER_KEY_ID, publicKeySignatureVerifierKeyId);
        }
    }

    default boolean isUseJwksUrl() {
        return Boolean.parseBoolean(getConfig().get(USE_JWKS_URL));
    }

    default void setUseJwksUrl(boolean useJwksUrl) {
        getConfig().put(USE_JWKS_URL, String.valueOf(useJwksUrl));
    }

    default String getIssuer() {
        return getConfig().get(ISSUER);
    }

    default void setIssuer(String issuer) {
        getConfig().put(ISSUER, issuer);
    }

    default String getJwksUrl() {
        return getConfig().get(JWKS_URL);
    }

    default void setJwksUrl(String jwksUrl) {
        getConfig().put(JWKS_URL, jwksUrl);
    }

    String getInternalId();

    String getAlias();
}
