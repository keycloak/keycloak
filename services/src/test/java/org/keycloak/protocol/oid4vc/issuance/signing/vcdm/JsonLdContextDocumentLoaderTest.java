package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for the hardened JSON-LD context document loader.
 * <p>
 * Tests that perform HTTP requests use a local {@link HttpServer} and a loader configured to
 * allow the {@code localhost} host over plain {@code http} (a test-only escape hatch of
 * {@link JsonLdContextDocumentLoader}).
 */
public class JsonLdContextDocumentLoaderTest {

    private static final String CONTEXT_DOCUMENT =
            "{\"@context\":{\"id\":\"@id\",\"type\":\"@type\"," +
                    "\"credentialSubject\":\"https://www.w3.org/2018/credentials#credentialSubject\"}}";

    private static HttpServer server;
    private static String baseUrl;
    private static int contextRequests;
    private static int redirectRequests;

    @BeforeClass
    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/context", exchange -> {
            contextRequests++;
            handleRequest(exchange, 200, CONTEXT_DOCUMENT, "application/ld+json");
        });
        server.createContext("/redirect", exchange -> {
            redirectRequests++;
            exchange.getResponseHeaders().add("Location", baseUrl + "/context");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirectToFile", exchange -> {
            exchange.getResponseHeaders().add("Location", "file:///tmp/somewhere");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        // A dedicated pool so the blocking /slow handler cannot starve other tests.
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    @Test(expected = JsonLdError.class)
    public void testRejectsPlainHttpSchemeByDefault() throws JsonLdError {
        // Default policy is https-only; the allowlist is never reached for plain http.
        JsonLdContextDocumentLoader loader = new JsonLdContextDocumentLoader();
        loader.loadDocument(URI.create("http://www.w3.org/ns/credentials/v1"), new DocumentLoaderOptions());
    }

    @Test(expected = JsonLdError.class)
    public void testRejectsHostNotInAllowlist() throws JsonLdError {
        // Well-formed https URL, but the host is not in the allowlist.
        JsonLdContextDocumentLoader loader = new JsonLdContextDocumentLoader();
        loader.loadDocument(URI.create("https://contexts.example.org/credentials/v1"), new DocumentLoaderOptions());
    }

    @Test
    public void testCachesDocumentAcrossLoads() throws JsonLdError {
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                Set.of("localhost"), Duration.ofSeconds(5), Duration.ofSeconds(5));
        URI url = URI.create(baseUrl + "/context");
        contextRequests = 0;

        Document first = loader.loadDocument(url, new DocumentLoaderOptions());
        Document second = loader.loadDocument(url, new DocumentLoaderOptions());

        Assert.assertNotNull("Context document should be loaded", first);
        Assert.assertSame("Second load should return the cached document", first, second);
        Assert.assertEquals("The context should be fetched only once", 1, contextRequests);
    }

    @Test
    public void testFollowsRedirectAndCachesResult() throws JsonLdError {
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                Set.of("localhost"), Duration.ofSeconds(5), Duration.ofSeconds(5));
        URI url = URI.create(baseUrl + "/redirect");
        redirectRequests = 0;
        contextRequests = 0;

        loader.loadDocument(url, new DocumentLoaderOptions());
        loader.loadDocument(url, new DocumentLoaderOptions());

        Assert.assertEquals("The redirect target should be fetched only once", 1, redirectRequests);
        Assert.assertEquals("The context should be fetched only once", 1, contextRequests);
    }

    @Test(expected = JsonLdError.class)
    public void testRejectsRedirectToUnsupportedScheme() throws JsonLdError {
        // Even when the initial URL is allowed, the https-only policy applies to redirect hops.
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                Set.of("localhost"), Duration.ofSeconds(5), Duration.ofSeconds(5));
        loader.loadDocument(URI.create(baseUrl + "/redirectToFile"), new DocumentLoaderOptions());
    }

    @Test(timeout = 5000, expected = JsonLdError.class)
    public void testRequestTimeout() throws JsonLdError {
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                Set.of("localhost"), Duration.ofMillis(500), Duration.ofMillis(500));
        loader.loadDocument(URI.create(baseUrl + "/slow"), new DocumentLoaderOptions());
    }

    private static void handleRequest(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }
}
