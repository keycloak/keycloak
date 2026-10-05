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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.KeystoreUtil.TruststoreFormat;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.def.DefaultCryptoProvider;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    public void testSetSystemTruststoreUsesPkcs12ByDefault() throws Exception {
        URL url = TruststoreBuilderTest.class.getResource("/truststores/keycloak.pem");
        File dataDir = temporaryFolder.newFolder();

        TruststoreBuilder.setAndGetSystemTruststore(new String[] { url.getPath() }, false, dataDir.getAbsolutePath(), null);

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

        TruststoreBuilder.setAndGetSystemTruststore(new String[] { url.getPath() }, false, dataDir.getAbsolutePath(), null);

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
    public void reSaveOverExistingTruststoreDoesNotSetCertProtectionAlgorithm() throws Exception {
        File dataDir = temporaryFolder.newFolder();

        KeyStore startup = TruststoreBuilder.createPkcs12KeyStore();
        startup.setCertificateEntry("startup", generateCertificate("startup"));

        X509Certificate reloadedCertificate = generateCertificate("reloaded");
        KeyStore updated = TruststoreBuilder.createPkcs12KeyStore();
        updated.setCertificateEntry("reloaded", reloadedCertificate);

        List<String> mutations = new ArrayList<>();
        Properties recording = new Properties() {
            @Override
            public synchronized Object setProperty(String key, String value) {
                if (CERT_PROTECTION_ALGORITHM_KEY.equals(key)) {
                    mutations.add(value);
                }
                return super.setProperty(key, value);
            }
        };
        recording.putAll(System.getProperties());
        Properties original = System.getProperties();
        System.setProperties(recording);
        int mutationsAfterInitialCreate;
        try {
            TruststoreBuilder.saveTruststore(startup, dataDir.getAbsolutePath(), null);
            mutationsAfterInitialCreate = mutations.size();
            TruststoreBuilder.saveTruststore(updated, dataDir.getAbsolutePath(), null);
        } finally {
            System.setProperties(original);
        }

        assertTrue("initial truststore creation must set " + CERT_PROTECTION_ALGORITHM_KEY
                + " (proves the recorder detects mutations)", mutationsAfterInitialCreate > 0);
        assertEquals("re-saving over an existing truststore must not set " + CERT_PROTECTION_ALGORITHM_KEY
                + " (mutations: " + mutations + ")", mutationsAfterInitialCreate, mutations.size());

        KeyStore onDisk = TruststoreBuilder.loadStore(new File(dataDir, "keycloak-truststore.p12").getAbsolutePath(),
                TruststoreBuilder.PKCS12, null);
        assertNotNull(onDisk.getCertificateAlias(reloadedCertificate));
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

    @Test
    public void reMergeAfterFileContentReplacementReflectsNewCertificate() throws Exception {
        File rotatingFile = temporaryFolder.newFile("rotating.pem");
        X509Certificate before = generateCertificate("before");
        writePemCertificate(rotatingFile, before);

        KeyStore firstMerge = TruststoreBuilder.createMergedTruststore(new String[] { rotatingFile.getAbsolutePath() }, false);
        assertNotNull(firstMerge.getCertificateAlias(before));

        X509Certificate after = generateCertificate("after");
        writePemCertificate(rotatingFile, after);

        KeyStore secondMerge = TruststoreBuilder.createMergedTruststore(new String[] { rotatingFile.getAbsolutePath() }, false);
        assertNotNull(secondMerge.getCertificateAlias(after));
        assertNull(secondMerge.getCertificateAlias(before));
    }

    @Test
    public void mergingMultipleFilesIncludesAllCertificates() throws Exception {
        X509Certificate first = generateCertificate("first");
        X509Certificate second = generateCertificate("second");
        File firstFile = writePemCertificate(temporaryFolder.newFile("first.pem"), first);
        File secondFile = writePemCertificate(temporaryFolder.newFile("second.pem"), second);

        KeyStore merged = TruststoreBuilder.createMergedTruststore(
                new String[] { firstFile.getAbsolutePath(), secondFile.getAbsolutePath() }, false);

        assertNotNull(merged.getCertificateAlias(first));
        assertNotNull(merged.getCertificateAlias(second));
    }

    @Test
    public void sameCertificateInMultipleFilesIsStoredOnce() throws Exception {
        X509Certificate certificate = generateCertificate("shared");
        File firstFile = writePemCertificate(temporaryFolder.newFile("first.pem"), certificate);
        File secondFile = writePemCertificate(temporaryFolder.newFile("second.pem"), certificate);

        KeyStore merged = TruststoreBuilder.createMergedTruststore(
                new String[] { firstFile.getAbsolutePath(), secondFile.getAbsolutePath() }, false);

        assertEquals(1, Collections.list(merged.aliases()).size());
    }

    @Test
    public void reSavePreservesExistingFilePermissions() throws Exception {
        File dataDir = temporaryFolder.newFolder();
        Path generated = new File(dataDir, "keycloak-truststore.p12").toPath();

        KeyStore startup = TruststoreBuilder.createPkcs12KeyStore();
        startup.setCertificateEntry("startup", generateCertificate("startup"));
        TruststoreBuilder.saveTruststore(startup, dataDir.getAbsolutePath(), null);

        Assume.assumeTrue("POSIX file permissions are required for this test",
                Files.getFileAttributeView(generated, PosixFileAttributeView.class) != null);

        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(generated, permissions);

        KeyStore updated = TruststoreBuilder.createPkcs12KeyStore();
        updated.setCertificateEntry("reloaded", generateCertificate("reloaded"));
        TruststoreBuilder.saveTruststore(updated, dataDir.getAbsolutePath(), null);

        assertEquals("re-saving over an existing truststore must preserve its file permissions",
                permissions, Files.getPosixFilePermissions(generated));
    }

    private static X509Certificate generateCertificate(String commonName) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return CryptoIntegration.getProvider().getCertificateUtils().generateV1SelfSignedCertificate(keyPair, commonName);
    }

    private static File writePemCertificate(File file, X509Certificate certificate) throws Exception {
        Files.writeString(file.toPath(), PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(certificate)));
        return file;
    }

}
