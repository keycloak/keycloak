/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.keys;

import java.io.File;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jws.AlgorithmType;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.JavaKeystoreKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectCryptoHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.crypto.CryptoHelper;
import org.keycloak.testframework.crypto.KeystoreInfo;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.KeyUtils;
import org.keycloak.testsuite.util.saml.SamlConstants;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest(config = JavaKeystoreKeyProviderTest.JavaKeystoreVaultConfig.class)
public class JavaKeystoreKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectCryptoHelper
    CryptoHelper cryptoHelper;

    @TempDir
    public static File folder;

    protected Logger log = Logger.getLogger(this.getClass());

    private String keyAlgorithm;

    private KeystoreInfo generatedKeystore;

    @Test
    public void createJksRSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.JKS, AlgorithmType.RSA, Algorithm.RS256, false);
    }

    @Test
    public void createPkcs12RSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.PKCS12, AlgorithmType.RSA, Algorithm.RS256, true);
    }

    @Test
    public void createBcfksRSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.BCFKS, AlgorithmType.RSA, Algorithm.RS256, false);
    }

    @Test
    public void createJksECDSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.JKS, AlgorithmType.ECDSA, Algorithm.ES256, true);
    }

    @Test
    public void createPkcs12ECDSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.PKCS12, AlgorithmType.ECDSA, Algorithm.ES256, false);
    }

    @Test
    public void createBcfksECDSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.BCFKS, AlgorithmType.ECDSA, Algorithm.ES256, true);
    }

    @Test
    public void createJksECDSAECDHES() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.JKS, AlgorithmType.ECDSA, Algorithm.ECDH_ES, true);
    }

    @Test
    public void createPkcs12ECDSAECDHESA192KW() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.PKCS12, AlgorithmType.ECDSA, Algorithm.ECDH_ES_A192KW, false);
    }

    @Test
    public void createBcfksECDSAECDHESA256KW() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.BCFKS, AlgorithmType.ECDSA, Algorithm.ECDH_ES_A256KW, true);
    }

    @Test
    public void createBcfksAES() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.BCFKS, AlgorithmType.AES, Algorithm.AES, false);
    }

    @Test
    public void createHMAC() throws Exception {
        // BC provider fails storing HMAC in BCFKS (although BCFIPS works)
        createSuccess(cryptoHelper.isFips() ? KeystoreUtil.KeystoreFormat.BCFKS : KeystoreUtil.KeystoreFormat.PKCS12, AlgorithmType.HMAC, Algorithm.HS256, true);
    }

    @Test
    public void createJksEdDSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.JKS, AlgorithmType.EDDSA, Algorithm.EdDSA, true);
    }

    @Test
    public void createPkcs12EdDSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.PKCS12, AlgorithmType.EDDSA, Algorithm.EdDSA, true);
    }

    @Test
    public void createBcfksEdDSA() throws Exception {
        createSuccess(KeystoreUtil.KeystoreFormat.BCFKS, AlgorithmType.EDDSA, Algorithm.EdDSA, true);
    }

    private void createSuccess(KeystoreUtil.KeystoreFormat keystoreType, AlgorithmType algorithmType, String keyAlgorithm, boolean vault) throws Exception {
        cryptoHelper.keystore().assumeKeystoreTypeSupported(keystoreType);
        generateKeystore(keystoreType, algorithmType, keyAlgorithm);

        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", priority, keyAlgorithm, vault? "${vault.keystore_password}" : "password");

        Response response = realm.admin().components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        realm.cleanup().add(r -> r.components().component(id).remove());

        ComponentRepresentation createdRep = realm.admin().components().component(id).toRepresentation();
        assertEquals(6, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));
        assertEquals(vault? "${vault.keystore_password}" : ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst("keystorePassword"));
        assertEquals(vault? "${vault.keystore_password}" : ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst("keyPassword"));

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();

        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);

        assertEquals(id, key.getProviderId());
        switch (algorithmType) {
            case RSA -> {
                assertEquals(algorithmType.name(), key.getType());
                PublicKey exp = PemUtils.decodePublicKey(generatedKeystore.getCertificateInfo().getPublicKey(), KeyType.RSA);
                PublicKey got = PemUtils.decodePublicKey(key.getPublicKey(), KeyType.RSA);
                assertEquals(exp, got);
                assertEquals(generatedKeystore.getCertificateInfo().getCertificate(), key.getCertificate());
            }
            case ECDSA -> {
                assertEquals("EC", key.getType());
                PublicKey exp = PemUtils.decodePublicKey(generatedKeystore.getCertificateInfo().getPublicKey(), KeyType.EC);
                PublicKey got = PemUtils.decodePublicKey(key.getPublicKey(), KeyType.EC);
                assertEquals(exp, got);
                assertEquals(generatedKeystore.getCertificateInfo().getCertificate(), key.getCertificate());
            }
            case AES, HMAC -> {
                assertEquals(KeyType.OCT, key.getType());
                assertEquals(keyAlgorithm, key.getAlgorithm());
            }
            case EDDSA -> {
                assertEquals(KeyType.OKP, key.getType());
                assertEquals(keyAlgorithm, key.getAlgorithm());
            }
        }

        assertEquals(priority, key.getProviderPriority());
    }

    @Test
    public void invalidKeystore() throws Exception {
        generateKeystore(cryptoHelper.keystore().getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keystore", "/nosuchfile");

        Response response = realm.admin().components().add(rep);
        assertError(response, "Failed to load keys. File not found on server.");
    }

    @Test
    public void invalidKeystorePassword() throws Exception {
        generateKeystore(cryptoHelper.keystore().getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keystore", "invalid");

        Response response = realm.admin().components().add(rep);
        assertError(response, "Failed to load keys. File not found on server.");
    }

    @Test
    public void invalidKeyAlias() throws Exception {
        generateKeystore(cryptoHelper.keystore().getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keyAlias", "invalid");

        Response response = realm.admin().components().add(rep);
        assertError(response, "Alias invalid does not exists in the keystore.");
    }

    @Test
    public void invalidKeyPassword() throws Exception {
        KeystoreUtil.KeystoreFormat keystoreType = cryptoHelper.keystore().getPreferredKeystoreType();
        if (keystoreType == KeystoreUtil.KeystoreFormat.PKCS12) {
            // only the keyStore password is significant with PKCS12. Hence we need to test with different keystore type
            String[] supportedKsTypes = cryptoHelper.getExpectedSupportedKeyStoreTypes();
            if (supportedKsTypes.length <= 1) {
                Assertions.fail("Only PKCS12 type is supported, but invalidKeyPassword() scenario cannot be tested with it");
            }
            keystoreType = Enum.valueOf(KeystoreUtil.KeystoreFormat.class, supportedKsTypes[1]);
            log.infof("Fallback to keystore type '%s' for the invalidKeyPassword() test", keystoreType);
        }
        generateKeystore(keystoreType, AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keyPassword", "invalid");

        Response response = realm.admin().components().add(rep);
        Assertions.assertEquals(400, response.getStatus());
        assertError(response, "Failed to load keys. Key in the keystore cannot be recovered.");
    }

    @Test
    public void invalidKeyAlgorithmCreatedECButRegisteredRSA() throws Exception {
        generateKeystore(cryptoHelper.keystore().getPreferredKeystoreType(), AlgorithmType.ECDSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), Algorithm.RS256);

        Response response = realm.admin().components().add(rep);
        assertError(response, "Invalid RS256 key for alias keyalias. Algorithm is EC.");
    }

    @Test
    public void invalidKeyUsageForRS256() throws Exception {
        generateKeystore(cryptoHelper.keystore().getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), Algorithm.RS256);
        rep.getConfig().putSingle(Attributes.KEY_USE, "enc");

        Response response = realm.admin().components().add(rep);
        assertError(response, "Invalid use enc for algorithm RS256.");
    }

    @Test
    public void invalidKeystoreExpiredCertificate() throws Exception {
        generateRSAExpiredCertificateStore(cryptoHelper.keystore().getPreferredKeystoreType());
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);

        Response response = realm.admin().components().add(rep);
        assertError(response, "Certificate error on server.");
    }

    @Test
    public void testExpiredCertificateInOneHour() throws Exception {
        this.keyAlgorithm = Algorithm.RS256;
        generateRSAExpiredInOneHourCertificateStore(cryptoHelper.keystore().getPreferredKeystoreType());
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);

        try (Response response = realm.admin().components().add(rep)) {
            String id = ApiUtil.getCreatedId(response);
            realm.cleanup().add(r -> r.components().component(id).remove());
        }

        KeysMetadataRepresentation keys = realm.admin().keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        PublicKey exp = PemUtils.decodePublicKey(generatedKeystore.getCertificateInfo().getPublicKey(), KeyType.RSA);
        PublicKey got = PemUtils.decodePublicKey(key.getPublicKey(), KeyType.RSA);
        assertEquals(exp, got);
        assertEquals(generatedKeystore.getCertificateInfo().getCertificate(), key.getCertificate());
        assertEquals(KeyStatus.ACTIVE.name(), key.getStatus());

        timeOffSet.set(3610);

        keys = realm.admin().keys().getKeyMetadata();
        key = keys.getKeys().get(0);
        assertEquals(KeyStatus.PASSIVE.name(), key.getStatus());
    }

    protected void assertError(Response response, String error) {
        if (!response.hasEntity()) {
            fail("No error message set");
        }

        ErrorRepresentation errorRepresentation = response.readEntity(ErrorRepresentation.class);
        MatcherAssert.assertThat(errorRepresentation.getErrorMessage(), Matchers.containsString(error));
        response.close();
    }

    protected ComponentRepresentation createRep(String name, long priority, String algorithm) {
        return createRep(name, priority, algorithm, "password");
    }

    protected ComponentRepresentation createRep(String name, long priority, String algorithm, String password) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realm.admin().toRepresentation().getId());
        rep.setProviderId(JavaKeystoreKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle("keystore", generatedKeystore.getKeystoreFile().getAbsolutePath());
        rep.getConfig().putSingle("keystorePassword", password);
        rep.getConfig().putSingle("keyAlias", "keyalias");
        rep.getConfig().putSingle("keyPassword", password);
        rep.getConfig().putSingle("algorithm", algorithm);
        return rep;
    }

    private void generateKeystore(KeystoreUtil.KeystoreFormat keystoreType, AlgorithmType algorithmType, String keyAlgorithm) throws Exception {
        this.keyAlgorithm = keyAlgorithm;
        switch (algorithmType) {
            case RSA -> {
                this.generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, keystoreType, "keyalias", "password", "password");
            }
            case ECDSA -> {
                this.generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateECKey(Algorithm.ES256));
            }
            case AES -> {
                this.generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateSecretKey(Algorithm.AES, 256));
            }
            case HMAC -> {
                this.generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateSecretKey(Algorithm.HS256, 256));
            }
            case EDDSA -> {
                this.generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateEdDSAKey(Algorithm.Ed25519));
            }
        }
    }

    private void generateRSAExpiredCertificateStore(KeystoreUtil.KeystoreFormat keystoreType) throws Exception {
        PrivateKey privKey = PemUtils.decodePrivateKey(SamlConstants.SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY);
        X509Certificate cert = PemUtils.decodeCertificate(SamlConstants.SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE);
        this.generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, keystoreType, "keyalias", "password", "password", privKey, cert);
    }

    private void generateRSAExpiredInOneHourCertificateStore(KeystoreUtil.KeystoreFormat keystoreType) throws Exception {
        KeyPair keyPair = org.keycloak.common.util.KeyUtils.generateRsaKeyPair(2048);
        Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(
                keyPair, "test", new BigInteger("1"), Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        this.generatedKeystore = cryptoHelper.keystore().generateKeystore(folder, keystoreType, "keyalias", "password", "password", keyPair.getPrivate(), cert);
    }

    public static class JavaKeystoreVaultConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            try {
                URL url = JavaKeystoreKeyProviderTest.class.getResource("vault");
                if (url == null) {
                    throw new RuntimeException("Unable to find the vault folder in the classpath for the default_keystore__password file!");
                }
                return config.option("vault", "file").option("vault-dir", Path.of(url.toURI()).toString());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
