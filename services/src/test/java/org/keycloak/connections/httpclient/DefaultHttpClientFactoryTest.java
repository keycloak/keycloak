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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSession;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.utils.ScopeUtil;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultHttpClientFactoryTest {
	private static final String DISABLE_TRUST_MANAGER_PROPERTY = "disable-trust-manager";
	private static final String TEST_DOMAIN = "keycloak.org";
	private static final String MAX_RETRIES_PROPERTY = "max-retries";

	// Common objects for tests
	private DefaultHttpClientFactory factory;
	private KeycloakSession session;

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
		org.junit.Assert.assertNotNull("HTTP client should not be null", client);
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
