package org.keycloak.test.examples;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = TlsEnabledTest.TlsEnabledServerConfig.class)
public class TlsEnabledTest {

    @InjectHttpClient
    HttpClient httpClient;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectCertificates
    ManagedCertificates managedCertificates;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;


    @Test
    public void testCertSupplier() throws KeyStoreException {
        Assertions.assertNotNull(managedCertificates);

        KeyStore trustStore = managedCertificates.getClientTrustStore();
        Assertions.assertNotNull(trustStore);

        X509Certificate cert = managedCertificates.getKeycloakServerCertificate();
        Assertions.assertNotNull(cert);
        Assertions.assertEquals(cert.getSerialNumber(), ((X509Certificate) trustStore.getCertificate(ManagedCertificates.CERT_ENTRY)).getSerialNumber());
    }

    @Test
    public void testCertDetails() throws CertificateNotYetValidException, CertificateExpiredException {
        X509Certificate cert = managedCertificates.getKeycloakServerCertificate();

        cert.checkValidity();
        Assertions.assertEquals("CN=localhost", cert.getSubjectX500Principal().getName());
        Assertions.assertEquals("CN=localhost", cert.getIssuerX500Principal().getName());
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


    public static class TlsEnabledServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
