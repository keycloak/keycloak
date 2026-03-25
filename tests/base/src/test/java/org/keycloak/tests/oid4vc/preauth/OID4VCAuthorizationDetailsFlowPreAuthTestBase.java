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

package org.keycloak.tests.oid4vc.preauth;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.EventMatchers;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.DEFAULT_CODE_LIFESPAN_S;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for authorization details flow tests.
 * Contains common test logic that can be reused by JWT and SD-JWT specific test classes.
 */
public abstract class OID4VCAuthorizationDetailsFlowPreAuthTestBase extends OID4VCIssuerTestBase {

    protected String getBearerToken(OAuthClient oauthClient, ClientRepresentation client, String scopeName) {
        AccessTokenResponse tokenResponse = oauthClient
                .openid(false)
                .scope(scopeName)
                .doPasswordGrantRequest("john", "password");
        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
        return tokenResponse.getAccessToken();
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

        String credentialConfigurationId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        events.clear();

        CredentialOfferUriResponse credentialOfferURIResponse = oauth.oid4vc()
                .credentialOfferUriRequest(credentialConfigurationId)
                .preAuthorized(true)
                .bearerToken(token)
                .targetUser("john")
                .send();
        assertEquals(HttpStatus.SC_OK, credentialOfferURIResponse.getStatusCode());
        CredentialOfferURI credOfferUri = credentialOfferURIResponse.getCredentialOfferURI();

        EventRepresentation offerUriEvent = events.poll();
        EventAssertion.assertSuccess(offerUriEvent)
                .clientId(OID4VCI_CLIENT_ID)
                .type(EventType.VERIFIABLE_CREDENTIAL_CREATE_OFFER)
                .hasSessionId()
                .details(Details.USERNAME, "john")
                .details(Details.CREDENTIAL_TYPE, credentialConfigurationId);

        events.clear();

        CredentialOfferResponse credentialOfferResponse = oauth.oid4vc().doCredentialOfferRequest(credOfferUri);
        assertEquals(HttpStatus.SC_OK, credentialOfferResponse.getStatusCode());
        ctx.credentialsOffer = credentialOfferResponse.getCredentialsOffer();

        EventRepresentation unauthenticatedOfferEvent = events.poll();
        EventAssertion.assertSuccess(unauthenticatedOfferEvent)
                .clientId(OID4VCI_CLIENT_ID)
                .type(EventType.VERIFIABLE_CREDENTIAL_OFFER_REQUEST)
                .details(Details.CREDENTIAL_TYPE, credentialConfigurationId);

        events.clear();

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
    public void testPreAuthorizedCodeWithAuthorizationDetailsCredentialConfigurationId() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        OID4VCAuthorizationDetail authDetail = new OID4VCAuthorizationDetail();
        authDetail.setType(OPENID_CREDENTIAL);
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        // Assert no session referenced in the access token and token response.
        AccessToken parsedToken = oauth.verifyToken(tokenResponse.getAccessToken(), AccessToken.class);
        assertNull(parsedToken.getSessionId());
        assertNull(tokenResponse.getSessionState());
        // Assert scope in the token matches with the credential requested
        assertEquals(parsedToken.getScope(), getCredentialClientScope().getName());
        assertEquals(tokenResponse.getScope(), getCredentialClientScope().getName());

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertEquals(1, authDetailsResponse.size());
        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        assertEquals(OPENID_CREDENTIAL, authDetailResponse.getType());
        assertEquals(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID), authDetailResponse.getCredentialConfigurationId());
        assertNotNull(authDetailResponse.getCredentialIdentifiers());
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());
        String firstIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull(firstIdentifier, "Identifier should not be null");
        assertFalse(firstIdentifier.isEmpty(), "Identifier should not be empty");
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
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertEquals(1, authDetailsResponse.size());
        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        assertEquals(OPENID_CREDENTIAL, authDetailResponse.getType());
        assertEquals(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID), authDetailResponse.getCredentialConfigurationId());
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
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        // Should fail because the claim is not supported by the credential configuration
        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertTrue((tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Error when processing authorization_details")) ||
                        (tokenResponse.getError() != null && tokenResponse.getError().contains("Error when processing authorization_details")),
                "Error message should indicate authorization_details processing error");
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
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        // Should fail because the mandatory claim is not supported
        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertTrue((tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Error when processing authorization_details")) ||
                        (tokenResponse.getError() != null && tokenResponse.getError().contains("Error when processing authorization_details")),
                "Error message should indicate authorization_details processing error");
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
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        // Should fail if the complex path is not supported
        int statusCode = tokenResponse.getStatusCode();
        if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            assertTrue((tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Error when processing authorization_details")) ||
                            (tokenResponse.getError() != null && tokenResponse.getError().contains("Error when processing authorization_details")),
                    "Error message should indicate authorization_details processing error");
        } else {
            // If it succeeds, verify the response structure
            assertEquals(HttpStatus.SC_OK, statusCode);
            List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
            assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
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
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertEquals("invalid_authorization_details", tokenResponse.getError());
        assertTrue(tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Invalid authorization_details")
                        && tokenResponse.getErrorDescription().contains("credential_configuration_id is required"),
                "Error description should indicate missing credential_configuration_id. Actual: " + tokenResponse.getErrorDescription());
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
        authDetail.setCredentialConfigurationId(getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID));
        authDetail.setClaims(Arrays.asList(claim));
        authDetail.setLocations(Collections.singletonList(ctx.credentialIssuer.getCredentialIssuer()));

        List<OID4VCAuthorizationDetail> authDetails = List.of(authDetail);

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertEquals("invalid_authorization_details", tokenResponse.getError());
        assertTrue(tokenResponse.getErrorDescription() != null && tokenResponse.getErrorDescription().contains("Invalid authorization_details")
                        && tokenResponse.getErrorDescription().contains("path is required"),
                "Error description should indicate invalid claims path. Actual: " + tokenResponse.getErrorDescription());
    }

    @Test
    public void testPreAuthorizedCodeWithEmptyAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Send empty authorization_details array - should fail
        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(List.of())
                .send();

        assertEquals(HttpStatus.SC_BAD_REQUEST, tokenResponse.getStatusCode());
        assertEquals("invalid_authorization_details", tokenResponse.getError());
        assertNotNull(tokenResponse.getErrorDescription(), "Error description should be present");
    }

    @Test
    public void testPreAuthorizedCodeWithCredentialOfferBasedAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Test Pre-Authorized Code Flow without authorization_details parameter
        // The system should generate authorization_details based on credential_configuration_ids from the credential offer

        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertEquals(ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size(),
                "Should have authorization_details for each credential configuration in the offer");

        // Verify each credential configuration from the offer has corresponding authorization_details
        for (int i = 0; i < ctx.credentialsOffer.getCredentialConfigurationIds().size(); i++) {
            String expectedConfigId = ctx.credentialsOffer.getCredentialConfigurationIds().get(i);
            OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(i);

            assertEquals(OPENID_CREDENTIAL, authDetailResponse.getType());
            assertEquals(expectedConfigId, authDetailResponse.getCredentialConfigurationId(),
                    "Credential configuration ID should match the one from the offer");
            assertNotNull(authDetailResponse.getCredentialIdentifiers(), "Credential identifiers should be present");
            assertEquals(1, authDetailResponse.getCredentialIdentifiers().size(), "Should have exactly one credential identifier");

            String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
            assertNotNull(credentialIdentifier, "Identifier should not be null");
            assertFalse(credentialIdentifier.isEmpty(), "Identifier should not be empty");
        }
    }

    @Test
    public void testPreAuthorizedFlowWithCredentialOfferBasedAuthorizationDetails() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());

        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Step 1: Request token without authorization_details parameter (no scope needed)
        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        String credentialIdentifier;
        String credentialConfigurationId;
        OID4VCAuthorizationDetail authDetailResponse;

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertEquals(ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size(),
                "Should have authorization_details for each credential configuration in the offer");

        // Use the first authorization detail for credential request
        authDetailResponse = authDetailsResponse.get(0);
        assertNotNull(authDetailResponse.getCredentialIdentifiers(), "Credential identifiers should be present");
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should not be null");

        credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull(credentialConfigurationId, "Credential configuration id should not be null");


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
            events.clear();

            Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                    .credentialIdentifier(credentialIdentifier)
                    .bearerToken(tokenResponse.getAccessToken())
                    .send();

            assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode());

            EventRepresentation credentialEvent = events.poll();
            EventAssertion.assertSuccess(credentialEvent)
                    .clientId(OID4VCI_CLIENT_ID)
                    .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST)
                    .details(Details.USERNAME, "john")
                    .details(Details.CREDENTIAL_TYPE, credentialConfigurationId);

            // Parse the credential response
            CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
            assertNotNull(parsedResponse, "Credential response should not be null");
            assertNotNull(parsedResponse.getCredentials(), "Credentials should be present");
            assertEquals(1, parsedResponse.getCredentials().size(), "Should have exactly one credential");

            // Step 3: Verify that the issued credential structure is valid
            CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
            assertNotNull(credentialWrapper, "Credential wrapper should not be null");

            // The credential is stored as Object, so we need to cast it
            Object credentialObj = credentialWrapper.getCredential();
            assertNotNull(credentialObj, "Credential object should not be null");

            // Verify the credential structure based on format
            verifyCredentialStructure(credentialObj);
        }
    }

    @Test
    public void testPreAuthorizedCodeTokenEndpointRestriction() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Step 1: Get pre-authorized code token
        AccessTokenResponse accessTokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String preAuthorizedToken = accessTokenResponse.getAccessToken();
        assertNotNull(preAuthorizedToken, "Access token should be present");

        List<OID4VCAuthorizationDetail> authDetailsResponse = accessTokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present");
        assertFalse(authDetailsResponse.isEmpty(), "authorization_details should not be empty");

        String credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should be present");

        // Step 2: Verify token works at credential endpoint (should succeed)
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(preAuthorizedToken)
                .send();

        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode(),
                "Pre-authorized code token should work at credential endpoint");

        // Step 3: Verify token is rejected at Account REST API endpoint (uses BearerTokenAuthenticator)
        // Account endpoint uses BearerTokenAuthenticator which enforces the restriction
        // Use versioned path to get REST API (not HTML UI)
        String accountEndpoint = keycloakUrls.getBase() + "/realms/" + oauth.getRealm() + "/account/v1";

        HttpGet getAccount = new HttpGet(accountEndpoint);
        getAccount.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + preAuthorizedToken);
        getAccount.addHeader(HttpHeaders.ACCEPT, "application/json");

        try (CloseableHttpResponse accountResponse = oauth.httpClient().get().execute(getAccount)) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, accountResponse.getStatusLine().getStatusCode(),
                    "Pre-authorized code token should be rejected at account endpoint");
        }

        // Step 4: Verify token is rejected at Admin REST API endpoint (uses BearerTokenAuthenticator)
        // Admin endpoint uses BearerTokenAuthenticator which enforces the restriction
        String adminEndpoint = keycloakUrls.getAdmin() + "/realms/" + oauth.getRealm();
        HttpGet getAdmin = new HttpGet(adminEndpoint);
        getAdmin.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + preAuthorizedToken);

        try (CloseableHttpResponse adminResponse = oauth.httpClient().get().execute(getAdmin)) {
            assertEquals(HttpStatus.SC_UNAUTHORIZED, adminResponse.getStatusLine().getStatusCode(),
                    "Pre-authorized code token should be rejected at admin endpoint");
        }
    }

    @Test
    public void testStandardTokenAllowedAtEndpoint() throws Exception {
        // Verify that a standard OIDC token (password grant, openid scope only) is accepted at a standard
        // OIDC endpoint (userinfo). This ensures the fail-closed logic does not block normal Keycloak flows.
        // We use userinfo instead of the account REST API to avoid dependency on account client/API setup.

        AccessTokenResponse response = oauth.client(OID4VCI_CLIENT_ID, "test-secret")
                .scope(OAuth2Constants.SCOPE_OPENID)
                .doPasswordGrantRequest("john", "password");
        String accessToken = response.getAccessToken();

        UserInfoResponse userInfoResponse = oauth.doUserInfoRequest(accessToken);
        assertEquals(HttpStatus.SC_OK, userInfoResponse.getStatusCode(),
                "Standard token should be allowed at userinfo endpoint");
    }

    @Test
    public void testMultipleCredentialConfigurationsFromOffer() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        Oid4vcTestContext ctx = prepareOid4vcTestContext(token);

        // Verify that the credential offer has multiple configurations (if supported by the test setup)
        assertTrue(ctx.credentialsOffer.getCredentialConfigurationIds() != null &&
                        !ctx.credentialsOffer.getCredentialConfigurationIds().isEmpty(),
                "Credential offer should have at least one credential configuration");

        // Step 1: Request token without authorization_details parameter
        AccessTokenResponse tokenResponse = oauth.oid4vc()
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .send();

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());

        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");

        // Verify that we have authorization_details for each credential configuration in the offer
        assertEquals(ctx.credentialsOffer.getCredentialConfigurationIds().size(), authDetailsResponse.size(),
                "Should have authorization_details for each credential configuration in the offer");

        // Verify each authorization detail
        for (int i = 0; i < authDetailsResponse.size(); i++) {
            OID4VCAuthorizationDetail authDetail = authDetailsResponse.get(i);
            String expectedConfigId = ctx.credentialsOffer.getCredentialConfigurationIds().get(i);

            // Verify structure
            assertEquals(OPENID_CREDENTIAL, authDetail.getType(), "Type should be openid_credential");
            assertEquals(expectedConfigId, authDetail.getCredentialConfigurationId(),
                    "Credential configuration ID should match the one from the offer");
            assertNotNull(authDetail.getCredentialIdentifiers(), "Credential identifiers should be present");
            assertEquals(1, authDetail.getCredentialIdentifiers().size(),
                    "Should have exactly one credential identifier per configuration");

            // Verify identifier format
            String credentialIdentifier = authDetail.getCredentialIdentifiers().get(0);
            assertNotNull(credentialIdentifier, "Credential identifier should not be null");
            assertFalse(credentialIdentifier.isEmpty(), "Credential identifier should not be empty");
        }

        // Verify that all credential identifiers are unique
        Set<String> allIdentifiers = authDetailsResponse.stream()
                .flatMap(auth -> auth.getCredentialIdentifiers().stream())
                .collect(Collectors.toSet());
        assertEquals(authDetailsResponse.size(), allIdentifiers.size(),
                "All credential identifiers should be unique");
    }

    @Test
    public void testCompleteFlowWithClaimsValidation() throws Exception {
        AccessTokenResponse tokenResponse = preAuthzCodeSuccessful();
        assertSuccessfulCredentialRequest(tokenResponse);
    }

    @Test
    public void testCompleteFlowWithExpiredCredentialOffer() throws Exception {
        AccessTokenResponse tokenResponse = preAuthzCodeSuccessful();
        // Make sure that offer is expired
        timeOffSet.set(DEFAULT_CODE_LIFESPAN_S + 10);
        assertFailedCredentialRequest(tokenResponse);
    }

    @Test
    public void testCompleteFlowWithConsumedCredentialOffer() throws Exception {
        AccessTokenResponse tokenResponse = preAuthzCodeSuccessful();
        assertSuccessfulCredentialRequest(tokenResponse);
        // Second credential request should fail as offer is already expired
        assertFailedCredentialRequest(tokenResponse);
    }

    private AccessTokenResponse preAuthzCodeSuccessful() throws Exception {
        String token = getBearerToken(oauth, client, getCredentialClientScope().getName());
        String credConfigId = getCredentialClientScope().getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);
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
                .preAuthorizedCodeGrantRequest(ctx.credentialsOffer.getPreAuthorizedCode())
                .endpoint(ctx.openidConfig.getTokenEndpoint())
                .authorizationDetails(authDetails)
                .send();

        String credentialIdentifier;
        String credentialConfigurationId;
        OID4VCAuthorizationDetail authDetailResponse;

        assertEquals(HttpStatus.SC_OK, tokenResponse.getStatusCode());
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        assertNotNull(authDetailsResponse, "authorization_details should be present in the response");
        assertEquals(1, authDetailsResponse.size());

        authDetailResponse = authDetailsResponse.get(0);
        assertNotNull(authDetailResponse.getCredentialIdentifiers(), "Credential identifiers should be present");
        assertEquals(1, authDetailResponse.getCredentialIdentifiers().size());

        credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        assertNotNull(credentialIdentifier, "Credential identifier should not be null");

        credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        assertNotNull(credentialConfigurationId, "Credential configuration id should not be null");

        // Step 2: Request the actual credential using the identifier and config id
        events.clear();
        return tokenResponse;
    }

    private void assertSuccessfulCredentialRequest(AccessTokenResponse tokenResponse) {
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        String credentialConfigurationId = authDetailResponse.getCredentialConfigurationId();
        String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);
        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(tokenResponse.getAccessToken())
                .send();

        assertEquals(HttpStatus.SC_OK, credentialResponse.getStatusCode());

        // Verify CREDENTIAL_REQUEST event was fired
        EventRepresentation claimsCredentialEvent = events.poll();
        EventAssertion.assertSuccess(claimsCredentialEvent)
                .clientId(OID4VCI_CLIENT_ID)
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST)
                .hasSessionId()
                .details(Details.USERNAME, "john")
                .details(Details.CREDENTIAL_TYPE, credentialConfigurationId);

        // Parse the credential response
        CredentialResponse parsedResponse = credentialResponse.getCredentialResponse();
        assertNotNull(parsedResponse, "Credential response should not be null");
        assertNotNull(parsedResponse.getCredentials(), "Credentials should be present");
        assertEquals(1, parsedResponse.getCredentials().size(), "Should have exactly one credential");

        // Step 3: Verify that the issued credential contains the requested claims AND may contain additional claims
        CredentialResponse.Credential credentialWrapper = parsedResponse.getCredentials().get(0);
        assertNotNull(credentialWrapper, "Credential wrapper should not be null");

        // The credential is stored as Object, so we need to cast it
        Object credentialObj = credentialWrapper.getCredential();
        assertNotNull(credentialObj, "Credential object should not be null");

        // Verify the credential structure based on format
        verifyCredentialStructure(credentialObj);
    }

    private void assertFailedCredentialRequest(AccessTokenResponse tokenResponse) {
        List<OID4VCAuthorizationDetail> authDetailsResponse = tokenResponse.getOID4VCAuthorizationDetails();
        OID4VCAuthorizationDetail authDetailResponse = authDetailsResponse.get(0);
        String credentialIdentifier = authDetailResponse.getCredentialIdentifiers().get(0);

        Oid4vcCredentialResponse credentialResponse = oauth.oid4vc().credentialRequest()
                .credentialIdentifier(credentialIdentifier)
                .bearerToken(tokenResponse.getAccessToken())
                .send();
        assertEquals(HttpStatus.SC_BAD_REQUEST, credentialResponse.getStatusCode());

        // Verify VERIFIABLE_CREDENTIAL_REQUEST_ERROR event was fired
        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .clientId(OID4VCI_CLIENT_ID)
                .error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue())
                .hasSessionId();
        MatcherAssert.assertThat(errorEvent.getUserId(), EventMatchers.isUUID());
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

        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR)
                .clientId(null)
                .userId(null)
                .sessionId(null)
                .error(ErrorType.INVALID_CREDENTIAL_REQUEST.getValue())
                .details(Details.REASON, "Request payload is null or empty.");

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

        EventRepresentation invalidIdEvent = events.poll();
        EventAssertion.assertError(invalidIdEvent)
                .clientId(OID4VCI_CLIENT_ID)
                .type(EventType.VERIFIABLE_CREDENTIAL_REQUEST_ERROR);
        assertTrue(
                ErrorType.UNKNOWN_CREDENTIAL_IDENTIFIER.getValue().equals(invalidIdEvent.getError())
                        || ErrorType.UNKNOWN_CREDENTIAL_CONFIGURATION.getValue().equals(invalidIdEvent.getError()),
                "Expected unknown_credential_identifier or unknown_credential_configuration, got: " + invalidIdEvent.getError());

    }

    /**
     * Verify the credential structure based on the format.
     * Subclasses can override this to provide format-specific verification.
     */
    protected void verifyCredentialStructure(Object credentialObj) {
        // Default implementation - subclasses should override
        assertNotNull(credentialObj, "Credential object should not be null");
    }

}
