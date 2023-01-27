package org.keycloak.testsuite.util;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.representations.idm.KeysMetadataRepresentation;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * @author mhajas
 */
public class KeyUtils {

    public static PublicKey publicKeyFromString(String key) {
        try {
            KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(KeyType.RSA);
            byte[] encoded = Base64.getDecoder().decode(key);
            return kf.generatePublic(new X509EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey privateKeyFromString(String key) {
        try {
            KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(KeyType.RSA);
            byte[] encoded = Base64.getDecoder().decode(key);
            return kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeysMetadataRepresentation.KeyMetadataRepresentation getActiveSigningKey(KeysMetadataRepresentation keys, String algorithm) {
        for (KeysMetadataRepresentation.KeyMetadataRepresentation k : keys.getKeys()) {
            if (k.getAlgorithm().equals(algorithm) && KeyStatus.valueOf(k.getStatus()).isActive() && KeyUse.SIG.equals(k.getUse())) {
                return k;
            }
        }
        throw new RuntimeException("Active key not found");
    }

    public static KeysMetadataRepresentation.KeyMetadataRepresentation getActiveEncKey(KeysMetadataRepresentation keys, String algorithm) {
        for (KeysMetadataRepresentation.KeyMetadataRepresentation k : keys.getKeys()) {
            if (k.getAlgorithm().equals(algorithm) && KeyStatus.valueOf(k.getStatus()).isActive() && KeyUse.ENC.equals(k.getUse())) {
                return k;
            }
        }
        throw new RuntimeException("Active key not found");
    }

    /**
     * @return key sizes, which are expected to be supported by Keycloak server for {@link org.keycloak.keys.GeneratedRsaKeyProviderFactory} and {@link org.keycloak.keys.GeneratedRsaEncKeyProviderFactory}.
     */
    public static String[] getExpectedSupportedRsaKeySizes() {
        String expectedKeySizes = System.getProperty("auth.server.supported.rsa.key.sizes");
        if (expectedKeySizes == null || expectedKeySizes.trim().isEmpty()) {
            fail("System property 'auth.server.supported.rsa.key.sizes' should be set");
        }
        return expectedKeySizes.split(",");
    }

    /**
     * @return Lowest key size supported by Keycloak server for {@link org.keycloak.keys.GeneratedRsaKeyProviderFactory}.
     * It is usually 1024, but can be 2048 in some environments (typically in FIPS environments)
     */
    public static int getLowestSupportedRsaKeySize() {
        return Integer.parseInt(getExpectedSupportedRsaKeySizes()[0]);
    }

}
