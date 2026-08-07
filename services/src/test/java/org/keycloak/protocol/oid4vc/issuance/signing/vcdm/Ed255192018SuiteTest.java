package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.protocol.oid4vc.issuance.signing.CredentialSignerException;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;

import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * End-to-end unit tests for {@link Ed255192018Suite} covering the JSON-LD context resolution
 * performed while transforming the credential for signing.
 */
public class Ed255192018SuiteTest {

    private static final String CONTEXT_DOCUMENT =
            "{\"@context\":{\"id\":\"@id\",\"type\":\"@type\"," +
                    "\"credentialSubject\":\"https://www.w3.org/2018/credentials#credentialSubject\"," +
                    "\"issuanceDate\":\"https://www.w3.org/2018/credentials#issuanceDate\"," +
                    "\"VerifiableCredential\":\"https://www.w3.org/2018/credentials#VerifiableCredential\"}}";

    private static HttpServer server;
    private static String contextUrl;
    private static int contextRequests;

    @BeforeClass
    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/context", exchange -> {
            contextRequests++;
            byte[] bytes = CONTEXT_DOCUMENT.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/ld+json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            exchange.close();
        });
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
        contextUrl = "http://localhost:" + server.getAddress().getPort() + "/context";
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    @Test
    public void testSignatureUsesCachedContextAndIsDeterministic() {
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                Set.of("localhost"), Duration.ofSeconds(5), Duration.ofSeconds(5));
        Ed255192018Suite suite = new Ed255192018Suite(testSigner(), loader);
        contextRequests = 0;

        byte[] firstSignature = suite.getSignature(createCredential());
        Assert.assertNotNull("A signature should be produced", firstSignature);
        Assert.assertTrue("The signature should not be empty", firstSignature.length > 0);

        // Signing a second credential must reuse the cached context document.
        suite.getSignature(createCredential());
        Assert.assertEquals("The context document should be fetched only once", 1, contextRequests);

        // URDNA2015 canonicalization and the signature are deterministic.
        Assert.assertArrayEquals("Signing the same credential twice must produce the same signature",
                firstSignature, suite.getSignature(createCredential()));
    }

    @Test(expected = CredentialSignerException.class)
    public void testRejectsContextFromNonAllowlistedHost() {
        // The default loader only allows well-known https hosts, so an https URL on a host
        // outside the allowlist fails fast without any network access.
        Ed255192018Suite suite = new Ed255192018Suite(testSigner());
        VerifiableCredential credential = createCredential();
        credential.setContext(List.of("https://contexts.example.org/credentials/v1"));

        suite.getSignature(credential);
    }

    private static VerifiableCredential createCredential() {
        return new VerifiableCredential()
                .setContext(List.of(contextUrl))
                .setType(List.of("VerifiableCredential"))
                .setIssuanceDate(Instant.ofEpochSecond(10))
                .setCredentialSubject(new CredentialSubject().setClaims(Map.of("test", "value")));
    }

    private static SignatureSignerContext testSigner() {
        return new SignatureSignerContext() {
            @Override
            public String getKid() {
                return "test-kid";
            }

            @Override
            public String getAlgorithm() {
                return "EdDSA";
            }

            @Override
            public String getHashAlgorithm() {
                return "SHA-256";
            }

            @Override
            public byte[] sign(byte[] data) {
                // Return the input so the signature reflects the canonicalized payload.
                return data.clone();
            }
        };
    }
}
