package org.keycloak.jose.jws.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;

// KEYCLOAK-6770 JWS signatures using PS256 or ES256 algorithms for signing
public class ECDSAProvider implements SignatureProvider {
    public static String getJavaAlgorithm(Algorithm alg) {
        switch (alg) {
            case ES256:
                return "SHA256withECDSA";
            case ES384:
                return "SHA384withECDSA";
            case ES512:
                return "SHA512withECDSA";
            default:
                throw new IllegalArgumentException("Not an ECDSA Algorithm");
        }
    }

    public static Signature getSignature(Algorithm alg) {
        try {
            return Signature.getInstance(getJavaAlgorithm(alg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sign(byte[] data, Algorithm algorithm, PrivateKey privateKey) {
        try {
            Signature signature = getSignature(algorithm);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(JWSInput input, PublicKey publicKey) {
        try {
            Signature verifier = getSignature(input.getHeader().getAlgorithm());
            verifier.initVerify(publicKey);
            verifier.update(input.getEncodedSignatureInput().getBytes("UTF-8"));
            return verifier.verify(input.getSignature());
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public boolean verify(JWSInput input, String key) {
        return false;
    }

}
