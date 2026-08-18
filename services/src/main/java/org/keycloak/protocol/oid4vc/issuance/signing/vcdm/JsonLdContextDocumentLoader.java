package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.connections.httpclient.SafeInputStream;
import org.keycloak.models.KeycloakSession;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.http.HttpClient;
import com.apicatalog.jsonld.http.HttpResponse;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.HttpLoader;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * A {@link DocumentLoader} for the JSON-LD {@code @context} documents referenced by linked-data
 * verifiable credentials (LDP_VC). It hardens the default loader of the JSON-LD library, which
 * otherwise issues a fresh, uncached HTTP GET per context URL on every credential issuance,
 * without a request timeout or any restriction on the target:
 * <ul>
 *   <li><b>Allowlist</b> - only {@code https} is permitted and the host of every context URL must
 *   be in {@link #DEFAULT_ALLOWED_HOSTS}. The policy applies to the initial URL and to every
 *   redirect hop.</li>
 *   <li><b>Cache</b> - context documents are cached in memory in a bounded LRU cache keyed by
 *   URL, so each context is fetched at most once per server lifetime. Contexts are stable, so
 *   caching avoids repeated outbound requests and signature inconsistencies caused by content
 *   drift between issuances.</li>
 *   <li><b>Transport</b> - requests are sent through Keycloak's configured
 *   {@link HttpClientProvider} client, so the server's proxy, TLS and timeout settings apply.
 *   The response body is consumed with a size limit, so an oversized or stalled context host
 *   cannot exhaust the heap or block the credential-issuance worker thread indefinitely.</li>
 * </ul>
 */
public class JsonLdContextDocumentLoader implements DocumentLoader {

    /**
     * Well-known hosts serving stable JSON-LD context documents for verifiable credentials.
     * {@code digitalbazaar.github.io} is included because the standard security-suite contexts
     * on {@code w3id.org} are served through a redirect to that host.
     */
    public static final Set<String> DEFAULT_ALLOWED_HOSTS = Set.of(
            "www.w3.org", "w3id.org", "json-ld.org", "digitalbazaar.github.io");

    private static final int MAX_RESPONSE_SIZE = 1024 * 1024;
    private static final int MAX_CACHE_ENTRIES = 100;

    private static volatile DocumentLoader sharedLoader;

    private final Set<String> allowedHosts;
    private final boolean allowInsecureScheme;
    private final DocumentLoader delegate;
    private final Map<URI, Document> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<URI, Document> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };
    private final Map<URI, CompletableFuture<Document>> inflight = new ConcurrentHashMap<>();

    /** Creates a loader with the default https-only allowlist, using the given HTTP client. */
    public JsonLdContextDocumentLoader(CloseableHttpClient client) {
        this(client, DEFAULT_ALLOWED_HOSTS, false);
    }

    JsonLdContextDocumentLoader(CloseableHttpClient client, Set<String> allowedHosts, boolean allowInsecureScheme) {
        this.allowedHosts = allowedHosts.stream()
                .map(host -> host.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.allowInsecureScheme = allowInsecureScheme;
        this.delegate = new HttpLoader(new HttpClientImpl(client, this.allowedHosts, allowInsecureScheme));
    }

    /**
     * Creates a loader for tests that serve contexts from a local server over plain {@code http}.
     */
    static JsonLdContextDocumentLoader forTesting(CloseableHttpClient client, Set<String> allowedHosts) {
        return new JsonLdContextDocumentLoader(client, allowedHosts, true);
    }

    /**
     * Returns the loader shared by all credential signing suites. The loader is created once per
     * server lifetime from the platform HTTP client, so the context cache is shared across
     * issuances and every request honors the server's outbound HTTP configuration.
     */
    public static DocumentLoader defaultInstance(KeycloakSession session) {
        DocumentLoader loader = sharedLoader;
        if (loader == null) {
            synchronized (JsonLdContextDocumentLoader.class) {
                if (sharedLoader == null) {
                    sharedLoader = new JsonLdContextDocumentLoader(
                            session.getProvider(HttpClientProvider.class).getHttpClient());
                }
                loader = sharedLoader;
            }
        }
        return loader;
    }

    @Override
    public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
        validate(url);

        Document cached = getCached(url);
        if (cached != null) {
            return cached;
        }

        // Coalesce concurrent misses for the same URL: the caller that creates the in-flight entry
        // performs the load, everyone else waits on the shared future, so only one request reaches
        // the context host. Waiters inherit the result of the in-flight load, including a failure,
        // so a failed load cannot trigger duplicate concurrent requests.
        CompletableFuture<Document> future = new CompletableFuture<>();
        CompletableFuture<Document> existing = inflight.putIfAbsent(url, future);
        if (existing != null) {
            return await(existing);
        }

        try {
            // A just-completed load may have populated the cache between the check above and here.
            cached = getCached(url);
            if (cached == null) {
                cached = delegate.loadDocument(url, options);
                putCached(url, cached);
            }
            future.complete(cached);
            return cached;
        } catch (JsonLdError | RuntimeException e) {
            future.completeExceptionally(e);
            throw e;
        } finally {
            // Only the caller that created the entry removes it, after the load completed,
            // so a failed load cannot leak the entry.
            inflight.remove(url, future);
        }
    }

    private static Document await(CompletableFuture<Document> future) throws JsonLdError {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof JsonLdError jsonLdError) {
                throw jsonLdError;
            }
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e);
        }
    }

    private Document getCached(URI url) {
        synchronized (cache) {
            return cache.get(url);
        }
    }

    private void putCached(URI url, Document document) {
        synchronized (cache) {
            cache.put(url, document);
        }
    }

    private void validate(URI url) throws JsonLdError {
        if (url == null) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Cannot load a null JSON-LD context URL.");
        }
        validateTarget(url, allowedHosts, allowInsecureScheme);
    }

    private static void validateTarget(URI url, Set<String> allowedHosts, boolean allowInsecureScheme) throws JsonLdError {
        if (!isSchemeAllowed(url.getScheme(), allowInsecureScheme)) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED,
                    "Refusing to load JSON-LD context from unsupported scheme '" + url.getScheme() + "': " + url);
        }
        String host = url.getHost();
        if (host == null || !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED,
                    "Refusing to load JSON-LD context from host not in allowlist '" + host + "': " + url);
        }
    }

    private static boolean isSchemeAllowed(String scheme, boolean allowInsecureScheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return true;
        }
        // http is only tolerated as an explicit, test-only escape hatch.
        return allowInsecureScheme && "http".equalsIgnoreCase(scheme);
    }

    /**
     * Sends requests through the given client, enforcing the scheme and host allowlist for every
     * request including the redirect hops performed by the JSON-LD loader.
     */
    private static final class HttpClientImpl implements HttpClient {

        private final CloseableHttpClient client;
        private final Set<String> allowedHosts;
        private final boolean allowInsecureScheme;

        HttpClientImpl(CloseableHttpClient client, Set<String> allowedHosts, boolean allowInsecureScheme) {
            this.client = client;
            this.allowedHosts = allowedHosts;
            this.allowInsecureScheme = allowInsecureScheme;
        }

        @Override
        public HttpResponse send(URI targetUri, String requestProfile) throws JsonLdError {
            // Every redirect hop is re-validated against the same scheme and host policy.
            validateTarget(targetUri, allowedHosts, allowInsecureScheme);
            HttpGet request = new HttpGet(targetUri);
            request.setHeader("Accept", requestProfile == null ? "application/ld+json" : requestProfile);
            try (CloseableHttpResponse response = client.execute(request)) {
                return new HttpResponseImpl(response.getStatusLine().getStatusCode(),
                        readBoundedBody(response.getEntity()),
                        response.getFirstHeader("content-type"),
                        response.getFirstHeader("location"),
                        response.getHeaders("link"));
            } catch (IOException e) {
                throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e);
            }
        }

        private static byte[] readBoundedBody(HttpEntity entity) throws IOException {
            if (entity == null) {
                return new byte[0];
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = new SafeInputStream(entity.getContent(), MAX_RESPONSE_SIZE)) {
                in.transferTo(buffer);
            }
            return buffer.toByteArray();
        }
    }

    private static final class HttpResponseImpl implements HttpResponse {

        private final int statusCode;
        private final byte[] body;
        private final Optional<String> contentType;
        private final Optional<String> location;
        private final List<String> links;

        HttpResponseImpl(int statusCode, byte[] body, Header contentType, Header location, Header[] links) {
            this.statusCode = statusCode;
            this.body = body;
            this.contentType = Optional.ofNullable(contentType).map(Header::getValue);
            this.location = Optional.ofNullable(location).map(Header::getValue);
            this.links = Arrays.stream(links).map(Header::getValue).toList();
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public InputStream body() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public Collection<String> links() {
            return links;
        }

        @Override
        public Optional<String> contentType() {
            return contentType;
        }

        @Override
        public Optional<String> location() {
            return location;
        }

        @Override
        public void close() {
            // The body is fully buffered and the connection is released by the client.
        }
    }
}
