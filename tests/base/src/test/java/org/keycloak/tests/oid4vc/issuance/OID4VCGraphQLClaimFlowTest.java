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

package org.keycloak.tests.oid4vc.issuance;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.Response;

import org.keycloak.VCFormat;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.ProofType;
import org.keycloak.representations.idm.oid4vc.UserVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.keycloak.models.oid4vci.CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT;
import static org.keycloak.tests.oid4vc.OID4VCIssuerTestBase.TEST_PASSWORD;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test for the OID4VC GraphQL claim mapper.
 *
 * <p>Starts a mock GraphQL server, creates a credential scope with the GraphQL mapper,
 * runs the full authorization code flow, and verifies the issued JWT credential
 * contains the claims returned by the mock GraphQL endpoint.
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCGraphQLClaimFlowTest extends OID4VCIssuerTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    static HttpServer mockServer;
    static String graphqlEndpoint;
    static final AtomicInteger requestCount = new AtomicInteger(0);

    @BeforeAll
    static void startMockGraphQLServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/graphql", exchange -> {
            requestCount.incrementAndGet();
            var body = new String(exchange.getRequestBody().readAllBytes());
            String response;
            if (body.contains("booking")) {
                String claimsRef = "unknown";
                JsonNode reqJson = JSON.readTree(body);
                JsonNode vars = reqJson.path("variables");
                if (vars.has("id")) {
                    claimsRef = vars.get("id").asText();
                }
                response = """
                    {"data":{"booking":{
                        "claimsRef":"%s",
                        "flightNumber":"BA123",
                        "route":"LHR-JFK",
                        "departure":"2026-10-15T08:30:00Z",
                        "bookingRef":"XKCD42"
                    }}}""".formatted(claimsRef);
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
        mockServer.createContext("/graphql-500", exchange -> {
            requestCount.incrementAndGet();
            var resp = "Internal Server Error";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(500, resp.length());
            try (var os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
        });
        mockServer.start();
        graphqlEndpoint = "http://localhost:" + mockServer.getAddress().getPort() + "/graphql";
    }

    @AfterAll
    static void stopMockGraphQLServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    public void testGraphQLClaimInIssuedCredential() throws Exception {
        String scopeName = "graphql-test-scope";

        OID4VCTestContext ctx = createScopeWithGraphQLMapper(scopeName, graphqlEndpoint, "id=user:username");

        requestCount.set(0);

        Oid4vcCredentialResponse credResponse = wallet.fetchCredentialByScope(ctx, scopeName);

        assertTrue(requestCount.get() > 0, "Mock GraphQL server should have been called during issuance");

        // Object responses are spread into individual top-level claims (claim.name "ticket" is unused for
        // object responses) so each field gets its own independent SD-JWT disclosure.
        JsonNode subject = decodeCredentialSubject(credResponse);
        assertEquals("alice", subject.path("claimsRef").asText());
        assertEquals("BA123", subject.path("flightNumber").asText());
        assertEquals("LHR-JFK", subject.path("route").asText());
        assertEquals("2026-10-15T08:30:00Z", subject.path("departure").asText());
        assertEquals("XKCD42", subject.path("bookingRef").asText());
    }

    @Test
    public void testGraphQLEndpointFailureDuringIssuance() throws Exception {
        String scopeName = "graphql-error-scope";
        String failingEndpoint = "http://localhost:" + mockServer.getAddress().getPort() + "/graphql-500";

        OID4VCTestContext ctx = createScopeWithGraphQLMapper(scopeName, failingEndpoint, "id=user:username");

        requestCount.set(0);

        Oid4vcCredentialResponse credResponse = fetchCredentialExpectingError(ctx, scopeName);

        assertTrue(requestCount.get() > 0, "Mock GraphQL server should have been called");
        assertEquals(500, credResponse.getStatusCode(), "Credential request should fail with HTTP 500");
    }

    @Test
    public void testUnsupportedVariableSourceDuringIssuance() throws Exception {
        String scopeName = "graphql-bad-prefix-scope";

        OID4VCTestContext ctx = createScopeWithGraphQLMapper(scopeName, graphqlEndpoint, "id=username");

        Oid4vcCredentialResponse credResponse = fetchCredentialExpectingError(ctx, scopeName);

        // The mapper's validate() rejects the unrecognized 'username' source before any claim resolution is attempted
        assertEquals(400, credResponse.getStatusCode(), "Credential request should fail with HTTP 400");
        assertEquals("invalid_credential_request", credResponse.getError());
        assertTrue(credResponse.getErrorDescription().contains("Unsupported variable source"),
                credResponse.getErrorDescription());
    }

    // ---- Helpers ----

    private JsonNode decodeCredentialSubject(Oid4vcCredentialResponse credResponse) throws Exception {
        CredentialResponse parsedResponse = credResponse.getCredentialResponse();
        String jwt = (String) parsedResponse.getCredentials().get(0).getCredential();
        String[] jwtParts = jwt.split("\\.");
        assertEquals(3, jwtParts.length, "JWT should have header.payload.signature");
        JsonNode payload = JSON.readTree(Base64.getUrlDecoder().decode(jwtParts[1]));
        return payload.path("vc").path("credentialSubject");
    }

    private OID4VCTestContext createScopeWithGraphQLMapper(String scopeName, String endpoint, String variableMapping) {
        CredentialScopeRepresentation scope = new CredentialScopeRepresentation(scopeName)
                .setFormat(VCFormat.JWT_VC)
                .setCredentialConfigurationId(scopeName)
                .setCryptographicBindingMethods(CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT)
                .setRequiredProofTypes(ProofType.JWT)
                .setBindingRequired(true);

        scope.setProtocolMappers(List.of(
                ProtocolMapperUtils.getGraphQLClaimMapper(
                        "ticket", endpoint,
                        "query($id: String!) { booking(claimsRef: $id) { claimsRef flightNumber route departure bookingRef } }",
                        variableMapping, "booking")
        ));

        String scopeId;
        try (Response response = testRealm.admin().clientScopes().create(scope)) {
            scopeId = ApiUtil.getCreatedId(response);
        }
        testRealm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());
        testRealm.admin().clients().get(client.getId()).addOptionalClientScope(scopeId);

        OID4VCTestContext ctx = new OID4VCTestContext(client, scope);
        String userId = testRealm.admin().users().search(ctx.getHolder()).get(0).getId();
        UserVerifiableCredentialRepresentation credRep = new UserVerifiableCredentialRepresentation();
        credRep.setCredentialScopeName(scopeName);
        testRealm.admin().users().get(userId).verifiableCredentials().createCredential(credRep);

        return ctx;
    }

    private Oid4vcCredentialResponse fetchCredentialExpectingError(OID4VCTestContext ctx, String scopeName) {
        AuthorizationEndpointResponse authResponse = wallet.authorizationRequest()
                .scope(scopeName)
                .send(ctx.getHolder(), TEST_PASSWORD);

        String authCode = authResponse.getCode();
        assertNotNull(authCode, "No Authorization Code");

        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, authCode).send();
        assertNotNull(tokenResponse.getAccessToken(), "No Access Token");

        return wallet.credentialRequest(ctx, tokenResponse.getAccessToken())
                .credentialIdentifier(ctx.getAuthorizedCredentialIdentifier())
                .proofs(wallet.generateJwtProof(ctx))
                .send();
    }
}
