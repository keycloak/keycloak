package org.keycloak.crypto.fips.test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Environment;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.truststore.FileTruststoreProviderFactory;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
    public void generatedTruststoreIsReadWithItsWriterProvider() throws Exception {
        // given
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

        Collection<? extends Certificate> certificates;
        try (InputStream input = Files.newInputStream(bundle.toPath())) {
            certificates = CertificateFactory.getInstance("X.509").generateCertificates(input);
        }
        FileTruststoreProviderFactory factory = new FileTruststoreProviderFactory();
        Properties originalProperties = (Properties) System.getProperties().clone();
        try {
            // when
            TruststoreBuilder.setSystemTruststore(new String[] { bundle.getAbsolutePath() }, false,
                    temporaryFolder.getRoot().getAbsolutePath());
            factory.init(new Config.SystemPropertiesScope("keycloak.truststore.file."));
            KeyStore truststore = factory.create(null).getTruststore();

            // then
            assertThat(System.getProperty(TruststoreBuilder.SYSTEM_TRUSTSTORE_PROVIDER_KEY), equalTo("BCFIPS"));
            assertThat(truststore.getProvider().getName(), equalTo("BCFIPS"));
            assertThat(truststore.getType(), equalTo(CryptoIntegration.getProvider().getPreferredGeneratedTrustStoreType().name()));
            assertThat(truststore.size(), equalTo(certificates.size()));
            for (Certificate certificate : certificates) {
                assertThat(truststore.getCertificateAlias(certificate), notNullValue());
            }
        } finally {
            System.setProperties(originalProperties);
        }
    }
}
