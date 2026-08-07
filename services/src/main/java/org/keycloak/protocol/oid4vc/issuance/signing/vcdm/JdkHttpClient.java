package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
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
        // (headers and body) is awaited with a hard deadline and cancelled on expiry. The body
        // is buffered with a size limit that aborts the exchange as soon as it is exceeded, so
        // an oversized response cannot exhaust the server heap.
        CompletableFuture<HttpResponse<byte[]>> future =
                client.sendAsync(request, boundedBodyHandler(MAX_RESPONSE_SIZE));
        try {
            return new Response(future.get(requestTimeout.toMillis(), TimeUnit.MILLISECONDS));
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

    private static HttpResponse.BodyHandler<byte[]> boundedBodyHandler(int maxSize) {
        return responseInfo -> boundedBodySubscriber(maxSize);
    }

    private static HttpResponse.BodySubscriber<byte[]> boundedBodySubscriber(int maxSize) {
        return new HttpResponse.BodySubscriber<>() {
            private final CompletableFuture<byte[]> body = new CompletableFuture<>();
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            private volatile Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(List<ByteBuffer> items) {
                for (ByteBuffer item : items) {
                    int remaining = item.remaining();
                    if (buffer.size() + remaining > maxSize) {
                        subscription.cancel();
                        body.completeExceptionally(new IOException("JSON-LD context document exceeds " + maxSize + " bytes"));
                        return;
                    }
                    byte[] chunk = new byte[remaining];
                    item.get(chunk);
                    buffer.writeBytes(chunk);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                body.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                body.complete(buffer.toByteArray());
            }

            @Override
            public CompletionStage<byte[]> getBody() {
                return body;
            }
        };
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
