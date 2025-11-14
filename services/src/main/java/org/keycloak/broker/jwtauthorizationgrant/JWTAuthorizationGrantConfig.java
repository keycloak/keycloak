package org.keycloak.broker.jwtauthorizationgrant;


import java.util.Map;

import static org.keycloak.broker.oidc.OIDCIdentityProviderConfig.JWKS_URL;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.ISSUER;

public interface JWTAuthorizationGrantConfig {

    public static final String JWT_AUTHORIZATION_GRANT_ENABLED = "jwtAuthorizationGrantEnabled";

    public static final String JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED = "jwtAuthorizationGrantAssertionReuseAllowed";

    public static final String JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION = "jwtAuthorizationGrantMaxAllowedAssertionExpiration";

    public static final String JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG = "jwtAuthorizationGrantAssertionSignatureAlg";

    public static final String JWT_AUTHORIZATION_GRANT_ALLOWED_CLOCK_SKEW = "jwtAuthorizationGrantAllowedClockSkew";

    Map<String, String> getConfig();

    default boolean getJWTAuthorizationGrantEnabled() {
        return Boolean.parseBoolean(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_ENABLED, "false"));
    }

    default boolean getJWTAuthorizationGrantAssertionReuseAllowed() {
        return Boolean.parseBoolean(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED, "false"));
    }

    default int getJWTAuthorizationGrantMaxAllowedAssertionExpiration() {
        return Integer.parseInt(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION, "300"));
    }

    default String getJWTAuthorizationGrantAssertionSignatureAlg() {
        return getConfig().get(JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG);
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

    default String getIssuer() {
        return getConfig().get(ISSUER);
    }

    default String getJwksUrl() {
        return getConfig().get(JWKS_URL);
    }

    String getInternalId();
}
