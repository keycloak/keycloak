package org.keycloak.test.examples;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.InjectCertificate;
import org.keycloak.testframework.https.ManagedCertificate;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

@KeycloakIntegrationTest(config = TlsEnabledTest.TlsEnabledServerConfig.class)
public class TlsEnabledTest {

    @InjectHttpClient
    HttpClient httpClient;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectCertificate
    ManagedCertificate managedCertificate;

    @Test
    public void testCertSupplier() throws KeyStoreException, CertificateNotYetValidException, CertificateExpiredException {
        Assertions.assertNotNull(managedCertificate);
        Assertions.assertEquals("kc-server-testing.keystore", managedCertificate.getKeycloakServerKeyStorePath().getFileName().toString());

        KeyStore keyStore = managedCertificate.getKeyStore();
        Assertions.assertNotNull(keyStore);

        X509Certificate cert = managedCertificate.getCertificate();
        cert.checkValidity();

        Assertions.assertEquals(cert.getSerialNumber(), ((X509Certificate) keyStore.getCertificate("cert")).getSerialNumber());
    }

    @Test
    public void testHttpClient() throws IOException {
        HttpGet req = new HttpGet("https://localhost:8443");
        HttpResponse resp = httpClient.execute(req);
        Assertions.assertEquals(200, resp.getStatusLine().getStatusCode());
    }

    @Test
    public void testAdminClient() {
        adminClient.realm("default");
    }

    @Test
    public void testOAuthClient() {
        oAuthClient.doWellKnownRequest();
    }


    public static class TlsEnabledServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
