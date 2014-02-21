package org.keycloak;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.AccessToken;

import java.io.IOException;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RSATokenVerifier {

    public static AccessToken verifyToken(String tokenString, PublicKey realmKey, String realm) throws VerificationException {
        JWSInput input = new JWSInput(tokenString);
        boolean verified = false;
        try {
            verified = RSAProvider.verify(input, realmKey);
        } catch (Exception ignore) {

        }
        if (!verified) throw new VerificationException("Token signature not validated");

        AccessToken token = null;
        try {
            token = input.readJsonContent(AccessToken.class);
        } catch (IOException e) {
            throw new VerificationException(e);
        }
        if (!token.isActive()) {
            throw new VerificationException("Token is not active.");
        }
        String user = token.getSubject();
        if (user == null) {
            throw new VerificationException("Token user was null");
        }
        if (!realm.equals(token.getAudience())) {
            throw new VerificationException("Token audience doesn't match domain");

        }
        return token;
    }
}
