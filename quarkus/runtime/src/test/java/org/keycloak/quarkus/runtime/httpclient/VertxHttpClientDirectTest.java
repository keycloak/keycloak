/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.quarkus.runtime.httpclient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.connections.httpclient.ProxyMappings;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.DnsResolver;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

/**
 * Tests for {@link VertxHttpClientProvider#createHttpClient(DnsResolver, boolean)}: direct requests
 * connect to the address the resolver returns, while TLS still verifies the URL's hostname; requests
 * go through the proxy only when it is enabled and the proxy settings route them to it; redirects
 * are not followed.
 */
public class VertxHttpClientDirectTest {

    private static final String HOST = "push.example.test";

    private Vertx vertx;
    private final AtomicInteger requests = new AtomicInteger();

    @Before
    public void startVertx() {
        vertx = Vertx.vertx();
        // Proxy mappings are cached by host name across instances.
        ProxyMappings.clearCache();
    }

    @After
    public void stopVertx() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    public void connectsToResolvedAddress() throws Exception {
        int port = startServer(new HttpServerOptions());
        try (CloseableHttpClient client = directClient(new WebClientOptions(), loopback());
             CloseableHttpResponse res = client.execute(new HttpGet("http://" + HOST + ":" + port + "/hello"))) {
            assertEquals(200, res.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void failsOverToNextAddressWhenConnectionFails() throws Exception {
        int port = startServer(new HttpServerOptions());
        try (CloseableHttpClient client = directClient(new WebClientOptions(), refusedThenLoopback());
             CloseableHttpResponse res = client.execute(new HttpGet("http://" + HOST + ":" + port + "/hello"))) {
            assertEquals(200, res.getStatusLine().getStatusCode());
        }
        assertEquals(1, requests.get());
    }

    @Test
    public void sendsOneShotBodyIntactAfterFailover() throws Exception {
        int port = startServer(new HttpServerOptions());
        HttpPost post = new HttpPost("http://" + HOST + ":" + port + "/echo");
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream("signed-set".getBytes(StandardCharsets.UTF_8))));
        try (CloseableHttpClient client = directClient(new WebClientOptions(), refusedThenLoopback());
             CloseableHttpResponse res = client.execute(post)) {
            assertEquals(200, res.getStatusLine().getStatusCode());
            assertEquals("signed-set", EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void failsOverWhenConnectingTimesOut() throws Exception {
        // 127.0.0.2 accepts TCP connections but never answers, so the TLS handshake hangs there.
        try (ServerSocket silent = new ServerSocket()) {
            silent.bind(new InetSocketAddress("127.0.0.2", 0));
            SelfSignedCertificate certificate = SelfSignedCertificate.create(HOST);
            int port = startServer(new HttpServerOptions().setSsl(true).setKeyCertOptions(certificate.keyCertOptions()),
                    silent.getLocalPort());
            WebClientOptions options = new WebClientOptions().setTrustOptions(certificate.trustOptions()).setVerifyHost(true);

            HttpGet get = new HttpGet("https://" + HOST + ":" + port + "/hello");
            // A socket timeout shorter than the connect timeout must not cut the connection wait short.
            get.setConfig(RequestConfig.custom().setConnectionRequestTimeout(1000).setSocketTimeout(300).build());
            DnsResolver silentThenLoopback = host -> new InetAddress[]{
                    InetAddress.getByName("127.0.0.2"), InetAddress.getByName("127.0.0.1")};
            try (CloseableHttpClient client = directClient(options, silentThenLoopback);
                 CloseableHttpResponse res = client.execute(get)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void connectionCompletingAsWaitExpiresIsNotFailedOver() throws Exception {
        // The connection callback completes `connected` just as the wait times out.
        CompletableFuture<Void> connected = new CompletableFuture<>() {
            @Override
            public Void get(long timeout, TimeUnit unit) throws TimeoutException {
                complete(null);
                throw new TimeoutException();
            }
        };
        // Must not report a connection failure: the request may already be sent on this connection.
        VertxHttpClientBridge.awaitConnection(connected, 100, SocketAddress.inetSocketAddress(443, "127.0.0.1"));
    }

    @Test
    public void connectionTimeoutStopsLateConnectionFromSending() {
        CompletableFuture<Void> connected = new CompletableFuture<>();
        assertThrows(ConnectException.class, () -> VertxHttpClientBridge.awaitConnection(connected, 50,
                SocketAddress.inetSocketAddress(443, "127.0.0.1")));
        // The connection callback only sends after completing `connected` itself.
        assertFalse(connected.complete(null));
    }

    @Test
    public void anyConnectionFailureIsReportedAsConnectFailure() {
        // Vert.x reports its own connection timeout with an exception that is not an IOException.
        CompletableFuture<Void> connected = new CompletableFuture<>();
        connected.completeExceptionally(new IllegalStateException("timeout getting a connection"));
        assertThrows(ConnectException.class, () -> VertxHttpClientBridge.awaitConnection(connected, 50,
                SocketAddress.inetSocketAddress(443, "127.0.0.1")));
    }

    @Test
    public void doesNotFollowRedirects() throws Exception {
        int port = startServer(new HttpServerOptions());
        try (CloseableHttpClient client = directClient(new WebClientOptions().setFollowRedirects(true), loopback());
             CloseableHttpResponse res = client.execute(new HttpGet("http://" + HOST + ":" + port + "/redirect"))) {
            assertEquals(302, res.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void rejectedResolutionOpensNoConnection() throws Exception {
        int port = startServer(new HttpServerOptions());
        try (CloseableHttpClient client = directClient(new WebClientOptions(), rejecting())) {
            assertThrows(UnknownHostException.class,
                    () -> client.execute(new HttpGet("http://" + HOST + ":" + port + "/hello")));
        }
        assertEquals(0, requests.get());
    }

    @Test
    public void withProxySendsProxiedRequestsThroughProxy() throws Exception {
        // The test server stands in for a proxy on a private address; the target never resolves.
        int port = startServer(new HttpServerOptions());
        ProxyMappings proxyMappings = ProxyMappings.valueOf(".*proxied.*;http://127.0.0.1:" + port);
        try (CloseableHttpClient client = restrictedClient(new WebClientOptions(), rejecting(), proxyMappings, true);
             CloseableHttpResponse res = client.execute(new HttpGet("http://push.proxied.test/hello"))) {
            assertEquals(200, res.getStatusLine().getStatusCode());
        }
        assertEquals(1, requests.get());
    }

    @Test
    public void withProxyChecksRequestsExcludedFromProxy() throws Exception {
        int port = startServer(new HttpServerOptions());
        ProxyMappings proxyMappings = ProxyMappings.valueOf(".*proxied.*;http://127.0.0.1:" + port);
        try (CloseableHttpClient client = restrictedClient(new WebClientOptions(), rejecting(), proxyMappings, true)) {
            assertThrows(UnknownHostException.class,
                    () -> client.execute(new HttpGet("http://direct.example.test:" + port + "/hello")));
        }
        assertEquals(0, requests.get());
    }

    @Test
    public void tlsVerifiesUrlHostnameNotPinnedAddress() throws Exception {
        SelfSignedCertificate certificate = SelfSignedCertificate.create(HOST);
        int port = startServer(new HttpServerOptions().setSsl(true).setKeyCertOptions(certificate.keyCertOptions()));
        WebClientOptions options = new WebClientOptions().setTrustOptions(certificate.trustOptions()).setVerifyHost(true);

        // The certificate names HOST only, so this succeeds only if TLS checks HOST, not 127.0.0.1.
        try (CloseableHttpClient client = directClient(options, loopback());
             CloseableHttpResponse res = client.execute(new HttpGet("https://" + HOST + ":" + port + "/hello"))) {
            assertEquals(200, res.getStatusLine().getStatusCode());
        }

        // Hostname verification is still on: another name pinned to the same address fails.
        try (CloseableHttpClient client = directClient(options, loopback())) {
            assertThrows(IOException.class,
                    () -> client.execute(new HttpGet("https://other.example.test:" + port + "/hello")));
        }
    }

    private CloseableHttpClient directClient(WebClientOptions options, DnsResolver resolver) {
        // A proxy mapping to an unreachable port: any request routed through it would fail.
        return restrictedClient(options, resolver, ProxyMappings.valueOf(".*;http://127.0.0.1:1"), false);
    }

    private CloseableHttpClient restrictedClient(WebClientOptions options, DnsResolver resolver,
                                                 ProxyMappings proxyMappings, boolean useProxy) {
        HttpClient httpClient = vertx.createHttpClient(options);
        VertxHttpClientProvider provider = new VertxHttpClientProvider(WebClient.wrap(httpClient, options), httpClient,
                10_000_000L, 5000L, 0, 1000L, 2.0, false, 0.5, proxyMappings);
        return provider.createHttpClient(resolver, useProxy);
    }

    private static DnsResolver rejecting() {
        return host -> {
            throw new UnknownHostException("rejected " + host);
        };
    }

    private int startServer(HttpServerOptions options) throws Exception {
        return startServer(options, 0);
    }

    private int startServer(HttpServerOptions options, int port) throws Exception {
        HttpServer server = vertx.createHttpServer(options).requestHandler(req -> {
            requests.incrementAndGet();
            if ("/redirect".equals(req.path())) {
                req.response().setStatusCode(302).putHeader("Location", "/hello").end();
            } else if ("/echo".equals(req.path())) {
                req.body().onSuccess(received -> req.response().end(received));
            } else {
                req.response().end("Hello world!");
            }
        });
        return server.listen(port, "127.0.0.1").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();
    }

    private static DnsResolver refusedThenLoopback() {
        // Nothing listens on 127.0.0.2, so connecting there is refused.
        return host -> new InetAddress[]{InetAddress.getByName("127.0.0.2"), InetAddress.getByName("127.0.0.1")};
    }

    private static DnsResolver loopback() {
        return host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")};
    }
}
