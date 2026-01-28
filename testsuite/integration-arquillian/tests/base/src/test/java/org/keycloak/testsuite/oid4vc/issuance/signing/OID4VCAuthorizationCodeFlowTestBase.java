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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialRequest;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.OPENID_CREDENTIAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        CredentialIssuerMetadataResponse metadataResponse = oauth.oid4vc()
                .issuerMetadataRequest()
                .endpoint(getRealmMetadataPath(TEST_REALM_NAME))
                .send();
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
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCredentialIdentifier(credentialIdentifier);

        // Credential request with second token should fail using OID4VCI utilities
        Oid4vcCredentialRequest credentialRequestBuilder = oauth.oid4vc()
                .credentialRequest()
                .endpoint(ctx.credentialIssuer.getCredentialEndpoint())
                .bearerToken(secondTokenResponse.getAccessToken())
                .credentialIdentifier(credentialIdentifier);

        Oid4vcCredentialResponse credentialResponse = credentialRequestBuilder.send();

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
    @Test
    public void testCompleteFlowWithClaimsValidationAuthorizationCode_credentialRequestWithConfigurationId() throws Exception {
        BiFunction<String, String, CredentialRequest> credRequestSupplier = (credentialConfigurationId, credentialIdentifier) -> {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialConfigurationId(credentialConfigurationId);
            return credentialRequest;
        };

        testCompleteFlowWithClaimsValidationAuthorizationCode(credRequestSupplier);
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
        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Request the actual credential using the refreshed token
        Oid4vcCredentialRequest credentialRequest = oauth.oid4vc()
                .credentialRequest()
                .endpoint(ctx.credentialIssuer.getCredentialEndpoint())
                .bearerToken(accessToken);
        if (credentialIdentifier != null) {
            credentialRequest.credentialIdentifier(credentialIdentifier);
        }
        Oid4vcCredentialResponse credentialResponse = credentialRequest.send();
        assertSuccessfulCredentialResponse(credentialResponse);
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

        // 1 - Update user to have missing "lastName" (mandatory attribute)
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "john");
        UserRepresentation userRep = user.toRepresentation();
        // NOTE: Need to call both "setLastName" and set attributes to be able to set last name as null
        userRep.setAttributes(Collections.emptyMap());
        userRep.setLastName(null);
        user.update(userRep);

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
        events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .client(client.getClientId())
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .error(Errors.INVALID_REQUEST)
                .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                .assertEvent();

        // 3 - Update user to add "lastName"
        userRep.setLastName("Doe");
        user.update(userRep);

        // 4 - Test the credential-request again. Should be OK now
        credentialResponse = credentialRequest.send();
        assertSuccessfulCredentialResponse(credentialResponse);
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
        protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY, "true");
        clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);

        try {
            // 2 - Update user to have missing "lastName" (mandatory attribute by authorization_details parameter) and "firstName" (mandatory attribute by protocol mapper)
            UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "john");
            UserRepresentation userRep = user.toRepresentation();
            // NOTE: Need to call both "setLastName" and set attributes to be able to set last name as null
            userRep.setAttributes(Collections.emptyMap());
            userRep.setFirstName(null);
            userRep.setLastName(null);
            user.update(userRep);

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
            events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                    .client(client.getClientId())
                    .user(AssertEvents.isUUID())
                    .session(AssertEvents.isSessionId())
                    .error(Errors.INVALID_REQUEST)
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();

            // 3 - Update user to add "lastName", but keep "firstName" missing. Credential request should still fail
            userRep.setLastName("Doe");
            userRep.setFirstName(null);
            user.update(userRep);

            // Clear events before credential request
            events.clear();

            credentialResponse = credentialRequest.send();
            assertErrorCredentialResponse(credentialResponse);

            // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
            events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                    .client(client.getClientId())
                    .user(AssertEvents.isUUID())
                    .session(AssertEvents.isSessionId())
                    .error(Errors.INVALID_REQUEST)
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();

            // 4 - Update user to add "firstName", but missing "lastName"
            userRep.setLastName(null);
            userRep.setFirstName("John");
            user.update(userRep);

            // Clear events before credential request
            events.clear();

            credentialResponse = credentialRequest.send();
            assertErrorCredentialResponse(credentialResponse);

            // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
            events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                    .client(client.getClientId())
                    .user(AssertEvents.isUUID())
                    .session(AssertEvents.isSessionId())
                    .error(Errors.INVALID_REQUEST)
                    .detail(Details.REASON, Matchers.containsString("The requested claims are not available in the user profile"))
                    .assertEvent();

            // 5 - Update user to both "firstName" and "lastName". Credential request should be successful
            userRep.setLastName("Doe");
            userRep.setFirstName("John");
            user.update(userRep);

            credentialResponse = credentialRequest.send();
            assertSuccessfulCredentialResponse(credentialResponse);
        } finally {
            // 6 - Revert protocol mapper config
            protocolMapper.getConfig().put(Oid4vcProtocolMapperModel.MANDATORY, "false");
            clientScopeResource.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);
        }
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
        CredentialRequest credentialRequest = credentialRequestSupplier.apply(credentialConfigurationId, credentialIdentifier);

        Oid4vcCredentialRequest request = oauth.oid4vc()
                .credentialRequest()
                .endpoint(ctx.credentialIssuer.getCredentialEndpoint())
                .bearerToken(tokenResponse.getAccessToken());

        if (credentialRequest.getCredentialConfigurationId() != null) {
            request.credentialConfigurationId(credentialRequest.getCredentialConfigurationId());
        }
        if (credentialRequest.getCredentialIdentifier() != null) {
            request.credentialIdentifier(credentialRequest.getCredentialIdentifier());
        }

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
}
