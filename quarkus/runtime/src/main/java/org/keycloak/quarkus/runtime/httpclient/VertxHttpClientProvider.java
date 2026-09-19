package org.keycloak.quarkus.runtime.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.connections.httpclient.ProxyMappings;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
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
    private final ProxyMappings proxyMappings;

    VertxHttpClientProvider(WebClient webClient, HttpClient httpClient,
                            long maxConsumedResponseSize, long socketTimeoutMs, int maxRetries,
                            long initialBackoffMillis, double backoffMultiplier, boolean useJitter, double jitterFactor,
                            ProxyMappings proxyMappings) {
        this.webClient = webClient;
        this.httpClient = httpClient;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        this.socketTimeoutMs = socketTimeoutMs;
        this.maxRetries = maxRetries;
        this.initialBackoffMillis = initialBackoffMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.useJitter = useJitter;
        this.jitterFactor = jitterFactor;
        this.proxyMappings = proxyMappings;
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
            var req = webClient.postAbs(uri)
                    .as(BodyCodec.none())
                    .putHeader("Content-Type", "text/plain; charset=ISO-8859-1");
            long timeout = getEffectiveTimeoutMs();
            if (timeout > 0) {
                req.timeout(timeout);
            }
            ProxyOptions proxy = resolveProxy(uri);
            if (proxy != null) {
                req.proxy(proxy);
            }
            req.sendBuffer(Buffer.buffer(text, "ISO-8859-1"))
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
        return doGet(uri, null, (resp, future) -> {
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
                    try {
                        if (accumulated.length() == 0) {
                            future.completeExceptionally(
                                    new NonRetryableIOException("No content returned from HTTP call"));
                        } else {
                            String charset = extractCharset(contentType);
                            future.complete(charset != null
                                    ? accumulated.toString(charset) : accumulated.toString());
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
            });

            resp.exceptionHandler(ex -> {
                if (!aborted.get()) {
                    future.completeExceptionally(ex);
                }
            });
        });
    }

    @Override
    public InputStream getInputStream(String uri) throws IOException {
        return getInputStream(uri, null);
    }

    @Override
    public InputStream getInputStream(String uri, Map<String, String> headers) throws IOException {
        return doGet(uri, headers, (resp, future) -> {
            resp.pause();
            future.complete(new ChunkedInputStream(resp));
        });
    }

    private <T> T doGet(String uri, Map<String, String> headers,
                         BiConsumer<HttpClientResponse, CompletableFuture<T>> responseHandler) throws IOException {
        return executeWithRetry(() -> {
            CompletableFuture<T> future = new CompletableFuture<>();
            RequestOptions reqOptions = new RequestOptions()
                    .setMethod(HttpMethod.GET)
                    .setAbsoluteURI(uri)
                    .setIdleTimeout((int) getEffectiveTimeoutMs());
            ProxyOptions proxy = resolveProxy(uri);
            if (proxy != null) {
                reqOptions.setProxyOptions(proxy);
            }

            httpClient.request(reqOptions).onComplete(reqAr -> {
                if (reqAr.failed()) {
                    future.completeExceptionally(reqAr.cause());
                    return;
                }

                var clientReq = reqAr.result();
                if (headers != null) {
                    headers.forEach(clientReq::putHeader);
                }

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

                    responseHandler.accept(resp, future);
                });

                clientReq.end();
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
            long timeout = getEffectiveTimeoutMs();
            if (timeout > 0) {
                req.timeout(timeout);
            }
            ProxyOptions proxy = resolveProxy(uri);
            if (proxy != null) {
                req.proxy(proxy);
            }
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
        return awaitResult(future, getEffectiveTimeoutMs());
    }

    long getEffectiveTimeoutMs() {
        return socketTimeoutMs > 0
                ? Math.max(socketTimeoutMs, DEFAULT_TIMEOUT_SECONDS * 1000)
                : 0;
    }

    ProxyOptions resolveProxy(String uri) {
        if (proxyMappings == null) {
            return null;
        }
        try {
            String hostname = new URI(uri).getHost();
            return resolveProxyForHost(hostname);
        } catch (URISyntaxException e) {
            logger.debugf("Cannot resolve proxy for URI '%s': %s", uri, e.getMessage());
            return null;
        }
    }

    ProxyOptions resolveProxyForHost(String hostname) {
        if (proxyMappings == null || hostname == null) {
            return null;
        }
        ProxyMappings.ProxyMapping mapping = proxyMappings.getProxyFor(hostname);
        if (mapping == null || mapping.getProxyHost() == null) {
            return null;
        }
        int port = mapping.getProxyHost().getPort();
        if (port <= 0) {
            port = "https".equalsIgnoreCase(mapping.getProxyHost().getSchemeName()) ? 443 : 80;
        }
        ProxyOptions options = new ProxyOptions()
                .setType(ProxyType.HTTP)
                .setHost(mapping.getProxyHost().getHostName())
                .setPort(port);
        if (mapping.getProxyCredentials() != null) {
            options.setUsername(mapping.getProxyCredentials().getUserName())
                    .setPassword(mapping.getProxyCredentials().getPassword());
        }
        return options;
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

    // TODO replace with ReadStream.blockingStream() when migrating to Vert.x 5
    static class ChunkedInputStream extends InputStream {
        private static final Buffer END = Buffer.buffer(0);
        private final HttpClientResponse response;
        private final BlockingQueue<Buffer> chunks = new LinkedBlockingQueue<>();
        private volatile Throwable error;
        private byte[] current;
        private int pos;
        private volatile boolean ended;

        ChunkedInputStream(HttpClientResponse response) {
            this.response = response;
            response.handler(chunks::add);
            response.endHandler(v -> chunks.add(END));
            response.exceptionHandler(ex -> {
                error = ex;
                chunks.add(END);
            });
        }

        @Override
        public int read() throws IOException {
            if (ended) return -1;
            if (current == null || pos >= current.length) {
                if (!advance()) return -1;
            }
            return current[pos++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) return 0;
            if (ended) return -1;
            if (current == null || pos >= current.length) {
                if (!advance()) return -1;
            }
            int n = Math.min(len, current.length - pos);
            System.arraycopy(current, pos, b, off, n);
            pos += n;
            return n;
        }

        private boolean advance() throws IOException {
            try {
                while (true) {
                    response.fetch(1);
                    Buffer buf = chunks.take();
                    if (buf == END) {
                        ended = true;
                        if (error != null) {
                            throw new IOException("Error reading response", error);
                        }
                        return false;
                    }
                    current = buf.getBytes();
                    pos = 0;
                    if (current.length > 0) {
                        return true;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while reading response", e);
            }
        }

        @Override
        public void close() throws IOException {
            if (!ended) {
                ended = true;
                response.request().reset();
            }
            chunks.clear();
            chunks.add(END);
        }
    }
}
