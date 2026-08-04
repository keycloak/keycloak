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

package org.keycloak.admin.client.token;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TokenManagerClientAssertionTest {

    private static final String REALM = "test";

    private HttpServer server;
    private String serverUrl;
    private final List<RecordedRequest> requests = new ArrayList<>();

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::recordRequest);
        server.start();
        serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void grantTokenSendsClientAssertionWithoutClientId() {
        Keycloak keycloak = adminClient(assertionSupplier());

        keycloak.tokenManager().grantToken();

        RecordedRequest request = requests.get(0);
        assertEquals("/realms/test/protocol/openid-connect/token", request.path);
        assertEquals(OAuth2Constants.CLIENT_CREDENTIALS, request.param(OAuth2Constants.GRANT_TYPE));
        assertEquals(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT, request.param(OAuth2Constants.CLIENT_ASSERTION_TYPE));
        assertEquals("signed-jwt-1", request.param(OAuth2Constants.CLIENT_ASSERTION));
        assertNull(request.param(OAuth2Constants.CLIENT_ID));
        assertNull(request.headers.getFirst("Authorization"));
    }

    @Test
    public void grantTokenUsesNewAssertionFromSupplierEachTime() {
        Keycloak keycloak = adminClient(assertionSupplier());

        keycloak.tokenManager().grantToken();
        keycloak.tokenManager().grantToken();

        assertEquals("signed-jwt-1", requests.get(0).param(OAuth2Constants.CLIENT_ASSERTION));
        assertEquals("signed-jwt-2", requests.get(1).param(OAuth2Constants.CLIENT_ASSERTION));
    }

    @Test
    public void refreshTokenSendsClientAssertion() {
        Keycloak keycloak = adminClient(assertionSupplier());

        keycloak.tokenManager().grantToken();
        keycloak.tokenManager().refreshToken();

        RecordedRequest request = requests.get(1);
        assertEquals("/realms/test/protocol/openid-connect/token", request.path);
        assertEquals(OAuth2Constants.REFRESH_TOKEN, request.param(OAuth2Constants.GRANT_TYPE));
        assertEquals("refresh-token-1", request.param(OAuth2Constants.REFRESH_TOKEN));
        assertEquals(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT, request.param(OAuth2Constants.CLIENT_ASSERTION_TYPE));
        assertEquals("signed-jwt-2", request.param(OAuth2Constants.CLIENT_ASSERTION));
        assertNull(request.param(OAuth2Constants.CLIENT_ID));
    }

    @Test
    public void logoutSendsClientAssertion() {
        Keycloak keycloak = adminClient(assertionSupplier());

        keycloak.tokenManager().grantToken();
        keycloak.tokenManager().logout();

        RecordedRequest request = requests.get(1);
        assertEquals("/realms/test/protocol/openid-connect/logout", request.path);
        assertEquals("refresh-token-1", request.param(OAuth2Constants.REFRESH_TOKEN));
        assertEquals(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT, request.param(OAuth2Constants.CLIENT_ASSERTION_TYPE));
        assertEquals("signed-jwt-2", request.param(OAuth2Constants.CLIENT_ASSERTION));
        assertNull(request.param(OAuth2Constants.CLIENT_ID));
    }

    @Test
    public void clientAssertionTakesPrecedenceOverClientSecretBasicAuthentication() {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(REALM)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId("admin-client")
                .clientSecret("ignored-secret")
                .clientAssertion(assertionSupplier())
                .build();

        keycloak.tokenManager().grantToken();

        RecordedRequest request = requests.get(0);
        assertEquals("signed-jwt-1", request.param(OAuth2Constants.CLIENT_ASSERTION));
        assertNull(request.headers.getFirst("Authorization"));
    }

    private Keycloak adminClient(Supplier<ClientAssertion> clientAssertionSupplier) {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(REALM)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientAssertion(clientAssertionSupplier)
                .build();
    }

    private Supplier<ClientAssertion> assertionSupplier() {
        AtomicInteger counter = new AtomicInteger();
        return () -> ClientAssertion.jwt("signed-jwt-" + counter.incrementAndGet());
    }

    private void recordRequest(HttpExchange exchange) throws IOException {
        RecordedRequest request = new RecordedRequest(exchange.getRequestURI().getPath(), exchange.getRequestHeaders(), parseForm(readRequestBody(exchange)));
        requests.add(request);

        if (request.path.endsWith("/logout")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String response = "{\"access_token\":\"access-token-" + requests.size()
                + "\",\"expires_in\":60,\"refresh_expires_in\":60,\"refresh_token\":\"refresh-token-"
                + requests.size() + "\",\"token_type\":\"Bearer\"}";
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseForm(String body) throws IOException {
        Map<String, String> form = new HashMap<>();
        if (body.isEmpty()) {
            return form;
        }
        for (String pair : body.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator == -1 ? pair : pair.substring(0, separator);
            String value = separator == -1 ? "" : pair.substring(separator + 1);
            form.put(urlDecode(key), urlDecode(value));
        }
        return form;
    }

    private String urlDecode(String value) throws IOException {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
    }

    private static class RecordedRequest {

        private final String path;
        private final Headers headers;
        private final Map<String, String> form;

        private RecordedRequest(String path, Headers headers, Map<String, String> form) {
            this.path = path;
            this.headers = headers;
            this.form = form;
        }

        private String param(String name) {
            return form.get(name);
        }
    }
}
