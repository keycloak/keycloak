/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.utils.ScopeUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.DnsResolver;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultHttpClientFactoryTest {
	private static final String DISABLE_TRUST_MANAGER_PROPERTY = "disable-trust-manager";
	private static final String TEST_DOMAIN = "keycloak.org";
	private static final String MAX_RETRIES_PROPERTY = "max-retries";

        // HTTP server for tests
        private static HttpServer server;

	// Common objects for tests
	private DefaultHttpClientFactory factory;
	private KeycloakSession session;

        private static class RedirectHandler implements HttpHandler {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (exchange) {
                    exchange.getResponseHeaders().add("Location", "http://localhost:8280/hello");
                    exchange.sendResponseHeaders(302, 0);
                }
            }
        }

        private static class HelloWorldHandler implements HttpHandler {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try (exchange) {
                    byte[] res = "Hello world!".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/plain;charset=utf-8");
                    exchange.sendResponseHeaders(200, res.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(res);
                    }
                }
            }
        }

        @BeforeClass
        public static void startHttpServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(8280), 0);
            server.createContext("/redirect", new RedirectHandler());
            server.createContext("/hello", new HelloWorldHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        }

        @AfterClass
        public static void stopHttpServer() {
            server.stop(0);
        }

        @Before
        public void clearProxyMappingsCache() {
            // Proxy mappings are cached by host name across instances.
            ProxyMappings.clearCache();
        }

	/**
	 * Helper method to create and initialize factory with default settings
	 */
	private HttpClientProvider createDefaultProvider() {
		factory = new DefaultHttpClientFactory();
		factory.init(ScopeUtil.createScope(new HashMap<>()));
		session = new ResteasyKeycloakSession(new ResteasyKeycloakSessionFactory());
		return factory.create(session);
	}

	/**
	 * Helper method to create and initialize factory with custom settings
	 */
	private HttpClientProvider createProviderWithProperties(Map<String, String> values) {
		factory = new DefaultHttpClientFactory();
		factory.init(ScopeUtil.createScope(values));
		session = new ResteasyKeycloakSession(new ResteasyKeycloakSessionFactory());
		return factory.create(session);
	}

	@Test
	public void createHttpClientProviderWithDisableTrustManager() throws IOException {
		// Create provider with trust manager disabled
		Map<String, String> values = new HashMap<>();
		values.put(DISABLE_TRUST_MANAGER_PROPERTY, "true");
		HttpClientProvider provider = createProviderWithProperties(values);

		Optional<String> testURL = getTestURL();
		Assume.assumeTrue("Could not get test url for domain", testURL.isPresent());
		try (CloseableHttpClient httpClient = provider.getHttpClient();
				CloseableHttpResponse response = httpClient.execute(new HttpGet(testURL.get()))) {
			assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
		}
	}

	@Test(expected = SSLPeerUnverifiedException.class)
	public void createHttpClientProviderWithUnvailableURL() throws IOException {
		// Create provider with default settings
		HttpClientProvider provider = createDefaultProvider();

		try (CloseableHttpClient httpClient = provider.getHttpClient()) {
			Optional<String> testURL = getTestURL();
			Assume.assumeTrue("Could not get test url for domain", testURL.isPresent());
			httpClient.execute(new HttpGet(testURL.get()));
		}
	}

	@Test
	public void testGetHttpClientWithRetries() throws IOException {
		// Create provider with retry config
		Map<String, String> values = new HashMap<>();
		values.put(MAX_RETRIES_PROPERTY, "3");
		HttpClientProvider provider = createProviderWithProperties(values);

		// Get HTTP client (now has retry built-in)
		CloseableHttpClient client = provider.getHttpClient();

		// Verify client is not null
		org.junit.Assert.assertNotNull("HTTP client should not be null", client);
	}

	@Test
	public void testFactoryInitWithRetryProperties() {
		// Create factory with custom retry properties
		Map<String, String> values = new HashMap<>();
		values.put(MAX_RETRIES_PROPERTY, "5");

		// Create provider with custom properties
		HttpClientProvider provider = createProviderWithProperties(values);

		// Get HTTP client
		CloseableHttpClient client = provider.getHttpClient();

		// Verify client is not null
		Assert.assertNotNull("HTTP client should not be null", client);
	}

        @Test
        public void testRedirectDefault() throws IOException {
            HttpClientProvider provider = createDefaultProvider();

            try (CloseableHttpClient httpClient = provider.getHttpClient()) {
                try (CloseableHttpResponse res1 = httpClient.execute(new HttpGet("http://localhost:8280/redirect"))) {
                    Assert.assertEquals(302, res1.getStatusLine().getStatusCode());
                    Assert.assertEquals("http://localhost:8280/hello", res1.getHeaders("Location")[0].getValue());
                    try (CloseableHttpResponse res2 = httpClient.execute(new HttpGet(res1.getHeaders("Location")[0].getValue()))) {
                        Assert.assertEquals(200, res2.getStatusLine().getStatusCode());
                        Assert.assertEquals("Hello world!", EntityUtils.toString(res2.getEntity(), StandardCharsets.UTF_8));
                    }
                }
            }
        }

        @Test
        public void testRedirectEnabled() throws IOException {
            HttpClientProvider provider = createProviderWithProperties(
                    Map.of(DefaultHttpClientFactory.ALLOW_REDIRECTS, Boolean.TRUE.toString()));

            try (CloseableHttpClient httpClient = provider.getHttpClient()) {
                try (CloseableHttpResponse res2 = httpClient.execute(new HttpGet("http://localhost:8280/redirect"))) {
                    Assert.assertEquals(200, res2.getStatusLine().getStatusCode());
                    Assert.assertEquals("Hello world!", EntityUtils.toString(res2.getEntity(), StandardCharsets.UTF_8));
                }
            }
        }

        @Test
        public void testDirectHttpClientUsesGivenResolver() throws IOException {
            HttpClientProvider provider = createDefaultProvider();
            DnsResolver resolver = host -> {
                assertEquals("push.example.test", host);
                return new InetAddress[]{InetAddress.getLoopbackAddress()};
            };

            try (CloseableHttpClient httpClient = provider.createHttpClient(resolver, false);
                 CloseableHttpResponse res = httpClient.execute(new HttpGet("http://push.example.test:8280/hello"))) {
                Assert.assertEquals(200, res.getStatusLine().getStatusCode());
            }
        }

        @Test
        public void testProxiedHttpClientSendsProxiedRequestsThroughProxy() throws IOException {
            // The test server stands in for a proxy on a private address; the target never resolves.
            HttpClientProvider provider = createProviderWithProperties(Map.of(
                    "proxy-mappings", ".*proxied.*;http://127.0.0.1:8280"));

            try (CloseableHttpClient httpClient = provider.createHttpClient(rejectingResolver(), true);
                 CloseableHttpResponse res = httpClient.execute(new HttpGet("http://push.proxied.test/hello"))) {
                Assert.assertEquals(200, res.getStatusLine().getStatusCode());
            }
        }

        @Test
        public void testProxiedHttpClientChecksRequestsExcludedFromProxy() throws IOException {
            HttpClientProvider provider = createProviderWithProperties(Map.of(
                    "proxy-mappings", ".*proxied.*;http://127.0.0.1:8280"));

            try (CloseableHttpClient httpClient = provider.createHttpClient(rejectingResolver(), true)) {
                Assert.assertThrows(UnknownHostException.class,
                        () -> httpClient.execute(new HttpGet("http://direct.example.test:8280/hello")));
            }
        }

        @Test
        public void testProxiedHttpClientChecksDirectRequestsToProxyHost() throws IOException {
            // Not matched by the mapping, so this goes directly to the proxy's host and must be checked.
            HttpClientProvider provider = createProviderWithProperties(Map.of(
                    "proxy-mappings", ".*proxied.*;http://127.0.0.1:8280"));

            try (CloseableHttpClient httpClient = provider.createHttpClient(rejectingResolver(), true)) {
                Assert.assertThrows(UnknownHostException.class,
                        () -> httpClient.execute(new HttpGet("http://127.0.0.1:8280/hello")));
            }
        }

        private static DnsResolver rejectingResolver() {
            return host -> {
                throw new UnknownHostException("rejected " + host);
            };
        }

        @Test
        public void testDirectHttpClientIgnoresRedirectsAndProxy() throws IOException {
            // The shared client would follow the redirect and route through the unreachable proxy.
            HttpClientProvider provider = createProviderWithProperties(Map.of(
                    DefaultHttpClientFactory.ALLOW_REDIRECTS, Boolean.TRUE.toString(),
                    "proxy-mappings", ".*;http://127.0.0.1:1"));

            try (CloseableHttpClient httpClient = provider.createHttpClient(SystemDefaultDnsResolver.INSTANCE, false);
                 CloseableHttpResponse res = httpClient.execute(new HttpGet("http://localhost:8280/redirect"))) {
                Assert.assertEquals(302, res.getStatusLine().getStatusCode());
            }
        }

	private Optional<String> getTestURL() {
		try {
			// Convert domain name to ip to make request by ip
			return Optional.of("https://" + InetAddress.getByName(TEST_DOMAIN).getHostAddress());
		} catch (UnknownHostException e) {
			return Optional.empty();
		}
	}

}
