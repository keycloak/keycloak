package org.keycloak.jose.jws.crypto;


import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.util.Base64Url;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HMACProvider {
    private static String getJavaAlgorithm(Algorithm alg) {
        switch (alg) {
            case HS256:
                return "HMACSHA256";
            case HS384:
                return "HMACSHA384";
            case HS512:
                return "HMACSHA512";
            default:
                throw new IllegalArgumentException("Not a MAC Algorithm");
        }
    }

    private static Mac getMAC(final Algorithm alg) {

        try {
            return Mac.getInstance(getJavaAlgorithm(alg));

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException("Unsupported HMAC algorithm: " + e.getMessage(), e);
        }
    }

    public static byte[] sign(byte[] data, Algorithm algorithm, byte[] sharedSecret) {
        try {
            Mac mac = getMAC(algorithm);
            mac.init(new SecretKeySpec(sharedSecret, mac.getAlgorithm()));
            mac.update(data);
            return mac.doFinal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sign(byte[] data, Algorithm algorithm, SecretKey key) {
        try {
            Mac mac = getMAC(algorithm);
            mac.init(key);
            mac.update(data);
            return mac.doFinal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(JWSInput input, SecretKey key) {
        try {
            byte[] signature = sign(input.getContent(), input.getHeader().getAlgorithm(), key);
            String x = Base64Url.encode(signature);
            return x.equals(input.getEncodedSignature());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean verify(JWSInput input, byte[] sharedSecret) {
        try {
            byte[] signature = sign(input.getEncodedSignatureInput().getBytes("UTF-8"), input.getHeader().getAlgorithm(), sharedSecret);
            String x = Base64Url.encode(signature);
            return x.equals(input.getEncodedSignature());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
