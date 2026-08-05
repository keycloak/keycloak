package org.keycloak.quarkus.runtime.httpclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
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

    private final WebClient webClient;

    VertxHttpClientBridge(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        URI uri = resolveUri(target, request);
        String method = request.getRequestLine().getMethod();

        io.vertx.ext.web.client.HttpRequest<Buffer> vertxRequest = webClient.requestAbs(
                io.vertx.core.http.HttpMethod.valueOf(method), uri.toString());

        for (Header header : request.getAllHeaders()) {
            vertxRequest.putHeader(header.getName(), header.getValue());
        }

        // Apply per-request timeout from Apache RequestConfig if present
        long timeoutMs = VertxHttpClientProvider.DEFAULT_TIMEOUT_SECONDS * 1000;
        if (request instanceof HttpRequestBase) {
            RequestConfig rc = ((HttpRequestBase) request).getConfig();
            if (rc != null) {
                int socketTimeout = rc.getSocketTimeout();
                if (socketTimeout > 0) {
                    vertxRequest.timeout(socketTimeout);
                    timeoutMs = socketTimeout;
                }
            }
        }

        Buffer bodyBuffer = null;
        if (request instanceof HttpEntityEnclosingRequestBase) {
            HttpEntity entity = ((HttpEntityEnclosingRequestBase) request).getEntity();
            if (entity != null) {
                bodyBuffer = readEntity(entity);
            }
        }

        CompletableFuture<io.vertx.ext.web.client.HttpResponse<Buffer>> future = new CompletableFuture<>();

        if (bodyBuffer != null) {
            vertxRequest.sendBuffer(bodyBuffer).onComplete(ar -> {
                if (ar.succeeded()) future.complete(ar.result());
                else future.completeExceptionally(ar.cause());
            });
        } else {
            vertxRequest.send().onComplete(ar -> {
                if (ar.succeeded()) future.complete(ar.result());
                else future.completeExceptionally(ar.cause());
            });
        }

        io.vertx.ext.web.client.HttpResponse<Buffer> vertxResponse =
                VertxHttpClientProvider.awaitResult(future, timeoutMs);
        return toApacheResponse(vertxResponse);
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

    private CloseableHttpResponse toApacheResponse(io.vertx.ext.web.client.HttpResponse<Buffer> vertxResponse) {
        StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1,
                vertxResponse.statusCode(), vertxResponse.statusMessage());

        CloseableBasicHttpResponse response = new CloseableBasicHttpResponse(statusLine);

        if (vertxResponse.headers() != null) {
            for (String name : vertxResponse.headers().names()) {
                for (String value : vertxResponse.headers().getAll(name)) {
                    response.addHeader(new BasicHeader(name, value));
                }
            }
        }

        Buffer body = vertxResponse.body();
        if (body != null && body.length() > 0) {
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContent(new ByteArrayInputStream(body.getBytes()));
            entity.setContentLength(body.length());

            String contentType = vertxResponse.getHeader("Content-Type");
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
