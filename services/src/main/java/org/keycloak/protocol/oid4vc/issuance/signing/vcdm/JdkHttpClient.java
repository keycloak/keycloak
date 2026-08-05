package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

/**
 * Thin wrapper around the JDK HTTP client used to fetch JSON-LD context documents.
 * <p>
 * It isolates the {@code java.net.http} types so that the JSON-LD loader can implement the
 * titanium interfaces ({@code com.apicatalog.jsonld.http.HttpClient}/{@code HttpResponse})
 * without a naming collision, which would otherwise force fully qualified names.
 */
final class JdkHttpClient {

    private final HttpClient client;
    private final Duration requestTimeout;

    JdkHttpClient(Duration connectTimeout, Duration requestTimeout) {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(connectTimeout)
                .build();
        this.requestTimeout = requestTimeout;
    }

    Response send(URI targetUri, String acceptHeader) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(targetUri)
                .header("Accept", acceptHeader)
                .timeout(requestTimeout)
                .build();
        return new Response(client.send(request, HttpResponse.BodyHandlers.ofInputStream()));
    }

    /**
     * JDK-backed HTTP response exposing only the information needed by the JSON-LD loader.
     */
    static final class Response {

        private final HttpResponse<InputStream> delegate;

        Response(HttpResponse<InputStream> delegate) {
            this.delegate = delegate;
        }

        int statusCode() {
            return delegate.statusCode();
        }

        InputStream body() {
            return delegate.body();
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
            // The body stream is consumed and closed by the document reader.
        }
    }
}
