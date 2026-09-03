package org.keycloak.quarkus.runtime.httpclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.keycloak.connections.httpclient.HttpClientProvider;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;

public class VertxHttpClientProvider implements HttpClientProvider {

    private static final Logger logger = Logger.getLogger(VertxHttpClientProvider.class);
    static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private final WebClient webClient;
    private final HttpClient httpClient;
    private final CloseableHttpClient bridge;
    private final long maxConsumedResponseSize;
    private final long socketTimeoutMs;
    private final int maxRetries;
    private final long initialBackoffMillis;
    private final double backoffMultiplier;
    private final boolean useJitter;
    private final double jitterFactor;

    VertxHttpClientProvider(WebClient webClient, HttpClient httpClient,
                            long maxConsumedResponseSize, long socketTimeoutMs, int maxRetries,
                            long initialBackoffMillis, double backoffMultiplier, boolean useJitter, double jitterFactor) {
        this.webClient = webClient;
        this.httpClient = httpClient;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        this.socketTimeoutMs = socketTimeoutMs;
        this.maxRetries = maxRetries;
        this.initialBackoffMillis = initialBackoffMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.useJitter = useJitter;
        this.jitterFactor = jitterFactor;
        this.bridge = new VertxHttpClientBridge(httpClient, this);
    }

    @Override
    public CloseableHttpClient getHttpClient() {
        return bridge;
    }

