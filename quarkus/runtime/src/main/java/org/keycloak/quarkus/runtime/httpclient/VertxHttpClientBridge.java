package org.keycloak.quarkus.runtime.httpclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class VertxHttpClientBridge extends CloseableHttpClient {

    private final HttpClient httpClient;
    private final VertxHttpClientProvider provider;

    VertxHttpClientBridge(HttpClient httpClient, VertxHttpClientProvider provider) {
        this.httpClient = httpClient;
        this.provider = provider;
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        return provider.executeWithRetry(() -> doExecuteInternal(target, request));
    }

    private CloseableHttpResponse doExecuteInternal(HttpHost target, HttpRequest request) throws IOException {
        URI uri = resolveUri(target, request);
        String method = request.getRequestLine().getMethod();
        long maxSize = provider.getMaxConsumedResponseSize();

        long timeoutMs = VertxHttpClientProvider.DEFAULT_TIMEOUT_SECONDS * 1000;
        if (request instanceof HttpRequestBase) {
            RequestConfig rc = ((HttpRequestBase) request).getConfig();
            if (rc != null) {
                int socketTimeout = rc.getSocketTimeout();
                int connRequestTimeout = rc.getConnectionRequestTimeout();
                int effectiveTimeout = Integer.MAX_VALUE;
                if (socketTimeout > 0) {
                    effectiveTimeout = socketTimeout;
                }
                if (connRequestTimeout > 0) {
                    effectiveTimeout = Math.min(effectiveTimeout, connRequestTimeout);
                }
                if (effectiveTimeout < Integer.MAX_VALUE) {
                    timeoutMs = effectiveTimeout;
                }
            }
        }

        Buffer bodyBuffer = null;
        Header entityContentType = null;
        Header entityContentEncoding = null;
        if (request instanceof HttpEntityEnclosingRequestBase) {
            HttpEntity entity = ((HttpEntityEnclosingRequestBase) request).getEntity();
            if (entity != null) {
                bodyBuffer = readEntity(entity);
                entityContentType = entity.getContentType();
                entityContentEncoding = entity.getContentEncoding();
            }
        }

        RequestOptions reqOptions = new RequestOptions()
                .setMethod(HttpMethod.valueOf(method))
                .setAbsoluteURI(uri.toString())
                .setTimeout(timeoutMs);

        CompletableFuture<CloseableHttpResponse> future = new CompletableFuture<>();
        Buffer sendBody = bodyBuffer;
        Header sendContentType = entityContentType;
        Header sendContentEncoding = entityContentEncoding;

        httpClient.request(reqOptions).onComplete(reqAr -> {
            if (reqAr.failed()) {
                future.completeExceptionally(reqAr.cause());
                return;
            }

            HttpClientRequest clientReq = reqAr.result();

            for (Header header : request.getAllHeaders()) {
                clientReq.putHeader(header.getName(), header.getValue());
            }

            if (sendContentType != null && !request.containsHeader(sendContentType.getName())) {
                clientReq.putHeader(sendContentType.getName(), sendContentType.getValue());
            }
            if (sendContentEncoding != null && !request.containsHeader(sendContentEncoding.getName())) {
                clientReq.putHeader(sendContentEncoding.getName(), sendContentEncoding.getValue());
            }

            clientReq.response().onComplete(respAr -> {
                if (respAr.failed()) {
                    future.completeExceptionally(respAr.cause());
                    return;
                }

                HttpClientResponse resp = respAr.result();
                Buffer accumulated = Buffer.buffer();
                AtomicLong bytesReceived = new AtomicLong();
                AtomicBoolean aborted = new AtomicBoolean();

                resp.handler(chunk -> {
                    long total = bytesReceived.addAndGet(chunk.length());
                    if (total > maxSize) {
                        if (aborted.compareAndSet(false, true)) {
                            resp.request().reset();
                            future.completeExceptionally(new IOException(
                                    "Response size exceeds limit of " + maxSize + " bytes"));
                        }
                    } else {
                        accumulated.appendBuffer(chunk);
                    }
                });

                resp.endHandler(v -> {
                    if (!aborted.get()) {
                        future.complete(toApacheResponse(resp, accumulated));
                    }
                });

                resp.exceptionHandler(ex -> {
                    if (!aborted.get()) {
                        future.completeExceptionally(ex);
                    }
                });
            });

            if (sendBody != null) {
                clientReq.end(sendBody);
            } else {
                clientReq.end();
            }
        });

        return VertxHttpClientProvider.awaitResult(future, timeoutMs);
    }

    @Override
    public void close() {
    }

    @Override
    @Deprecated
    public HttpParams getParams() {
        return null;
    }

    @Override
    @Deprecated
    public ClientConnectionManager getConnectionManager() {
        return null;
    }

    private URI resolveUri(HttpHost target, HttpRequest request) throws IOException {
        try {
            URI requestUri = new URI(request.getRequestLine().getUri());
            if (requestUri.isAbsolute()) {
                return requestUri;
            }
            if (target != null) {
                return new URI(target.getSchemeName(), null, target.getHostName(), target.getPort(),
                        requestUri.getPath(), requestUri.getQuery(), requestUri.getFragment());
            }
            return requestUri;
        } catch (URISyntaxException e) {
            throw new IOException("Invalid request URI", e);
        }
    }

    private Buffer readEntity(HttpEntity entity) throws IOException {
        try (InputStream is = entity.getContent()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return Buffer.buffer(baos.toByteArray());
        }
    }

    private CloseableHttpResponse toApacheResponse(HttpClientResponse resp, Buffer body) {
        StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1,
                resp.statusCode(), resp.statusMessage());

        CloseableBasicHttpResponse response = new CloseableBasicHttpResponse(statusLine);

        for (String name : resp.headers().names()) {
            for (String value : resp.headers().getAll(name)) {
                response.addHeader(new BasicHeader(name, value));
            }
        }

        if (body != null && body.length() > 0) {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(body.getBytes()));
            entity.setContentLength(body.length());

            String contentType = resp.getHeader("Content-Type");
            if (contentType != null) {
                entity.setContentType(contentType);
            }
            response.setEntity(entity);
        }

        return response;
    }

    private static class CloseableBasicHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {

        CloseableBasicHttpResponse(StatusLine statusLine) {
            super(statusLine);
        }

        @Override
        public void close() throws IOException {
            HttpEntity entity = getEntity();
            if (entity != null && entity.getContent() != null) {
                entity.getContent().close();
            }
        }
    }
}
