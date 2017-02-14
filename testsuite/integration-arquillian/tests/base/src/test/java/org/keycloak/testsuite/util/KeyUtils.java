package org.keycloak.testsuite.util;

import org.keycloak.common.util.BouncyIntegration;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author mhajas
 */
public class KeyUtils {
    static {
        BouncyIntegration.init();
    }


    public static PublicKey publicKeyFromString(String key) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] encoded = Base64.getDecoder().decode(key);
            return kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey privateKeyFromString(String key) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            byte[] encoded = Base64.getDecoder().decode(key);
            return kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
