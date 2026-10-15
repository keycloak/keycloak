package org.keycloak.crypto.fips.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Environment;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.truststore.FileTruststoreProviderFactory;
import org.keycloak.truststore.TruststoreBuilder;
import org.keycloak.truststore.TruststoreProvider;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
    public void testMultiCertificatePemTruststoreInNonApprovedMode() throws Exception {
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        testMultiCertificatePemTruststoreUsesConfiguredProvider(KeystoreUtil.TruststoreFormat.PKCS12);
    }

    @Test
    public void testMultiCertificatePemTruststoreInApprovedMode() throws Exception {
        Assume.assumeTrue(CryptoServicesRegistrar.isInApprovedOnlyMode());
        testMultiCertificatePemTruststoreUsesConfiguredProvider(KeystoreUtil.TruststoreFormat.BCFKS);
    }

    private void testMultiCertificatePemTruststoreUsesConfiguredProvider(KeystoreUtil.TruststoreFormat expectedFormat) throws Exception {
        // given
        File bundle = temporaryFolder.newFile("bundle.pem");
        Set<String> expectedCertificates = new HashSet<>();
        try (OutputStream output = Files.newOutputStream(bundle.toPath())) {
            for (String resource : new String[] { "/certs/UPN-cert.pem", "/certs/ANS-cert.pem" }) {
                try (InputStream input = getClass().getResourceAsStream(resource)) {
                    assertNotNull(resource, input);
                    byte[] certificatePem = input.readAllBytes();
                    output.write(certificatePem);
                    output.write('\n');
                    Certificate certificate = CertificateFactory.getInstance("X.509")
                            .generateCertificate(new ByteArrayInputStream(certificatePem));
                    expectedCertificates.add(Base64.getEncoder().encodeToString(certificate.getEncoded()));
                }
            }
        }

        String truststoreKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_KEY;
        String typeKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_TYPE_KEY;
        String passwordKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_PASSWORD_KEY;
        String providerKey = TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY;
        String originalTruststoreOrig = System.getProperty(truststoreKey + ".orig");
        String originalTypeOrig = System.getProperty(typeKey + ".orig");
        String originalPasswordOrig = System.getProperty(passwordKey + ".orig");
        String originalProviderOrig = System.getProperty(providerKey + ".orig");
        String originalTruststore = System.getProperty(truststoreKey);
        String originalType = System.getProperty(typeKey);
        String originalPassword = System.getProperty(passwordKey);
        String originalProvider = System.getProperty(providerKey);
        try {
            // when
            TruststoreBuilder.setSystemTruststore(new String[] { bundle.getAbsolutePath() }, false,
                    temporaryFolder.getRoot().getAbsolutePath());

            // then
            assertThat(System.getProperty(typeKey), equalTo(expectedFormat.name()));
            FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
            factory.init(new Config.SystemPropertiesScope("keycloak.truststore.file."));
            TruststoreProvider provider = factory.create(null);

            assertThat(certificates(provider.getTruststore()), equalTo(expectedCertificates));
            assertThat(provider.getTruststore().getProvider().getName(), equalTo("BCFIPS"));
            assertThat(System.getProperty(providerKey), equalTo(provider.getTruststore().getProvider().getName()));
        } finally {
            restoreProperty(truststoreKey, originalTruststore);
            restoreProperty(typeKey, originalType);
            restoreProperty(passwordKey, originalPassword);
            restoreProperty(providerKey, originalProvider);
            restoreProperty(truststoreKey + ".orig", originalTruststoreOrig);
            restoreProperty(typeKey + ".orig", originalTypeOrig);
            restoreProperty(passwordKey + ".orig", originalPasswordOrig);
            restoreProperty(providerKey + ".orig", originalProviderOrig);
        }
    }

    private static Set<String> certificates(KeyStore truststore) throws Exception {
        Set<String> certificates = new HashSet<>();
        for (String alias : Collections.list(truststore.aliases())) {
            certificates.add(Base64.getEncoder().encodeToString(truststore.getCertificate(alias).getEncoded()));
        }
        return certificates;
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
