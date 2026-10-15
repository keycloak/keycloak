/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OID4VCGraphQLClaimMapperTest {

    static HttpServer server;
    static String endpoint;
    static KeycloakSession keycloakSession;

    @BeforeClass
    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/graphql", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes());
            String response;
            if (body.contains("booking")) {
                response = """
                    {"data":{"booking":{
                        "flightNumber":"BA123",
                        "route":"LHR-JFK",
                        "departure":"2026-10-15T08:30:00Z",
                        "bookingRef":"XKCD42"
                    }}}""";
            } else {
                response = """
                    {"errors":[{"message":"Unknown query"}]}""";
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (var os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
        endpoint = "http://localhost:" + server.getAddress().getPort() + "/graphql";
        keycloakSession = stubKeycloakSession();
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    @Test
    public void testSetClaimFromGraphQL() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "ticket");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(passengerId: $id) { flightNumber route departure bookingRef } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=username");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "booking");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        mapper.setClaim(claims, session);

        assertEquals("BA123", claims.get("flightNumber"));
        assertEquals("LHR-JFK", claims.get("route"));
        assertEquals("2026-10-15T08:30:00Z", claims.get("departure"));
        assertEquals("XKCD42", claims.get("bookingRef"));
    }

    @Test
    public void testSetClaimScalarValue() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "flightNumber");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(passengerId: $id) { flightNumber } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=username");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "booking.flightNumber");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        mapper.setClaim(claims, session);

        assertEquals("BA123", claims.get("flightNumber"));
        assertNull(claims.get("route"));
    }

    @Test
    public void testGraphQLError() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "data");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY, "{ unknownField }");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "result");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("GraphQL errors:"));
    }

    @Test
    public void testMissingEndpoint() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "data");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("misconfigured"));
    }

    private static KeycloakSession stubKeycloakSession() {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        InvocationHandler providerHandler = (proxy, method, args) -> {
            if ("getHttpClient".equals(method.getName())) {
                return httpClient;
            }
            if ("getMaxConsumedResponseSize".equals(method.getName())) {
                return HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE;
            }
            return null;
        };

        HttpClientProvider httpProvider = (HttpClientProvider) Proxy.newProxyInstance(
                HttpClientProvider.class.getClassLoader(),
                new Class[]{HttpClientProvider.class},
                providerHandler);

        InvocationHandler sessionHandler = (proxy, method, args) -> {
            if ("getProvider".equals(method.getName()) && args[0] == HttpClientProvider.class) {
                return httpProvider;
            }
            return null;
        };

        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class[]{KeycloakSession.class},
                sessionHandler);
    }

    private static UserSessionModel stubUserSession(String username) {
        Map<String, Stream<String>> attrs = new HashMap<>();
        attrs.put("username", Stream.of(username));

        InvocationHandler userHandler = (proxy, method, args) -> {
            if ("getAttributeStream".equals(method.getName())) {
                String attr = (String) args[0];
                Stream<String> val = attrs.get(attr);
                return val != null ? val : Stream.empty();
            }
            if ("getGroupsStream".equals(method.getName())) {
                return Stream.empty();
            }
            return null;
        };

        UserModel user = (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class[]{UserModel.class},
                userHandler);

        InvocationHandler sessionHandler = (proxy, method, args) -> {
            if ("getUser".equals(method.getName())) {
                return user;
            }
            return null;
        };

        return (UserSessionModel) Proxy.newProxyInstance(
                UserSessionModel.class.getClassLoader(),
                new Class[]{UserSessionModel.class},
                sessionHandler);
    }
}
