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

package org.keycloak.testsuite.keys;

import jakarta.ws.rs.core.Response;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.keycloak.common.crypto.FipsMode;
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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.saml.AbstractSamlTest;
import org.keycloak.testsuite.util.KeyUtils;
import org.keycloak.testsuite.util.KeystoreUtils;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@EnableVault
public class JavaKeystoreKeyProviderTest extends AbstractKeycloakTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;
    private KeystoreUtils.KeystoreInfo generatedKeystore;
    private String keyAlgorithm;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

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
        createSuccess(isFips()? KeystoreUtil.KeystoreFormat.BCFKS : KeystoreUtil.KeystoreFormat.PKCS12, AlgorithmType.HMAC, Algorithm.HS256, true);
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
        KeystoreUtils.assumeKeystoreTypeSupported(keystoreType);
        generateKeystore(keystoreType, algorithmType, keyAlgorithm);

        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createRep("valid", priority, keyAlgorithm, vault? "${vault.keystore_password}" : "password");

        Response response = adminClient.realm("test").components().add(rep);
        String id = ApiUtil.getCreatedId(response);
        getCleanup().addComponentId(id);

        ComponentRepresentation createdRep = adminClient.realm("test").components().component(id).toRepresentation();
        assertEquals(6, createdRep.getConfig().size());
        assertEquals(Long.toString(priority), createdRep.getConfig().getFirst("priority"));
        assertEquals(vault? "${vault.keystore_password}" : ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst("keystorePassword"));
        assertEquals(vault? "${vault.keystore_password}" : ComponentRepresentation.SECRET_VALUE, createdRep.getConfig().getFirst("keyPassword"));

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();

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
        generateKeystore(KeystoreUtils.getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keystore", "/nosuchfile");

        Response response = adminClient.realm("test").components().add(rep);
        assertError(response, "Failed to load keys. File not found on server.");
    }

    @Test
    public void invalidKeystorePassword() throws Exception {
        generateKeystore(KeystoreUtils.getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keystore", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertError(response, "Failed to load keys. File not found on server.");
    }

    @Test
    public void invalidKeyAlias() throws Exception {
        generateKeystore(KeystoreUtils.getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keyAlias", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        assertError(response, "Alias invalid does not exists in the keystore.");
    }

    @Test
    public void invalidKeyPassword() throws Exception {
        KeystoreUtil.KeystoreFormat keystoreType = KeystoreUtils.getPreferredKeystoreType();
        if (keystoreType == KeystoreUtil.KeystoreFormat.PKCS12) {
            // only the keyStore password is significant with PKCS12. Hence we need to test with different keystore type
            String[] supportedKsTypes = KeystoreUtils.getSupportedKeystoreTypes();
            if (supportedKsTypes.length <= 1) {
                Assert.fail("Only PKCS12 type is supported, but invalidKeyPassword() scenario cannot be tested with it");
            }
            keystoreType = Enum.valueOf(KeystoreUtil.KeystoreFormat.class, supportedKsTypes[1]);
            log.infof("Fallback to keystore type '%s' for the invalidKeyPassword() test", keystoreType);
        }
        generateKeystore(keystoreType, AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);
        rep.getConfig().putSingle("keyPassword", "invalid");

        Response response = adminClient.realm("test").components().add(rep);
        Assert.assertEquals(400, response.getStatus());
        assertError(response, "Failed to load keys. Key in the keystore cannot be recovered.");
    }

    @Test
    public void invalidKeyAlgorithmCreatedECButRegisteredRSA() throws Exception {
        generateKeystore(KeystoreUtils.getPreferredKeystoreType(), AlgorithmType.ECDSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), Algorithm.RS256);

        Response response = adminClient.realm("test").components().add(rep);
        assertError(response, "Invalid RS256 key for alias keyalias. Algorithm is EC.");
    }

    @Test
    public void invalidKeyUsageForRS256() throws Exception {
        generateKeystore(KeystoreUtils.getPreferredKeystoreType(), AlgorithmType.RSA, Algorithm.RS256);
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), Algorithm.RS256);
        rep.getConfig().putSingle(Attributes.KEY_USE, "enc");

        Response response = adminClient.realm("test").components().add(rep);
        assertError(response, "Invalid use enc for algorithm RS256.");
    }

    @Test
    public void invalidKeystoreExpiredCertificate() throws Exception {
        generateRSAExpiredCertificateStore(KeystoreUtils.getPreferredKeystoreType());
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);

        Response response = adminClient.realm("test").components().add(rep);
        assertError(response, "Certificate error on server.");
    }

    @Test
    public void testExpiredCertificateInOneHour() throws Exception {
        this.keyAlgorithm = Algorithm.RS256;
        generateRSAExpiredInOneHourCertificateStore(KeystoreUtils.getPreferredKeystoreType());
        ComponentRepresentation rep = createRep("valid", System.currentTimeMillis(), keyAlgorithm);

        try (Response response = adminClient.realm("test").components().add(rep)) {
            String id = ApiUtil.getCreatedId(response);
            getCleanup().addComponentId(id);
        }

        KeysMetadataRepresentation keys = adminClient.realm("test").keys().getKeyMetadata();
        KeysMetadataRepresentation.KeyMetadataRepresentation key = keys.getKeys().get(0);
        assertEquals(AlgorithmType.RSA.name(), key.getType());
        PublicKey exp = PemUtils.decodePublicKey(generatedKeystore.getCertificateInfo().getPublicKey(), KeyType.RSA);
        PublicKey got = PemUtils.decodePublicKey(key.getPublicKey(), KeyType.RSA);
        assertEquals(exp, got);
        assertEquals(generatedKeystore.getCertificateInfo().getCertificate(), key.getCertificate());
        assertEquals(KeyStatus.ACTIVE.name(), key.getStatus());

        setTimeOffset(3610);

        keys = adminClient.realm("test").keys().getKeyMetadata();
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
        rep.setParentId(adminClient.realm("test").toRepresentation().getId());
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
                this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "keyalias", "password", "password");
            }
            case ECDSA -> {
                this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateECKey(Algorithm.ES256));
            }
            case AES -> {
                this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateSecretKey(Algorithm.AES, 256));
            }
            case HMAC -> {
                this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateSecretKey(Algorithm.HS256, 256));
            }
            case EDDSA -> {
                this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "keyalias", "password", "password",
                        KeyUtils.generateEdDSAKey(Algorithm.Ed25519));
            }
        }
    }

    private void generateRSAExpiredCertificateStore(KeystoreUtil.KeystoreFormat keystoreType) throws Exception {
        PrivateKey privKey = PemUtils.decodePrivateKey(AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_EXPIRED_PRIVATE_KEY);
        X509Certificate cert = PemUtils.decodeCertificate(AbstractSamlTest.SAML_CLIENT_SALES_POST_SIG_EXPIRED_CERTIFICATE);
        this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "keyalias", "password", "password", privKey, cert);
    }

    private void generateRSAExpiredInOneHourCertificateStore(KeystoreUtil.KeystoreFormat keystoreType) throws Exception {
        KeyPair keyPair = org.keycloak.common.util.KeyUtils.generateRsaKeyPair(2048);
        Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(
                keyPair, "test", new BigInteger("1"), Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));
        this.generatedKeystore = KeystoreUtils.generateKeystore(folder, keystoreType, "keyalias", "password", "password", keyPair.getPrivate(), cert);
    }

    private static boolean isFips() {
        return AuthServerTestEnricher.AUTH_SERVER_FIPS_MODE != FipsMode.DISABLED;
    }
}

