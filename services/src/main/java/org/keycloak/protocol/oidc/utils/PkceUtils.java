package org.keycloak.protocol.oidc.utils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.HashException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.cors.Cors;

import org.jboss.logging.Logger;

public class PkceUtils {

    private static final Logger logger = Logger.getLogger(PkceUtils.class);

    private static final Pattern VALID_CODE_VERIFIER_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    public static String generateCodeVerifier() {
        return Base64Url.encode(SecretGenerator.getInstance().randomBytes(64));
    }

    public static String encodeCodeChallenge(String codeVerifier, String codeChallengeMethod) {
        try {
            switch (codeChallengeMethod) {
                case OAuth2Constants.PKCE_METHOD_S256:
                    return generateS256CodeChallenge(codeVerifier);
                case OAuth2Constants.PKCE_METHOD_PLAIN:
                    // fall-trhough
                default:
                    return codeVerifier;
            }
        } catch(Exception ex) {
            return null;
        }
    }

    // https://tools.ietf.org/html/rfc7636#section-4.6
    public static String generateS256CodeChallenge(String codeVerifier) throws HashException {
        return HashUtils.sha256UrlEncodedHash(codeVerifier, StandardCharsets.ISO_8859_1);
    }

    public static boolean validateCodeChallenge(String verifier, String codeChallenge, String codeChallengeMethod) {

        try {
            switch (codeChallengeMethod) {
                case OAuth2Constants.PKCE_METHOD_PLAIN:
                    return verifier.equals(codeChallenge);
                case OAuth2Constants.PKCE_METHOD_S256:
                    return generateS256CodeChallenge(verifier).equals(codeChallenge);
                default:
                    return false;
            }
        } catch(Exception ex) {
            return false;
        }
    }

    public static void checkParamsForPkceEnforcedClient(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername, EventBuilder event, Cors cors) {
        // check whether code verifier is specified
        if (codeVerifier == null) {
            String errorMessage = "PKCE code verifier not specified";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.CODE_VERIFIER_MISSING);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }
        verifyCodeVerifier(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
    }

    public static void checkParamsForPkceNotEnforcedClient(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername, EventBuilder event, Cors cors) {
        if (codeChallenge != null && codeVerifier == null) {
            String errorMessage = "PKCE code verifier not specified";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.CODE_VERIFIER_MISSING);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }

        if (codeChallenge == null && codeVerifier != null) {
            String errorMessage = "PKCE code verifier specified but challenge not present in authorization";
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_CODE_VERIFIER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }

        if (codeChallenge != null) {
            verifyCodeVerifier(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        }
    }

    public static void verifyCodeVerifier(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername, EventBuilder event, Cors cors) {
        // check whether code verifier is formatted along with the PKCE specification

        if (!isValidPkceCodeVerifier(codeVerifier)) {
            String errorReason = "Invalid code verifier";
            String errorMessage = "PKCE verification failed: " + errorReason;
            event.detail(Details.REASON, errorReason);
            event.error(Errors.INVALID_CODE_VERIFIER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }

        logger.debugf("PKCE supporting Client, codeVerifier = %s", codeVerifier);
        String codeVerifierEncoded = codeVerifier;
        try {
            // https://tools.ietf.org/html/rfc7636#section-4.2
            // plain or S256
            if (codeChallengeMethod != null && codeChallengeMethod.equals(OAuth2Constants.PKCE_METHOD_S256)) {
                logger.debugf("PKCE codeChallengeMethod = %s", codeChallengeMethod);
                codeVerifierEncoded = PkceUtils.generateS256CodeChallenge(codeVerifier);
            } else {
                logger.debug("PKCE codeChallengeMethod is plain");
                codeVerifierEncoded = codeVerifier;
            }
        } catch (Exception nae) {
            String errorReason = "Unsupported algorithm specified";
            String errorMessage = "PKCE verification failed: " + errorReason;
            event.detail(Details.REASON, errorReason);
            event.error(Errors.PKCE_VERIFICATION_FAILED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        }
        if (!codeChallenge.equals(codeVerifierEncoded)) {
            String errorReason = "Code mismatch";
            String errorMessage = "PKCE verification failed: " + errorReason;
            event.detail(Details.REASON, errorReason);
            event.error(Errors.PKCE_VERIFICATION_FAILED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, errorMessage, Response.Status.BAD_REQUEST);
        } else {
            logger.debugf("PKCE verification success. codeVerifierEncoded = %s, codeChallenge = %s", codeVerifierEncoded, codeChallenge);
        }
    }

    private static boolean isValidPkceCodeVerifier(String codeVerifier) {
        if (codeVerifier.length() < OIDCLoginProtocol.PKCE_CODE_VERIFIER_MIN_LENGTH) {
            logger.debugf(" Error: PKCE codeVerifier length under lower limit , codeVerifier = %s", codeVerifier);
            return false;
        }
        if (codeVerifier.length() > OIDCLoginProtocol.PKCE_CODE_VERIFIER_MAX_LENGTH) {
            logger.debugf(" Error: PKCE codeVerifier length over upper limit , codeVerifier = %s", codeVerifier);
            return false;
        }
        Matcher m = VALID_CODE_VERIFIER_PATTERN.matcher(codeVerifier);
        return m.matches();
    }
}
