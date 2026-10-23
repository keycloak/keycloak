/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.truststore;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeystoreUtil.TruststoreFormat;
import org.keycloak.crypto.def.DefaultCryptoProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TruststoreBuilderTest {

    private static final String CERT_PROTECTION_ALGORITHM_KEY = "keystore.pkcs12.certProtectionAlgorithm";
    private static final String[] SYSTEM_PROPERTY_KEYS = {
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY + ".orig",
            CERT_PROTECTION_ALGORITHM_KEY
    };

    private final Map<String, String> originalSystemProperties = new HashMap<>();
    private CryptoProvider originalCryptoProvider;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        originalCryptoProvider = CryptoIntegration.isInitialised() ? CryptoIntegration.getProvider() : null;
        CryptoIntegration.setProvider(new DefaultCryptoProvider());
        for (String key : SYSTEM_PROPERTY_KEYS) {
            originalSystemProperties.put(key, System.getProperty(key));
        }
    }

    @After
    public void after() {
        for (String key : SYSTEM_PROPERTY_KEYS) {
            String value = originalSystemProperties.get(key);
            if (value == null) {
                System.getProperties().remove(key);
            } else {
                System.setProperty(key, value);
            }
        }
        CryptoIntegration.setProvider(originalCryptoProvider);
    }

    @Test
    public void testMergedTrustStore() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");

        KeyStore storeWithoutDefaults = TruststoreBuilder.createMergedTruststore(new String[] { url.getPath() }, false);
        ArrayList<String> storeWithoutDefaultsAliases = Collections.list(storeWithoutDefaults.aliases());
        assertEquals(2, storeWithoutDefaultsAliases.size());

        KeyStore storeWithDefaults = TruststoreBuilder.createMergedTruststore(new String[] { url.getPath() }, true);
        ArrayList<String> storeWithDefaultsAliases = Collections.list(storeWithDefaults.aliases());
        int certs = storeWithDefaultsAliases.size();
        assertTrue(certs > 2);
        assertTrue(storeWithDefaultsAliases.containsAll(storeWithoutDefaultsAliases));

        // saving / loading should provide the certs even without a password
        File saved = TruststoreBuilder.saveTruststore(storeWithDefaults, temporaryFolder.getRoot().getAbsolutePath(), null);

        KeyStore savedLoaded = TruststoreBuilder.loadStore(saved.getAbsolutePath(), TruststoreBuilder.PKCS12, null);
        assertEquals(certs, Collections.list(savedLoaded.aliases()).size());
    }

    @Test
    public void testJksDefaultTruststoreIsMerged() throws Exception {
        File javaHome = temporaryFolder.newFolder();
        File security = new File(javaHome, "lib" + File.separator + "security");
        assertTrue(security.mkdirs());

        // a JKS truststore named like the JRE one, so the format cannot be detected from the extension
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "jks-default-ca");
        KeyStore jks = KeyStore.getInstance("JKS");
        jks.load(null, null);
        jks.setCertificateEntry("jks-default-ca", certificate);
        try (OutputStream os = Files.newOutputStream(new File(security, "cacerts").toPath())) {
            jks.store(os, "changeit".toCharArray());
        }

        String originalJavaHome = System.getProperty("java.home");
        String originalCompat = Security.getProperty("keystore.type.compat");
        // mimic providers such as BC-FIPS that reject JKS content when loaded as PKCS12
        Security.setProperty("keystore.type.compat", "false");
        System.clearProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY);
        System.setProperty("java.home", javaHome.getAbsolutePath());
        try {
            KeyStore merged = TruststoreBuilder.createMergedTruststore(new String[0], true);
            List<String> aliases = Collections.list(merged.aliases());
            assertEquals(1, aliases.size());
            assertEquals(certificate, merged.getCertificate(aliases.get(0)));
        } finally {
            // an empty value would disable compatibility mode, so fall back to the JDK default
            Security.setProperty("keystore.type.compat", originalCompat == null ? "true" : originalCompat);
            System.setProperty("java.home", originalJavaHome);
        }
    }

    @Test
    public void testSetSystemTruststoreUsesPkcs12ByDefault() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        File dataDir = temporaryFolder.newFolder();

        TruststoreBuilder.setSystemTruststore(new String[] { url.getPath() }, false, dataDir.getAbsolutePath());

        File generated = new File(dataDir, "keycloak-truststore.p12");
        assertEquals(generated.getAbsolutePath(), System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY));
        assertEquals(TruststoreBuilder.PKCS12, System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY));
        assertEquals(TruststoreBuilder.DUMMY_PASSWORD, System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY));
        assertTrue(generated.exists());
        assertFalse(new File(dataDir, "keycloak-truststore.bcfks").exists());
    }

    @Test
    public void testSetSystemTruststoreUsesPreferredBcfksType() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        File dataDir = temporaryFolder.newFolder();
        CryptoIntegration.setProvider(new DefaultCryptoProvider() {
            @Override
            public TruststoreFormat getPreferredGeneratedTrustStoreType() {
                return TruststoreFormat.BCFKS;
            }
        });

        TruststoreBuilder.setSystemTruststore(new String[] { url.getPath() }, false, dataDir.getAbsolutePath());

        File generated = new File(dataDir, "keycloak-truststore.bcfks");
        assertEquals(generated.getAbsolutePath(), System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY));
        assertEquals(TruststoreFormat.BCFKS.name(), System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY));
        assertEquals(TruststoreBuilder.DUMMY_PASSWORD, System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY));
        assertTrue(generated.exists());
        assertFalse(new File(dataDir, "keycloak-truststore.p12").exists());

        KeyStore savedLoaded = TruststoreBuilder.loadStore(generated.getAbsolutePath(), TruststoreFormat.BCFKS.name(),
                TruststoreBuilder.DUMMY_PASSWORD);
        assertEquals(2, Collections.list(savedLoaded.aliases()).size());
    }

    @Test
    public void testSaveTruststoreRestoresPkcs12CertProtectionAlgorithm() throws Exception {
        System.setProperty(CERT_PROTECTION_ALGORITHM_KEY, "OLD");
        KeyStore truststore = TruststoreBuilder.createPkcs12KeyStore();

        TruststoreBuilder.saveTruststore(truststore, temporaryFolder.getRoot().getAbsolutePath(), null);

        assertEquals("OLD", System.getProperty(CERT_PROTECTION_ALGORITHM_KEY));
    }

    @Test
    public void testMergedTrustStoreFromDirectory() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");

        KeyStore storeWithoutDefaults = TruststoreBuilder
                .createMergedTruststore(new String[] { new File(url.getPath()).getParent() }, false);
        ArrayList<String> storeWithoutDefaultsAliases = Collections.list(storeWithoutDefaults.aliases());
        assertEquals(2, storeWithoutDefaultsAliases.size());
    }

    @Test
    public void testFailsWithInvalidFile() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/invalid");

        assertThrows(RuntimeException.class, () -> TruststoreBuilder
                .createMergedTruststore(new String[] { new File(url.getPath()).getAbsolutePath() }, false));
    }

    @Test
    public void testFailsWithMalformedPemInDirectory() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        File directory = temporaryFolder.newFolder();
        File bundle = new File(directory, "bundle.pem");
        Files.copy(new File(url.toURI()).toPath(), bundle.toPath());
        Files.writeString(bundle.toPath(), "\n-----BEGIN CERTIFICATE-----\nnot-base64\n-----END CERTIFICATE-----\n",
                StandardOpenOption.APPEND);

        assertThrows(RuntimeException.class, () -> TruststoreBuilder
                .createMergedTruststore(new String[] { directory.getAbsolutePath() }, false));
    }

    @Test
    public void testRejectsPemAsGeneratedTruststoreFormat() {
        assertThrows(IllegalArgumentException.class, () -> TruststoreBuilder.createTrustStore(TruststoreFormat.PEM));
    }

    @Test
    public void testKubernetesCaAndServiceCaIncludedWhenFilesExist() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String existingFile = new File(url.getPath()).getAbsolutePath();

        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, existingFile, existingFile);

        assertEquals(2, trustStores.size());
        assertEquals(existingFile, trustStores.get(0));
        assertEquals(existingFile, trustStores.get(1));
    }

    @Test
    public void testKubernetesCaAndServiceCaNotIncludedWhenFilesDoNotExist() throws Exception {
        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, "/non/existing/ca.crt", "/non/existing/service-ca.crt");

        assertTrue(trustStores.isEmpty());
    }

    @Test
    public void testOnlyKubernetesCaIncludedWhenServiceCaDoesNotExist() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String existingFile = new File(url.getPath()).getAbsolutePath();

        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, existingFile, "/non/existing/service-ca.crt");

        assertEquals(1, trustStores.size());
        assertEquals(existingFile, trustStores.get(0));
    }

    @Test
    public void testKubernetesCaPreservesExistingTrustStoreEntries() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String existingFile = new File(url.getPath()).getAbsolutePath();

        List<String> trustStores = new ArrayList<>();
        trustStores.add("/some/existing/truststore.p12");
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, existingFile, "/non/existing/service-ca.crt");

        assertEquals(2, trustStores.size());
        assertEquals("/some/existing/truststore.p12", trustStores.get(0));
        assertEquals(existingFile, trustStores.get(1));
    }

    @Test
    public void testKubernetesCaIgnoresDirectories() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        String directory = new File(url.getPath()).getParent();

        List<String> trustStores = new ArrayList<>();
        TruststoreBuilder.includeKubernetesTrustStorePaths(trustStores, directory, "/non/existing/service-ca.crt");

        assertTrue(trustStores.isEmpty());
    }

}
