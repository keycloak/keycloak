package org.keycloak.testsuite.util;

import java.security.PrivateKey;

import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.ServerECDSASignatureSignerContext;
import org.keycloak.crypto.SignatureSignerContext;

public class SignatureSignerUtil {

    public static SignatureSignerContext createSigner(PrivateKey privateKey, String kid, String algorithm) {
        return createSigner(privateKey, kid, algorithm, null);
    }

    public static SignatureSignerContext createSigner(PrivateKey privateKey, String kid, String algorithm, String curve) {
        KeyWrapper keyWrapper = new KeyWrapper();
        keyWrapper.setAlgorithm(algorithm);
        keyWrapper.setKid(kid);
        keyWrapper.setPrivateKey(privateKey);
        keyWrapper.setCurve(curve);
        SignatureSignerContext signer;
        switch (algorithm) {
            case Algorithm.ES256:
            case Algorithm.ES384:
            case Algorithm.ES512:
                signer = new ServerECDSASignatureSignerContext(keyWrapper);
                break;
            default:
                signer = new AsymmetricSignatureSignerContext(keyWrapper);
        }
        return signer;
    }
}
