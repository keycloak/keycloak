package org.keycloak.jose.jwk;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.Algorithm;

/**
 * Adds and removes prefix to X.509 DER encoded public keys.
 */
public class AKPUtils {

    // See AKPJWKTest to generate new prefixes
    static final Map<String, byte[]> PREFIXES;
    static final Map<String, Integer> PUBLIC_KEY_LENGTHS;
    static {
        Map<String, byte[]> prefixes = new HashMap<>();
        prefixes.put(Algorithm.ML_DSA_44, new byte[] { 48, -126, 5, 50, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 3, 17, 3, -126, 5, 33, 0, });
        prefixes.put(Algorithm.ML_DSA_65, new byte[] { 48, -126, 7, -78, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 3, 18, 3, -126, 7, -95, 0, });
        prefixes.put(Algorithm.ML_DSA_87, new byte[] { 48, -126, 10, 50, 48, 11, 6, 9, 96, -122, 72, 1, 101, 3, 4, 3, 19, 3, -126, 10, 33, 0, });
        PREFIXES = Collections.unmodifiableMap(prefixes);

        Map<String, Integer> publicKeyLengths = new HashMap<>();
        publicKeyLengths.put(Algorithm.ML_DSA_44, 1312);
        publicKeyLengths.put(Algorithm.ML_DSA_65, 1952);
        publicKeyLengths.put(Algorithm.ML_DSA_87, 2592);
        PUBLIC_KEY_LENGTHS = Collections.unmodifiableMap(publicKeyLengths);
    }

    public static PublicKey fromEncodedPub(String publicKey, String algorithm) {
        byte[] prefix = getPrefix(algorithm);
        if (publicKey == null) {
            throw new IllegalArgumentException("Missing AKP public key");
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(publicKey);
            if (decoded.length != PUBLIC_KEY_LENGTHS.get(algorithm)) {
                throw new IllegalArgumentException("Invalid AKP public key length");
            }
            byte[] keyWithPadding = combine(prefix, decoded);

            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyWithPadding);
            KeyFactory keyFactory = CryptoIntegration.getProvider().getKeyFactory(algorithm);

            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("Invalid AKP public key", e);
        }
    }

    public static String toEncodedPub(PublicKey publicKey, String algorithm) {
        byte[] prefix = getPrefix(algorithm);
        if (publicKey == null) {
            throw new IllegalArgumentException("Missing AKP public key");
        }

        byte[] encoded = publicKey.getEncoded();
        int expectedLength = prefix.length + PUBLIC_KEY_LENGTHS.get(algorithm);
        if (encoded == null || encoded.length != expectedLength || !startsWith(encoded, prefix)) {
            throw new IllegalArgumentException("AKP public key does not match algorithm " + algorithm);
        }

        byte[] keyOutWithoutPadding = removePadding(encoded, prefix.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyOutWithoutPadding);
    }

    private static byte[] getPrefix(String algorithm) {
        byte[] prefix = PREFIXES.get(algorithm);
        if (prefix == null) {
            throw new IllegalArgumentException("Unsupported AKP algorithm " + algorithm);
        }
        return prefix;
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

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

}
