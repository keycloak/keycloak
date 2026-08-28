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

import org.keycloak.connections.httpclient.HttpClientProvider;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.client.predicate.ResponsePredicateResult;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;

public class VertxHttpClientProvider implements HttpClientProvider {

    private static final Logger logger = Logger.getLogger(VertxHttpClientProvider.class);
    static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private final WebClient webClient;
    private final CloseableHttpClient bridge;
    private final long maxConsumedResponseSize;
    private final int maxRetries;
    private final long initialBackoffMillis;
    private final double backoffMultiplier;
    private final boolean useJitter;
    private final double jitterFactor;

    VertxHttpClientProvider(WebClient webClient, long maxConsumedResponseSize, int maxRetries,
                            long initialBackoffMillis, double backoffMultiplier, boolean useJitter, double jitterFactor) {
        this.webClient = webClient;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        this.maxRetries = maxRetries;
        this.initialBackoffMillis = initialBackoffMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.useJitter = useJitter;
        this.jitterFactor = jitterFactor;
        this.bridge = new VertxHttpClientBridge(webClient, this);
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
            webClient.getAbs(uri)
                    .expect(responseSizePredicate())
                    .send()
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            io.vertx.ext.web.client.HttpResponse<Buffer> response = ar.result();
                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                Buffer body = response.body();
                                if (body == null || body.length() == 0) {
                                    future.completeExceptionally(new IOException("No content returned from HTTP call"));
                                } else if (body.length() > maxConsumedResponseSize) {
                                    future.completeExceptionally(new IOException(
                                            "Response size " + body.length() + " exceeds limit of " + maxConsumedResponseSize));
                                } else {
                                    String charset = extractCharset(response.getHeader("Content-Type"));
                                    future.complete(charset != null ? body.toString(charset) : body.toString());
                                }
                            } else {
                                future.completeExceptionally(new IOException(
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
            if (param.toLowerCase().startsWith("charset=")) {
                return param.substring("charset=".length()).trim();
            }
        }
        return null;
    }

    @Override
    public InputStream getInputStream(String uri) throws IOException {
        return new ByteArrayInputStream(fetchBody(uri, null).getBytes());
    }

    @Override
    public InputStream getInputStream(String uri, Map<String, String> headers) throws IOException {
        return new ByteArrayInputStream(fetchBody(uri, headers).getBytes());
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
                    io.vertx.ext.web.client.HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Buffer b = response.body();
                        if (b == null || b.length() == 0) {
                            future.completeExceptionally(new IOException("No content returned from HTTP call"));
                        } else {
                            future.complete(b.getBytes());
                        }
                    } else {
                        future.completeExceptionally(new IOException(
                                "HTTP " + response.statusCode() + " from " + uri));
                    }
                } else {
                    future.completeExceptionally(ar.cause());
                }
            });
            return awaitResult(future);
        });
    }

    private ResponsePredicate responseSizePredicate() {
        return ResponsePredicate.create(response -> {
            String cl = response.getHeader("Content-Length");
            if (cl != null) {
                try {
                    if (Long.parseLong(cl) > maxConsumedResponseSize) {
                        return ResponsePredicateResult.failure(
                                "Response Content-Length " + cl + " exceeds limit of " + maxConsumedResponseSize);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return ResponsePredicateResult.success();
        });
    }

    private Buffer fetchBody(String uri, Map<String, String> headers) throws IOException {
        return executeWithRetry(() -> {
            CompletableFuture<Buffer> future = new CompletableFuture<>();
            var req = webClient.getAbs(uri).expect(responseSizePredicate());
            if (headers != null) {
                headers.forEach(req::putHeader);
            }
            req.send().onComplete(ar -> {
                if (ar.succeeded()) {
                    io.vertx.ext.web.client.HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Buffer body = response.body();
                        if (body == null || body.length() == 0) {
                            future.completeExceptionally(new IOException("No content returned from HTTP call"));
                        } else if (body.length() > maxConsumedResponseSize) {
                            future.completeExceptionally(new IOException(
                                    "Response size " + body.length() + " exceeds limit of " + maxConsumedResponseSize));
                        } else {
                            future.complete(body);
                        }
                    } else {
                        future.completeExceptionally(new IOException(
                                "Unexpected HTTP status: " + response.statusCode() + " " + response.statusMessage()));
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

    static <T> T awaitResult(CompletableFuture<T> future) throws IOException {
        return awaitResult(future, DEFAULT_TIMEOUT_SECONDS * 1000);
    }

    static <T> T awaitResult(CompletableFuture<T> future, long timeoutMs) throws IOException {
        try {
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

    @FunctionalInterface
    interface RetryableOperation<T> {
        T execute() throws IOException;
    }
}
