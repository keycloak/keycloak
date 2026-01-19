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
    public static final String CLAIM_NAME_CNF = "cnf";
    public static final String CLAIM_NAME_JWK = "jwk";
    public static final String CLAIM_NAME_SUB = "sub";
    public static final String CLAIM_NAME_VC = "vc";
    public static final String CLAIM_NAME_VCT = "vct";

    // The JWT identifier uniquely identifies a SD_JWT credential
    // It is useful for:
    //   * replay protection of the SD-JWT
    //   * introspection caches
    //   * deduplication
    //   * credential revocation tracking (optional)
    public static final String CLAIM_NAME_JTI = "jti";

    // The credential subject identifier
    //   * A stable identifier for the VC subject
    //   * Would in most cases be the subject's DID
    //   * Can be mapped to user attributes
    public static final String CLAIM_NAME_SUBJECT_ID = "id";

    public static final String KEYBINDING_JWT_TYP = "kb+jwt";

    public static final String SD_HASH_DEFAULT_ALGORITHM = "sha-256";
    public static final int SD_JWT_KEY_BINDING_DEFAULT_ALLOWED_MAX_AGE = 5 * 60; // 5 minutes
    public static final int SD_JWT_DEFAULT_CLOCK_SKEW_SECONDS = 10;
    /**
     * JWT VC issuer endpoint {@see https://datatracker.ietf.org/doc/html/draft-ietf-oauth-sd-jwt-vc-13#section-5}
     */
    public static final String JWT_VC_ISSUER_END_POINT = "/.well-known/jwt-vc-issuer";

    /**
     * https://www.w3.org/TR/2022/REC-vc-data-model-20220303/#credential-subject
     */
    public static final String CREDENTIAL_SUBJECT = "credentialSubject";

    /**
     * https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-G.6.3
     */
    public static final String SIGNED_METADATA_JWT_TYPE = "openidvci-issuer-metadata+jwt";

    // --- Endpoints/Well-Known ---
    public static final String WELL_KNOWN_OPENID_CREDENTIAL_ISSUER = "openid-credential-issuer";
    public static final String RESPONSE_TYPE_IMG_PNG = "image/png";
    public static final String CREDENTIAL_OFFER_URI_CODE_SCOPE = "credential-offer";

    private OID4VCConstants() {
    }

    /**
     * from the OID4VCI specification:
     *
     * <pre>
     *  Appendix D.2. Attack Potential Resistance
     *
     *  This specification defines the following values for key_storage and user_authentication:
     *  iso_18045_high: It MUST be used when key storage or user authentication is resistant to attack with attack
     *  potential "High", equivalent to VAN.5 according to [ISO.18045].
     *  iso_18045_moderate: It MUST be used when key storage or user authentication is resistant to attack with attack
     *  potential "Moderate", equivalent to VAN.4 according to [ISO.18045]. iso_18045_enhanced-basic: It MUST be used
     *  when key storage or user authentication is resistant to attack with attack potential "Enhanced-Basic",
     *  equivalent to VAN.3 according to [ISO.18045]. iso_18045_basic: It MUST be used when key storage or user
     *  authentication is resistant to attack with attack potential "Basic", equivalent to VAN.2 according to
     *  [ISO.18045]. Specifications that extend this list MUST choose collision-resistant values.
     * </pre>
     * <p>
     * this tells us that the KeyAttestationResistance is potentially extendable, and must therefore be handled with
     * strings
     */
    public static class KeyAttestationResistanceLevels {

        public static final String HIGH = "iso_18045_high"; // VAN.5

        public static final String MODERATE = "iso_18045_moderate"; // VAN.4

        public static final String ENHANCED_BASIC = "iso_18045_enhanced-basic"; // VAN.3

        public static final String BASIC = "iso_18045_basic"; // VAN.2
    }
}
