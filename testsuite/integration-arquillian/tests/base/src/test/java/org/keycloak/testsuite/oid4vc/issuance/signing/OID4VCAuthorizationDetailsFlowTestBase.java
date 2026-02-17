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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCode;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Base class for authorization details flow tests.
 * Contains common test logic that can be reused by JWT and SD-JWT specific test classes.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public abstract class OID4VCAuthorizationDetailsFlowTestBase extends OID4VCIssuerEndpointTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void enableDirectAccessGrants() {
        // Enable direct access grants for test-app client to allow password grant
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
    }

    protected static class Oid4vcTestContext {
        CredentialsOffer credentialsOffer;
        CredentialIssuer credentialIssuer;
        OIDCConfigurationRepresentation openidConfig;
    }

    /**
     * Get the credential format for this test implementation.
     *
     * @return the credential format (e.g., "jwt_vc", "sd_jwt_vc")
     */
    protected abstract String getCredentialFormat();

    /**
     * Get the credential client scope for this test implementation.
     *
     * @return the client scope model
     */
    protected abstract ClientScopeRepresentation getCredentialClientScope();

    /**
     * Get the expected claim path for this test implementation.
     *
     * @return the claim path as a string
     */
    protected abstract String getExpectedClaimPath();

    protected Oid4vcTestContext prepareOid4vcTestContext(String token) throws Exception {
        Oid4vcTestContext ctx = new Oid4vcTestContext();

        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Clear events before credential offer URI request
        events.clear();

        CredentialOfferUriResponse credentialOfferURIResponse = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .bearerToken(token)
                .username("john")
                .send();
        assertEquals(HttpStatus.SC_OK, credentialOfferURIResponse.getStatusCode());
        CredentialOfferURI credentialOfferURI = credentialOfferURIResponse.getCredentialOfferURI();

        // Verify CREDENTIAL_OFFER_REQUEST event was fired
        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST)
                .client(client.getClientId())
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .detail(Details.USERNAME, "john")
                .detail(Details.CREDENTIAL_TYPE, credentialConfigurationId)
                .assertEvent();

        // Clear events before credential offer request
        events.clear();

        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc()
                .credentialOfferRequest(credentialOfferURI)
                .send();
        assertEquals(HttpStatus.SC_OK, credentialOfferResponse.getStatusCode());
        ctx.credentialsOffer = credentialOfferResponse.getCredentialsOffer();

        // Verify CREDENTIAL_OFFER_REQUEST event was fired (unauthenticated endpoint)
        events.expect(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST)
                .client(client.getClientId())
                .user(AssertEvents.isUUID())
                .session((String) null)
                .detail(Details.CREDENTIAL_TYPE, credentialConfigurationId)
                .assertEvent();

        CredentialIssuerMetadataResponse issuerMetadataResponse = oauth.oid4vc().issuerMetadataRequest()
                .endpoint(ctx.credentialsOffer.getIssuerMetadataUrl())
                .send();
        assertEquals(HttpStatus.SC_OK, issuerMetadataResponse.getStatusCode());
        ctx.credentialIssuer = issuerMetadataResponse.getMetadata();

        OpenIDProviderConfigurationResponse openIDProviderConfigurationResponse = oauth.wellknownRequest()
                .url(ctx.credentialIssuer.getAuthorizationServers().get(0))
                .send();
        assertEquals(HttpStatus.SC_OK, openIDProviderConfigurationResponse.getStatusCode());
        ctx.openidConfig = openIDProviderConfigurationResponse.getOidcConfiguration();

        return ctx;
    }

    @Test
    public void testAuthorizationCodeFlowWithAuthorizationDetails() throws Exception {

        Oid4vcTestContext ctx = new Oid4vcTestContext();

        // Get issuer metadata
        CredentialIssuerMetadataResponse issuerMetadataResponse = oauth.oid4vc().issuerMetadataRequest().send();
        assertEquals(HttpStatus.SC_OK, issuerMetadataResponse.getStatusCode());
        ctx.credentialIssuer = issuerMetadataResponse.getMetadata();

        // Get credential_configuration_id
        ClientScopeRepresentation credClientScope = getCredentialClientScope();
        String credConfigId = credClientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        // Build authorization_details
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(List.of(ctx.credentialIssuer.getCredentialIssuer()));

        String authDetailsJson = JsonSerialization.valueAsString(List.of(authDetail));
        String authDetailsEncoded = URLEncoder.encode(authDetailsJson, Charset.defaultCharset());

        // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
        AuthorizationEndpointResponse authEndpointResponse = oauth.loginForm()
                .scope(credClientScope.getName())
                .param("authorization_details", authDetailsEncoded)
                .doLogin("john","password");

        String authCode = authEndpointResponse.getCode();
        assertNotNull("No authorization code", authCode);

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        String accessToken = tokenResponse.getAccessToken();

        String credentialIdentifier;
        String credentialConfigurationId;
        OID4VCAuthorizationDetail authDetailResponse;

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals("Should have authorization_details for each credential configuration in the offer",
                1, authDetailsResponse.size());

        // Use the first authorization detail for credential request
        authDetailResponse = authDetailsResponse.get(0);
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should not be null", credentialIdentifier);

        credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull("Credential configuration id should not be null", credentialConfigurationId);

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(accessToken)
                .send();

        // Parse the credential response
        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
        assertNotNull("Credential response should not be null", parsedResponse);
        assertNotNull("Credentials should be present", parsedResponse.getCredentials());
        assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

        // Step 3: Verify that the issued credential structure is valid
        CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
        assertNotNull("Credential wrapper should not be null", credentialWrapper);

        // The credential is stored as Object, so we need to cast it
        Object credentialObj = credentialWrapper.getCredential();
        assertNotNull("Credential object should not be null", credentialObj);

        // Verify the credential structure based on format
        verifyCredentialStructure(credentialObj);
    }

    @Test
    public void testAuthorizationCodeFlowWithCredentialIdentifier() throws Exception {

        Oid4vcTestContext ctx = new Oid4vcTestContext();

        // Get issuer metadata
        CredentialIssuerMetadataResponse issuerMetadataResponse = oauth.oid4vc().issuerMetadataRequest().send();
        assertEquals(HttpStatus.SC_OK, issuerMetadataResponse.getStatusCode());
        ctx.credentialIssuer = issuerMetadataResponse.getMetadata();

        // Get credential_configuration_id
        ClientScopeRepresentation credClientScope = getCredentialClientScope();

        // Build authorization_details
        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialIdentifiers(List.of("credential_identifiers_not_allowed_here"));
        authDetail.setLocations(List.of(ctx.credentialIssuer.getCredentialIssuer()));

        String authDetailsJson = JsonSerialization.valueAsString(List.of(authDetail));
        String authDetailsEncoded = URLEncoder.encode(authDetailsJson, Charset.defaultCharset());

        // [TODO #44320] Requires Credential scope in AuthorizationRequest although already given in AuthorizationDetails
        AuthorizationEndpointResponse authEndpointResponse = oauth.loginForm()
                .scope(credClientScope.getName())
                .param("authorization_details", authDetailsEncoded)
                .doLogin("john","password");

        String authCode = authEndpointResponse.getCode();
        assertNotNull("No authorization code", authCode);

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authCode)
                .authorizationDetails(List.of(authDetail))
                .send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertTrue(tokenResponse.getErrorDescription().contains("credential_identifiers not allowed"));
    }

    @Test
    public void testPreAuthorizedCodeWithAuthorizationDetailsCredentialConfigurationId() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals(1, authDetailsResponse.size());
        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        assertEquals(OPENID_CREDENTIAL, authDetailResponse.getType());
        assertEquals(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID), authDetailResponse.getCredentialConfigurationId());
        assertNotNull(authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());
        String firstIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull("Identifier should not be null", firstIdentifier);
        assertFalse("Identifier should not be empty", firstIdentifier.isEmpty());
        try {
            UUID.fromString(firstIdentifier);
        } catch (IllegalArgumentException e) {
            fail("Identifier should be a valid UUID, but was: " + firstIdentifier);
        }
    }

    @Test
    public void testPreAuthorizedCodeWithAuthorizationDetailsAndClaims() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description for a claim that should be supported
        ClaimsDescription claim = new ClaimsDescription();

        // Construct claim path based on credential format
        List<Object> claimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            // SD-JWT doesn't use credentialSubject prefix
            claimPath = Arrays.asList(getExpectedClaimPath());
        } else {
            // JWT and other formats use credentialSubject prefix
            claimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        claim.setPath(claimPath);
        claim.setMandatory(true);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals(1, authDetailsResponse.size());
        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        assertEquals(OPENID_CREDENTIAL, authDetailResponse.getType());
        assertEquals(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID), authDetailResponse.getCredentialConfigurationId());
        assertNotNull(authDetailResponse.getClaims());
        assertEquals(1, authDetailResponse.getClaims().size());
        ClaimsDescription responseClaim = authDetailResponse.getClaims().get(0);

        List<Object> expectedClaimPath;
        if ("sd_jwt_vc".equals(getCredentialFormat())) {
            expectedClaimPath = Arrays.asList(getExpectedClaimPath());
        } else {
            expectedClaimPath = Arrays.asList("credentialSubject", getExpectedClaimPath());
        }
        assertEquals(expectedClaimPath, responseClaim.getPath());
        assertTrue(responseClaim.isMandatory());

        // Verify that credential identifiers are present
        assertNotNull(authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

    }

    @Test
    public void testPreAuthorizedCodeWithUnsupportedClaims() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description for a claim that should NOT be supported
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "unsupportedClaim"));
        claim.setMandatory(false);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        // Should fail because the claim is not supported by the credential configuration
        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertTrue("Error message should indicate authorization_details processing error",
                (tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Error when processing authorization_details")) ||
                        (tokenResponse.getError() != null && tokenResponse.getError().contains("Error when processing authorization_details")));
    }

    @Test
    public void testPreAuthorizedCodeWithMandatoryClaimMissing() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description for a mandatory claim
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "mandatoryClaim"));
        claim.setMandatory(true);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        // Should fail because the mandatory claim is not supported
        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertTrue("Error message should indicate authorization_details processing error",
                (tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Error when processing authorization_details")) ||
                        (tokenResponse.getError() != null && tokenResponse.getError().contains("Error when processing authorization_details")));
    }

    @Test
    public void testPreAuthorizedCodeWithComplexClaimsPath() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description with complex path
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(Arrays.asList("credentialSubject", "address", "street"));
        claim.setMandatory(false);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        // Should fail if the complex path is not supported
        int statusCode = tokenResponse.getStatusCode();
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            assertTrue("Error message should indicate authorization_details processing error",
                    (tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Error when processing authorization_details")) ||
                            (tokenResponse.getError() != null && tokenResponse.getError().contains("Error when processing authorization_details")));
        } else {
            // If it succeeds, verify the response structure
            assertEquals(HttpStatus.SC_OK, statusCode);
            List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
            assertNotNull("authorization_details should be present in the response", authDetailsResponse);
            assertEquals(1, authDetailsResponse.size());
        }
    }

    @Test
    public void testPreAuthorizedCodeWithInvalidAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        // Missing credential_configuration_id - should fail
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertEquals("invalid_authorization_details", tokenResponse.getError());
        assertTrue("Error description should indicate missing credential_configuration_id. Actual: " + tokenResponse.getErrorDescription(),
                tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Invalid authorization_details")
                        && tokenResponse.getErrorDescription().contains("credential_configuration_id is required"));
    }

    @Test
    public void testPreAuthorizedCodeWithInvalidClaims() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Create claims description with invalid path
        ClaimsDescription claim = new ClaimsDescription();
        claim.setPath(null); // Invalid: null path
        claim.setMandatory(false);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertEquals("invalid_authorization_details", tokenResponse.getError());
        assertTrue("Error description should indicate invalid claims path. Actual: " + tokenResponse.getErrorDescription(),
                tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Invalid authorization_details")
                        && tokenResponse.getErrorDescription().contains("path is required"));
    }

    @Test
    public void testPreAuthorizedCodeWithEmptyAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Send empty authorization_details array - should fail
        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(List.of())
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertEquals("invalid_request", tokenResponse.getError());
        assertNotNull("Error description should be present", tokenResponse.getErrorDescription());
    }

    @Test
    public void testPreAuthorizedCodeWithCredentialOfferBasedAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Test Pre-Authorized Code Flow without authorization_details parameter
        // The system should generate authorization_details based on credential_configuration_ids from the credential offer

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals("Should have authorization_details for each credential configuration in the offer",
                ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size());

        // Verify each credential configuration from the offer has corresponding authorization_details
        for (int i = 0; i < ctx.credentialsOffer.getCredentialConfigurationIds().size(); i++) {
            String expectedConfigId = ctx.credentialsOffer.getCredentialConfigurationIds().get(i);
            OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(i);

            assertEquals(OPENID_CREDENTIAL, authDetailResponse.getType());
            assertEquals("Credential configuration ID should match the one from the offer",
                    expectedConfigId, authDetailResponse.getCredentialConfigurationId());
            assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
            assertEquals("Should have exactly one credential identifier", 1, authDetailResponse.getCredentialIdentifiers().size());

            String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
            assertNotNull("Identifier should not be null", credentialIdentifier);
            assertFalse("Identifier should not be empty", credentialIdentifier.isEmpty());
            try {
                UUID.fromString(credentialIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Identifier should be a valid UUID, but was: " + credentialIdentifier);
            }
        }
    }

    @Test
    public void testPreAuthorizedFlowWithCredentialOfferBasedAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());

        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);
        PreAuthorizedCode preAuthorizedCode = ctx.credentialsOffer.getGrants().getPreAuthorizedCode();

        // Step 1: Request token without authorization_details parameter (no scope needed)
        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(preAuthorizedCode.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        String credentialIdentifier;
        String credentialConfigurationId;
        OID4VCAuthorizationDetail authDetailResponse;

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals("Should have authorization_details for each credential configuration in the offer",
                ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size());

        // Use the first authorization detail for credential request
        authDetailResponse = authDetailsResponse.get(0);
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should not be null", credentialIdentifier);

        credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull("Credential configuration id should not be null", credentialConfigurationId);


        // Step 2: Request the actual credential using ONLY the identifier (no credential_configuration_id)
        // This tests that the mapping from credential identifier to credential configuration ID works as expected.
        //
        // The Pre-Authorized code flow is treated as a separate authentication event.
        // Even if the underlying user and client match an existing session.
        // A new user session is created because:
        //      * The pre-auth code is defined as a standalone authentication mechanism.
        //      * It does not assume the caller already has an authenticated session.
        //      * It must guarantee isolation of state tied to the VC issuance flow.
        {
            // Clear events before credential request
            events.clear();

            Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credentialIdentifier)
                    .bearerToken(tokenResponse.getAccessToken())
                    .send();

            assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode());

            // Verify CREDENTIAL_REQUEST event was fired
            events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST)
                    .client(client.getClientId())
                    .user(AssertEvents.isUUID())
                    .session(AssertEvents.isSessionId())
                    .detail(Details.USERNAME, "john")
                    .detail(Details.CREDENTIAL_TYPE, credentialConfigurationId)
                    .assertEvent();

            // Parse the credential response
            CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
            assertNotNull("Credential response should not be null", parsedResponse);
            assertNotNull("Credentials should be present", parsedResponse.getCredentials());
            assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

            // Step 3: Verify that the issued credential structure is valid
            CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
            assertNotNull("Credential wrapper should not be null", credentialWrapper);

            // The credential is stored as Object, so we need to cast it
            Object credentialObj = credentialWrapper.getCredential();
            assertNotNull("Credential object should not be null", credentialObj);

            // Verify the credential structure based on format
            verifyCredentialStructure(credentialObj);
        }
    }

    @Test
    public void testPreAuthorizedCodeTokenEndpointRestriction() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);
        PreAuthorizedCode preAuthorizedCode = ctx.credentialsOffer.getGrants().getPreAuthorizedCode();

        // Step 1: Get pre-authorized code token
        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(preAuthorizedCode.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String preAuthorizedToken = accessTokenResponse.getAccessToken();
        assertNotNull("Access token should be present", preAuthorizedToken);

        List<OID4VCAuthorizationDetail> authDetailsResponse = accessTokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present", authDetailsResponse);
        assertFalse("authorization_details should not be empty", authDetailsResponse.isEmpty());

        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should be present", credentialIdentifier);

        // Step 2: Verify token works at credential endpoint (should succeed)
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(preAuthorizedToken)
                .send();

        assertEquals("Pre-authorized code token should work at credential endpoint",
                HttpStatus.SC_OK, credentialResponse.getStatusCode());

        // Step 3: Verify token is rejected at Account REST API endpoint (uses BearerTokenAuthenticator)
        // Account endpoint uses BearerTokenAuthenticator which enforces the restriction
        // Use versioned path to get REST API (not HTML UI)
        String accountEndpoint = OAuthClient.AUTH_SERVER_ROOT + "/realms/" + oauth.getRealm() + "/account/v1";

        HttpGet getAccount = new HttpGet(accountEndpoint);
        getAccount.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + preAuthorizedToken);
        getAccount.addHeader(HttpHeaders.ACCEPT, "application/json");

        try (CloseableHttpResponse accountResponse = httpClient.execute(getAccount)) {
            assertEquals("Pre-authorized code token should be rejected at account endpoint",
                    HttpStatus.SC_UNAUTHORIZED, accountResponse.getStatusLine().getStatusCode());
        }

        // Step 4: Verify token is rejected at Admin REST API endpoint (uses BearerTokenAuthenticator)
        // Admin endpoint uses BearerTokenAuthenticator which enforces the restriction
        String adminEndpoint = oauth.AUTH_SERVER_ROOT + "/admin/realms/" + oauth.getRealm();
        HttpGet getAdmin = new HttpGet(adminEndpoint);
        getAdmin.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + preAuthorizedToken);

        try (CloseableHttpResponse adminResponse = httpClient.execute(getAdmin)) {
            assertEquals("Pre-authorized code token should be rejected at admin endpoint",
                    HttpStatus.SC_UNAUTHORIZED, adminResponse.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testStandardTokenAllowedAtEndpoint() throws Exception {
        // Verify that a standard OIDC token (e.g. from password grant)
        // which has an "UNKNOWN" grant type context, is ALLOWED (backward compatibility).
        // This ensures the fail-closed logic doesn't accidentally block standard Keycloak flows.

        // 1. Get standard token
        oauth.realm("test");
        oauth.client("test-app", "password");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        String accessToken = response.getAccessToken();

        // 2. Use at Account API (which would be restricted if it were a pre-authorized token)
        String accountEndpoint = OAuthClient.AUTH_SERVER_ROOT + "/realms/test/account";
        HttpGet getAccount = new HttpGet(accountEndpoint);
        getAccount.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        getAccount.addHeader(HttpHeaders.ACCEPT, "application/json");

        try (CloseableHttpResponse accountResponse = httpClient.execute(getAccount)) {
            // Should be 200 OK because standard tokens are allowed
            assertEquals("Standard token should be allowed at account endpoint",
                    HttpStatus.SC_OK, accountResponse.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testMultipleCredentialConfigurationsFromOffer() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Verify that the credential offer has multiple configurations (if supported by the test setup)
        assertTrue("Credential offer should have at least one credential configuration",
                ctx.credentialsOffer.getCredentialConfigurationIds() != null &&
                        !ctx.credentialsOffer.getCredentialConfigurationIds().isEmpty());

        // Step 1: Request token without authorization_details parameter
        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);

        // Verify that we have authorization_details for each credential configuration in the offer
        assertEquals("Should have authorization_details for each credential configuration in the offer",
                ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size());

        // Verify each authorization detail
        for (int i = 0; i < authDetailsResponse.size(); i++) {
            OID4VCAuthorizationDetail authDetail = authDetailsResponse.get(i);
            String expectedConfigId = ctx.credentialsOffer.getCredentialConfigurationIds().get(i);

            // Verify structure
            assertEquals("Type should be openid_credential", OPENID_CREDENTIAL, authDetail.getType());
            assertEquals("Credential configuration ID should match the one from the offer",
                    expectedConfigId, authDetail.getCredentialConfigurationId());
            assertNotNull("Credential identifiers should be present", authDetail.getCredentialIdentifiers());
            assertEquals("Should have exactly one credential identifier per configuration",
                    1, authDetail.getCredentialIdentifiers().size());

            // Verify identifier format
            String credentialIdentifier = authDetail.getCredentialIdentifiers().get(0);
            assertNotNull("Credential identifier should not be null", credentialIdentifier);
            assertFalse("Credential identifier should not be empty", credentialIdentifier.isEmpty());
            try {
                UUID.fromString(credentialIdentifier);
            } catch (IllegalArgumentException e) {
                fail("Credential identifier should be a valid UUID, but was: " + credentialIdentifier);
            }
        }

        // Verify that all credential identifiers are unique
        Set<String> allIdentifiers = authDetailsResponse.stream()
                .flatMap(auth -> auth.getCredentialIdentifiers().stream())
                .collect(Collectors.toSet());
        assertEquals("All credential identifiers should be unique",
                authDetailsResponse.size(), allIdentifiers.size());
    }

    @Test
    public void testCompleteFlowWithClaimsValidation() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        String credConfigId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Step 1: Request token with authorization details containing specific claims
        // This tests that requested claims are validated and present in the final credential
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

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(credConfigId);
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));
        authDetail.setClaims(List.of(claim));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        String credentialIdentifier;
        String credentialConfigurationId;
        OID4VCAuthorizationDetail authDetailResponse;

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOid4vcAuthorizationDetails();
        assertNotNull("authorization_details should be present in the response", authDetailsResponse);
        assertEquals(1, authDetailsResponse.size());

        authDetailResponse = authDetailsResponse.get(0);
        assertNotNull("Credential identifiers should be present", authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull("Credential identifier should not be null", credentialIdentifier);

        credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull("Credential configuration id should not be null", credentialConfigurationId);

        // Step 2: Request the actual credential using the identifier and config id
        // Clear events before credential request
        events.clear();

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(token)
                .send();

        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode());

        // Verify CREDENTIAL_REQUEST event was fired
        events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST)
                .client(client.getClientId())
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .detail(Details.USERNAME, "john")
                .detail(Details.CREDENTIAL_TYPE, credentialConfigurationId)
                .assertEvent();

        // Parse the credential response
        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
        assertNotNull("Credential response should not be null", parsedResponse);
        assertNotNull("Credentials should be present", parsedResponse.getCredentials());
        assertEquals("Should have exactly one credential", 1, parsedResponse.getCredentials().size());

        // Step 3: Verify that the issued credential contains the requested claims AND may contain additional claims
        CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
        assertNotNull("Credential wrapper should not be null", credentialWrapper);

        // The credential is stored as Object, so we need to cast it
        Object credentialObj = credentialWrapper.getCredential();
        assertNotNull("Credential object should not be null", credentialObj);

        // Verify the credential structure based on format
        verifyCredentialStructure(credentialObj);
    }

    @Test
    public void testCredentialRequestWithEmptyPayload() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        events.clear();

        // Request credential with empty payload
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc()
                .credentialRequest(null)
                .bearerToken(token)
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());

        // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
        // Note: When payload is empty, error is thrown before authentication, so user/session are null
        events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .client((String) null)
                .user((String) null)
                .session((String) null)
                .error(Errors.INVALID_REQUEST)
                .detail(Details.REASON, "Request payload is null or empty.")
                .assertEvent();
    }

    @Test
    public void testCredentialRequestWithInvalidCredentialIdentifier() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        events.clear();

        // Request credential with invalid credential identifier
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier("invalid-credential-identifier")
                .bearerToken(token)
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());

        // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
        events.expect(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .client(client.getClientId())
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isSessionId())
                .error(Errors.INVALID_REQUEST)
                .assertEvent();
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
