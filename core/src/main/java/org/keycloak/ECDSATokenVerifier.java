package org.keycloak;

import java.security.PublicKey;

import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class ECDSATokenVerifier extends JWSTokenVerifier {

    private ECDSATokenVerifier(String tokenString) {
        super(tokenString);
    }

    public static JWSTokenVerifier create(String tokenString) {
        return new ECDSATokenVerifier(tokenString);
    }

    public static AccessToken verifyToken(String tokenString, PublicKey publicKey, String realmUrl) throws VerificationException {
        return ECDSATokenVerifier.create(tokenString).publicKey(publicKey).realmUrl(realmUrl).verify().getToken();
    }

    public static AccessToken verifyToken(String tokenString, PublicKey publicKey, String realmUrl, boolean checkActive, boolean checkTokenType) throws VerificationException {
        return ECDSATokenVerifier.create(tokenString).publicKey(publicKey).realmUrl(realmUrl).checkActive(checkActive).checkTokenType(checkTokenType).verify().getToken();
    }
}
