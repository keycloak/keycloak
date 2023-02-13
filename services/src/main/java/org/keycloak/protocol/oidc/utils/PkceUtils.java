package org.keycloak.protocol.oidc.utils;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.events.Errors;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.events.EventBuilder;
import org.keycloak.services.resources.Cors;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;

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
    public static String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes(StandardCharsets.ISO_8859_1));
        byte[] digestBytes = md.digest();
        return Base64Url.encode(digestBytes);
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
            logger.warnf("PKCE code verifier not specified, authUserId = %s, authUsername = %s", authUserId, authUsername);
            event.error(Errors.CODE_VERIFIER_MISSING);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE code verifier not specified", Response.Status.BAD_REQUEST);
        }
        verifyCodeVerifier(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
    }

    public static void checkParamsForPkceNotEnforcedClient(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername, EventBuilder event, Cors cors) {
        if (codeChallenge != null && codeVerifier == null) {
            logger.warnf("PKCE code verifier not specified, authUserId = %s, authUsername = %s", authUserId, authUsername);
            event.error(Errors.CODE_VERIFIER_MISSING);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE code verifier not specified", Response.Status.BAD_REQUEST);
        }

        if (codeChallenge != null) {
            verifyCodeVerifier(codeVerifier, codeChallenge, codeChallengeMethod, authUserId, authUsername, event, cors);
        }
    }

    public static void verifyCodeVerifier(String codeVerifier, String codeChallenge, String codeChallengeMethod, String authUserId, String authUsername, EventBuilder event, Cors cors) {
        // check whether code verifier is formatted along with the PKCE specification

        if (!isValidPkceCodeVerifier(codeVerifier)) {
            logger.infof("PKCE invalid code verifier");
            event.error(Errors.INVALID_CODE_VERIFIER);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE invalid code verifier", Response.Status.BAD_REQUEST);
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
            logger.infof("PKCE code verification failed, not supported algorithm specified");
            event.error(Errors.PKCE_VERIFICATION_FAILED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE code verification failed, not supported algorithm specified", Response.Status.BAD_REQUEST);
        }
        if (!codeChallenge.equals(codeVerifierEncoded)) {
            logger.warnf("PKCE verification failed. authUserId = %s, authUsername = %s", authUserId, authUsername);
            event.error(Errors.PKCE_VERIFICATION_FAILED);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_GRANT, "PKCE verification failed", Response.Status.BAD_REQUEST);
        } else {
            logger.debugf("PKCE verification success. codeVerifierEncoded = %s, codeChallenge = %s", codeVerifierEncoded, codeChallenge);
        }
    }

    private static boolean isValidPkceCodeVerifier(String codeVerifier) {
        if (codeVerifier.length() < OIDCLoginProtocol.PKCE_CODE_VERIFIER_MIN_LENGTH) {
            logger.infof(" Error: PKCE codeVerifier length under lower limit , codeVerifier = %s", codeVerifier);
            return false;
        }
        if (codeVerifier.length() > OIDCLoginProtocol.PKCE_CODE_VERIFIER_MAX_LENGTH) {
            logger.infof(" Error: PKCE codeVerifier length over upper limit , codeVerifier = %s", codeVerifier);
            return false;
        }
        Matcher m = VALID_CODE_VERIFIER_PATTERN.matcher(codeVerifier);
        return m.matches();
    }
}
