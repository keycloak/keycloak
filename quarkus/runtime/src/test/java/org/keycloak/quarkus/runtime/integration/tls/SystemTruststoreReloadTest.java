package org.keycloak.quarkus.runtime.integration.tls;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.quarkus.runtime.integration.tls.SystemTruststoreReload.SystemTruststoreSourceAndKeystore;
import org.keycloak.truststore.TruststoreBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemTruststoreReloadTest {

    private static final String[] SYSTEM_PROPERTY_KEYS = {
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY,
            TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY + ".orig",
            TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY + ".orig"
    };

    private final Map<String, String> originalSystemProperties = new HashMap<>();
    private CryptoProvider originalCryptoProvider;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        originalCryptoProvider = CryptoIntegration.isInitialised() ? CryptoIntegration.getProvider() : null;
        CryptoIntegration.setProvider(new DefaultCryptoProvider());
        // start like a fresh server: no system truststore has been generated yet
        for (String key : SYSTEM_PROPERTY_KEYS) {
            originalSystemProperties.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
    }

    @After
    public void after() {
        for (String key : SYSTEM_PROPERTY_KEYS) {
            String value = originalSystemProperties.get(key);
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        }
        CryptoIntegration.setProvider(originalCryptoProvider);
    }

    @Test
    public void rotationRightAfterStartupMergeIsReloaded() throws Exception {
        File source = writePem(temporaryFolder.newFile("ca.pem"), generateCertificate("startup CA"));
        SystemTruststoreSourceAndKeystore startup = SystemTruststoreReload.loadSystemTruststore(
                new String[] { source.getAbsolutePath() }, true, temporaryFolder.newFolder().getAbsolutePath(), null, true);

        // the CA is rotated after the startup merge read it, but before the reload is set up
        writePem(source, generateCertificate("rotated CA"));
        SystemTruststoreReload reload = new SystemTruststoreReload(startup);

        assertTrue("a rotation right after the startup merge must be reloaded", reload.reloadIfChanged());
    }

    @Test
    public void sourcesUnchangedSinceStartupMergeAreNotReloaded() throws Exception {
        File source = writePem(temporaryFolder.newFile("ca.pem"), generateCertificate("startup CA"));
        SystemTruststoreSourceAndKeystore startup = SystemTruststoreReload.loadSystemTruststore(
                new String[] { source.getAbsolutePath() }, true, temporaryFolder.newFolder().getAbsolutePath(), null, true);

        SystemTruststoreReload reload = new SystemTruststoreReload(startup);

        assertFalse("sources unchanged since the startup merge must not be reloaded", reload.reloadIfChanged());
    }

    private static X509Certificate generateCertificate(String commonName) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return CryptoIntegration.getProvider().getCertificateUtils().generateV1SelfSignedCertificate(keyPair, commonName);
    }

    private static File writePem(File file, X509Certificate certificate) throws Exception {
        Files.writeString(file.toPath(), PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(certificate)));
        return file;
    }
}
