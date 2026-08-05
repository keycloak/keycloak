package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.keycloak.utils.StringUtil;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.http.HttpClient;
import com.apicatalog.jsonld.http.HttpResponse;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.HttpLoader;

/**
 * A {@link DocumentLoader} for the JSON-LD {@code @context} documents referenced by linked-data
 * verifiable credentials (LDP_VC). It hardens the default loader of the JSON-LD library, which
 * otherwise issues a fresh, uncached HTTP GET per context URL on every credential issuance,
 * without a request timeout or any restriction on the target:
 * <ul>
 *   <li><b>Allowlist</b> - only {@code https} is permitted and the host of every context URL must
 *   be in the allowlist. The policy applies to the initial URL and to every redirect hop;
 *   well-known context redirect destinations are allowed by default and any other destination can
 *   be allowed via the {@value #ALLOWED_HOSTS_PROPERTY} system property.</li>
 *   <li><b>Cache</b> - context documents are cached in memory in a bounded LRU cache keyed by
 *   URL, so each context is fetched at most once per server lifetime. Contexts are stable, so
 *   caching avoids repeated outbound requests and signature inconsistencies caused by content
 *   drift between issuances.</li>
 *   <li><b>Timeout</b> - connect and per-request timeouts are enforced, and the full response
 *   body is consumed within the request timeout, so a slow or unresponsive context host cannot
 *   block the credential-issuance worker thread indefinitely.</li>
 * </ul>
 */
public class JsonLdContextDocumentLoader implements DocumentLoader {

    /** Well-known hosts serving stable JSON-LD context documents for verifiable credentials. */
    public static final Set<String> DEFAULT_ALLOWED_HOSTS = Set.of(
            "www.w3.org", "w3id.org", "json-ld.org", "digitalbazaar.github.io");

    /**
     * System property to extend the allowlist of permitted context hosts with a comma-separated
     * list of additional hosts, e.g. {@code -Doid4vc.allowedContextHosts=contexts.example.org}.
     */
    public static final String ALLOWED_HOSTS_PROPERTY = "oid4vc.allowedContextHosts";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_CACHE_ENTRIES = 100;

    private static final DocumentLoader DEFAULT_INSTANCE = new JsonLdContextDocumentLoader();

    private final Set<String> allowedHosts;
    private final boolean allowInsecureScheme;
    private final DocumentLoader delegate;
    private final Map<URI, Document> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<URI, Document> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };
    private final Map<URI, Object> locks = new ConcurrentHashMap<>();

    public JsonLdContextDocumentLoader() {
        this(resolveAllowedHosts(), false, CONNECT_TIMEOUT, REQUEST_TIMEOUT);
    }

    /**
     * Creates a loader for tests that serve contexts from a local server over plain http and
     * use short timeouts.
     */
    static JsonLdContextDocumentLoader forTesting(Set<String> allowedHosts, Duration connectTimeout, Duration requestTimeout) {
        return new JsonLdContextDocumentLoader(allowedHosts, true, connectTimeout, requestTimeout);
    }

    private JsonLdContextDocumentLoader(Set<String> allowedHosts, boolean allowInsecureScheme,
                                        Duration connectTimeout, Duration requestTimeout) {
        this.allowedHosts = allowedHosts.stream()
                .map(host -> host.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.allowInsecureScheme = allowInsecureScheme;
        this.delegate = new HttpLoader(new HardenedHttpClient(allowedHosts, connectTimeout, requestTimeout, allowInsecureScheme));
    }

    /** Shared instance used by the credential signing suites. */
    public static DocumentLoader defaultInstance() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
        validate(url);

        Document cached = getCached(url);
        if (cached != null) {
            return cached;
        }

        // Coalesce concurrent misses for the same URL so only one request populates the cache.
        Object lock = locks.computeIfAbsent(url, u -> new Object());
        synchronized (lock) {
            cached = getCached(url);
            if (cached == null) {
                cached = delegate.loadDocument(url, options);
                putCached(url, cached);
            }
            locks.remove(url, lock);
            return cached;
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
                    "Refusing to load JSON-LD context from host not in allowlist '" + host + "': " + url
                            + ". Add the host via -D" + ALLOWED_HOSTS_PROPERTY + "=... if it is trusted.");
        }
    }

    private static boolean isSchemeAllowed(String scheme, boolean allowInsecureScheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return true;
        }
        // http is only tolerated as an explicit, test-only escape hatch.
        return allowInsecureScheme && "http".equalsIgnoreCase(scheme);
    }

    private static Set<String> resolveAllowedHosts() {
        Set<String> hosts = new HashSet<>(DEFAULT_ALLOWED_HOSTS);
        String configured = System.getProperty(ALLOWED_HOSTS_PROPERTY);
        if (StringUtil.isNotBlank(configured)) {
            Arrays.stream(configured.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .forEach(hosts::add);
        }
        return hosts;
    }

    /**
     * Enforces the scheme and host allowlist for every request, including redirect hops performed
     * by the loader, and applies a connect and a per-request timeout.
     */
    private static final class HardenedHttpClient implements HttpClient {

        private final Set<String> allowedHosts;
        private final JdkHttpClient client;
        private final boolean allowInsecureScheme;

        HardenedHttpClient(Set<String> allowedHosts, Duration connectTimeout, Duration requestTimeout, boolean allowInsecureScheme) {
            this.allowedHosts = allowedHosts;
            this.client = new JdkHttpClient(connectTimeout, requestTimeout);
            this.allowInsecureScheme = allowInsecureScheme;
        }

        @Override
        public HttpResponse send(URI targetUri, String requestProfile) throws JsonLdError {
            // Every redirect hop is re-validated against the same scheme and host policy.
            validateTarget(targetUri, allowedHosts, allowInsecureScheme);
            try {
                return new HttpResponseImpl(client.send(targetUri, requestProfile == null ? "application/ld+json" : requestProfile));
            } catch (IOException e) {
                throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e);
            }
        }
    }

    private static final class HttpResponseImpl implements HttpResponse {

        private final JdkHttpClient.Response response;

        HttpResponseImpl(JdkHttpClient.Response response) {
            this.response = response;
        }

        @Override
        public int statusCode() {
            return response.statusCode();
        }

        @Override
        public InputStream body() {
            return response.body();
        }

        @Override
        public Collection<String> links() {
            return response.links();
        }

        @Override
        public Optional<String> contentType() {
            return response.contentType();
        }

        @Override
        public Optional<String> location() {
            return response.location();
        }

        @Override
        public void close() {
            response.close();
        }
    }
}
