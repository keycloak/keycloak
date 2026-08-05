package org.keycloak.tests.ssl;

import java.util.concurrent.TimeUnit;

import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests that the {@link org.keycloak.quarkus.runtime.services.MisdirectedFilter} returns HTTP 421 for misdirected HTTP/2 requests.
 */
@KeycloakIntegrationTest(config = MisdirectedRequestTest.ServerConfig.class)
class MisdirectedRequestTest {

    @InjectCertificates(config = TlsEnabledConfig.class)
    ManagedCertificates managedCertificates;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    void misdirectedRequestDetection() throws Exception {
        Vertx vertx = Vertx.vertx();
        try {
            HttpClient client = vertx.createHttpClient(new HttpClientOptions()
                    .setSsl(true)
                    .setTrustAll(true)
                    .setVerifyHost(false)
                    .setProtocolVersion(HttpVersion.HTTP_2)
                    .setUseAlpn(true));
            try {
                int port = keycloakUrls.getBaseUrl().getPort();

                assertThat("Matching indicated to authority is allowed",
                        sendRequest(client, port, "servicehost.com", "servicehost.com", 8443), is(200));

                // null sniHostname → defaults to "localhost" (non-FQDN → Java skips SNI → indicatedServerName is null)
                assertThat("No indicated name is allowed",
                        sendRequest(client, port, null, "example.com", 443), is(200));

                // connection originated from another backend, but we're reusing it for a request to the keycloak server
                assertThat("Matching a known host is allowed",
                        sendRequest(client, port, "other-example.com", "example.com", 443), is(200));

                // connection originated from keycloak, but the browser is mistakenly reusing for another service
                assertThat("Expected HTTP 421 Misdirected Request for SNI/authority mismatch",
                        sendRequest(client, port, "example.com", "misdirected.com", 443), is(421));
            } finally {
                // Timeout set to minutes to allow manual debugging
                client.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.MINUTES);
            }
        } finally {
            // Timeout set to minutes to allow manual debugging
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.MINUTES);
        }
    }

    private int sendRequest(HttpClient client, int serverPort, String sniHostname, String authorityHost, int authorityPort) throws Exception {
        RequestOptions options = new RequestOptions()
                .setServer(SocketAddress.inetSocketAddress(serverPort, "localhost"))
                .setPort(serverPort)
                .setSsl(true)
                .setURI("/realms/master")
                .setMethod(HttpMethod.GET);

        if (sniHostname != null) {
            options.setHost(sniHostname);
        }

        return client.request(options)
                .compose(req -> {
                    req.authority(HostAndPort.create(authorityHost, authorityPort));
                    return req.send();
                })
                .map(HttpClientResponse::statusCode)
                .toCompletionStage()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
    }

    static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.option("hostname", "https://example.com");
        }
    }

    static class TlsEnabledConfig implements CertificatesConfig {
        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
