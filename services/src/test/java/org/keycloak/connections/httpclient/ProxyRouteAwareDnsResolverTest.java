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
package org.keycloak.connections.httpclient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link ProxyRouteAwareDnsResolver}: the proxy of a proxied route is resolved with all of
 * its addresses, so the client fails over between them, while every direct target is checked.
 */
public class ProxyRouteAwareDnsResolverTest {

    private static final String PROXY_HOST = "proxy.example.test";

    private HttpServer proxy;

    @Before
    public void startProxy() throws IOException {
        // Bound to 127.0.0.1 only, so connecting to the same port on 127.0.0.2 is refused.
        proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        proxy.createContext("/hello", exchange -> {
            try (exchange) {
                byte[] res = "Hello world!".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, res.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(res);
                }
            }
        });
        proxy.start();
    }

    @After
    public void stopProxy() {
        proxy.stop(0);
    }

    @Test
    public void proxiedRequestFailsOverBetweenProxyAddresses() throws IOException {
        DnsResolver proxyResolver = host -> {
            Assert.assertEquals(PROXY_HOST, host);
            return new InetAddress[]{InetAddress.getByName("127.0.0.2"), InetAddress.getByName("127.0.0.1")};
        };
        try (CloseableHttpClient client = client(proxyResolver);
             CloseableHttpResponse res = client.execute(new HttpGet("http://push.proxied.test/hello"))) {
            Assert.assertEquals(200, res.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void directRequestToProxyHostIsChecked() throws IOException {
        DnsResolver proxyResolver = host -> new InetAddress[]{InetAddress.getByName("127.0.0.1")};
        try (CloseableHttpClient client = client(proxyResolver)) {
            Assert.assertThrows(UnknownHostException.class, () -> client.execute(
                    new HttpGet("http://" + PROXY_HOST + ":" + proxy.getAddress().getPort() + "/hello")));
        }
    }

    private CloseableHttpClient client(DnsResolver proxyResolver) {
        DnsResolver rejecting = host -> {
            throw new UnknownHostException("rejected " + host);
        };
        ProxyMappings proxyMappings = ProxyMappings.valueOf(
                ".*proxied.*;http://" + PROXY_HOST + ":" + proxy.getAddress().getPort());
        ProxyRouteAwareDnsResolver routing = new ProxyRouteAwareDnsResolver(
                new ProxyMappingsAwareRoutePlanner(proxyMappings), rejecting, proxyResolver);
        return HttpClients.custom().setRoutePlanner(routing).setDnsResolver(routing).disableRedirectHandling().build();
    }
}
