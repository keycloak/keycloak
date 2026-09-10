package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.connections.httpclient.HttpClientBuilder;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
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
    private static ExecutorService executor;
    private static CloseableHttpClient client;
    private static String baseUrl;
    private static final AtomicInteger contextRequests = new AtomicInteger();
    private static final AtomicInteger redirectRequests = new AtomicInteger();

    @BeforeClass
    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        // The port is already bound here, so the base URL is available to the handlers below.
        baseUrl = "http://localhost:" + server.getAddress().getPort();
        server.createContext("/context", exchange -> {
            contextRequests.incrementAndGet();
            handleRequest(exchange, 200, CONTEXT_DOCUMENT, "application/ld+json");
        });
        server.createContext("/redirect", exchange -> {
            redirectRequests.incrementAndGet();
            exchange.getResponseHeaders().add("Location", baseUrl + "/context");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirectToFile", exchange -> {
            exchange.getResponseHeaders().add("Location", "file:///tmp/somewhere");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/redirectToEvil", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://not-allowed.example.org/context");
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
        server.createContext("/delayed", exchange -> {
            contextRequests.incrementAndGet();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handleRequest(exchange, 200, CONTEXT_DOCUMENT, "application/ld+json");
        });
        server.createContext("/slowBody", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/ld+json");
            exchange.sendResponseHeaders(200, 1000);
            OutputStream os = exchange.getResponseBody();
            os.write("{\"@context\":".getBytes(StandardCharsets.UTF_8));
            os.flush();
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        server.createContext("/large", exchange -> {
            byte[] body = new byte[2 * 1024 * 1024];
            Arrays.fill(body, (byte) 'a');
            exchange.getResponseHeaders().set("Content-Type", "application/ld+json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            } catch (IOException ignored) {
                // the client aborts the exchange once the size limit is exceeded
            }
            exchange.close();
        });
        // A dedicated pool so the blocking /slow handler cannot starve other tests.
        executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.start();
        // Short socket timeout so the slow-handler tests fail fast instead of hanging.
        client = new HttpClientBuilder()
                .socketTimeout(500, TimeUnit.MILLISECONDS)
                .disableRedirectHandling()
                .build();
    }

    @AfterClass
    public static void stopServer() throws IOException {
        executor.shutdownNow();
        server.stop(0);
        client.close();
    }

    @Test(expected = JsonLdError.class)
    public void testRejectsPlainHttpSchemeByDefault() throws JsonLdError {
        // Default policy is https-only; the allowlist is never reached for plain http.
        JsonLdContextDocumentLoader loader = new JsonLdContextDocumentLoader(client);
        loader.loadDocument(URI.create("http://www.w3.org/ns/credentials/v1"), new DocumentLoaderOptions());
    }

    @Test(expected = JsonLdError.class)
    public void testRejectsHostNotInAllowlist() throws JsonLdError {
        // Well-formed https URL, but the host is not in the allowlist.
        JsonLdContextDocumentLoader loader = new JsonLdContextDocumentLoader(client);
        loader.loadDocument(URI.create("https://contexts.example.org/credentials/v1"), new DocumentLoaderOptions());
    }

    @Test
    public void testCachesDocumentAcrossLoads() throws JsonLdError {
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));
        URI url = URI.create(baseUrl + "/context");
        contextRequests.set(0);

        Document first = loader.loadDocument(url, new DocumentLoaderOptions());
        Document second = loader.loadDocument(url, new DocumentLoaderOptions());

        Assert.assertNotNull("Context document should be loaded", first);
        Assert.assertSame("Second load should return the cached document", first, second);
        Assert.assertEquals("The context should be fetched only once", 1, contextRequests.get());
    }

    @Test
    public void testFollowsRedirectAndCachesResult() throws JsonLdError {
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));
        URI url = URI.create(baseUrl + "/redirect");
        redirectRequests.set(0);
        contextRequests.set(0);

        loader.loadDocument(url, new DocumentLoaderOptions());
        loader.loadDocument(url, new DocumentLoaderOptions());

        Assert.assertEquals("The redirect target should be fetched only once", 1, redirectRequests.get());
        Assert.assertEquals("The context should be fetched only once", 1, contextRequests.get());
    }

    @Test(expected = JsonLdError.class)
    public void testRejectsRedirectToUnsupportedScheme() throws JsonLdError {
        // Even when the initial URL is allowed, the https-only policy applies to redirect hops.
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));
        loader.loadDocument(URI.create(baseUrl + "/redirectToFile"), new DocumentLoaderOptions());
    }

    @Test(expected = JsonLdError.class)
    public void testRejectsRedirectToNonAllowlistedHost() throws JsonLdError {
        // Redirect hops are validated against the same host allowlist as the initial URL.
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));
        loader.loadDocument(URI.create(baseUrl + "/redirectToEvil"), new DocumentLoaderOptions());
    }

    @Test(timeout = 5000, expected = JsonLdError.class)
    public void testRequestTimeout() throws JsonLdError {
        // A server that never answers is cut off by the client's socket timeout.
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));
        loader.loadDocument(URI.create(baseUrl + "/slow"), new DocumentLoaderOptions());
    }

    @Test(timeout = 5000, expected = JsonLdError.class)
    public void testSlowBodyTimesOut() throws JsonLdError {
        // The body is consumed through the client's socket timeout, so a server that sends
        // headers and then stalls must fail instead of blocking the caller.
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));
        loader.loadDocument(URI.create(baseUrl + "/slowBody"), new DocumentLoaderOptions());
    }

    @Test(timeout = 5000, expected = JsonLdError.class)
    public void testRejectsOversizedContext() throws JsonLdError {
        // The body is buffered with a size limit, so an oversized response must be aborted
        // instead of exhausting the heap.
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));
        loader.loadDocument(URI.create(baseUrl + "/large"), new DocumentLoaderOptions());
    }

    @Test
    public void testFailedLoadDoesNotLeakInFlightEntry() throws Exception {
        // A failed load must release the in-flight entry, otherwise every URL that ever fails
        // keeps an entry in the inflight map for the lifetime of the server.
        JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                client, Set.of("localhost"));

        Assert.assertThrows(JsonLdError.class, () -> loader.loadDocument(
                URI.create(baseUrl + "/large"), new DocumentLoaderOptions()));

        Field inflight = JsonLdContextDocumentLoader.class.getDeclaredField("inflight");
        inflight.setAccessible(true);
        Map<?, ?> inFlightEntries = (Map<?, ?>) inflight.get(loader);
        Assert.assertTrue("Failed loads must not leak in-flight entries", inFlightEntries.isEmpty());
    }

    @Test(timeout = 15000)
    public void testConcurrentLoadsAreCoalesced() throws Exception {
        // Concurrent misses for the same URL must share a single load: the first caller fetches
        // the document and the others wait on the in-flight future instead of fetching again.
        try (CloseableHttpClient slowClient = new HttpClientBuilder()
                .socketTimeout(5000, TimeUnit.MILLISECONDS)
                .disableRedirectHandling()
                .build()) {
            JsonLdContextDocumentLoader loader = JsonLdContextDocumentLoader.forTesting(
                    slowClient, Set.of("localhost"));
            URI url = URI.create(baseUrl + "/delayed");
            contextRequests.set(0);

            int threads = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> results = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    results.add(pool.submit(() -> {
                        start.await();
                        return loader.loadDocument(url, new DocumentLoaderOptions());
                    }));
                }
                start.countDown();
                for (Future<?> result : results) {
                    result.get(10, TimeUnit.SECONDS);
                }
            } finally {
                pool.shutdownNow();
            }

            Assert.assertEquals("Concurrent loads of the same URL must fetch it only once",
                    1, contextRequests.get());
        }
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
