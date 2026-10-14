package org.keycloak.quarkus.runtime.httpclient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.SocketAddress;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.DnsResolver;
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
    private final DnsResolver directResolver;

    VertxHttpClientBridge(HttpClient httpClient, VertxHttpClientProvider provider) {
        this(httpClient, provider, null);
    }

    /**
     * @param directResolver when set, requests resolve their host with it and connect to the
     *                       resolved address directly, without proxy or redirects
     */
    VertxHttpClientBridge(HttpClient httpClient, VertxHttpClientProvider provider, DnsResolver directResolver) {
        this.httpClient = httpClient;
        this.provider = provider;
        this.directResolver = directResolver;
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
        if (context != null) {
            throw new UnsupportedOperationException(
                    "HttpContext is not supported by the Vert.x HTTP client bridge (HTTP_CLIENT_V2). "
                    + "Remove the HttpContext parameter or switch back to the Apache HTTP client (HTTP_CLIENT v1).");
        }
        URI uri = resolveUri(target, request);
        // Resolved once, outside the retry loop: a rejected or unknown host is not retried and
        // keeps its exception type, as with the Apache client.
        if (directResolver == null) {
            return provider.executeWithRetry(() -> doExecuteInternal(uri, null, request));
        }
        List<SocketAddress> servers = resolveDirectServers(uri);
        return provider.executeWithRetry(() -> executeWithFailover(uri, servers, request));
    }

    private List<SocketAddress> resolveDirectServers(URI uri) throws IOException {
        InetAddress[] addresses = directResolver.resolve(uri.getHost());
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException(uri.getHost());
        }
        int port = uri.getPort() != -1 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        List<SocketAddress> servers = new ArrayList<>(addresses.length);
        for (InetAddress address : addresses) {
            servers.add(SocketAddress.inetSocketAddress(port, address.getHostAddress()));
        }
        return servers;
    }

    /**
     * Tries each resolved address in turn, like the Apache client, moving on only when the
     * connection itself fails, so nothing has been sent to the previous address.
     */
    private CloseableHttpResponse executeWithFailover(URI uri, List<SocketAddress> servers, HttpRequest request) throws IOException {
        IOException lastFailure = null;
        for (SocketAddress server : servers) {
            try {
                return doExecuteInternal(uri, server, request);
            } catch (ConnectException | NoRouteToHostException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }

    private CloseableHttpResponse doExecuteInternal(URI uri, SocketAddress server, HttpRequest request) throws IOException {
        String method = request.getRequestLine().getMethod();
        long maxSize = provider.getMaxConsumedResponseSize();

        long idleTimeoutMs = provider.getEffectiveTimeoutMs();
        long connectTimeoutMs = -1;
        if (request instanceof HttpRequestBase) {
            RequestConfig rc = ((HttpRequestBase) request).getConfig();
            if (rc != null) {
                int socketTimeout = rc.getSocketTimeout();
                if (socketTimeout > 0) {
                    idleTimeoutMs = socketTimeout;
                }
                int connRequestTimeout = rc.getConnectionRequestTimeout();
                if (connRequestTimeout > 0) {
                    connectTimeoutMs = connRequestTimeout;
                }
            }
        }

        Buffer bodyBuffer = null;
        Header entityContentType = null;
        Header entityContentEncoding = null;
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (entity != null) {
                bodyBuffer = readEntity(entity);
                entityContentType = entity.getContentType();
                entityContentEncoding = entity.getContentEncoding();
            }
        }

        RequestOptions reqOptions = new RequestOptions()
                .setMethod(HttpMethod.valueOf(method))
                .setAbsoluteURI(uri.toString())
                .setIdleTimeout(idleTimeoutMs);
        if (connectTimeoutMs > 0) {
            reqOptions.setConnectTimeout(connectTimeoutMs);
        }
        if (server != null) {
            // The URI host still drives the Host header, SNI and certificate verification.
            reqOptions.setServer(server).setFollowRedirects(false);
        } else {
            ProxyOptions proxy = provider.resolveProxyForHost(uri.getHost());
            if (proxy != null) {
                reqOptions.setProxyOptions(proxy);
            }
        }

        CompletableFuture<CloseableHttpResponse> future = new CompletableFuture<>();
        AtomicReference<HttpClientRequest> requestRef = new AtomicReference<>();
        Buffer sendBody = bodyBuffer;
        Header sendContentType = entityContentType;
        Header sendContentEncoding = entityContentEncoding;

        httpClient.request(reqOptions).onComplete(reqAr -> {
            if (reqAr.failed()) {
                future.completeExceptionally(reqAr.cause());
                return;
            }

            HttpClientRequest clientReq = reqAr.result();
            requestRef.set(clientReq);
            if (future.isDone()) {
                clientReq.reset();
                return;
            }

            for (Header header : request.getAllHeaders()) {
                clientReq.headers().add(header.getName(), header.getValue());
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
                            future.completeExceptionally(new VertxHttpClientProvider.NonRetryableIOException(
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

        try {
            return VertxHttpClientProvider.awaitResult(future, idleTimeoutMs);
        } catch (IOException e) {
            future.completeExceptionally(e);
            HttpClientRequest req = requestRef.get();
            if (req != null) {
                req.reset();
            }
            throw e;
        }
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
