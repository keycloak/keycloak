package org.keycloak.testsuite.util.oauth;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureVerifierContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureVerifierContext;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.JsonWebToken;

public class TokensManager {

    private final KeyManager keyManager;

    TokensManager(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public <T extends JsonWebToken> T verifyToken(String token, Class<T> clazz) {
        try {
            TokenVerifier<T> verifier = TokenVerifier.create(token, clazz);
            String kid = verifier.getHeader().getKeyId();
            String algorithm = verifier.getHeader().getAlgorithm().name();
            KeyWrapper key = keyManager.getPublicKey(algorithm, kid);
            AsymmetricSignatureVerifierContext verifierContext;
            switch (algorithm) {
                case Algorithm.ES256, Algorithm.ES384, Algorithm.ES512 ->
                        verifierContext = new ServerECDSASignatureVerifierContext(key);
                default -> verifierContext = new AsymmetricSignatureVerifierContext(key);
            }
            verifier.verifierContext(verifierContext);
            verifier.verify();
            return verifier.getToken();
        } catch (VerificationException e) {
            throw new RuntimeException("Failed to decode token", e);
        }
    }

    public <T extends JsonWebToken> T parseToken(String token, Class<T> clazz) {
        try {
            return new JWSInput(token).readJsonContent(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
