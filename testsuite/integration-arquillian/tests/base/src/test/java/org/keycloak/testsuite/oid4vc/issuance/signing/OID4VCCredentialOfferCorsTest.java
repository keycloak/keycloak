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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.Profile;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.cors.Cors;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.clientId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for CORS functionality on OID4VCI credential offer endpoints.
 * Tests both the authenticated credential-offer-uri endpoint and the
 * session-based credential-offer/{sessionCode} endpoint.
 *
 * @author <a href="https://github.com/forkimenjeckayang">Forkim Akwichek</a>
 */
@EnableFeature(value = Profile.Feature.OID4VC_VCI, skipRestart = true)
public class OID4VCCredentialOfferCorsTest extends OID4VCIssuerEndpointTest {

    private static final String VALID_CORS_URL = "http://localtest.me:8180";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180";
    private static final String ANOTHER_VALID_CORS_URL = "http://another.localtest.me:8180";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);

        // Find the existing client and add web origins for CORS testing
        testRealm.getClients().stream()
                .filter(client -> client.getClientId().equals(clientId))
                .findFirst()
                .ifPresent(client -> {
                    client.setDirectAccessGrantsEnabled(true);
                    client.setWebOrigins(Arrays.asList(VALID_CORS_URL, ANOTHER_VALID_CORS_URL));
                });
    }

    @Test
    public void testCredentialOfferUriCorsValidOrigin() throws Exception {

        AccessTokenResponse tokenResponse = getAccessToken();

        // Test credential offer URI endpoint with valid origin
        String offerUriUrl = getCredentialOfferUriUrl();

        try (CloseableHttpResponse response = makeCorsRequest(offerUriUrl, VALID_CORS_URL, tokenResponse.getAccessToken())) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsHeaders(response, VALID_CORS_URL);

            // Verify response content
            String responseBody = getResponseBody(response);
            CredentialOfferURI offerUri = JsonSerialization.readValue(responseBody, CredentialOfferURI.class);
            assertNotNull("Credential offer URI should not be null", offerUri.getIssuer());
            assertNotNull("Nonce should not be null", offerUri.getNonce());
        }
    }

    @Test
    public void testCredentialOfferUriCorsInvalidOrigin() throws Exception {

        AccessTokenResponse tokenResponse = getAccessToken();

        // Test credential offer URI endpoint with invalid origin
        String offerUriUrl = getCredentialOfferUriUrl();

        try (CloseableHttpResponse response = makeCorsRequest(offerUriUrl, INVALID_CORS_URL, tokenResponse.getAccessToken())) {
            // Should still return 200 OK but without CORS headers
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertNoCorsHeaders(response);
        }
    }

    @Test
    public void testCredentialOfferUriCorsPreflightRequest() throws Exception {
        // Test preflight request for credential offer URI endpoint
        String offerUriUrl = getCredentialOfferUriUrl();

        try (CloseableHttpResponse response = makePreflightRequest(offerUriUrl, VALID_CORS_URL)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsPreflightHeaders(response, VALID_CORS_URL);
        }
    }

    @Test
    public void testCredentialOfferUriCorsPreflightInvalidOrigin() throws Exception {
        // Test preflight request with invalid origin
        String offerUriUrl = getCredentialOfferUriUrl();

        try (CloseableHttpResponse response = makePreflightRequest(offerUriUrl, INVALID_CORS_URL)) {
            // Preflight should succeed and return CORS headers for all origins (standard CORS behavior)
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsPreflightHeaders(response, INVALID_CORS_URL);
        }
    }

    @Test
    public void testCredentialOfferSessionCorsValidOrigin() throws Exception {
        // First get a credential offer URI to obtain a session code
        AccessTokenResponse tokenResponse = getAccessToken();
        String sessionCode = getSessionCodeFromOfferUri(tokenResponse.getAccessToken());

        // Test credential offer endpoint with valid origin
        String offerUrl = getCredentialOfferUrl(sessionCode);

        try (CloseableHttpResponse response = makeCorsRequest(offerUrl, VALID_CORS_URL, null)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsHeadersForSessionEndpoint(response, VALID_CORS_URL);

            // Verify response content
            String responseBody = getResponseBody(response);
            CredentialsOffer offer = JsonSerialization.readValue(responseBody, CredentialsOffer.class);
            assertNotNull("Credential offer should not be null", offer.getCredentialIssuer());
            assertNotNull("Credential configuration IDs should not be null", offer.getCredentialConfigurationIds());
        }
    }

    @Test
    public void testCredentialOfferSessionCorsInvalidOrigin() throws Exception {
        // First get a credential offer URI to obtain a session code
        AccessTokenResponse tokenResponse = getAccessToken();
        String sessionCode = getSessionCodeFromOfferUri(tokenResponse.getAccessToken());

        // Test credential offer endpoint with invalid origin
        String offerUrl = getCredentialOfferUrl(sessionCode);

        try (CloseableHttpResponse response = makeCorsRequest(offerUrl, INVALID_CORS_URL, null)) {
            // Should still return 200 OK and include CORS headers (allows all origins)
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsHeadersForSessionEndpoint(response, INVALID_CORS_URL);
        }
    }

    @Test
    public void testCredentialOfferSessionCorsPreflightRequest() throws Exception {
        // First get a credential offer URI to obtain a session code
        AccessTokenResponse tokenResponse = getAccessToken();
        String sessionCode = getSessionCodeFromOfferUri(tokenResponse.getAccessToken());

        // Test preflight request for credential offer endpoint
        String offerUrl = getCredentialOfferUrl(sessionCode);

        try (CloseableHttpResponse response = makePreflightRequest(offerUrl, VALID_CORS_URL)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsPreflightHeadersForSessionEndpoint(response, VALID_CORS_URL);
        }
    }

    @Test
    public void testCredentialOfferUriQrCodeCorsValidOrigin() throws Exception {

        AccessTokenResponse tokenResponse = getAccessToken();

        // Test credential offer URI QR code endpoint with valid origin
        String offerUriUrl = getCredentialOfferUriUrl() + "&type=qr-code";

        try (CloseableHttpResponse response = makeCorsRequest(offerUriUrl, VALID_CORS_URL, tokenResponse.getAccessToken())) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsHeaders(response, VALID_CORS_URL);

            // Verify response is PNG image
            String contentType = response.getFirstHeader("Content-Type").getValue();
            assertTrue("Response should be PNG image", contentType.contains("image/png"));
        }
    }

    @Test
    public void testMultipleValidOrigins() throws Exception {
        // Test that multiple valid origins work
        AccessTokenResponse tokenResponse = getAccessToken();
        String offerUriUrl = getCredentialOfferUriUrl();

        // Test with first valid origin
        try (CloseableHttpResponse response1 = makeCorsRequest(offerUriUrl, VALID_CORS_URL, tokenResponse.getAccessToken())) {
            assertEquals(HttpStatus.SC_OK, response1.getStatusLine().getStatusCode());
            assertCorsHeaders(response1, VALID_CORS_URL);
        }

        // Test with second valid origin
        try (CloseableHttpResponse response2 = makeCorsRequest(offerUriUrl, ANOTHER_VALID_CORS_URL, tokenResponse.getAccessToken())) {
            assertEquals(HttpStatus.SC_OK, response2.getStatusLine().getStatusCode());
            assertCorsHeaders(response2, ANOTHER_VALID_CORS_URL);
        }
    }

    @Test
    public void testUnauthenticatedCredentialOfferUri() throws Exception {
        // Test credential offer URI endpoint without authentication
        String offerUriUrl = getCredentialOfferUriUrl();

        try (CloseableHttpResponse response = makeCorsRequest(offerUriUrl, VALID_CORS_URL, null)) {
            // Should return 400 Bad Request
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            // Should still include CORS headers for error responses
            assertCorsHeaders(response, VALID_CORS_URL);
        }
    }

    // Helper methods

    private AccessTokenResponse getAccessToken() throws Exception {
        oauth.realm("test");
        oauth.client(client.getClientId(), client.getSecret());

        return oauth.doPasswordGrantRequest("john", "password");
    }

    private String getCredentialOfferUriUrl() {
        return getBasePath("test") + "credential-offer-uri?credential_configuration_id=" + jwtTypeCredentialConfigurationIdName;
    }

    private String getCredentialOfferUrl(String sessionCode) {
        return getBasePath("test") + "credential-offer/" + sessionCode;
    }

    private String getSessionCodeFromOfferUri(String accessToken) throws Exception {
        String offerUriUrl = getCredentialOfferUriUrl();

        try (CloseableHttpResponse response = makeCorsRequest(offerUriUrl, VALID_CORS_URL, accessToken)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

            String responseBody = getResponseBody(response);
            CredentialOfferURI offerUri = JsonSerialization.readValue(responseBody, CredentialOfferURI.class);

            return offerUri.getNonce();
        }
    }

    private CloseableHttpResponse makeCorsRequest(String url, String origin, String accessToken) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("Origin", origin);
        request.setHeader("Accept", "application/json");

        if (accessToken != null) {
            request.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }

        return httpClient.execute(request);
    }

    private CloseableHttpResponse makePreflightRequest(String url, String origin) throws IOException {
        HttpOptions request = new HttpOptions(url);
        request.setHeader("Origin", origin);
        request.setHeader("Access-Control-Request-Method", "GET");
        request.setHeader("Access-Control-Request-Headers", "Authorization, Accept");

        return httpClient.execute(request);
    }

    private String getResponseBody(CloseableHttpResponse response) throws IOException {
        return new String(response.getEntity().getContent().readAllBytes());
    }

    private void assertCorsHeaders(CloseableHttpResponse response, String expectedOrigin) {
        assertNotNull("Access-Control-Allow-Origin header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("Access-Control-Allow-Origin should match request origin",
                expectedOrigin, response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN).getValue());

        assertNotNull("Access-Control-Allow-Credentials header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertEquals("Access-Control-Allow-Credentials should be true",
                "true", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS).getValue());
    }

    private void assertCorsHeadersForSessionEndpoint(CloseableHttpResponse response, String expectedOrigin) {
        assertNotNull("Access-Control-Allow-Origin header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("Access-Control-Allow-Origin should match request origin",
                expectedOrigin, response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN).getValue());

        // Session-based endpoints don't require credentials since they use session codes for security
        // and allow all origins, so credentials header should be false for security reasons
        Header credentialsHeader = response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        assertNotNull("Access-Control-Allow-Credentials header should be present for session endpoints",
                credentialsHeader);
        assertEquals("Access-Control-Allow-Credentials should be false when allowing all origins",
                "false", credentialsHeader.getValue());
    }

    private void assertCorsPreflightHeaders(CloseableHttpResponse response, String expectedOrigin) {
        assertCorsHeaders(response, expectedOrigin);

        assertNotNull("Access-Control-Allow-Methods header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS));

        String allowedMethods = response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS).getValue();
        Set<String> methods = Arrays.stream(allowedMethods.split(", "))
                .collect(Collectors.toSet());

        assertTrue("GET should be allowed", methods.contains("GET"));
        assertTrue("OPTIONS should be allowed", methods.contains("OPTIONS"));
    }

    private void assertCorsPreflightHeadersForSessionEndpoint(CloseableHttpResponse response, String expectedOrigin) {
        assertCorsHeadersForSessionEndpoint(response, expectedOrigin);

        assertNotNull("Access-Control-Allow-Methods header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS));

        String allowedMethods = response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS).getValue();
        Set<String> methods = Arrays.stream(allowedMethods.split(", "))
                .collect(Collectors.toSet());

        assertTrue("GET should be allowed", methods.contains("GET"));
        assertTrue("OPTIONS should be allowed", methods.contains("OPTIONS"));
    }

    private void assertNoCorsHeaders(CloseableHttpResponse response) {
        assertNull("Access-Control-Allow-Origin header should not be present", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull("Access-Control-Allow-Credentials header should not be present", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }
}
