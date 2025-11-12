package org.keycloak;

/**
 * Constants related to the OID4VC and related specifications (like sd-jwt)
 */
public class OID4VCConstants {

    // Sd-JWT constants
    public static final String SDJWT_DELIMITER = "~";
    public static final String SD_HASH = "sd_hash";
    /**
     * SD-JWT-Credentials {@see https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html}
     */
    public static final String SD_JWT_VC_FORMAT = "dc+sd-jwt";
    public static final String CLAIM_NAME_SD = "_sd";
    public static final String CLAIM_NAME_SD_HASH_ALGORITHM = "_sd_alg";
    public static final String CLAIM_NAME_SD_UNDISCLOSED_ARRAY = "...";

    public static final String CLAIM_NAME_IAT = "iat";
    public static final String CLAIM_NAME_EXP = "exp";
    public static final String CLAIM_NAME_NBF = "nbf";
    public static final String CLAIM_NAME_ISSUER = "iss";

    public static final String SD_HASH_DEFAULT_ALGORITHM = "sha-256";
    public static final int SD_JWT_KEY_BINDING_DEFAULT_ALLOWED_MAX_AGE = 5 * 60; // 5 minutes
    public static final int SD_JWT_DEFAULT_CLOCK_SKEW_SECONDS = 10;
    /**
     * JWT VC issuer endpoint {@see https://datatracker.ietf.org/doc/html/draft-ietf-oauth-sd-jwt-vc-13#section-5}
     */
    public static final String JWT_VC_ISSUER_END_POINT = "/.well-known/jwt-vc-issuer";
}
