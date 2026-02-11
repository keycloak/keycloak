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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.InvalidTokenRequest;
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.InvalidCredentialRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for authorization code flow tests with authorization details and claims validation.
 * Contains common test logic that can be reused by JWT and SD-JWT specific test classes.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public abstract class OID4VCAuthorizationCodeFlowTestBase extends OID4VCIssuerEndpointTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    /**
     * Test context for OID4VC tests
     */
    protected static class Oid4vcTestContext {
        public CredentialIssuer credentialIssuer;
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
     * Get the name of the protocol mapper for firstName
     */
    protected abstract String getFirstNameProtocolMapperName();

    /**
     * Prepare OID4VC test context by fetching issuer metadata and credential offer
     */
    protected Oid4vcTestContext prepareOid4vcTestContext() throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        // Get credential issuer metadata
        CredentialIssuerMetadataResponse metadataResponse = oauth.oid4vc().doIssuerMetadataRequest();
        assertEquals(HttpStatus.SC_OK, metadataResponse.getStatusCode());
        ctx.credentialIssuer = metadataResponse.getMetadata();

        // Get OpenID configuration
        OpenIDProviderConfigurationResponse openIDProviderConfigurationResponse = oauth.wellknownRequest()
                .url(ctx.credentialIssuer.getAuthorizationServers().get(0))
                .send();
        assertEquals(HttpStatus.SC_OK, openIDProviderConfigurationResponse.getStatusCode());
        ctx.openidConfig = openIDProviderConfigurationResponse.getOidcConfiguration();

        return ctx;
    }

    /**
     * Test that verifies that a second regular SSO login should NOT return authorization_details
     * from a previous OID4VCI login.
     */
    @Test
    public void testSecondSSOLoginDoesNotReturnAuthorizationDetails() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // ===== STEP 1: First login with OID4VCI (should return authorization_details) =====
        AccessTokenResponse firstTokenResponse = authzCodeFlow(ctx);
        String credentialIdentifier = assertTokenResponse(firstTokenResponse);
        assertNotNull("Credential identifier should be present in first token", credentialIdentifier);

        // ===== STEP 2: Second login - Regular SSO (should NOT return authorization_details) =====
        // Second login WITHOUT OID4VCI scope and WITHOUT authorization_details.
        oauth.client(client.getClientId(), "password");
        oauth.scope(OAuth2Constants.SCOPE_OPENID);
        oauth.openLoginForm();

        String secondCode = oauth.parseLoginResponse().getCode();
        assertNotNull("Second authorization code should not be null", secondCode);

        // Exchange second code for tokens WITHOUT authorization_details using OAuthClient
        AccessTokenResponse secondTokenResponse = oauth.accessTokenRequest(secondCode)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .send();
        assertEquals("Second token exchange should succeed", HttpStatus.SC_OK, secondTokenResponse.getStatusCode());

        // ===== STEP 3: Verify second token does NOT have authorization_details =====
        assertNull("Second token (regular SSO) should NOT have authorization_details", secondTokenResponse.getAuthorizationDetails());

        // ===== STEP 4: Verify second token cannot be used for credential requests =====

        // Credential request with second token should fail using OID4VCI utilities
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(secondTokenResponse.getAccessToken())
                .send();

        // Credential request with second token should fail
        // The second token doesn't have the OID4VCI scope, so it should fail
        assertEquals("Credential request with token without OID4VCI scope should fail",
            HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());

        String error = credentialResponse.getError();
        String errorDescription = credentialResponse.getErrorDescription();

        assertEquals("Credential request should fail with unknown credential configuration when OID4VCI scope is missing",
            "UNKNOWN_CREDENTIAL_CONFIGURATION", error);
        assertEquals("Scope check failure", errorDescription);
    }

    // Test for the whole authorization_code flow with the credentialRequest using credential_configuration_id
    // Note: When authorization_details are present in the token, credential_identifier must be used instead
    // This test verifies that using credential_configuration_id fails when authorization_details are present
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_credentialRequestWithConfigurationId() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform authorization code flow to get authorization code (includes authorization_details)
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Clear events before credential request
        events.clear();

        // Request the credential using credential_configuration_id (should fail when authorization_details are present)
        HttpPost postCredential = new HttpPost(ctx.credentialIssuer.getCredentialEndpoint());
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getAccessToken());
        postCredential.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialConfigurationId(credentialConfigurationId);

        String requestBody = JsonSerialization.writeValueAsString(credentialRequest);
        postCredential.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));

        try (CloseableHttpResponse credentialResponse = httpClient.execute(postCredential)) {
            assertEquals("Using credential_configuration_id with token that has authorization_details should fail",
                    HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusLine().getStatusCode());

            String errorBody = IOUtils.toString(credentialResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            assertTrue("Error should indicate that credential_identifier must be used. Actual error: " + errorBody,
                    errorBody.contains("credential_identifier") || errorBody.contains("authorization_details"));
        }
    }

    // Test for the whole authorization_code flow with the credentialRequest using credential_identifier
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_credentialRequestWithCredentialIdentifier() throws Exception {
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        testCompleteFlowWithClaimsValidationAuthorizationCode(credRequestSupplier);
    }

    // Tests that when token is refreshed, the new access-token can be used as well for credential-request
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_refreshToken() throws Exception {
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform authorization code flow to get authorization code
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);

        // Refresh token now
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponseRef = oauth.refreshRequest(tokenResponse.getRefreshToken()).send();

        // Extract values from refreshed token for credential request
        String accessToken = tokenResponseRef.getAccessToken();
        List<OID4VCAuthorizationDetailResponse> authDetails = tokenResponseRef.getOid4vcAuthorizationDetails();

        String credentialIdentifier = null;
        if (authDetails != null && !authDetails.isEmpty()) {
            List<String> credentialIdentifiers = authDetails.get(0).getCredentialIdentifiers();
            if (credentialIdentifiers != null && !credentialIdentifiers.isEmpty()) {
                credentialIdentifier = credentialIdentifiers.get(0);
            }
        }

        // Request the actual credential using the refreshed token
        Oid4vcCredentialResponse credRequestResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(accessToken)
                .send();
        assertSuccessfulCredentialResponse(credRequestResponse);
    }

    // Test for the authorization_code flow with "mandatory" claim specified in the "authorization_details" parameter
    @Test
    public void testCompleteFlow_mandatoryClaimsInAuthzDetailsParameter() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        // Store original user state for cleanup
        UserState userState = storeUserState();

        try {
            // 1 - Update user to have missing "lastName" (mandatory attribute)
            // NOTE: Need to call both "setLastName" and set attributes to be able to set last name as null
            userState.userRep.setAttributes(Collections.emptyMap());
            userState.userRep.setLastName(null);
            userState.user.update(userState.userRep);

            // 2 - Test the flow. Credential request should fail due the missing "lastName"
            // Perform authorization code flow to get authorization code
            AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
            String credentialIdentifier = assertTokenResponse(tokenResponse);
            String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

            // Clear events before credential request
            events.clear();

            // Request the actual credential using the identifier
            Oid4vcCredentialRequest credentialRequest = getCredentialRequest(ctx, credRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);
            Oid4vcCredentialResponse credentialResponse = credentialRequest.send();

            assertErrorCredentialResponse(credentialResponse);

            // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired with details about missing mandatory claim
            expectCredentialRequestError()
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();

            // 3 - Update user to add "lastName"
            userState.userRep.setLastName("Doe");
            userState.user.update(userState.userRep);

            // 4 - Test the credential-request again. Should be OK now
            credentialResponse = credentialRequest.send();
            assertSuccessfulCredentialResponse(credentialResponse);
        } finally {
            // Restore original user state
            restoreUserState(userState);
        }
    }


    // Tests that Keycloak should use authorization_details from accessToken when processing mandatory claims
    @Test
    public void testCorrectAccessTokenUsed() throws Exception {
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Update user to have missing "lastName"
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "john");
        UserRepresentation userRep = user.toRepresentation();
        // NOTE: Need to call both "setLastName" and set attributes to be able to set last name as null
        userRep.setAttributes(Collections.emptyMap());
        userRep.setLastName(null);
        user.update(userRep);

        try {
            // Create token with authorization_details, which does not require "lastName" to be mandatory attribute
            AccessTokenResponse tokenResponse = authzCodeFlow(ctx, Collections.emptyList(), false);

            // Create another token with authorization_details, which require "lastName" to be mandatory attribute
            AccessTokenResponse tokenResponseWithMandatoryLastName = authzCodeFlow(ctx, mandatoryLastNameClaimsSupplier(), true);

            // Request with mandatory lastName will fail as user does not have "lastName"
            String credentialIdentifier = assertTokenResponse(tokenResponseWithMandatoryLastName);
            String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

            Oid4vcCredentialRequest credentialRequest = getCredentialRequest(ctx, credRequestSupplier, tokenResponseWithMandatoryLastName, credentialConfigurationId, credentialIdentifier);
            Oid4vcCredentialResponse credentialResponse = credentialRequest.send();
            assertErrorCredentialResponse_mandatoryClaimsMissing(credentialResponse);

            // Request without mandatory lastName should work. Authorization_Details from accessToken will be used by Keycloak for processing this request
            credentialIdentifier = assertTokenResponse(tokenResponse);
            credentialRequest = getCredentialRequest(ctx, credRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);
            credentialResponse = credentialRequest.send();
            assertSuccessfulCredentialResponse(credentialResponse);
        } finally {
            // Revert user changes and add lastName back
            userRep.setLastName("Doe");
            user.update(userRep);
        }
    }


    // Test for the authorization_code flow with "mandatory" claim specified in the "authorization_details" parameter as well as
    // mandatory claims in the protocol mappers configuration
    @Test
    public void testCompleteFlow_mandatoryClaimsInAuthzDetailsParameterAndProtocolMappersConfig() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(credentialIdentifier);
            return credentialRequest;
        };

        // 1 - Update "firstName" protocol mapper to be mandatory
        ClientScopeResource clientScopeResource = ApiUtil.findClientScopeByName(testRealm(), getCredentialClientScope().getName());
        assertNotNull(clientScopeResource);
        ProtocolMapperRepresentation protocolMapper = clientScopeResource.getProtocolMappers().getMappers()
                .stream()
                .filter(protMapper -> getFirstNameProtocolMapperName().equals(protMapper.getName()))
                .findFirst()
                .orElseThrow((() -> new RuntimeException("Not found protocol mapper with name 'firstName-mapper'.")));

        // Store original protocol mapper config for cleanup
        String originalMandatoryValue = protocolMapper.getConfig().get(Oid4vcProtocolMapperModel.MANDATORY);
        protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY, "true");
        clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);

        // Store original user state for cleanup
        UserState userState = storeUserState();

        try {
            // 2 - Update user to have missing "lastName" (mandatory attribute by authorization_details parameter) and "firstName" (mandatory attribute by protocol mapper)
            // NOTE: Need to call both "setLastName" and set attributes to be able to set last name as null
            userState.userRep.setAttributes(Collections.emptyMap());
            userState.userRep.setFirstName(null);
            userState.userRep.setLastName(null);
            userState.user.update(userState.userRep);

            // 2 - Test the flow. Credential request should fail due the missing "lastName"
            // Perform authorization code flow to get authorization code
            AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
            String credentialIdentifier = assertTokenResponse(tokenResponse);
            String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

            // Clear events before credential request
            events.clear();

            // Request the actual credential using the identifier
            Oid4vcCredentialRequest credentialRequest = getCredentialRequest(ctx, credRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);
            Oid4vcCredentialResponse credentialResponse = credentialRequest.send();

            assertErrorCredentialResponse(credentialResponse);

            // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired with details about missing mandatory claim
            expectCredentialRequestError()
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();

            // 3 - Update user to add "lastName", but keep "firstName" missing. Credential request should still fail
            userState.userRep.setLastName("Doe");
            userState.userRep.setFirstName(null);
            userState.user.update(userState.userRep);

            // Clear events before credential request
            events.clear();

            credentialResponse = credentialRequest.send();
            assertErrorCredentialResponse(credentialResponse);

            // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
            expectCredentialRequestError()
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();

            // 4 - Update user to add "firstName", but missing "lastName"
            userState.userRep.setLastName(null);
            userState.userRep.setFirstName("John");
            userState.user.update(userState.userRep);

            // Clear events before credential request
            events.clear();

            credentialResponse = credentialRequest.send();
            assertErrorCredentialResponse(credentialResponse);

            // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
            expectCredentialRequestError()
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();

            // 5 - Update user to both "firstName" and "lastName". Credential request should be successful
            userState.userRep.setLastName("Doe");
            userState.userRep.setFirstName("John");
            userState.user.update(userState.userRep);

            credentialResponse = credentialRequest.send();
            assertSuccessfulCredentialResponse(credentialResponse);
        } finally {
            // Restore original user state
            restoreUserState(userState);

            // Restore original protocol mapper config
            protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY,
                    originalMandatoryValue != null ? originalMandatoryValue : "false");
            clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);
        }
    }


    /**
     * Test that reusing an authorization code fails with invalid_grant error.
     * This is a security-critical test to ensure codes can only be used once.
     */
    @Test
    public void testAuthorizationCodeReuse() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Create authorization details for token exchange
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx);
        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Perform authorization code flow with authorization_details in authorization request
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetailsJson);

        // First token exchange - should succeed
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .send();
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        // Clear events before second attempt
        events.clear();

        // Second token exchange with same code - should fail
        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getStatusCode());
        assertTrue("Error response should indicate invalid grant",
                "invalid_grant".equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null && errorResponse.getErrorDescription().contains("Code not valid")));

        // Verify error event was fired
        // Note: When code is reused, user is null but session from first successful use may still exist
        events.expect(EventType.CODE_TO_TOKEN_ERROR)
                .client(client.getClientId())
                .user((String) null)
                .session(AssertEvents.isSessionId())
                .error(Errors.INVALID_CODE)
                .assertEvent();
    }

    /**
     * Test that an invalid/malformed authorization code is rejected.
     */
    @Test
    public void testInvalidAuthorizationCode() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Attempt token exchange with invalid code
        events.clear();

        AccessTokenResponse errorResponse = oauth.accessTokenRequest("invalid-code-12345")
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getStatusCode());
        assertTrue("Error response should indicate invalid grant",
                "invalid_grant".equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null && errorResponse.getErrorDescription().contains("Code not valid")));

        // Verify error event was fired
        // Note: When code is invalid (never valid), there is no session because authentication never occurred
        events.expect(EventType.CODE_TO_TOKEN_ERROR)
                .client(client.getClientId())
                .user((String) null)
                .session((String) null)
                .error(Errors.INVALID_CODE)
                .assertEvent();
    }

    @Test
    public void testTokenExchangeWithoutAuthorizationDetails() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform authorization code flow to get authorization code
        String code = performAuthorizationCodeLogin();

        // Attempt token exchange without authorization_details
        events.clear();

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .send();

        assertEquals("Token exchange should succeed without authorization_details (it's optional)",
                HttpStatus.SC_OK, tokenResponse.getStatusCode());
        assertNotNull("Access token should be present", tokenResponse.getAccessToken());
        assertNull("Response should not contain authorization_details when not provided in request",
                tokenResponse.getAuthorizationDetails());
    }

    /**
     * Test that mismatched credential_configuration_id in authorization_details is rejected.
     */
    @Test
    public void testMismatchedCredentialConfigurationId() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Create authorization details with mismatched credential_configuration_id
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx, "unknown-credential-config-id");
        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Perform authorization code flow with authorization_details in authorization request
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetailsJson);

        // Attempt token exchange without resubmitting authorization_details
        events.clear();

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getStatusCode());
        assertTrue("Error response should indicate authorization_details processing error",
                "invalid_request".equals(errorResponse.getError()) ||
                "unknown_credential_configuration".equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null && errorResponse.getErrorDescription().contains("authorization_details")));
    }

    /**
     * Test that missing redirect_uri in token exchange fails.
     */
    @Test
    public void testTokenExchangeWithoutRedirectUri() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Create authorization details for token exchange
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx);
        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Perform authorization code flow with authorization_details in authorization request
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetailsJson);

        // Attempt token exchange without redirect_uri
        events.clear();

        AccessTokenResponse errorResponse = new InvalidTokenRequest(code, oauth)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .withClientId(client.getClientId())
                .withClientSecret("password")
                // redirect_uri is intentionally omitted
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getStatusCode());
        assertTrue("Error response should indicate invalid request",
                "invalid_request".equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null && errorResponse.getErrorDescription().contains("redirect_uri")));
    }

    /**
     * Test that redirect_uri mismatch between authorization and token requests fails.
     */
    @Test
    public void testTokenExchangeWithMismatchedRedirectUri() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Create authorization details for token exchange
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx);
        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Perform authorization code flow with authorization_details in authorization request
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetailsJson);

        // Attempt token exchange with mismatched redirect_uri
        events.clear();

        AccessTokenResponse errorResponse = new InvalidTokenRequest(code, oauth)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .withClientId(client.getClientId())
                .withClientSecret("password")
                .withRedirectUri("http://invalid-redirect-uri")
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getStatusCode());
        assertTrue("Error response should indicate redirect_uri mismatch",
                "invalid_grant".equals(errorResponse.getError()) ||
                "invalid_request".equals(errorResponse.getError()) ||
                (errorResponse.getErrorDescription() != null &&
                 (errorResponse.getErrorDescription().contains("redirect_uri") ||
                  errorResponse.getErrorDescription().contains("Incorrect redirect_uri"))));
    }

    /**
     * Test that malformed JSON at credential endpoint fails with proper error.
     */
    @Test
    public void testCredentialRequestWithMalformedJson() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform authorization code flow to get authorization code and token
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
        String credentialIdentifier = assertTokenResponse(tokenResponse);
        assertNotNull("Token should not be null", tokenResponse.getAccessToken());

        // Create a malformed JSON payload (invalid JSON syntax)
        String malformedJson = "{\"credential_identifier\":\"" + credentialIdentifier + "\", invalid json}";

        // Request credential with malformed JSON using InvalidCredentialRequest
        // This tests error handling for invalid JSON payloads
        events.clear();

        Oid4vcCredentialResponse credentialResponse = new InvalidCredentialRequest(oauth, malformedJson)
                .endpoint(ctx.credentialIssuer.getCredentialEndpoint())
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());
        
        // For malformed JSON, the error might be in error or errorDescription fields
        // or the parsing might fail entirely, but we should still get a 400 status
        String error = credentialResponse.getError();
        String errorDescription = credentialResponse.getErrorDescription();
        
        // Verify error response indicates a problem (either error field is set, or errorDescription contains relevant info)
        assertTrue("Error response should indicate JSON parsing failure or invalid request",
                error != null ||
                (errorDescription != null && 
                 (errorDescription.contains("invalid_credential_request") ||
                  errorDescription.contains("Failed to parse JSON") ||
                  errorDescription.contains("JSON") ||
                  errorDescription.contains("parse"))));

        // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
        // Note: JSON parsing fails before authentication, so client/user/session are not set in the event
        expectCredentialRequestErrorWithoutAuth().assertEvent();
    }

    /**
     * Test that invalid client_secret in token exchange fails.
     */
    @Test
    public void testTokenExchangeWithInvalidClientSecret() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Create authorization details for token exchange
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx);
        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Perform authorization code flow with authorization_details in authorization request
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetailsJson);

        // Attempt token exchange with invalid client_secret
        events.clear();

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "wrong-secret")
                .send();

        assertEquals(HttpStatus.SC_UNAUTHORIZED, errorResponse.getStatusCode());
        assertEquals("unauthorized_client", errorResponse.getError());
    }

    /**
     * Test that missing client_id in token exchange fails.
     */
    @Test
    public void testTokenExchangeWithoutClientId() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Create authorization details for token exchange
        OID4VCAuthorizationDetail authDetail = createAuthorizationDetail(ctx);
        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);
        String authDetailsJson = JsonSerialization.writeValueAsString(authDetails);

        // Perform authorization code flow with authorization_details in authorization request
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(authDetailsJson);

        // Attempt token exchange without client_id
        // This tests error handling for missing client_id parameter
        events.clear();

        AccessTokenResponse errorResponse = new InvalidTokenRequest(code, oauth)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .withClientSecret("password")  // Set client_secret but omit client_id
                // client_id is intentionally omitted
                .send();

        int statusCode = errorResponse.getStatusCode();
        assertTrue("Should return 400 or 401 for missing client_id",
                statusCode == HttpStatus.SC_BAD_REQUEST || statusCode == HttpStatus.SC_UNAUTHORIZED);
        assertTrue("Error should be invalid_request or invalid_client",
                "invalid_request".equals(errorResponse.getError()) || "invalid_client".equals(errorResponse.getError()));
    }

    /**
     * Test that malformed authorization_details JSON is rejected.
     */
    @Test
    public void testTokenExchangeWithMalformedAuthorizationDetails() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform authorization code flow with malformed authorization_details in the authorization request.
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm()
                .param(OAuth2Constants.AUTHORIZATION_DETAILS, "invalid-json")
                .doLogin("john", "password");
        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        // Attempt token exchange without resubmitting authorization_details (stored value is malformed)
        events.clear();

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getStatusCode());
        assertEquals(Errors.INVALID_AUTHORIZATION_DETAILS, errorResponse.getError());
        assertTrue("Error description should indicate authorization_details processing error",
                errorResponse.getErrorDescription() != null && errorResponse.getErrorDescription().contains("authorization_details"));
    }

    /**
     * Test that token request authorization_details cannot exceed what was granted in the authorization request.
     */
    @Test
    public void testTokenExchangeRejectsAuthorizationDetailsNotGranted() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Authorization request with a valid authorization_details entry
        OID4VCAuthorizationDetail grantedDetail = createAuthorizationDetail(ctx);
        String grantedAuthDetailsJson = JsonSerialization.writeValueAsString(List.of(grantedDetail));
        String code = performAuthorizationCodeLoginWithAuthorizationDetails(grantedAuthDetailsJson);

        // Token request attempts to change authorization_details (different credential configuration)
        OID4VCAuthorizationDetail differentDetail = createAuthorizationDetail(ctx, "different-credential-config-id");
        List<OID4VCAuthorizationDetail> differentAuthDetails = List.of(differentDetail);

        events.clear();

        AccessTokenResponse errorResponse = oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .authorizationDetails(differentAuthDetails)
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, errorResponse.getStatusCode());
        assertEquals(Errors.INVALID_AUTHORIZATION_DETAILS, errorResponse.getError());
        assertTrue("Error description should indicate authorization_details mismatch",
                errorResponse.getErrorDescription() != null && errorResponse.getErrorDescription().contains("authorization_details"));
    }

    /**
     * Test that credential request with unknown credential_configuration_id fails.
     */
    @Test
    public void testCredentialRequestWithUnknownCredentialConfigurationId() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform successful authorization code flow to get token
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);

        // Clear events before credential request
        events.clear();

        // Request credential with unknown credential_configuration_id only (no credential_identifier).
        // Server now requires credential_identifier when authorization_details are present,
        // so this request is treated as an invalid credential request.
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialConfigurationId("unknown-credential-config-id")
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());
        assertEquals("INVALID_CREDENTIAL_REQUEST", credentialResponse.getError());

        // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
        expectCredentialRequestError().assertEvent();
    }

    /**
     * Test that credential request with mismatched credential_identifier fails.
     */
    @Test
    public void testCredentialRequestWithMismatchedCredentialIdentifier() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform successful authorization code flow to get token
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
        assertTokenResponse(tokenResponse);

        // Clear events before credential request
        events.clear();

        CredentialRequest credRequest = new CredentialRequest().setCredentialIdentifier("00000000-0000-0000-0000-000000000000");

        // Request credential with mismatched credential_identifier (from different flow)
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier("00000000-0000-0000-0000-000000000000")
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());
        assertEquals("UNKNOWN_CREDENTIAL_IDENTIFIER", credentialResponse.getError());

        // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
        expectCredentialRequestError().assertEvent();
    }

    /**
     * Test that credential request without credential_configuration_id or credential_identifier fails.
     */
    @Test
    public void testCredentialRequestWithoutIdentifier() throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform successful authorization code flow to get token
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
        assertTokenResponse(tokenResponse);

        // Clear events before credential request
        events.clear();

        // Request credential without credential_configuration_id or credential_identifier.
        // Server now requires credential_identifier when authorization_details are present,
        // so an empty credential request results in INVALID_CREDENTIAL_REQUEST.
        Oid4vcCredentialResponse credentialResponse = new InvalidCredentialRequest(oauth, "{}")
                .endpoint(ctx.credentialIssuer.getCredentialEndpoint())
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());
        assertEquals("INVALID_CREDENTIAL_REQUEST", credentialResponse.getError());

        // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
        expectCredentialRequestError().assertEvent();
    }

    private void testCompleteFlowWithClaimsValidationAuthorizationCode(BiFunction<String, String, CredentialRequest> credentialRequestSupplier) throws Exception {
        Oid4vcTestContext ctx = prepareOid4vcTestContext();

        // Perform authorization code flow to get authorization code
        AccessTokenResponse tokenResponse = authzCodeFlow(ctx);
        String credentialIdentifier = assertTokenResponse(tokenResponse);
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Request the actual credential using the identifier
        Oid4vcCredentialRequest credentialRequest = getCredentialRequest(ctx, credentialRequestSupplier, tokenResponse, credentialConfigurationId, credentialIdentifier);
        Oid4vcCredentialResponse credentialResponse = credentialRequest.send();

        assertSuccessfulCredentialResponse(credentialResponse);
    }

    // Successful authorization_code flow
    private AccessTokenResponse authzCodeFlow(Oid4vcTestContext ctx) throws Exception {
        return authzCodeFlow(ctx, mandatoryLastNameClaimsSupplier(), false);
    }

    private List<ClaimsDescription> mandatoryLastNameClaimsSupplier() {
        // Create authorization details with mandatory claims for "lastName" user attribute
        ClaimsDescription claim = new ClaimsDescription();

        // Construct claim path based on credential format
        List<Object> claimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            claimPath = Collections.singletonList(getExpectedClaimPath());
        } else {
            claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        claim.setPath(claimPath);
        claim.setMandatory(true);
        return List.of(claim);
    }

    // Successful authorization_code flow
    private AccessTokenResponse authzCodeFlow(Oid4vcTestContext ctx, List<ClaimsDescription> claimsForAuthorizationDetailsParameter, boolean expectUserAlreadyAuthenticated) throws Exception {
        // Perform authorization code flow to get authorization code
        oauth.client(client.getClientId(), "password");
        oauth.scope(getCredentialClientScope().getName()); // Add the credential scope
        if (expectUserAlreadyAuthenticated) {
            oauth.openLoginForm();
        } else {
            oauth.loginForm().doLogin("john", "password");
        }

        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(claimsForAuthorizationDetailsParameter);
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        // Exchange authorization code for tokens with authorization_details
        return oauth.accessTokenRequest(code)
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .client(client.getClientId(), "password")
                .authorizationDetails(authDetails)
                .send();
    }

    // Test successful token response. Returns "Credential identifier" of the VC credential
    private String assertTokenResponse(AccessTokenResponse tokenResponse) throws Exception {
        // Extract authorization_details from token response
        List<OID4VCAuthorizationDetailResponse> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals(1, authDetailsResponse.size());

        OID4VCAuthorizationDetailResponse authDetailResponse = authDetailsResponse.get(0);
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        String credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull("Credential configuration id should not be null", credentialConfigurationId);

        List<String> credentialIdentifiers = authDetailResponse.getCredentialIdentifiers();
        assertNotNull("Credential identifiers should not be null", credentialIdentifiers);
        assertEquals("Credential identifiers expected to have 1 item. It had " + credentialIdentifiers.size() + " with value " + credentialIdentifiers,
                1, credentialIdentifiers.size());
        return credentialIdentifiers.get(0);
    }

    private Oid4vcCredentialRequest getCredentialRequest(Oid4vcTestContext ctx, BiFunction<String, String, CredentialRequest> credentialRequestSupplier, AccessTokenResponse tokenResponse,
                                                         String credentialConfigurationId, String credentialIdentifier) throws Exception {
        // Request the actual credential using the identifier
        CredentialRequest credRequest = credentialRequestSupplier.apply(credentialConfigurationId, credentialIdentifier);

        Oid4vcCredentialRequest request = oauth.oid4vc()
                .credentialRequest(credRequest)
                .bearerToken(tokenResponse.getAccessToken());

        return request;
    }

    private void assertSuccessfulCredentialResponse(Oid4vcCredentialResponse credentialResponse) throws Exception {
        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode());

        // Parse the credential response
        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
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

    private void assertErrorCredentialResponse(Oid4vcCredentialResponse credentialResponse) throws Exception {
        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());
        String error = credentialResponse.getError();
        assertEquals("Credential issuance failed: No elements selected after processing claims path pointer. The requested claims are not available in the user profile.", error);
    }

    private void assertErrorCredentialResponse_mandatoryClaimsMissing(Oid4vcCredentialResponse credentialResponse) throws Exception {
        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());
        String error = credentialResponse.getError();
        assertEquals("Credential issuance failed: No elements selected after processing claims path pointer. The requested claims are not available in the user profile.", error);
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
     * Creates a standard AuthorizationDetail for token exchange.
     *
     * @param ctx the test context
     * @param credentialConfigurationId the credential configuration ID (null to use default)
     * @return the AuthorizationDetail
     */
    protected OID4VCAuthorizationDetail createAuthorizationDetail(Oid4vcTestContext ctx, String credentialConfigurationId) {
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credentialConfigurationId != null
                ? credentialConfigurationId
                : getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));
        return authDetail;
    }

    /**
     * Creates a standard AuthorizationDetail for token exchange using default credential configuration.
     *
     * @param ctx the test context
     * @return the AuthorizationDetail
     */
    protected OID4VCAuthorizationDetail createAuthorizationDetail(Oid4vcTestContext ctx) {
        return createAuthorizationDetail(ctx, null);
    }

    /**
     * Performs authorization code login flow and returns the authorization code.
     *
     * @return the authorization code
     */
    protected String performAuthorizationCodeLogin() {
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm().doLogin("john", "password");
        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);
        return code;
    }

    /**
     * Performs authorization code login flow with provided authorization_details JSON in the authorization request.
     *
     * @param authorizationDetailsJson authorization_details JSON to send with the authorization request
     * @return the authorization code
     */
    protected String performAuthorizationCodeLoginWithAuthorizationDetails(String authorizationDetailsJson) {
        oauth.client(client.getClientId());
        oauth.scope(getCredentialClientScope().getName());
        oauth.loginForm()
                // Encode JSON so UriBuilder does not treat '{' or '}' as URI template characters
                .param(OAuth2Constants.AUTHORIZATION_DETAILS,
                        URLEncoder.encode(authorizationDetailsJson, StandardCharsets.UTF_8))
                .doLogin("john", "password");
        String code = oauth.parseLoginResponse().getCode();
        assertNotNull("Authorization code should not be null", code);
        return code;
    }

    /**
     * Creates an event expectation for VERIFIABLE_CREDENTIAL_REQUEST_ERROR with standard fields.
     *
     * @return the event expectation
     */
    protected AssertEvents.ExpectedEvent expectCredentialRequestError() {
        return events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .client(client.getClientId())
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .error(Errors.INVALID_REQUEST);
    }

    /**
     * Creates an event expectation for VERIFIABLE_CREDENTIAL_REQUEST_ERROR without client/user/session
     * (for cases where authentication hasn't occurred yet, e.g., malformed JSON).
     *
     * @return the event expectation
     */
    protected AssertEvents.ExpectedEvent expectCredentialRequestErrorWithoutAuth() {
        return events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .client((String) null)
                .user((String) null)
                .session((String) null)
                .error(Errors.INVALID_REQUEST);
    }

    /**
     * Stores the original user state for later restoration.
     *
     * @return a UserState object containing the original state and the user resource
     */
    protected UserState storeUserState() {
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "john");
        UserRepresentation userRep = user.toRepresentation();
        return new UserState(user, userRep,
                userRep.getFirstName(),
                userRep.getLastName(),
                userRep.getAttributes() != null ? new HashMap<>(userRep.getAttributes()) : null);
    }

    /**
     * Restores the user state from a UserState object.
     *
     * @param userState the stored user state
     * @throws Exception if restoration fails
     */
    protected void restoreUserState(UserState userState) throws Exception {
        UserRepresentation userRep = userState.user.toRepresentation();
        userRep.setFirstName(userState.originalFirstName);
        userRep.setLastName(userState.originalLastName);
        userRep.setAttributes(Objects.requireNonNullElse(userState.originalAttributes, Collections.emptyMap()));
        userState.user.update(userRep);
    }

    /**
     * Helper class to store user state for cleanup.
     */
    protected static class UserState {
        final UserResource user;
        final UserRepresentation userRep;
        final String originalFirstName;
        final String originalLastName;
        final Map<String, List<String>> originalAttributes;

        UserState(UserResource user, UserRepresentation userRep, String originalFirstName,
                  String originalLastName, Map<String, List<String>> originalAttributes) {
            this.user = user;
            this.userRep = userRep;
            this.originalFirstName = originalFirstName;
            this.originalLastName = originalLastName;
            this.originalAttributes = originalAttributes;
        }
    }
}
