package org.keycloak.tests.oid4vc.presentation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.keycloak.crypto.KeyWrapper;
import org.keycloak.tests.oid4vc.OID4VCProofTestUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

class TestIssuerEndpoint implements AutoCloseable {

    private static final char[] PASSWORD = "password".toCharArray();

    private final HttpsServer server;
    private String metadata = "{}";

    private TestIssuerEndpoint(HttpsServer server) {
        this.server = server;
    }

    static TestIssuerEndpoint start() throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        TestIssuerEndpoint endpoint = new TestIssuerEndpoint(server);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext()));
        server.createContext("/.well-known/jwt-vc-issuer", endpoint::handleMetadataRequest);
        server.start();
        return endpoint;
    }

    String issuer() {
        return "https://localhost:" + server.getAddress().getPort();
    }

    void metadata(TestIssuerMetadata metadata) {
        this.metadata = metadata.json();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private void handleMetadataRequest(HttpExchange exchange) throws IOException {
        byte[] body = metadata.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static SSLContext sslContext() throws Exception {
        KeyWrapper key = OID4VCProofTestUtils.createRsaKeyPair("issuer-endpoint-server");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, PASSWORD);
        keyStore.setKeyEntry(
                "issuer-endpoint-server",
                (PrivateKey) key.getPrivateKey(),
                PASSWORD,
                certificateChain(key).toArray(Certificate[]::new));

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    private static List<Certificate> certificateChain(KeyWrapper key) {
        if (key.getCertificateChain() != null && !key.getCertificateChain().isEmpty()) {
            return List.copyOf(key.getCertificateChain());
        }
        return List.of(key.getCertificate());
    }
}
