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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oid4vc.model.Claim;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
            } else if (body.contains("roles")) {
                response = """
                    {"data":{"roles":["pilot","crew","admin"]}}""";
            } else if (body.contains("typedData")) {
                response = """
                    {"data":{"typedData":{
                        "count":42,
                        "active":true,
                        "nickname":null,
                        "metadata":{"key":"value"}
                    }}}""";
            } else if (body.contains("sensitiveData")) {
                response = """
                    {"data":{"sensitiveData":{
                        "sub":"attacker-controlled",
                        "safeField":"ok"
                    }}}""";
            } else if (body.contains("userData")) {
                response = """
                    {"data":{"userData":{
                        "id":"user-123",
                        "sub":"alice",
                        "email":"alice@example.com"
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
        server.createContext("/graphql-500", exchange -> {
            var resp = "Internal Server Error";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(500, resp.length());
            try (var os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
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
                "query($id: String!) { booking(claimsRef: $id) { flightNumber route departure bookingRef } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=user:username");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "booking");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        mapper.setClaim(claims, session);

        // Object responses are spread into individual top-level claims (claim.name "ticket" is unused for
        // object responses) so each field gets its own independent SD-JWT disclosure.
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
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=user:username");
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
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY, "query { unknownField }");
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

    @Test
    public void testBlankEndpointRejected() {
        var mapper = createMapper("data", "  ", "query { booking { flightNumber } }", null, "booking");
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("endpoint missing"));
    }

    @Test
    public void testBlankQueryRejected() {
        var mapper = createMapper("data", endpoint, "  ", null, "booking");
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("query missing"));
    }

    @Test
    public void testMutationRejected() {
        var mapper = createMapper("data", endpoint,
                "mutation { deleteBooking(id: \"123\") { success } }", null, null);
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testSubscriptionRejected() {
        var mapper = createMapper("data", endpoint,
                "subscription { bookingUpdated { flightNumber } }", null, null);
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testMutationHiddenBehindLeadingCommentRejected() {
        var mapper = createMapper("data", endpoint,
                "# fetch booking\nmutation { deleteBooking(id: \"123\") { success } }", null, null);
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testMutationHiddenBehindLeadingFragmentRejected() {
        var mapper = createMapper("data", endpoint,
                "fragment BookingFields on Booking { flightNumber }\n"
                        + "mutation { deleteBooking(id: \"123\") { success } }", null, null);
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testShorthandQueryFormRejected() {
        // The anonymous shorthand form ('{ ... }' with no 'query' keyword) is valid GraphQL but is
        // deliberately not supported: it can't carry variable definitions, and this mapper's purpose is to
        // call an external endpoint parameterized by a variable (e.g. an id). Requiring the explicit 'query'
        // keyword also keeps the allow-list check in isUnsupportedDocument() a single, simple prefix match.
        var mapper = createMapper("data", endpoint, "{ roles }", null, "roles");
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testUnwrapScalarTypes() {
        var session = stubUserSession("alice");

        // Number
        var numClaims = new HashMap<String, Object>();
        createMapper("count", endpoint, "query { typedData { count } }", null, "typedData.count")
                .setClaim(numClaims, session);
        assertEquals(42, numClaims.get("count"));

        // Boolean
        var boolClaims = new HashMap<String, Object>();
        createMapper("active", endpoint, "query { typedData { active } }", null, "typedData.active")
                .setClaim(boolClaims, session);
        assertEquals(true, boolClaims.get("active"));

        // Null
        var nullClaims = new HashMap<String, Object>();
        createMapper("nickname", endpoint, "query { typedData { nickname } }", null, "typedData.nickname")
                .setClaim(nullClaims, session);
        assertNull(nullClaims.get("nickname"));
    }

    @Test
    public void testNestedObjectRejected() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "data");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query { typedData { count active nickname metadata } }");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "typedData");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("nested objects"));
    }

    @Test
    public void testNoResponsePathWithNestedData() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "data");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=user:username");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("nested objects"));
    }

    @Test
    public void testMissingResponsePath() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "data");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=user:username");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "nonExistent");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("No data at response path"));
    }

    @Test
    public void testHttpError() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "data");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT,
                "http://localhost:" + server.getAddress().getPort() + "/graphql-500");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=user:username");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "booking");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("HTTP 500"));
    }

    @Test
    public void testResolveVariablesEdgeCases() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "ticket");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber route departure bookingRef } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING,
                "id=user:username, malformed, missing=user:nonExistentAttr");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "booking");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        mapper.setClaim(claims, session);

        assertEquals("BA123", claims.get("flightNumber"));
    }

    @Test
    public void testUnsupportedVariableSourcePrefix() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);

        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "ticket");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=username");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "booking");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("Unsupported variable source"));
    }

    @Test
    public void testArrayResponse() {
        var mapper = createMapper("roles", endpoint, "query { roles }", null, "roles");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        mapper.setClaim(claims, session);

        @SuppressWarnings("unchecked")
        var roles = (java.util.List<Object>) claims.get("roles");
        assertNotNull("Claim 'roles' must be present", roles);
        assertEquals(3, roles.size());
        assertEquals("pilot", roles.get(0));
        assertEquals("crew", roles.get(1));
        assertEquals("admin", roles.get(2));
    }

    @Test
    public void testArrayFieldInObjectResponse() {
        var mapper = createMapper("data", endpoint, "query { roles }", null, null);

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        mapper.setClaim(claims, session);

        @SuppressWarnings("unchecked")
        var roles = (java.util.List<Object>) claims.get("roles");
        assertNotNull("Field 'roles' must be present", roles);
        assertEquals(3, roles.size());
    }

    @Test
    public void testMissingClaimName() {
        var mapper = createMapper(null, endpoint,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }",
                "id=user:username", "booking.flightNumber");

        var session = stubUserSession("alice");

        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("claim name missing"));
    }

    // validate() / validateConfig() — the same shape checks as setClaim's guards above, but surfaced as a
    // ProtocolMapperConfigException before any mapper's claims are set, so credential issuance fails with a
    // client-visible invalid_credential_request instead of an opaque server error. See OID4VCIssuerEndpoint,
    // which calls validate() for every mapper ahead of setClaim.

    @Test
    public void testValidateRejectsMissingClaimName() {
        var mapper = createMapper(null, endpoint, "query { booking { flightNumber } }", null, "booking");
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("claim name missing"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testValidateRejectsNullConfig() {
        // RepresentationToModel.toModel() passes an admin request's "config": null straight through
        // removeEmptyString(), which returns null for a null input — so ProtocolMapperModel.getConfig() can
        // be null. Must not NPE before reaching the missing-claim-name check.
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(null);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("claim name missing"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testSetClaimRejectsNullConfigWithoutNPE() {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(null);
        mapper.setMapperModel(mapperModel, "jwt_vc_json");

        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("claim name missing"));
    }

    @Test
    public void testValidateRejectsMissingEndpoint() {
        var mapper = createMapper("data", "  ", "query { booking { flightNumber } }", null, "booking");
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("endpoint missing"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testValidateRejectsMissingQuery() {
        var mapper = createMapper("data", endpoint, "  ", null, "booking");
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("query missing"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testValidateRejectsMutation() {
        var mapper = createMapper("data", endpoint,
                "mutation { deleteBooking(id: \"123\") { success } }", null, null);
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("must start with 'query'"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testValidateRejectsSubscription() {
        var mapper = createMapper("data", endpoint,
                "subscription { bookingUpdated { flightNumber } }", null, null);
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("must start with 'query'"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testValidateRejectsMutationHiddenBehindLeadingComment() {
        // A naive "startsWith(mutation)" denylist check is bypassed by a leading '#' comment line. The
        // allow-list check (document must start with 'query' or '{') rejects this by construction.
        var mapper = createMapper("data", endpoint,
                "# fetch booking\nmutation { deleteBooking(id: \"123\") { success } }", null, null);
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testValidateRejectsMutationHiddenBehindLeadingFragment() {
        // A naive denylist check is also bypassed by a fragment definition preceding the operation; rejected
        // for the same reason as the leading-comment case above.
        var mapper = createMapper("data", endpoint,
                "fragment BookingFields on Booking { flightNumber }\n"
                        + "mutation { deleteBooking(id: \"123\") { success } }", null, null);
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testValidateRejectsQueryPrecededByCommentOrFragment() {
        // Out of scope for now: a leading comment or fragment definition before the operation is rejected
        // even when the operation itself is a legitimate query, since the mapper only recognizes a single
        // operation starting at the beginning of the document. This can be relaxed later if needed.
        var mapper = createMapper("ticket", endpoint,
                "# fetch booking\nfragment BookingFields on Booking { flightNumber }\n"
                        + "query($id: String!) { booking(claimsRef: $id) { ...BookingFields } }",
                "id=user:username", "booking");
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("must start with 'query'"));
    }

    @Test
    public void testValidateRejectsUnsupportedVariableSourcePrefix() {
        var mapper = createMapper("ticket", endpoint,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }", "id=claimsRef", "booking");
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("Unsupported variable source"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testValidateRejectsUnresolvableQuery() {
        // Unlike the leading-comment/fragment cases above (rejected because the document doesn't start with
        // 'query'), this is a 'query'-keyword document whose selection set itself uses a fragment spread —
        // passes the document-shape check, but GraphQLSelectionSetParser can't resolve it. Metadata must be
        // authoritative, so this is rejected as invalid configuration rather than silently degraded.
        var mapper = createMapper("ticket", endpoint,
                "query { booking { ...BookingFields } }", null, "booking");
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("could not be resolved"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    /**
     * Reproduces the exact lookup ProtocolMappersResource.validateModel performs on a caught
     * ProtocolMapperConfigException: {@code messages.getProperty(ex.getMessageKey(), ex.getMessage())}.
     * Properties is backed by Hashtable, which throws NullPointerException on a null key, so every exception
     * thrown by this mapper must carry a non-null, non-blank message key or that admin API call degrades to an
     * HTTP 500 instead of the intended HTTP 400 validation error.
     */
    private static void assertMessageKeyIsSafeForAdminApi(ProtocolMapperConfigException ex) {
        assertNotNull(ex.getMessageKey(), "messageKey must not be null, or Properties.getProperty(null, ...) throws NPE");
        assertFalse(ex.getMessageKey().isBlank());
        var properties = new java.util.Properties();
        assertEquals(ex.getMessage(), properties.getProperty(ex.getMessageKey(), ex.getMessage()));
    }

    @Test
    public void testValidateAcceptsWellFormedConfig() throws ProtocolMapperConfigException {
        var mapper = createMapper("ticket", endpoint,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }", "id=user:username", "booking");
        mapper.validate();
    }

    // Reserved-claim rejection for spread fields — GraphQLSelectionSetParser lets this happen at config time
    // for a parseable query, since the field names it can ever produce are statically known from the query
    // text (GraphQL cannot return a field that wasn't requested). See testSetClaimRejectsReservedSpreadField
    // below for the runtime-only backstop that still applies when the query can't be parsed statically.

    @Test
    public void testValidateRejectsReservedSpreadFieldAtConfigTime() {
        var mapper = createMapper("ticket", endpoint,
                "query { sensitiveData { sub safeField } }", null, "sensitiveData");
        var ex = assertThrows(ProtocolMapperConfigException.class, mapper::validate);
        assertTrue(ex.getMessage().contains("reserved claim name"));
        assertMessageKeyIsSafeForAdminApi(ex);
    }

    @Test
    public void testSetClaimRejectsReservedSpreadField() {
        // Bypasses validate() (as if a mapper with this config were already saved) to exercise the runtime
        // backstop directly: the external endpoint actually returned a field named 'sub'.
        var mapper = createMapper("ticket", endpoint,
                "query { sensitiveData { sub safeField } }", null, "sensitiveData");
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        var ex = assertThrows(RuntimeException.class, () -> mapper.setClaim(claims, session));
        assertTrue(ex.getMessage().contains("reserved claim name"));
    }

    // Copilot issue #7: mDoc spread fields incorrectly rejected by mapping guard
    @Test
    public void testMdocSpreadFieldsExemptFromReservedClaimCheck() throws ProtocolMapperConfigException {
        // mDoc format is exempt from reserved-claim checks because its reserved claims live in namespaced
        // data elements that mappers cannot override (see OID4VCMapper.validateAgainstSensitiveMappings).
        var mapper = createMapperWithFormat("ticket", endpoint,
                "query { userData { id sub email } }", null, "userData", "mso_mdoc");

        // Should not throw - mDoc spreads 'id' and 'sub' without collision
        mapper.validate();

        // Runtime should also accept these field names for mDoc
        var session = stubUserSession("alice");
        var claims = new HashMap<String, Object>();
        mapper.setClaim(claims, session);
        assertEquals(3, claims.size());
        assertTrue(claims.containsKey("id"));
        assertTrue(claims.containsKey("sub"));
        assertTrue(claims.containsKey("email"));
    }

    // getMetadataAttributePaths() — issuer metadata declares a GraphQL mapper's produced claims accurately,
    // using the same static query parsing as the reserved-claim check above.

    @Test
    public void testMetadataPathsListEachSpreadFieldForObjectResponse() {
        var mapper = createMapper("ticket", endpoint,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber route } }", "id=user:username", "booking");
        var paths = mapper.getMetadataAttributePaths();
        assertEquals(List.of(List.of("credentialSubject", "flightNumber"), List.of("credentialSubject", "route")), paths);
    }

    @Test
    public void testMetadataPathsUseClaimNameForScalarResponse() {
        var mapper = createMapper("ticket", endpoint,
                "query { typedData { count } }", null, "typedData.count");
        var paths = mapper.getMetadataAttributePaths();
        assertEquals(List.of(List.of("credentialSubject", "ticket")), paths);
    }

    @Test
    public void testMetadataPathsEmptyForUnresolvableQueryBypassingValidation() {
        // validateMapperConfig rejects this query (see testValidateRejectsUnresolvableQuery), so this only
        // covers getMetadataAttributePaths() being called directly against a config that bypassed validation
        // (e.g. direct realm import) — it must still decline to guess rather than declare something wrong.
        var mapper = createMapper("ticket", endpoint,
                "query { booking { ...BookingFields } }", null, "booking");
        assertEquals(List.of(), mapper.getMetadataAttributePaths());
    }

    /**
     * Exercises the real {@link Claim#parseAll} / {@link org.keycloak.protocol.oid4vc.model.Claims#parse}
     * pipeline end to end — the actual path the issuer's .well-known metadata is built through — rather than
     * calling {@link OID4VCGraphQLClaimMapper#getMetadataAttributePaths()} directly. The other metadata tests
     * above only cover this mapper in isolation; this one would have caught a real mismatch between what this
     * mapper reports and what the shared Claim/Claims plumbing (changed to support a mapper contributing more
     * than one claim) actually does with it.
     */
    @Test
    public void testClaimParseAllListsSpreadFieldThroughRealMetadataPipeline() {
        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, "ticket");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY,
                "query($id: String!) { booking(passengerId: $id) { flightNumber } }");
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, "id=user:username");
        config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, "booking");
        config.put(CredentialScopeModel.VC_INCLUDE_IN_METADATA, "true");

        var mapperModel = new ProtocolMapperModel();
        mapperModel.setProtocolMapper(OID4VCGraphQLClaimMapper.MAPPER_ID);
        mapperModel.setConfig(config);
        var oid4vcMapperModel = new Oid4vcProtocolMapperModel(mapperModel);

        InvocationHandler sessionHandler = (proxy, method, args) -> {
            if ("getProvider".equals(method.getName()) && args != null && args.length == 2
                    && args[0] == ProtocolMapper.class && OID4VCGraphQLClaimMapper.MAPPER_ID.equals(args[1])) {
                return new OID4VCGraphQLClaimMapper();
            }
            return null;
        };
        var session = (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(), new Class[]{KeycloakSession.class}, sessionHandler);

        var claims = Claim.parseAll(session, "dc+sd-jwt", oid4vcMapperModel);
        var names = claims.stream().map(Claim::getName).toList();
        assertEquals(List.of("flightNumber"), names);
    }

    @Test
    public void testValidateConfigRejectsUnsupportedVariableSourcePrefix() {
        var mapper = createMapper("ticket", endpoint,
                "query($id: String!) { booking(claimsRef: $id) { flightNumber } }", "id=claimsRef", "booking");

        RealmModel realm = (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(), new Class[]{RealmModel.class}, (proxy, method, args) -> null);
        ProtocolMapperContainerModel client = (ProtocolMapperContainerModel) Proxy.newProxyInstance(
                ProtocolMapperContainerModel.class.getClassLoader(), new Class[]{ProtocolMapperContainerModel.class},
                (proxy, method, args) -> null);

        var ex = assertThrows(ProtocolMapperConfigException.class,
                () -> mapper.validateConfig(keycloakSession, realm, client, mapper.mapperModel));
        assertTrue(ex.getMessage().contains("Unsupported variable source"));
    }

    private static OID4VCGraphQLClaimMapper createMapper(String claimName, String endpoint,
                                                           String query, String variableMapping, String responsePath) {
        return createMapperWithFormat(claimName, endpoint, query, variableMapping, responsePath, "jwt_vc_json");
    }

    private static OID4VCGraphQLClaimMapper createMapperWithFormat(String claimName, String endpoint,
                                                                     String query, String variableMapping,
                                                                     String responsePath, String format) {
        var mapper = (OID4VCGraphQLClaimMapper) new OID4VCGraphQLClaimMapper().create(keycloakSession);
        var config = new HashMap<String, String>();
        config.put(OID4VCMapper.CLAIM_NAME, claimName);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_ENDPOINT, endpoint);
        config.put(OID4VCGraphQLClaimMapper.GRAPHQL_QUERY, query);
        if (variableMapping != null) {
            config.put(OID4VCGraphQLClaimMapper.GRAPHQL_VARIABLE_MAPPING, variableMapping);
        }
        if (responsePath != null) {
            config.put(OID4VCGraphQLClaimMapper.RESPONSE_PATH, responsePath);
        }
        // mDoc format requires a namespace
        if ("mso_mdoc".equals(format)) {
            config.put(OID4VCMapper.MDOC_NAMESPACE, "org.example.test");
        }
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setConfig(config);
        mapper.setMapperModel(mapperModel, format);
        return mapper;
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
        Map<String, String> attrs = new HashMap<>();
        attrs.put("username", username);

        InvocationHandler userHandler = (proxy, method, args) -> {
            if ("getAttributeStream".equals(method.getName())) {
                String attr = (String) args[0];
                String val = attrs.get(attr);
                return val != null ? Stream.of(val) : Stream.empty();
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
