package org.keycloak;

import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RSATokenVerifier {
    public static AccessToken verifyToken(String tokenString, PublicKey realmKey, String realmUrl) throws VerificationException {
        return verifyToken(tokenString, realmKey, realmUrl, true, true);
    }

    public static AccessToken verifyToken(String tokenString, PublicKey realmKey, String realmUrl, boolean checkActive, boolean checkTokenType) throws VerificationException {
        JWSInput input = null;
        try {
            input = new JWSInput(tokenString);
        } catch (JWSInputException e) {
            throw new VerificationException("Couldn't parse token", e);
        }
        if (!isPublicKeyValid(input, realmKey)) throw new VerificationException("Invalid token signature.");

        AccessToken token;
        try {
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new VerificationException("Couldn't parse token signature", e);
        }
        String user = token.getSubject();
        if (user == null) {
            throw new VerificationException("Token user was null.");
        }
        if (realmUrl == null) {
            throw new VerificationException("Realm URL is null. Make sure to add auth-server-url to the configuration of your adapter!");
        }
        if (!realmUrl.equals(token.getIssuer())) {
            throw new VerificationException("Token audience doesn't match domain. Token issuer is " + token.getIssuer() + ", but URL from configuration is " + realmUrl);

        }

        if (checkTokenType) {
            String type = token.getType();
            if (type == null || !type.equalsIgnoreCase(TokenUtil.TOKEN_TYPE_BEARER)) {
                throw new VerificationException("Token type is incorrect. Expected '" + TokenUtil.TOKEN_TYPE_BEARER + "' but was '" + type + "'");
            }
        }
        if (checkActive && !token.isActive()) {
            throw new VerificationException("Token is not active.");
        }

        return token;
    }

    private static boolean isPublicKeyValid(JWSInput input, PublicKey realmKey) throws VerificationException {
        try {
            return RSAProvider.verify(input, realmKey);
        } catch (Exception e) {
            throw new VerificationException("Token signature not validated.", e);
        }
    }
}
