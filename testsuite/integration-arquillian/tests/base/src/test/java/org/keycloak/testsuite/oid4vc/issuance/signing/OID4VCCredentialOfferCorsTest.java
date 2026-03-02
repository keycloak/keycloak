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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;
import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferStorage;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.cors.Cors;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for CORS functionality on OID4VCI credential offer endpoints.
 * Tests both the authenticated create-credential-offer endpoint and the
 * session-based credential-offer/{nonce} endpoint.
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
        // Clear events from token retrieval
        events.clear();

        // Test credential offer URI endpoint with valid origin
        CredentialOfferUriResponse response = oauth.oid4vc()
                .credentialOfferUriRequest(jwtTypeCredentialConfigurationIdName)
                .bearerToken(tokenResponse.getAccessToken())
                .header("Origin", VALID_CORS_URL)
                .preAuthorized(false)
                .send();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertCorsHeaders(response, VALID_CORS_URL);

        // Verify response content
        CredentialOfferURI offerUri = response.getCredentialOfferURI();
        assertNotNull("Credential offer URI should not be null", offerUri.getIssuer());
        assertNotNull("Nonce should not be null", offerUri.getNonce());

        // Verify CREDENTIAL_OFFER_REQUEST event was fired
        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST)
                .client(clientId)
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .assertEvent();
    }

    @Test
    public void testCredentialOfferUriCorsInvalidOrigin() throws Exception {

        AccessTokenResponse tokenResponse = getAccessToken();

        // Test credential offer URI endpoint with invalid origin
        CredentialOfferUriResponse response = oauth.oid4vc()
                .credentialOfferUriRequest(jwtTypeCredentialConfigurationIdName)
                .bearerToken(tokenResponse.getAccessToken())
                .header("Origin", INVALID_CORS_URL)
                .preAuthorized(false)
                .send();

        // Should still return 200 OK but without CORS headers
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertNoCorsHeaders(response);
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
        // First get a credential offer URI to obtain a nonce
        AccessTokenResponse tokenResponse = getAccessToken();
        CredentialOfferURI credOfferUri = getCredentialOfferUri(tokenResponse.getAccessToken());

        // Clear events before credential offer request
        events.clear();

        // Test credential offer endpoint with valid origin
        CredentialOfferResponse response = oauth.oid4vc()
                .credentialOfferRequest(credOfferUri)
                .header("Origin", VALID_CORS_URL)
                .send();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertCorsHeadersForSessionEndpoint(response, VALID_CORS_URL);

        // Verify response content
        CredentialsOffer offer = response.getCredentialsOffer();
        assertNotNull("Credential offer should not be null", offer.getCredentialIssuer());
        assertNotNull("Credential configuration IDs should not be null", offer.getCredentialConfigurationIds());

        // The credential_type detail contains the credential configuration ID from the offer
        // We already assert that credentialConfigurationIds is not null and not empty above
        String expectedCredentialType = offer.getCredentialConfigurationIds().get(0);

        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST)
                .client(clientId)
                .user(AssertEvents.isUUID())
                .session((String) null) // No session for unauthenticated endpoint
                .detail(Details.CREDENTIAL_TYPE, expectedCredentialType)
                .assertEvent();
    }

    @Test
    public void testCredentialOfferSessionCorsInvalidOrigin() throws Exception {
        // First get a credential offer URI to obtain a nonce
        AccessTokenResponse tokenResponse = getAccessToken();
        CredentialOfferURI credOfferUri = getCredentialOfferUri(tokenResponse.getAccessToken());

        // Test credential offer endpoint with invalid origin
        CredentialOfferResponse response = oauth.oid4vc()
                .credentialOfferRequest(credOfferUri)
                .header("Origin", INVALID_CORS_URL)
                .send();

        // Should still return 200 OK and include CORS headers (allows all origins)
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        assertCorsHeadersForSessionEndpoint(response, INVALID_CORS_URL);
    }

    @Test
    public void testCredentialOfferSessionCorsPreflightRequest() throws Exception {
        // First get a credential offer URI to obtain a nonce
        AccessTokenResponse tokenResponse = getAccessToken();
        CredentialOfferURI credOfferUri = getCredentialOfferUri(tokenResponse.getAccessToken());

        // Test preflight request for credential offer endpoint
        String offerUrl = credOfferUri.getCredentialOfferUri();

        try (CloseableHttpResponse response = makePreflightRequest(offerUrl, VALID_CORS_URL)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsPreflightHeadersForSessionEndpoint(response, VALID_CORS_URL);
        }
    }

    @Test
    public void testCredentialOfferUriQrCodeCorsValidOrigin() throws Exception {

        AccessTokenResponse tokenResponse = getAccessToken();

        // Test credential offer URI QR code endpoint with valid origin
        String offerUriUrl = getCredentialOfferUriUrl() + "&type=qr";

        // QR code endpoint returns binary data, so we need to use direct HTTP for this test
        try (CloseableHttpResponse response = makeCorsRequest(offerUriUrl, VALID_CORS_URL, tokenResponse.getAccessToken())) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            assertCorsHeadersFromCloseableResponse(response, VALID_CORS_URL);

            // Verify response is PNG image
            String contentType = response.getFirstHeader("Content-Type").getValue();
            assertTrue("Response should be PNG image", contentType.contains("image/png"));
        }
    }

    @Test
    public void testMultipleValidOrigins() throws Exception {
        // Test that multiple valid origins work
        AccessTokenResponse tokenResponse = getAccessToken();

        // Test with first valid origin
        CredentialOfferUriResponse response1 = oauth.oid4vc()
                .credentialOfferUriRequest(jwtTypeCredentialConfigurationIdName)
                .bearerToken(tokenResponse.getAccessToken())
                .header("Origin", VALID_CORS_URL)
                .preAuthorized(false)
                .send();
        assertEquals(HttpStatus.SC_OK, response1.getStatusCode());
        assertCorsHeaders(response1, VALID_CORS_URL);

        // Test with second valid origin
        CredentialOfferUriResponse response2 = oauth.oid4vc()
                .credentialOfferUriRequest(jwtTypeCredentialConfigurationIdName)
                .bearerToken(tokenResponse.getAccessToken())
                .header("Origin", ANOTHER_VALID_CORS_URL)
                .preAuthorized(false)
                .send();
        assertEquals(HttpStatus.SC_OK, response2.getStatusCode());
        assertCorsHeaders(response2, ANOTHER_VALID_CORS_URL);
    }

    @Test
    public void testUnauthenticatedCredentialOfferUri() throws Exception {
        // Test credential offer URI endpoint without authentication
        CredentialOfferUriResponse response = oauth.oid4vc()
                .credentialOfferUriRequest(jwtTypeCredentialConfigurationIdName)
                .header("Origin", VALID_CORS_URL)
                .send();

        // Should return 400 Bad Request
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        // Should still include CORS headers for error responses
        assertCorsHeaders(response, VALID_CORS_URL);
    }

    @Test
    public void testCredentialOfferUriWithInvalidCredentialConfig() throws Exception {
        AccessTokenResponse tokenResponse = getAccessToken();
        events.clear();

        // Test credential offer URI endpoint with invalid credential configuration ID
        CredentialOfferUriResponse response = oauth.oid4vc()
                .credentialOfferUriRequest("invalid-credential-config-id")
                .bearerToken(tokenResponse.getAccessToken())
                .header("Origin", VALID_CORS_URL)
                .send();

        // Should return 400 Bad Request
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());

        // Verify VERIFIABLE_CREDENTIAL_OFFER_REQUEST_ERROR event was fired
        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST_ERROR)
                .client(clientId)
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .error(Errors.INVALID_REQUEST)
                .assertEvent();
    }

    @Test
    public void testCredentialOfferWithExpiredNonce() throws Exception {
        events.clear();

        // Create an expired credential offer using testing client
        // Use AtomicReference to avoid serialization issues with lambda captures
        String issuerPath = getRealmPath(TEST_REALM_NAME);
        String nonce = testingClient.server(TEST_REALM_NAME).fetchString(session -> {
            CredentialsOffer credOffer = new CredentialsOffer()
                    .setCredentialIssuer(issuerPath)
                    .setGrants(new PreAuthorizedGrant().setPreAuthorizedCode(new PreAuthorizedCode().setPreAuthorizedCode("test-code")))
                    .setCredentialConfigurationIds(List.of(jwtTypeCredentialConfigurationIdName));

            CredentialOfferStorage offerStorage = session.getProvider(CredentialOfferStorage.class);
            // Create offer with expiration time just 1 second in the past
            // This ensures it's still findable in storage but marked as expired
            CredentialOfferState offerState = new CredentialOfferState(credOffer, null, null, Time.currentTime() - 1);
            offerStorage.putOfferState(session, offerState);
            session.getTransactionManager().commit();
            return offerState.getNonce();
        });
        assertNotNull("CredentialOffer nonce not null", nonce);

        events.clear();

        // Try to fetch the expired credential offer
        CredentialOfferResponse response = oauth.oid4vc()
                .credentialOfferRequest(nonce)
                .header("Origin", VALID_CORS_URL)
                .send();

        // Should return 400 Bad Request
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());

        // Verify VERIFIABLE_CREDENTIAL_OFFER_REQUEST_ERROR event was fired
        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST_ERROR)
                .client((String) null)
                .user((String) null)
                .session((String) null)
                // Storage prunes expired single-use entries before lookup; lookup failure yields INVALID_REQUEST
                // The error message indicates the offer was not found (pruned due to expiration) or already consumed
                .error(ErrorType.INVALID_CREDENTIAL_OFFER_REQUEST.getValue())
                .detail(Details.REASON, Matchers.anyOf(
                        Matchers.containsString("not found"),
                        Matchers.containsString("already consumed")))
                .assertEvent();
    }

    // Helper methods

    private AccessTokenResponse getAccessToken() throws Exception {
        return oauth.doPasswordGrantRequest("john", "password");
    }

    private String getCredentialOfferUriUrl() {
        String configId = jwtTypeCredentialConfigurationIdName;
        return getCredentialOfferUriUrl(configId, true, "john", null);
    }

    private String getCredentialOfferUriUrl(String configId, Boolean preAuthorized, String username, String clientId) {
        String res = getBasePath("test") + "create-credential-offer?credential_configuration_id=" + configId;
        if (preAuthorized != null)
            res += "&pre_authorized=" + preAuthorized;
        if (clientId != null)
            res += "&client_id=" + clientId;
        if (username != null)
            res += "&username=" + username;
        return res;
    }

    private CredentialOfferURI getCredentialOfferUri(String accessToken) throws Exception {

        CredentialOfferUriResponse response = oauth.oid4vc()
                .credentialOfferUriRequest(jwtTypeCredentialConfigurationIdName)
                .preAuthorized(true)
                .targetUser("john")
                .bearerToken(accessToken)
                .header("Origin", VALID_CORS_URL)
                .send();

        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getCredentialOfferURI();
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

    private void assertCorsHeaders(AbstractHttpResponse response, String expectedOrigin) {
        assertNotNull("Access-Control-Allow-Origin header should be present",
                response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("Access-Control-Allow-Origin should match request origin",
                expectedOrigin, response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));

        assertNotNull("Access-Control-Allow-Credentials header should be present",
                response.getHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertEquals("Access-Control-Allow-Credentials should be true",
                "true", response.getHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    private void assertCorsHeadersForSessionEndpoint(AbstractHttpResponse response, String expectedOrigin) {
        assertNotNull("Access-Control-Allow-Origin header should be present",
                response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("Access-Control-Allow-Origin should match request origin",
                expectedOrigin, response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));

        // Session-based endpoints don't require credentials since they use nonces for security
        // and allow all origins, so credentials header should be false for security reasons
        String credentialsHeader = response.getHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        assertNotNull("Access-Control-Allow-Credentials header should be present for session endpoints",
                credentialsHeader);
        assertEquals("Access-Control-Allow-Credentials should be false when allowing all origins",
                "false", credentialsHeader);
    }

    private void assertCorsPreflightHeaders(CloseableHttpResponse response, String expectedOrigin) {
        assertCorsHeadersFromCloseableResponse(response, expectedOrigin);

        assertNotNull("Access-Control-Allow-Methods header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS));

        String allowedMethods = response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS).getValue();
        Set<String> methods = Arrays.stream(allowedMethods.split(", "))
                .collect(Collectors.toSet());

        assertTrue("GET should be allowed", methods.contains("GET"));
        assertTrue("OPTIONS should be allowed", methods.contains("OPTIONS"));
    }

    private void assertCorsPreflightHeadersForSessionEndpoint(CloseableHttpResponse response, String expectedOrigin) {
        assertCorsHeadersForSessionEndpointFromCloseableResponse(response, expectedOrigin);

        assertNotNull("Access-Control-Allow-Methods header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS));

        String allowedMethods = response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS).getValue();
        Set<String> methods = Arrays.stream(allowedMethods.split(", "))
                .collect(Collectors.toSet());

        assertTrue("GET should be allowed", methods.contains("GET"));
        assertTrue("OPTIONS should be allowed", methods.contains("OPTIONS"));
    }

    private void assertCorsHeadersFromCloseableResponse(CloseableHttpResponse response, String expectedOrigin) {
        assertNotNull("Access-Control-Allow-Origin header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("Access-Control-Allow-Origin should match request origin",
                expectedOrigin, response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN).getValue());

        assertNotNull("Access-Control-Allow-Credentials header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertEquals("Access-Control-Allow-Credentials should be true",
                "true", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS).getValue());
    }

    private void assertCorsHeadersForSessionEndpointFromCloseableResponse(CloseableHttpResponse response, String expectedOrigin) {
        assertNotNull("Access-Control-Allow-Origin header should be present",
                response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("Access-Control-Allow-Origin should match request origin",
                expectedOrigin, response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN).getValue());

        // Session-based endpoints don't require credentials since they use nonces for security
        // and allow all origins, so credentials header should be false for security reasons
        Header credentialsHeader = response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        assertNotNull("Access-Control-Allow-Credentials header should be present for session endpoints",
                credentialsHeader);
        assertEquals("Access-Control-Allow-Credentials should be false when allowing all origins",
                "false", credentialsHeader.getValue());
    }

    private void assertNoCorsHeaders(AbstractHttpResponse response) {
        assertNull("Access-Control-Allow-Origin header should not be present", response.getHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull("Access-Control-Allow-Credentials header should not be present", response.getHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }
}
