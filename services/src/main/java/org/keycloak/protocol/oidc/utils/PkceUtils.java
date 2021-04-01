package org.keycloak.protocol.oidc.utils;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PkceUtils {

    public static String generateCodeVerifier() {
        return Base64Url.encode(KeycloakModelUtils.generateSecret(64));
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
}
