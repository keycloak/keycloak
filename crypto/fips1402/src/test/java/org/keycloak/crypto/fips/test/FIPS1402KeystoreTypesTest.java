package org.keycloak.crypto.fips.test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Environment;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.truststore.TruststoreBuilder;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402KeystoreTypesTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void testKeystoreFormatsInNonApprovedMode() {
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        Set<KeystoreUtil.KeystoreFormat> supportedKeystoreFormats = CryptoIntegration.getProvider().getSupportedKeyStoreTypes().collect(Collectors.toSet());
        assertThat(supportedKeystoreFormats, Matchers.containsInAnyOrder(
                KeystoreUtil.KeystoreFormat.PKCS12,
                KeystoreUtil.KeystoreFormat.BCFKS));
    }

    @Test
    public void testTruststoreFormatsInNonApprovedMode() {
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        Set<KeystoreUtil.TruststoreFormat> supportedTruststoreFormats = CryptoIntegration.getProvider().getSupportedTrustStoreTypes().collect(Collectors.toSet());
        assertThat(supportedTruststoreFormats, Matchers.containsInAnyOrder(
                KeystoreUtil.TruststoreFormat.PKCS12,
                KeystoreUtil.TruststoreFormat.BCFKS));
        assertThat(CryptoIntegration.getProvider().getPreferredGeneratedTrustStoreType(), Matchers.equalTo(KeystoreUtil.TruststoreFormat.PKCS12));
    }

    // BCFIPS approved mode supports only BCFKS. No JKS nor PKCS12 support for keystores
    @Test
    public void testKeystoreFormatsInApprovedMode() {
        Assume.assumeTrue(CryptoServicesRegistrar.isInApprovedOnlyMode());
        Set<KeystoreUtil.KeystoreFormat> supportedKeystoreFormats = CryptoIntegration.getProvider().getSupportedKeyStoreTypes().collect(Collectors.toSet());
        assertThat(supportedKeystoreFormats, Matchers.containsInAnyOrder(
                KeystoreUtil.KeystoreFormat.BCFKS));
    }

    @Test
    public void testTruststoreFormatsInApprovedMode() {
        Assume.assumeTrue(CryptoServicesRegistrar.isInApprovedOnlyMode());
        Set<KeystoreUtil.TruststoreFormat> supportedTruststoreFormats = CryptoIntegration.getProvider().getSupportedTrustStoreTypes().collect(Collectors.toSet());
        assertThat(supportedTruststoreFormats, Matchers.containsInAnyOrder(
                KeystoreUtil.TruststoreFormat.BCFKS));
        assertThat(CryptoIntegration.getProvider().getPreferredGeneratedTrustStoreType(), Matchers.equalTo(KeystoreUtil.TruststoreFormat.BCFKS));
    }

    @Test
    public void testMultiCertificatePemTruststoreInApprovedMode() throws Exception {
        Assume.assumeTrue(CryptoServicesRegistrar.isInApprovedOnlyMode());

        File bundle = temporaryFolder.newFile("bundle.pem");
        try (OutputStream output = Files.newOutputStream(bundle.toPath())) {
            for (String resource : new String[] { "/certs/UPN-cert.pem", "/certs/ANS-cert.pem" }) {
                try (InputStream input = getClass().getResourceAsStream(resource)) {
                    assertNotNull(resource, input);
                    input.transferTo(output);
                    output.write('\n');
                }
            }
        }

        String truststoreKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY;
        String typeKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY;
        String passwordKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY;
        String originalTruststore = System.getProperty(truststoreKey);
        String originalType = System.getProperty(typeKey);
        String originalPassword = System.getProperty(passwordKey);
        try {
            TruststoreBuilder.setSystemTruststore(new String[] { bundle.getAbsolutePath() }, false,
                    temporaryFolder.getRoot().getAbsolutePath());

            assertEquals(KeystoreUtil.TruststoreFormat.BCFKS.name(), System.getProperty(typeKey));
            KeyStore truststore = KeyStore.getInstance("BCFKS", "BCFIPS");
            try (InputStream input = Files.newInputStream(new File(System.getProperty(truststoreKey)).toPath())) {
                truststore.load(input, TruststoreBuilder.DUMMY_PASSWORD.toCharArray());
            }
            assertEquals(2, truststore.size());
        } finally {
            restoreProperty(truststoreKey, originalTruststore);
            restoreProperty(typeKey, originalType);
            restoreProperty(passwordKey, originalPassword);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
