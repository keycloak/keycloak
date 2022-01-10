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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assume;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.util.JsonConfigProvider;
import org.keycloak.services.util.JsonConfigProvider.JsonScope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultHttpClientFactoryTest {
	private static final String DISABLE_TRUST_MANAGER_PROPERTY = "disable-trust-manager";
	private static final String TEST_DOMAIN = "keycloak.org";

	@Test
	public void createHttpClientProviderWithDisableTrustManager() throws IOException{
		Map<String, String> values = new HashMap<>();
		values.put(DISABLE_TRUST_MANAGER_PROPERTY, "true");
		DefaultHttpClientFactory factory = new DefaultHttpClientFactory();
		factory.init(scope(values));
		KeycloakSession session = new DefaultKeycloakSession(new DefaultKeycloakSessionFactory());
		HttpClientProvider provider = factory.create(session);
        Optional<String> testURL = getTestURL();
        Assume.assumeTrue( "Could not get test url for domain", testURL.isPresent() );
		try (CloseableHttpClient httpClient = (CloseableHttpClient) provider.getHttpClient();
          CloseableHttpResponse response = httpClient.execute(new HttpGet(testURL.get()))) {
    		assertEquals(HttpStatus.SC_NOT_FOUND,response.getStatusLine().getStatusCode());
		}
	}

	@Test(expected = SSLPeerUnverifiedException.class)
	public void createHttpClientProviderWithUnvailableURL() throws IOException {
		DefaultHttpClientFactory factory = new DefaultHttpClientFactory();
		factory.init(scope(new HashMap<>()));
		KeycloakSession session = new DefaultKeycloakSession(new DefaultKeycloakSessionFactory());
		HttpClientProvider provider = factory.create(session);
		try (CloseableHttpClient httpClient = (CloseableHttpClient) provider.getHttpClient()) {
			Optional<String> testURL = getTestURL();
			Assume.assumeTrue("Could not get test url for domain", testURL.isPresent());
			httpClient.execute(new HttpGet(testURL.get()));
		}
	}

	private JsonScope scope(Map<String, String> properties) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode config = mapper.readTree(json(properties));
			return new JsonConfigProvider(config,new Properties()).new JsonScope(config);
		} catch (IOException e) {
			fail("Could not parse json");
		}
		return null;
	}

	private String json(Map<String, String> properties) {
		String[] params = properties.entrySet().stream().map(e -> param(e.getKey(), e.getValue())).toArray(String[]::new);

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append(StringUtils.join(params, ','));
		sb.append("}");
		
		return sb.toString();
	}

	private String param(String key, String value) {
		return "\"" + key + "\"" + " : " + "\"" + value + "\"";
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
