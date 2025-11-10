package org.keycloak.broker.jwtauthorizationgrant;



public interface JWTAuthorizationGrantConfig {

    public static final String JWT_AUTHORIZATION_GRANT_ENABLED = "jwtAuthorizationGrantEnabled";

    public static final String JWT_AUTHORIZATION_GRANT_ASSERTION_REUSE_ALLOWED = "jwtAuthorizationGrantAssertionReuseAllowed";

    public static final String JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION = "jwtAuthorizationGrantMaxAllowedAssertionExpiration";

    public static final String JWT_AUTHORIZATION_GRANT_ASSERTION_SIGNATURE_ALG = "jwtAuthorizationGrantAssertionSignatureAlg";

    public static final String JWT_AUTHORIZATION_GRANT_ALLOWED_CLOCK_SKEW = "jwtAuthorizationGrantAllowedClockSkew";

    boolean getJWTAuthorizationGrantEnabled();

    boolean getJWTAuthorizationGrantAssertionReuseAllowed();

    int getJWTAuthorizationGrantMaxAllowedAssertionExpiration();

    String getJWTAuthorizationGrantAssertionSignatureAlg();

    int getJWTAuthorizationGrantAllowedClockSkewAllowedClockSkew();

    String getIssuer();

    String getJwksUrl();

    String getInternalId();
}