    @Override
    public int postText(String uri, String text) throws IOException {
        return executeWithRetry(() -> {
            CompletableFuture<Integer> future = new CompletableFuture<>();
            webClient.postAbs(uri)
                    .as(BodyCodec.none())
                    .putHeader("Content-Type", "text/plain; charset=ISO-8859-1")
                    .sendBuffer(Buffer.buffer(text, "ISO-8859-1"))
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            future.complete(ar.result().statusCode());
                        } else {
                            future.completeExceptionally(ar.cause());
                        }
                    });
            return awaitResult(future);
        });
    }

    @Override
    public String getString(String uri) throws IOException {
        return executeWithRetry(() -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            RequestOptions reqOptions = new RequestOptions()
                    .setMethod(HttpMethod.GET)
                    .setAbsoluteURI(uri)
                    .setTimeout(getEffectiveTimeoutMs());

            httpClient.request(reqOptions).onComplete(reqAr -> {
                if (reqAr.failed()) {
                    future.completeExceptionally(reqAr.cause());
                    return;
                }

                var clientReq = reqAr.result();
                clientReq.response().onComplete(respAr -> {
                    if (respAr.failed()) {
                        future.completeExceptionally(respAr.cause());
                        return;
                    }

                    var resp = respAr.result();
                    int statusCode = resp.statusCode();
                    if (statusCode < 200 || statusCode >= 300) {
                        resp.request().reset();
                        future.completeExceptionally(new NonRetryableIOException(
                                "Unexpected HTTP status: " + statusCode + " " + resp.statusMessage()));
                        return;
                    }

                    String contentType = resp.getHeader("Content-Type");
                    Buffer accumulated = Buffer.buffer();
                    AtomicLong bytesReceived = new AtomicLong();
                    AtomicBoolean aborted = new AtomicBoolean();

                    resp.handler(chunk -> {
                        long total = bytesReceived.addAndGet(chunk.length());
                        if (total > maxConsumedResponseSize) {
                            if (aborted.compareAndSet(false, true)) {
                                resp.request().reset();
                                future.completeExceptionally(new NonRetryableIOException(
                                        "Response size " + total + " exceeds limit of " + maxConsumedResponseSize));
                            }
                        } else {
                            accumulated.appendBuffer(chunk);
                        }
                    });

                    resp.endHandler(v -> {
                        if (!aborted.get()) {
                            if (accumulated.length() == 0) {
                                future.completeExceptionally(
                                        new NonRetryableIOException("No content returned from HTTP call"));
                            } else {
                                String charset = extractCharset(contentType);
                                future.complete(charset != null
                                        ? accumulated.toString(charset) : accumulated.toString());
                            }
                        }
                    });

                    resp.exceptionHandler(ex -> {
                        if (!aborted.get()) {
                            future.completeExceptionally(ex);
                        }
                    });
                });

                clientReq.end();
            });

            return awaitResult(future);
        });
    }

    @Override
    public InputStream getInputStream(String uri) throws IOException {
        return new ByteArrayInputStream(doGet(uri, null).body().getBytes());
    }

    @Override
    public InputStream getInputStream(String uri, Map<String, String> headers) throws IOException {
        return new ByteArrayInputStream(doGet(uri, headers).body().getBytes());
    }

    private HttpResponse<Buffer> doGet(String uri, Map<String, String> headers) throws IOException {
        return executeWithRetry(() -> {
            CompletableFuture<HttpResponse<Buffer>> future = new CompletableFuture<>();
            var req = webClient.getAbs(uri);
            if (headers != null) {
                headers.forEach(req::putHeader);
            }
            req.send().onComplete(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Buffer body = response.body();
                        if (body == null || body.length() == 0) {
                            future.completeExceptionally(new NonRetryableIOException("No content returned from HTTP call"));
                        } else {
                            future.complete(response);
                        }
                    } else {
                        future.completeExceptionally(new NonRetryableIOException(
                                "Unexpected HTTP status: " + response.statusCode() + " " + response.statusMessage()));
                    }
                } else {
                    future.completeExceptionally(ar.cause());
                }
            });
            return awaitResult(future);
        });
    }

    private static String extractCharset(String contentType) {
        if (contentType == null) return null;
        for (String param : contentType.split(";")) {
            param = param.trim();
            if (param.regionMatches(true, 0, "charset=", 0, "charset=".length())) {
                String value = param.substring("charset=".length()).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }

    @Override
    public byte[] postBinary(String uri, byte[] body, Map<String, String> headers) throws IOException {
        return executeWithRetry(() -> {
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            var req = webClient.postAbs(uri);
            if (headers != null) {
                headers.forEach(req::putHeader);
            }
            req.sendBuffer(Buffer.buffer(body)).onComplete(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Buffer b = response.body();
                        if (b == null || b.length() == 0) {
                            future.completeExceptionally(new NonRetryableIOException("No content returned from HTTP call"));
                        } else {
                            future.complete(b.getBytes());
                        }
                    } else {
                        future.completeExceptionally(new NonRetryableIOException(
                                "HTTP " + response.statusCode() + " from " + uri));
                    }
                } else {
                    future.completeExceptionally(ar.cause());
                }
            });
            return awaitResult(future);
        });
    }

    @Override
    public long getMaxConsumedResponseSize() {
        return maxConsumedResponseSize;
    }

    @Override
    public void close() {
    }

    <T> T executeWithRetry(RetryableOperation<T> operation) throws IOException {
        if (maxRetries <= 0) {
            return operation.execute();
        }

        IOException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (NonRetryableIOException e) {
                throw e;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long baseDelay = (long) Math.min(
                            initialBackoffMillis * Math.pow(backoffMultiplier, attempt), 60_000.0);
                    long delay = baseDelay;
                    if (useJitter) {
                        double jitter = 1.0 - jitterFactor + (ThreadLocalRandom.current().nextDouble() * jitterFactor * 2.0);
                        delay = Math.max(0, (long) (baseDelay * jitter));
                    }
                    logger.debugf("HTTP request failed (attempt %d/%d), retrying in %dms: %s",
                            attempt + 1, maxRetries, delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                }
            }
        }
        throw lastException;
    }

    <T> T awaitResult(CompletableFuture<T> future) throws IOException {
        long timeoutMs = socketTimeoutMs > 0
                ? Math.max(socketTimeoutMs, DEFAULT_TIMEOUT_SECONDS * 1000)
                : 0;
        return awaitResult(future, timeoutMs);
    }

    long getEffectiveTimeoutMs() {
        return socketTimeoutMs > 0
                ? Math.max(socketTimeoutMs, DEFAULT_TIMEOUT_SECONDS * 1000)
                : 0;
    }

    static <T> T awaitResult(CompletableFuture<T> future, long timeoutMs) throws IOException {
        try {
            if (timeoutMs <= 0) {
                return future.get();
            }
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("HTTP request interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("HTTP request failed", cause);
        } catch (TimeoutException e) {
            throw new IOException("HTTP request timed out", e);
        }
    }

    static class NonRetryableIOException extends IOException {
        NonRetryableIOException(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    interface RetryableOperation<T> {
        T execute() throws IOException;
    }
}
