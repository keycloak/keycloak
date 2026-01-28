package org.keycloak.testsuite.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.keys.AbstractEcKeyProviderFactory;
import org.keycloak.keys.GeneratedEcdhKeyProviderFactory;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author mhajas
 */
public class KeyUtils {
    public static KeyPair generateECKey(String algorithm) {

        try {
            KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen("ECDSA");
            String domainParamNistRep = GeneratedEcdsaKeyProviderFactory
                    .convertJWSAlgorithmToECDomainParmNistRep(algorithm);
            if (domainParamNistRep == null) {
                domainParamNistRep = GeneratedEcdhKeyProviderFactory
                        .convertJWEAlgorithmToECDomainParmNistRep(algorithm);
            }
            String curve = AbstractEcKeyProviderFactory.convertECDomainParmNistRepToSecRep(domainParamNistRep);
            ECGenParameterSpec parameterSpec = new ECGenParameterSpec(curve);
            kpg.initialize(parameterSpec);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyPair generateEdDSAKey(String curve) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator kpg = CryptoIntegration.getProvider().getKeyPairGen(curve);
        return kpg.generateKeyPair();
    }

    public static SecretKey generateSecretKey(String algorithm, int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyGenerator keyGen = KeyGenerator.getInstance(JavaAlgorithm.getJavaAlgorithm(algorithm), BouncyIntegration.PROVIDER);
        keyGen.init(keySize);
        return keyGen.generateKey();
    }

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

    public static KeysMetadataRepresentation.KeyMetadataRepresentation getActiveEncryptionKey(KeysMetadataRepresentation keys, String algorithm) {
        for (KeysMetadataRepresentation.KeyMetadataRepresentation k : keys.getKeys()) {
            if (k.getAlgorithm().equals(algorithm) && KeyStatus.valueOf(k.getStatus()).isActive() && KeyUse.ENC.equals(k.getUse())) {
                return k;
            }
        }
        throw new RuntimeException("Active key not found");
    }

    public static KeysMetadataRepresentation.KeyMetadataRepresentation findActiveSigningKey(RealmResource realm) {
        return findRealmKeys(realm, rep -> rep.getPublicKey() != null && KeyStatus.valueOf(rep.getStatus()).isActive() && KeyUse.SIG.equals(rep.getUse()))
                .findFirst()
                .orElse(null);
    }

    public static KeysMetadataRepresentation.KeyMetadataRepresentation findActiveSigningKey(RealmResource realm, String alg) {
        return findRealmKeys(realm, rep -> rep.getPublicKey() != null && KeyStatus.valueOf(rep.getStatus()).isActive() && KeyUse.SIG.equals(rep.getUse()) && alg.equals(rep.getAlgorithm()))
                .findFirst()
                .orElse(null);
    }

    public static KeysMetadataRepresentation.KeyMetadataRepresentation findActiveEncryptingKey(RealmResource realm, String alg) {
        return findRealmKeys(realm, rep -> rep.getPublicKey() != null && KeyStatus.valueOf(rep.getStatus()).isActive() && KeyUse.ENC.equals(rep.getUse()) && alg.equals(rep.getAlgorithm()))
                .findFirst()
                .orElse(null);
    }

    public static Stream<KeysMetadataRepresentation.KeyMetadataRepresentation> findRealmKeys(RealmResource realm, Predicate<KeysMetadataRepresentation.KeyMetadataRepresentation> filter) {
        return realm.keys().getKeyMetadata().getKeys().stream().filter(filter);
    }

    public static AutoCloseable generateNewRealmKey(RealmResource realm, KeyUse keyUse, String algorithm, String priority) {
        String realmId = realm.toRepresentation().getId();

        ComponentRepresentation keys = new ComponentRepresentation();
        keys.setName("generated");
        keys.setProviderType(KeyProvider.class.getName());
        keys.setProviderId(keyUse == KeyUse.ENC ? "rsa-enc-generated" : "rsa-generated");
        keys.setParentId(realmId);
        keys.setConfig(new MultivaluedHashMap<>());
        keys.getConfig().putSingle("priority", priority);
        keys.getConfig().putSingle("keyUse", KeyUse.ENC.getSpecName());
        keys.getConfig().putSingle("algorithm", algorithm);
        Response response = realm.components().add(keys);
        assertEquals(201, response.getStatus());
        String id = ApiUtil.getCreatedId(response);
        response.close();

        return () -> realm.components().removeComponent(id);
    }

    public static AutoCloseable generateNewRealmKey(RealmResource realm, KeyUse keyUse, String algorithm) {
        return generateNewRealmKey(realm, keyUse, algorithm, "100");
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
