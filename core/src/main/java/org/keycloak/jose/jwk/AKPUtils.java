package org.keycloak.jose.jwk;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.crypto.Algorithm;

/**
 * Adds and removes prefix to X.509 DER encoded public keys.
 */
public class AKPUtils {

    // See AKPJWKTest to generate new prefixes
    static final Map<String, byte[]> PREFIXES = new HashMap<>();
    static {
        PREFIXES.put(Algorithm.ML_DSA_44, new byte[] { 48, -126, 5, 50, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 3, 17, 3, -126, 5, 33, 0, });
        PREFIXES.put(Algorithm.ML_DSA_65, new byte[] { 48, -126, 7, -78, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 3, 18, 3, -126, 7, -95, 0, });
        PREFIXES.put(Algorithm.ML_DSA_87, new byte[] { 48, -126, 10, 50, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 3, 19, 3, -126, 10, 33, 0, });
    }

    public static PublicKey fromEncodedPub(String publicKey, String algorithm) {
        try {
            byte[] prefix = PREFIXES.get(algorithm);
            byte[] keyWithPadding = combine(prefix, Base64.getUrlDecoder().decode(publicKey));

            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyWithPadding);
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String toEncodedPub(PublicKey publicKey, String algorithm) {
        byte[] prefix = PREFIXES.get(algorithm);
        byte[] keyOutWithoutPadding = removePadding(publicKey.getEncoded(), prefix.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyOutWithoutPadding);
    }

    private static byte[] combine(byte[] first, byte[] second) {
        byte[] c = new byte[first.length + second.length];
        System.arraycopy(first, 0, c, 0, first.length);
        System.arraycopy(second, 0, c, first.length, second.length);
        return c;
    }

    private static byte[] removePadding(byte[] bytes, int length) {
        byte[] b = new byte[bytes.length - length];
        System.arraycopy(bytes, length, b, 0, bytes.length - length);
        return b;
    }

}
