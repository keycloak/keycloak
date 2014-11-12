package org.keycloak.jose.jws.crypto;


import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RSAProvider {
    public static String getJavaAlgorithm(Algorithm alg) {
        switch (alg) {
            case RS256:
                return "SHA256withRSA";
            case RS384:
                return "SHA384withRSA";
            case RS512:
                return "SHA512withRSA";
            default:
                throw new IllegalArgumentException("Not an RSA Algorithm");
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
            throw new RuntimeException(e);
        }

    }


}
