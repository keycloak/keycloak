package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thin wrapper around the JDK HTTP client used to fetch JSON-LD context documents.
 * <p>
 * It isolates the {@code java.net.http} types so that the JSON-LD loader can implement the
 * titanium interfaces ({@code com.apicatalog.jsonld.http.HttpClient}/{@code HttpResponse})
 * without a naming collision, which would otherwise force fully qualified names.
 */
final class JdkHttpClient {

    private static final int MAX_RESPONSE_SIZE = 1024 * 1024;

    private final HttpClient client;
    private final Duration requestTimeout;

    JdkHttpClient(Duration connectTimeout, Duration requestTimeout) {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(connectTimeout)
                .build();
        this.requestTimeout = requestTimeout;
    }

    Response send(URI targetUri, String acceptHeader) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(targetUri)
                .header("Accept", acceptHeader)
                .timeout(requestTimeout)
                .build();
        // The request timeout alone does not bound the body download, so the whole exchange
        // (headers and body) is awaited with a hard deadline and cancelled on expiry. A server
        // that sends headers and then stalls therefore times out instead of blocking the caller.
        CompletableFuture<HttpResponse<byte[]>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
        try {
            HttpResponse<byte[]> response = future.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (response.body().length > MAX_RESPONSE_SIZE) {
                throw new IOException("JSON-LD context document exceeds " + MAX_RESPONSE_SIZE + " bytes: " + targetUri);
            }
            return new Response(response);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IOException("Timed out fetching JSON-LD context document: " + targetUri, e);
        } catch (ExecutionException e) {
            throw new IOException("Failed to fetch JSON-LD context document: " + targetUri, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching JSON-LD context document: " + targetUri, e);
        }
    }

    /**
     * JDK-backed HTTP response exposing only the information needed by the JSON-LD loader.
     */
    static final class Response {

        private final HttpResponse<byte[]> delegate;

        Response(HttpResponse<byte[]> delegate) {
            this.delegate = delegate;
        }

        int statusCode() {
            return delegate.statusCode();
        }

        InputStream body() {
            return new ByteArrayInputStream(delegate.body());
        }

        Collection<String> links() {
            return delegate.headers().map().get("link");
        }

        Optional<String> contentType() {
            return delegate.headers().firstValue("content-type");
        }

        Optional<String> location() {
            return delegate.headers().firstValue("location");
        }

        void close() {
            // The body is fully buffered and has no resources to release.
        }
    }
}
