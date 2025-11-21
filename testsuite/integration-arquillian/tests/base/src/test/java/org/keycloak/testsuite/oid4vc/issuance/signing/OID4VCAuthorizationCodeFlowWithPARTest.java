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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Test;

import static org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsProcessor.OPENID_CREDENTIAL_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for Authorization Code Flow with PAR (Pushed Authorization Request) containing authorization_details.
 * This test specifically verifies that when authorization_details is used in the PAR request,
 * it MUST be returned in the token response according to OID4VC specification.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class OID4VCAuthorizationCodeFlowWithPARTest extends OID4VCIssuerEndpointTest {

    /**
     * Test context for OID4VC tests
     */
    protected static class Oid4vcTestContext {
        public CredentialIssuer credentialIssuer;
        public OIDCConfigurationRepresentation openidConfig;
    }

    /**
     * Get the credential client scope
     */
    protected ClientScopeRepresentation getCredentialClientScope() {
        return jwtTypeCredentialClientScope;
    }

    /**
     * Get the expected claim path for the credential format
     */
    protected String getExpectedClaimPath() {
        return "given_name";
    }

    /**
     * Prepare OID4VC test context by fetching issuer metadata
     */
    protected Oid4vcTestContext prepareOid4vcTestContext() throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        // Get credential issuer metadata
        HttpGet getCredentialIssuer = new HttpGet(getRealmPath(TEST_REALM_NAME) + "/.well-known/openid-credential-issuer");
        try (CloseableHttpResponse response = httpClient.execute(getCredentialIssuer)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            ctx.credentialIssuer = JsonSerialization.readValue(s, CredentialIssuer.class);
        }

        // Get OpenID configuration
        HttpGet getOpenidConfiguration = new HttpGet(ctx.credentialIssuer.getAuthorizationServers().get(0) + "/.well-known/openid-configuration");
        try (CloseableHttpResponse response = httpClient.execute(getOpenidConfiguration)) {
            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            String s = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            ctx.openidConfig = JsonSerialization.readValue(s, OIDCConfigurationRepresentation.class);
        }

        return ctx;
    }

    @Test
    public void testAuthorizationCodeFlowWithPARAndAuthorizationDetails() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Step 1: Create PAR request with authorization_details
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Create authorization details with claims
        ClaimsDescription claim = new ClaimsDescription();
        List<Object> claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        claim.setPath(claimPath);
        claim.setMandatory(true);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(credentialConfigurationId);
        authDetail.setClaims(List.of(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Create PAR request
        HttpPost parRequest = new HttpPost(ctx.openidConfig.getPushedAuthorizationRequestEndpoint());
        List<NameValuePair> parParameters = new LinkedList<>();
        parParameters.add(new BasicNameValuePair(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, getCredentialClientScope().getName()));
        parParameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.STATE, "test-state"));
        parParameters.add(new BasicNameValuePair(OIDCLoginProtocol.NONCE_PARAM, "test-nonce"));

        UrlEncodedFormEntity parFormEntity = new UrlEncodedFormEntity(parParameters, StandardCharsets.UTF_8);
        parRequest.setEntity(parFormEntity);

        String requestUri;
        try (CloseableHttpResponse parResponse = httpClient.execute(parRequest)) {
            assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusLine().getStatusCode());
            String parResponseBody = IOUtils.toString(parResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            Map<String, Object> parResult = JsonSerialization.readValue(parResponseBody, Map.class);
            requestUri = (String) parResult.get("request_uri");
            assertNotNull("Request URI should not be null", requestUri);
        }

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm().requestUri(requestUri).doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Step 3: Exchange authorization code for tokens (WITHOUT authorization_details in token request)
        // This tests that authorization_details from PAR request is processed and returned
        HttpPost postToken = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> tokenParameters = new LinkedList<>();
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
        // Note: NO authorization_details parameter in token request - it should come from PAR

        UrlEncodedFormEntity tokenFormEntity = new UrlEncodedFormEntity(tokenParameters, StandardCharsets.UTF_8);
        postToken.setEntity(tokenFormEntity);

        AccessTokenResponse tokenResponse;
        try (CloseableHttpResponse tokenHttpResponse = httpClient.execute(postToken)) {
            assertEquals(HttpStatus.SC_OK, tokenHttpResponse.getStatusLine().getStatusCode());
            String tokenResponseBody = IOUtils.toString(tokenHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            tokenResponse = JsonSerialization.readValue(tokenResponseBody, AccessTokenResponse.class);
        }

        // Step 4: Verify authorization_details is present in token response
        List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(JsonSerialization.writeValueAsString(tokenResponse));
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals("Should have exactly one authorization detail", 1, authDetailsResponse.size());

        OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
        assertEquals("Type should be openid_credential", OPENID_CREDENTIAL_TYPE, authDetailResponse.getType());
        assertEquals("Credential configuration ID should match", credentialConfigurationId, authDetailResponse.getCredentialConfigurationId());

        // Verify claims are preserved
        assertNotNull("Claims should be present", authDetailResponse.getClaims());
        assertEquals("Should have exactly one claim", 1, authDetailResponse.getClaims().size());
        ClaimsDescription responseClaim = authDetailResponse.getClaims().get(0);
        assertEquals("Claim path should match", claimPath, responseClaim.getPath());
        assertTrue("Claim should be mandatory", responseClaim.isMandatory());

        // Verify credential identifiers are present
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals("Should have exactly one credential identifier", 1, authDetailResponse.getCredentialIdentifiers().size());

        String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should not be null", credentialIdentifier);
        assertFalse("Credential identifier should not be empty", credentialIdentifier.isEmpty());

        // Verify it's a valid UUID
        try {
            UUID.fromString(credentialIdentifier);
        } catch (IllegalArgumentException e) {
            fail("Credential identifier should be a valid UUID, but was: " + credentialIdentifier);
        }

        // Step 5: Request the actual credential using the identifier
        HttpPost postCredential = new HttpPost(ctx.credentialIssuer.getCredentialEndpoint());
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getToken());
        postCredential.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialIdentifier(credentialIdentifier);

        String requestBody = JsonSerialization.writeValueAsString(credentialRequest);
        postCredential.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
            assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusLine().getStatusCode());
            String responseBody = IOUtils.toString(credentialResponse.getEntity().getContent(), StandardCharsets.UTF_8);

            // Parse the credential response
            CredentialResponse parsedResponse = JsonSerialization.readValue(responseBody, CredentialResponse.class);
            assertNotNull("Credential response should not be null", parsedResponse);
            assertNotNull("Credentials should be present", parsedResponse.getCredentials());
            assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

            // Verify that the issued credential contains the requested claims
            CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
            assertNotNull("Credential wrapper should not be null", credentialWrapper);

            Object credentialObj = credentialWrapper.getCredential();
            assertNotNull("Credential object should not be null", credentialObj);

            // Verify the credential structure
            verifyCredentialStructure(credentialObj);
        }
    }

    @Test
    public void testAuthorizationCodeFlowWithPARAndAuthorizationDetailsFailure() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Step 1: Create PAR request with INVALID authorization_details
        // Create authorization details with INVALID credential configuration ID
        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId("INVALID_CONFIG_ID"); // This should cause failure
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Create PAR request
        HttpPost parRequest = new HttpPost(ctx.openidConfig.getPushedAuthorizationRequestEndpoint());
        List<NameValuePair> parParameters = new LinkedList<>();
        parParameters.add(new BasicNameValuePair(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, getCredentialClientScope().getName()));
        parParameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.STATE, "test-state"));
        parParameters.add(new BasicNameValuePair(OIDCLoginProtocol.NONCE_PARAM, "test-nonce"));

        UrlEncodedFormEntity parFormEntity = new UrlEncodedFormEntity(parParameters, StandardCharsets.UTF_8);
        parRequest.setEntity(parFormEntity);

        String requestUri;
        try (CloseableHttpResponse parResponse = httpClient.execute(parRequest)) {
            assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusLine().getStatusCode());
            String parResponseBody = IOUtils.toString(parResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            Map<String, Object> parResult = JsonSerialization.readValue(parResponseBody, Map.class);
            requestUri = (String) parResult.get("request_uri");
            assertNotNull("Request URI should not be null", requestUri);
        }

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm().requestUri(requestUri).doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Step 3: Exchange authorization code for tokens (should fail because of invalid authorization_details)
        HttpPost postToken = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> tokenParameters = new LinkedList<>();
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));

        UrlEncodedFormEntity tokenFormEntity = new UrlEncodedFormEntity(tokenParameters, StandardCharsets.UTF_8);
        postToken.setEntity(tokenFormEntity);

        try (CloseableHttpResponse tokenHttpResponse = httpClient.execute(postToken)) {
            // Should fail because authorization_details from PAR request cannot be processed
            assertEquals(HttpStatus.SC_BAD_REQUEST, tokenHttpResponse.getStatusLine().getStatusCode());
            String tokenResponseBody = IOUtils.toString(tokenHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue("Error message should indicate authorization_details processing failure",
                    tokenResponseBody.contains("authorization_details was used in authorization request but cannot be processed for token response"));
        }
    }

    @Test
    public void testAuthorizationCodeFlowWithPARButNoAuthorizationDetailsInTokenRequest() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Step 1: Create PAR request WITHOUT authorization_details
        HttpPost parRequest = new HttpPost(ctx.openidConfig.getPushedAuthorizationRequestEndpoint());
        List<NameValuePair> parParameters = new LinkedList<>();
        parParameters.add(new BasicNameValuePair(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.SCOPE, getCredentialClientScope().getName()));
        parParameters.add(new BasicNameValuePair(OAuth2Constants.STATE, "test-state"));
        parParameters.add(new BasicNameValuePair(OIDCLoginProtocol.NONCE_PARAM, "test-nonce"));

        UrlEncodedFormEntity parFormEntity = new UrlEncodedFormEntity(parParameters, StandardCharsets.UTF_8);
        parRequest.setEntity(parFormEntity);

        String requestUri;
        try (CloseableHttpResponse parResponse = httpClient.execute(parRequest)) {
            assertEquals(HttpStatus.SC_CREATED, parResponse.getStatusLine().getStatusCode());
            String parResponseBody = IOUtils.toString(parResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            Map<String, Object> parResult = JsonSerialization.readValue(parResponseBody, Map.class);
            requestUri = (String) parResult.get("request_uri");
            assertNotNull("Request URI should not be null", requestUri);
        }

        // Step 2: Perform authorization with PAR
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm().requestUri(requestUri).doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Step 3: Exchange authorization code for tokens
        HttpPost postToken = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> tokenParameters = new LinkedList<>();
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));

        UrlEncodedFormEntity tokenFormEntity = new UrlEncodedFormEntity(tokenParameters, StandardCharsets.UTF_8);
        postToken.setEntity(tokenFormEntity);

        AccessTokenResponse tokenResponse;
        try (CloseableHttpResponse tokenHttpResponse = httpClient.execute(postToken)) {
            assertEquals(HttpStatus.SC_OK, tokenHttpResponse.getStatusLine().getStatusCode());
            String tokenResponseBody = IOUtils.toString(tokenHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            tokenResponse = JsonSerialization.readValue(tokenResponseBody, AccessTokenResponse.class);
        }

        // Step 4: Verify NO authorization_details in token response (since none was in PAR request)
        List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(JsonSerialization.writeValueAsString(tokenResponse));
        assertTrue("authorization_details should NOT be present in the response when not used in PAR request",
                authDetailsResponse == null || authDetailsResponse.isEmpty());
    }

    /**
     * Verify the credential structure based on the format.
     * Subclasses can override this to provide format-specific verification.
     */
    protected void verifyCredentialStructure(Object credentialObj) {
        // Default implementation - subclasses should override
        assertNotNull("Credential object should not be null", credentialObj);
    }

    /**
     * Parse authorization details from the token response.
     */
    protected List<OID4VCAuthorizationDetailsResponse> parseAuthorizationDetails(String responseBody) {
        try {
            // Parse the JSON response to extract authorization_details
            Map<String, Object> responseMap = JsonSerialization.readValue(responseBody, Map.class);
            Object authDetailsObj = responseMap.get("authorization_details");

            if (authDetailsObj == null) {
                return Collections.emptyList();
            }

            // Convert to list of OID4VCAuthorizationDetailsResponse
            return JsonSerialization.readValue(JsonSerialization.writeValueAsString(authDetailsObj),
                    new TypeReference<List<OID4VCAuthorizationDetailsResponse>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse authorization_details from response", e);
        }
    }
}
