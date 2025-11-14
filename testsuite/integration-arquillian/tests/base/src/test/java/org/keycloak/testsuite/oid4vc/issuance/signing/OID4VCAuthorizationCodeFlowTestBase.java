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

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Base class for authorization code flow tests with authorization details and claims validation.
 * Contains common test logic that can be reused by JWT and SD-JWT specific test classes.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public abstract class OID4VCAuthorizationCodeFlowTestBase extends OID4VCIssuerEndpointTest {

    public static final String OPENID_CREDENTIAL_TYPE = "openid_credential";

    /**
     * Test context for OID4VC tests
     */
    protected static class Oid4vcTestContext {
        public CredentialIssuer credentialIssuer;
        public CredentialsOffer credentialsOffer;
        public OIDCConfigurationRepresentation openidConfig;
    }

    /**
     * Get the credential format (jwt_vc or sd_jwt_vc)
     */
    protected abstract String getCredentialFormat();

    /**
     * Get the credential client scope
     */
    protected abstract ClientScopeRepresentation getCredentialClientScope();

    /**
     * Get the expected claim path for the credential format
     */
    protected abstract String getExpectedClaimPath();

    /**
     * Prepare OID4VC test context by fetching issuer metadata and credential offer
     */
    protected Oid4vcTestContext prepareOid4vcTestContext(String token) throws Exception {
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
    public void testCompleteFlowWithClaimsValidationAuthorizationCode() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext(null);

        // Perform authorization code flow to get authorization code
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName()); // Add the credential scope
        oauth.loginForm().doLogin("john", "password");

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Create authorization details with claims for token exchange
        ClaimsDescription claim = new ClaimsDescription();

        // Construct claim path based on credential format
        List<Object> claimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            claimPath = Arrays.asList(getExpectedClaimPath());
        } else {
            claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        claim.setPath(claimPath);
        claim.setMandatory(true);

        AuthorizationDetail authDetail = new AuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL_TYPE);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<AuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Exchange authorization code for tokens with authorization_details
        HttpPost postToken = new HttpPost(ctx.openidConfig.getTokenEndpoint());
        List<NameValuePair> tokenParameters = new LinkedList<>();
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        tokenParameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, "password"));
        tokenParameters.add(new BasicNameValuePair("authorization_details", authDetailsJson));
        UrlEncodedFormEntity tokenFormEntity = new UrlEncodedFormEntity(tokenParameters, StandardCharsets.UTF_8);
        postToken.setEntity(tokenFormEntity);

        AccessTokenResponse tokenResponse;
        try (CloseableHttpResponse tokenHttpResponse = httpClient.execute(postToken)) {
            assertEquals(HttpStatus.SC_OK, tokenHttpResponse.getStatusLine().getStatusCode());
            String tokenResponseBody = IOUtils.toString(tokenHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            tokenResponse = JsonSerialization.readValue(tokenResponseBody, AccessTokenResponse.class);
        }

        // Extract authorization_details from token response
        List<OID4VCAuthorizationDetailsResponse> authDetailsResponse = parseAuthorizationDetails(JsonSerialization.writeValueAsString(tokenResponse));
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals(1, authDetailsResponse.size());

        OID4VCAuthorizationDetailsResponse authDetailResponse = authDetailsResponse.get(0);
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should not be null", credentialIdentifier);

        // Request the actual credential using the identifier
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

            // Verify that the issued credential contains the requested claims AND may contain additional claims
            CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
            assertNotNull("Credential wrapper should not be null", credentialWrapper);

            // The credential is stored as Object, so we need to cast it
            Object credentialObj = credentialWrapper.getCredential();
            assertNotNull("Credential object should not be null", credentialObj);

            // Verify the credential structure based on formatfix-authorization_details-processing
            verifyCredentialStructure(credentialObj);
        }
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
