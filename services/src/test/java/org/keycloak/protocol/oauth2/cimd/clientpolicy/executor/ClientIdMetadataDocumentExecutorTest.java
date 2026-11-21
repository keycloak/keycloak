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

package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * A client metadata document is fetched from a remote URL, so its content is untrusted input.
 * Malformed documents must be rejected with an {@code invalid_request} client policy error,
 * never with an unchecked exception (which surfaces as an HTTP 500 from the authorization endpoint).
 */
public class ClientIdMetadataDocumentExecutorTest {

    private HttpServer httpServer;
    private CloseableHttpClient httpClient;
    private ClientIdMetadataDocumentExecutor executor;

    @Before
    public void setup() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/cimd/metadata", exchange -> {
            // a syntactically valid JSON document whose value is null
            byte[] body = "null".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        httpServer.start();

        httpClient = HttpClients.createDefault();
        HttpClientProvider httpClientProvider = (HttpClientProvider) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{HttpClientProvider.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getHttpClient" -> httpClient;
                    case "getMaxConsumedResponseSize" -> HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE;
                    default -> null;
                });
        KeycloakSession session = (KeycloakSession) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{KeycloakSession.class}, (proxy, method, args) ->
                        "getProvider".equals(method.getName()) && args.length == 1 && args[0] == HttpClientProvider.class ? httpClientProvider : null);
        // default global settings: every lookup returns the supplied default value
        Config.Scope scope = (Config.Scope) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Config.Scope.class}, (proxy, method, args) -> args != null && args.length == 2 ? args[1] : null);

        executor = new ClientIdMetadataDocumentExecutor(session, new ClientIdMetadataDocumentExecutorFactoryProviderConfig(scope));
        executor.setupConfiguration(new ClientIdMetadataDocumentExecutor.Configuration());
    }

    @After
    public void tearDown() throws Exception {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Test
    public void nullMetadataDocumentIsRejectedAsInvalidRequest() throws Exception {
        URI clientId = new URI("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/cimd/metadata");
        URI redirectUri = new URI("http://127.0.0.1:" + httpServer.getAddress().getPort() + "/callback");
        try {
            AbstractClientIdMetadataDocumentExecutor.OIDCClientRepresentationWithCacheControl fetched =
                    executor.fetchClientMetadata(clientId, false, null);
            executor.verifyClientMetadata(clientId, redirectUri, fetched.getOidcClientRepresentation());
            fail("a null client metadata document must be rejected");
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getError());
            assertEquals(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_NOCONTENT, e.getErrorDetail());
        }
    }

    @Test
    public void nullRedirectUrisEntryIsRejectedAsInvalidRequest() throws Exception {
        String clientId = "https://client.example.com/cimd/metadata";
        OIDCClientRepresentation metadata = new OIDCClientRepresentation();
        metadata.setClientId(clientId);
        // e.g. "redirect_uris": ["https://client.example.com/callback", null]
        metadata.setRedirectUris(Arrays.asList("https://client.example.com/callback", null));
        try {
            executor.verifyClientMetadata(new URI(clientId), new URI("https://client.example.com/callback"), metadata);
            fail("a client metadata document with a null redirect_uris entry must be rejected");
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getError());
            assertEquals(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_MALFORMED_URL, e.getErrorDetail());
        }
    }
}
