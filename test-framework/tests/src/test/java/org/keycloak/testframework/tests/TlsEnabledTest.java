package org.keycloak.testframework.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = TlsEnabledTest.TlsServerConfig.class)
public class TlsEnabledTest {

    @InjectHttpClient
    HttpClient httpClient;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testCertSupplier() {
        Assertions.assertNotNull(managedCertificates);

        Assertions.assertNotNull(managedCertificates.getServerKeyStorePath());
        Assertions.assertNull(managedCertificates.getServerTrustStorePath());
        Assertions.assertNotNull(managedCertificates.getClientTrustStorePath());

        Assertions.assertNotNull(managedCertificates.getClientSSLContext());
    }

    @Test
    public void testHttpClient() throws IOException {
        URL baseUrl = keycloakUrls.getBaseUrl();
        Assertions.assertEquals("https", baseUrl.getProtocol());

        HttpGet req = new HttpGet(baseUrl.toString());
        HttpResponse resp = httpClient.execute(req);
        Assertions.assertEquals(200, resp.getStatusLine().getStatusCode());
    }

    @Test
    public void testAdminClient() {
        adminClient.realm("default");
    }

    @Test
    public void testOAuthClient() {
        Assertions.assertTrue(oAuthClient.doWellKnownRequest().getTokenEndpoint().startsWith("https://"));
    }

    @Test
    public void testServerHttpClient() {
        String url = keycloakUrls.getMasterRealm() + "/.well-known/openid-configuration";
        String response = runOnServer.fetchString(session -> session.getProvider(HttpClientProvider.class).getString(url));
        Assertions.assertTrue(response.contains("\"issuer\":\"" + keycloakUrls.getMasterRealm() + "\""));
    }

    @Test
    public void testServerTruststore() throws IOException {
        String expected = PemUtils.addCertificateBeginEnd(TlsServerConfig.CUSTOM_CERTIFICATE)
                + Files.readString(Path.of(managedCertificates.getClientTrustStorePath()));
        String trusted = runOnServer.fetchString(session -> {
            try (InputStream is = new FileInputStream(System.getProperty("javax.net.ssl.trustStore"))) {
                KeyStore truststore = KeyStore.getInstance(System.getProperty("javax.net.ssl.trustStoreType"));
                truststore.load(is, System.getProperty("javax.net.ssl.trustStorePassword").toCharArray());
                for (X509Certificate certificate : PemUtils.decodeCertificates(expected)) {
                    if (truststore.getCertificateAlias(certificate) == null) {
                        return "false";
                    }
                }
                return "true";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Assertions.assertEquals("true", trusted);
    }

    public static class TlsServerConfig implements KeycloakServerConfig {

        // a certificate configured by the test itself must be trusted alongside the managed certificate
        static final String CUSTOM_CERTIFICATE = generateCertificate();
        private static final Path CUSTOM_TRUSTSTORE = writeTruststore(CUSTOM_CERTIFICATE);

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("truststore-paths", CUSTOM_TRUSTSTORE.toString());
        }

        private static String generateCertificate() {
            if (!CryptoIntegration.isInitialised()) {
                CryptoIntegration.setProvider(new DefaultCryptoProvider());
            }
            KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
            return PemUtils.encodeCertificate(CertificateUtils.generateV1SelfSignedCertificate(keyPair, "custom-truststore"));
        }

        private static Path writeTruststore(String certificate) {
            try {
                Path path = Files.createTempFile("kc-testing-custom-truststore", ".pem");
                path.toFile().deleteOnExit();
                return Files.writeString(path, PemUtils.addCertificateBeginEnd(certificate));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class TlsEnabledConfig implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
